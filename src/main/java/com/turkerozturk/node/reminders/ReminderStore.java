package com.turkerozturk.node.reminders;

import java.sql.*;
import java.time.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

/** One bounded, protected CTB root contains portable versioned reminder metadata. No schema changes. */
public final class ReminderStore {
    private ReminderStore() { }
    public static final String TAG = "__sweetcherry_reminders_v1__";
    static final String OLD_PREFIX = "SweetCherry reminders v1\n\n";
    static final String PREFIX = "SweetCherry reminders v2\n\n";
    static final int MAX_ITEMS = 5000, MAX_CHARACTERS = 2_097_152;
    static final JsonMapper JSON = JsonMapper.builder().enable(tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
    public record Item(String id, long contentId, long occurrence, String targetName, String message,
            String due, String zone, String status, String created, String updated,
            String repeat,String leapDay,String anchor,String lastCompleted,String alarmId,String acknowledgedAt,String snoozedUntil) {
        public Item(String id,long contentId,long occurrence,String targetName,String message,String due,String zone,String status,String created,String updated){
            this(id,contentId,occurrence,targetName,message,due,zone,status,created,updated,"none","feb28","","",legacyAlarmId(id,due),"","");
        }
    }
    static String legacyAlarmId(String id,String due){return UUID.nameUUIDFromBytes((id+"\n"+due).getBytes(StandardCharsets.UTF_8)).toString();}
    public record Data(int version, String kind, String storeId, List<Item> items) { }
    public record LegacyItem(String id,long contentId,long occurrence,String targetName,String message,String due,String zone,String status,String created,String updated) { }
    public record LegacyData(int version,String kind,String storeId,List<LegacyItem> items) { }
    record Snapshot(long id, String raw, Data data, String revision) { }
    record Allocation(long id, long sequence) { }
    static String suffix(Connection c, boolean lock) throws SQLException {
        return lock && !c.getMetaData().getDatabaseProductName().equalsIgnoreCase("SQLite") ? " FOR UPDATE" : "";
    }
    static Snapshot read(Connection c, boolean lock) throws SQLException {
        Snapshot found = null;
        String sql = "SELECT node_id,tags,syntax,CASE WHEN LENGTH(txt)<=2097152 THEN txt ELSE NULL END,is_ro,level FROM node WHERE tags=? ORDER BY node_id" + suffix(c,lock);
        try (var q = c.prepareStatement(sql)) {
            q.setString(1,TAG);
            try (var rs = q.executeQuery()) {
                while (rs.next()) {
                    if (!TAG.equals(rs.getString(2))) continue;
                    if (found != null || !"plain-text".equals(rs.getString(3)) || (rs.getLong(5)&1)==0 || (rs.getLong(6)&3)!=3) throw problem("storeInvalid");
                    long id = rs.getLong(1); if(id<=0)throw problem("storeInvalid"); String raw = rs.getString(4);
                    found = new Snapshot(id,raw,decode(raw),hash(id,raw));
                }
            }
        }
        if (found == null) return new Snapshot(0,"",new Data(2,"reminders","",List.of()),hash(0,""));
        try (var q = c.prepareStatement("SELECT father_id,COALESCE(master_id,0) FROM children WHERE node_id=?" + suffix(c,lock))) {
            q.setLong(1,found.id());
            try (var rs = q.executeQuery()) { if (!rs.next() || rs.getLong(1)!=0 || rs.getLong(2)!=0 || rs.next()) throw problem("storeInvalid"); }
        }
        try (var q = c.prepareStatement("SELECT node_id FROM children WHERE father_id=? OR master_id=?" + suffix(c,lock))) {
            q.setLong(1,found.id()); q.setLong(2,found.id()); try (var rs = q.executeQuery()) { if (rs.next()) throw problem("storeInvalid"); }
        }
        return found;
    }
    static Data decode(String raw) {
        try {
            if (raw == null || raw.length() > MAX_CHARACTERS) throw new IllegalArgumentException();
            int version;String json;
            if(raw.startsWith(PREFIX)){version=2;json=raw.substring(PREFIX.length());}
            else if(raw.startsWith(OLD_PREFIX)){version=1;json=raw.substring(OLD_PREFIX.length());}
            else throw new IllegalArgumentException();
            Data data;
            if(version==1){
                var legacy=JSON.readValue(json,LegacyData.class);
                if(legacy.version()!=1||legacy.items()==null||legacy.items().size()>MAX_ITEMS)throw new IllegalArgumentException();
                var items=new ArrayList<Item>();for(var item:legacy.items())items.add(new Item(item.id(),item.contentId(),item.occurrence(),item.targetName(),item.message(),item.due(),item.zone(),item.status(),item.created(),item.updated()));
                data=new Data(2,legacy.kind(),legacy.storeId(),items);
            }else data=JSON.readValue(json,Data.class);
            if (data.version()!=2 || !"reminders".equals(data.kind()) || !uuid(data.storeId()) || data.items()==null || data.items().size()>MAX_ITEMS) throw new IllegalArgumentException();
            var seen = new HashSet<String>();var normalized=new ArrayList<Item>();
            for (var item : data.items()) {
                if (item==null || !uuid(item.id()) || !seen.add(item.id()) || item.contentId()<=0 || item.occurrence()<=0
                        || item.targetName()==null || item.targetName().length()>1000 || item.message()==null || item.message().length()>2000
                        || item.message().indexOf('\0')>=0 || !Set.of("pending","done","cancelled").contains(item.status())) throw new IllegalArgumentException();
                var due=Instant.parse(item.due());if(due.atZone(ZoneId.of(item.zone())).getSecond()!=0||due.getNano()!=0)throw new IllegalArgumentException(); Instant.parse(item.created()); Instant.parse(item.updated());
                String local = ReminderTime.local(item.due(),item.zone());
                if (local.length()!=16) throw new IllegalArgumentException();
                Item value=item;
                if(!Set.of("none","yearly").contains(value.repeat())||!Set.of("feb28","mar1").contains(value.leapDay())||!uuid(value.alarmId())||value.anchor()==null||value.lastCompleted()==null||value.acknowledgedAt()==null||value.snoozedUntil()==null)throw new IllegalArgumentException();
                if(value.repeat().equals("yearly")){
                    var anchor=LocalDateTime.parse(value.anchor());if(value.anchor().length()!=16||anchor.getYear()<1||anchor.getYear()>9999)throw new IllegalArgumentException();
                }else if(!value.anchor().isEmpty())throw new IllegalArgumentException();
                if(!value.lastCompleted().isEmpty())Instant.parse(value.lastCompleted());
                if(!value.acknowledgedAt().isEmpty())Instant.parse(value.acknowledgedAt());
                if(!value.snoozedUntil().isEmpty())Instant.parse(value.snoozedUntil());
                if(!value.acknowledgedAt().isEmpty()&&!value.snoozedUntil().isEmpty())throw new IllegalArgumentException();
                normalized.add(value);
            }
            return new Data(2,data.kind(),data.storeId(),List.copyOf(normalized));
        } catch (RuntimeException invalid) { throw problem("storeInvalid"); }
    }
    static boolean uuid(String value) { try { return value!=null && UUID.fromString(value).toString().equals(value); } catch (IllegalArgumentException invalid) { return false; } }
    static Allocation lockForWrite(Connection c) throws SQLException {
        // Existing node rows serialize reminder writers even before the first store exists.
        long maximum=0, sequence=0; String suffix=suffix(c,true);
        try (var q=c.prepareStatement("SELECT node_id FROM node ORDER BY node_id"+suffix);var rs=q.executeQuery()) {
            while(rs.next())maximum=Math.max(maximum,rs.getLong(1));
        }
        try (var q=c.prepareStatement("SELECT node_id,father_id,sequence FROM children ORDER BY node_id"+suffix);var rs=q.executeQuery()) {
            while(rs.next()){maximum=Math.max(maximum,rs.getLong(1));if(rs.getLong(2)==0)sequence=Math.max(sequence,rs.getLong(3));}
        }
        try{return new Allocation(Math.addExact(maximum,1),Math.addExact(sequence,1));}
        catch(ArithmeticException invalid){throw problem("limit");}
    }
    static void write(Connection c, Snapshot old, List<Item> items, Allocation allocation, Instant now) throws SQLException {
        if(items.size()>MAX_ITEMS)throw problem("limit");
        var data=new Data(2,"reminders",old.id()==0?UUID.randomUUID().toString():old.data().storeId(),List.copyOf(items));
        String raw=PREFIX+JSON.writeValueAsString(data);if(raw.length()>MAX_CHARACTERS)throw problem("limit");
        decode(raw);
        if(old.id()==0) {
            try(var q=c.prepareStatement("INSERT INTO node(node_id,name,txt,syntax,tags,is_ro,is_richtxt,has_codebox,has_table,has_image,level,ts_creation,ts_lastsave) VALUES(?,'REMINDERS',?,'plain-text',?,1,0,0,0,0,3,?,?)")) {
                q.setLong(1,allocation.id());q.setString(2,raw);q.setString(3,TAG);q.setLong(4,now.getEpochSecond());q.setLong(5,now.getEpochSecond());q.executeUpdate();
            }
            try(var q=c.prepareStatement("INSERT INTO children(node_id,father_id,sequence,master_id) VALUES(?,0,?,0)")) {
                q.setLong(1,allocation.id());q.setLong(2,allocation.sequence());q.executeUpdate();
            }
        } else {
            try(var q=c.prepareStatement("UPDATE node SET txt=?,ts_lastsave=? WHERE node_id=? AND tags=? AND txt=?")) {
                q.setString(1,raw);q.setLong(2,now.getEpochSecond());q.setLong(3,old.id());q.setString(4,TAG);q.setString(5,old.raw());
                if(q.executeUpdate()!=1)throw problem("changed");
            }
        }
    }
    private static String hash(long id,String raw) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((id+"\n"+raw).getBytes(StandardCharsets.UTF_8)));}
        catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
    static void expected(Snapshot store,String revision){if(!store.revision().equals(revision))throw problem("changed");}
    static ResponseStatusException problem(String reason){return new ResponseStatusException(HttpStatus.CONFLICT,reason);}
    public static boolean isTag(String tag){return TAG.equals(tag);}
    public static void guardHelper(Connection c,long id)throws SQLException {
        try(var q=c.prepareStatement("SELECT tags FROM node WHERE node_id=? OR node_id IN (SELECT master_id FROM children WHERE node_id=?)")) {
            q.setLong(1,id);q.setLong(2,id);try(var rs=q.executeQuery()){while(rs.next())if(isTag(rs.getString(1)))throw problem("reminder.protected");}
        }
    }
    public static void guardMove(Connection c,Collection<Long> ids,long parent)throws SQLException {
        if(parent>0)guardHelper(c,parent);for(long id:ids)guardHelper(c,id);
        if(ids.isEmpty())return;
        var helpers=new HashSet<Long>();
        try(var q=c.prepareStatement("SELECT node_id,tags FROM node WHERE tags=?")){q.setString(1,TAG);try(var rs=q.executeQuery()){while(rs.next())if(isTag(rs.getString(2)))helpers.add(rs.getLong(1));}}
        if(helpers.isEmpty())return;
        var parents=new HashMap<Long,Long>();try(var q=c.prepareStatement("SELECT node_id,father_id FROM children");var rs=q.executeQuery()){while(rs.next())parents.put(rs.getLong(1),rs.getLong(2));}
        for(long helper:helpers){var seen=new HashSet<Long>();for(long id=helper;id!=0&&seen.add(id);id=parents.getOrDefault(id,0L))if(ids.contains(id))throw problem("reminder.protected");}
    }
}
