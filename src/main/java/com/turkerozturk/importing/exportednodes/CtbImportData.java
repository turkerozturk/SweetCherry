package com.turkerozturk.importing.exportednodes;

import com.turkerozturk.jdbc.JdbcIdentifiers;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/** A bounded, transaction-consistent source snapshot. No source database writes. */
final class CtbImportData {
    static final Map<String, List<String>> COLUMNS = new LinkedHashMap<>();
    static {
        COLUMNS.put("node", List.of("node_id","name","txt","syntax","tags","is_ro","is_richtxt","has_codebox","has_table","has_image","level","ts_creation","ts_lastsave"));
        COLUMNS.put("children", List.of("node_id","father_id","sequence","master_id"));
        COLUMNS.put("codebox", List.of("node_id","offset","justification","txt","syntax","width","height","is_width_pix","do_highl_bra","do_show_linenum"));
        COLUMNS.put("grid", List.of("node_id","offset","justification","txt","col_min","col_max"));
        COLUMNS.put("image", List.of("node_id","offset","justification","anchor","png","filename","link","time"));
        COLUMNS.put("bookmark", List.of("node_id","sequence"));
    }
    final Map<String,List<Object[]>> tables = new LinkedHashMap<>();
    final Map<Long,Object[]> occurrences = new LinkedHashMap<>();
    String digest;
    private long bytes;
    private int rowCount;

    static CtbImportData read(Path path) throws Exception {
        return read(path, 64L*1024*1024, 200000);
    }
    static CtbImportData read(Path path,long maxBytes,int maxRecords) throws Exception {
        if(maxBytes<=0 || maxRecords<=0) throw new IllegalArgumentException("limit");
        var data = new CtbImportData();
        try (var source = DriverManager.getConnection("jdbc:sqlite:" + path.toUri() + "?mode=ro")) {
            source.setAutoCommit(false);
            var hash = MessageDigest.getInstance("SHA-256");
            var selections=new LinkedHashMap<String,List<String>>();
            long measuredBytes=0,measuredRecords=0;
            for (var entry : COLUMNS.entrySet()) {
                String table = entry.getKey();
                var present = new HashSet<String>();
                try (var s = source.createStatement(); var r = s.executeQuery("PRAGMA table_info(" + table + ")")) {
                    while (r.next()) present.add(r.getString("name"));
                }
                var select = new ArrayList<String>();
                for (String column : entry.getValue()) {
                    if (present.contains(column)) select.add(column.equals("master_id") ? "COALESCE(master_id,0)" : JdbcIdentifiers.quote(source,column));
                    else if (table.equals("children") && column.equals("master_id")) select.add("0");
                    else throw new IllegalArgumentException("schema");
                }
                var lengths=new ArrayList<String>();
                for(String column:select) lengths.add("COALESCE(length(CAST("+column+" AS BLOB)),0)");
                try(var s=source.createStatement();var r=s.executeQuery("SELECT COALESCE(SUM("+String.join("+",lengths)+"),0),COUNT(*) FROM "+table)) {
                    r.next();
                    measuredBytes=Math.addExact(measuredBytes,r.getLong(1));
                    measuredRecords=Math.addExact(measuredRecords,r.getLong(2));
                }
                selections.put(table,select);
            }
            long fileBytes=java.nio.file.Files.size(path);
            if(fileBytes>maxBytes || measuredBytes>maxBytes || measuredRecords>maxRecords)
                throw new CtbImportProblem(new CtbImportProblem.Limits(fileBytes,measuredBytes,measuredRecords,maxBytes,maxRecords));
            for(var entry:COLUMNS.entrySet()) {
                String table=entry.getKey();var select=selections.get(table);
                var rows = new ArrayList<Object[]>(); data.tables.put(table,rows);
                // Include every field in ordering: preview fingerprint is independent of physical row order.
                String order=java.util.stream.IntStream.rangeClosed(1,select.size()).mapToObj(Integer::toString).collect(java.util.stream.Collectors.joining(","));
                String sql = "SELECT " + String.join(",",select) + " FROM " + table + " ORDER BY " + order;
                try (var s = source.createStatement(); var r = s.executeQuery(sql)) {
                    while (r.next()) {
                        if (++data.rowCount > maxRecords) throw new IllegalArgumentException("limit");
                        Object[] row = new Object[select.size()];
                        for (int i=0;i<row.length;i++) {
                            Object value = entry.getValue().get(i).equals("png") ? r.getBytes(i+1) : r.getObject(i+1);
                            row[i] = value;
                            byte[] encoded = value == null ? new byte[0] : value instanceof byte[] b ? b : value.toString().getBytes(StandardCharsets.UTF_8);
                            data.bytes += encoded.length;
                            if (data.bytes > maxBytes) throw new IllegalArgumentException("limit");
                            hash.update((byte)(value == null ? 0 : value instanceof byte[] ? 1 : 2));
                            hash.update(java.nio.ByteBuffer.allocate(4).putInt(encoded.length).array()); hash.update(encoded);
                        }
                        rows.add(row);
                    }
                }
                hash.update(table.getBytes(StandardCharsets.US_ASCII));
            }
            source.rollback();
            data.digest = HexFormat.of().formatHex(hash.digest());
        }
        data.validate(); return data;
    }
    static long number(Object value) {
        if (!(value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte))
            throw new IllegalArgumentException("hierarchy");
        return ((Number)value).longValue();
    }
    void validate() {
        var real = new HashSet<Long>();
        for (var row : tables.get("node")) if (number(row[0])<=0 || !real.add(number(row[0]))) invalid();
        for (var row : tables.get("children")) {
            long id=number(row[0]);
            if (id<=0 || occurrences.put(id,row)!=null || number(row[2])<0 || number(row[1])<0) invalid();
        }
        for (var row : occurrences.values()) {
            long id=number(row[0]), parent=number(row[1]), master=number(row[3]);
            if (parent!=0 && !occurrences.containsKey(parent)) invalid();
            if (master==0) { if (!real.contains(id)) invalid(); }
            else {
                var target=occurrences.get(master);
                if (target==null || number(target[3])!=0 || !real.contains(master) || real.contains(id)) invalid();
            }
        }
        for (long id : real) if (!occurrences.containsKey(id)) invalid();
        var done=new HashSet<Long>();
        for (long id:occurrences.keySet()) {
            var path=new HashSet<Long>(); long current=id;
            while(current!=0 && !done.contains(current)) {
                if(!path.add(current)) invalid();
                current=number(occurrences.get(current)[1]);
            }
            done.addAll(path);
        }
        for (String table:List.of("image","grid","codebox"))
            for(var row:tables.get(table)) if(!real.contains(number(row[0]))) invalid();
        var titles=new HashMap<Long,String>();
        for(var row:tables.get("node"))titles.put(number(row[0]),String.valueOf(row[1]));
        var linkIssues=new ArrayList<CtbImportProblem.LinkIssue>();
        for(var row:tables.get("node")) if("custom-colors".equals(row[3]))
            inspectLinks((String)row[2],number(row[0]),titles.get(number(row[0])),"node.txt",true,linkIssues);
        for(var row:tables.get("image"))
            inspectLinks((String)row[6],number(row[0]),titles.get(number(row[0])),"image.link (offset "+row[1]+")",false,linkIssues);
        if(!linkIssues.isEmpty())throw new CtbImportProblem(linkIssues);
        var bookmarks=new HashSet<Long>();
        for(var row:tables.get("bookmark")) if(!occurrences.containsKey(number(row[0])) || !bookmarks.add(number(row[0]))) invalid();
    }
    private static void invalid() { throw new IllegalArgumentException("hierarchy"); }
    Map<String,Integer> counts() {
        var counts=new LinkedHashMap<String,Integer>(); tables.forEach((key,value)->counts.put(key,value.size())); return counts;
    }

    /** Caller owns the target transaction. All ids are allocated before any insert. */
    int insert(Connection target) throws SQLException {
        requireTransactionalTarget(target);
        long next=scalar(target,"SELECT COALESCE(MAX(node_id),0) FROM (SELECT node_id FROM node UNION ALL SELECT node_id FROM children) AS import_ids");
        var ids=new HashMap<Long,Long>();
        for(long old:occurrences.keySet()) ids.put(old,Math.addExact(next,ids.size()+1L));
        long sequence=scalar(target,"SELECT COALESCE(MAX(sequence),0) FROM children WHERE father_id=0");
        long bookmarkSequence=scalar(target,"SELECT COALESCE(MAX(sequence),0) FROM bookmark");
        var rootSequence=new HashMap<Long,Long>();
        occurrences.values().stream().filter(r->number(r[1])==0)
                .sorted(Comparator.<Object[]>comparingLong(r->number(r[2])).thenComparingLong(r->number(r[0])))
                .forEach(r->rootSequence.put(number(r[0]),(long)rootSequence.size()+1));
        tables.get("bookmark").sort(Comparator.comparingLong(r->number(r[1])));
        for(var entry:COLUMNS.entrySet()) {
            String table=entry.getKey(); var columns=entry.getValue();
            var quoted=new ArrayList<String>(); for(String column:columns) quoted.add(JdbcIdentifiers.quote(target,column));
            String sql="INSERT INTO " + JdbcIdentifiers.quote(target,table) + " ("+String.join(",",quoted)+") VALUES ("+String.join(",",Collections.nCopies(columns.size(),"?"))+")";
            try(var insert=target.prepareStatement(sql)) {
                long bookmarkIndex=0;
                for(var original:tables.get(table)) {
                    Object[] row=original.clone(); long old=number(row[0]); row[0]=ids.get(old);
                    if(table.equals("children")) {
                        long parent=number(row[1]),master=number(row[3]);
                        row[1]=parent==0?0L:ids.get(parent); row[3]=master==0?0L:ids.get(master);
                        if(parent==0) row[2]=Math.addExact(sequence,rootSequence.get(old));
                    } else if(table.equals("bookmark")) row[1]=Math.addExact(bookmarkSequence,++bookmarkIndex);
                    // Internal rich-text and image links must follow the newly allocated occurrence ids.
                    if(table.equals("node") && "custom-colors".equals(row[3])) row[2]=links((String)row[2],ids,true);
                    if(table.equals("image")) row[6]=links((String)row[6],ids,false);
                    for(int i=0;i<row.length;i++) insert.setObject(i+1,row[i]);
                    insert.executeUpdate();
                }
            }
        }
        return occurrences.size();
    }
    private void inspectLinks(String text,long sourceId,String title,String location,boolean xml,List<CtbImportProblem.LinkIssue> issues) {
        if(text==null)return;
        var matcher=linkPattern(xml).matcher(text);
        var seen=new HashSet<String>();
        while(matcher.find()) {
            String target=matcher.group(2);boolean exists;
            try{exists=occurrences.containsKey(Long.parseLong(target));}catch(NumberFormatException e){exists=false;}
            if(!exists && seen.add(target))issues.add(new CtbImportProblem.LinkIssue(sourceId,title,location,target));
        }
    }
    private static java.util.regex.Pattern linkPattern(boolean xml){
        return java.util.regex.Pattern.compile(xml ? "(\\blink\\s*=\\s*['\"]node\\s+)([0-9]+)" : "^(node\\s+)([0-9]+)");
    }
    static String links(String text,Map<Long,Long> ids,boolean xml) {
        if(text==null) return null;
        var pattern=linkPattern(xml);
        var matcher=pattern.matcher(text); var result=new StringBuffer();
        while(matcher.find()) {
            Long replacement; try { replacement=ids.get(Long.parseLong(matcher.group(2))); } catch(NumberFormatException e){replacement=null;}
            if(replacement==null) throw new IllegalArgumentException("links");
            matcher.appendReplacement(result,java.util.regex.Matcher.quoteReplacement(matcher.group(1)+replacement));
        }
        matcher.appendTail(result); return result.toString();
    }
    private static void requireTransactionalTarget(Connection target) throws SQLException {
        if(!target.getMetaData().supportsTransactions()) throw new SQLException("Transactional database required");
        String product=target.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
        if(product.contains("mysql") || product.contains("mariadb")) {
            try(var statement=target.createStatement();var rows=statement.executeQuery(
                    "SELECT TABLE_NAME, ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()")) {
                var transactional=new HashSet<String>();
                while(rows.next()) if("InnoDB".equalsIgnoreCase(rows.getString(2))) transactional.add(rows.getString(1));
                if(!transactional.containsAll(COLUMNS.keySet())) throw new SQLException("All CTB tables must use InnoDB");
            }
        }
    }
    private static long scalar(Connection c,String sql)throws SQLException {
        try(var s=c.createStatement();var r=s.executeQuery(sql)){r.next();return r.getLong(1);}
    }
}
