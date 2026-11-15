package com.turkerozturk.image.gallery;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Read-only projections: listing never selects the binary column. */
@Service
@Transactional(readOnly = true)
public class GalleryService {
    static final int PAGE_SIZE = 36;
    static final int MAX_BYTES = 64 * 1024 * 1024;
    @PersistenceContext private EntityManager manager;

    public record Options(int page, String search, String sort, String tag) {
        public Options(int page,String search,String sort){this(page,search,sort,"");}
        public Options {
            search = search == null ? "" : search.strip();
            tag = tag == null ? "" : tag;
            if (page < 1 || page > 1000000 || search.length() > 200
                    || tag.length() > com.turkerozturk.node.tags.NodeTags.MAX_INPUT_CHARACTERS || tag.indexOf(' ')>=0 || tag.indexOf('\0')>=0
                    || !Set.of("newest", "oldest", "node", "id").contains(sort))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }
    public record Item(long nodeId, int offset, String name, long created, String filename,
                       String link, long imageTime, long bytes) {
        public String id() { return nodeId + ":" + offset; }
        public String createdText() { return date(created); }
        public String imageTimeText() { return date(imageTime); }
        private static String date(long epoch) {
            if (epoch <= 0) return "—";
            try { return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault()).format(Instant.ofEpochSecond(epoch)); }
            catch (DateTimeException invalid) { return "—"; }
        }
    }
    public record Page(List<Item> items, long total, Options options, List<String> tags) {
        public Page(List<Item> items,long total,Options options){this(items,total,options,List.of());}
        public long pages() { return Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE); }
    }
    public record Payload(byte[] bytes, String filename) { }

    public Page list(Options options) {
        return manager.unwrap(Session.class).doReturningWork(c -> list(c, options));
    }
    public Payload payload(long nodeId, int offset) {
        return manager.unwrap(Session.class).doReturningWork(c -> payload(c, nodeId, offset));
    }

    private static String length(Connection c) throws SQLException {
        return c.getMetaData().getDatabaseProductName().equalsIgnoreCase("PostgreSQL")
                ? "OCTET_LENGTH(i.png)" : "LENGTH(i.png)";
    }
    private static String candidates() {
        // Empty filename is CherryTree's embedded-image convention. Named raster attachments
        // are included as well; anchors, LaTeX sources, SVG and other attachments are not.
        return "i.png IS NOT NULL AND COALESCE(i.anchor,'')='' AND (COALESCE(i.filename,'')=''"
                + " OR LOWER(i.filename) LIKE '%.png' OR LOWER(i.filename) LIKE '%.jpg'"
                + " OR LOWER(i.filename) LIKE '%.jpeg' OR LOWER(i.filename) LIKE '%.gif'"
                + " OR LOWER(i.filename) LIKE '%.bmp' OR LOWER(i.filename) LIKE '%.webp'"
                + " OR LOWER(i.filename) LIKE '%.tif' OR LOWER(i.filename) LIKE '%.tiff')";
    }
    static Page list(Connection c, Options options) throws SQLException {
        String from = " FROM image i JOIN node n ON n.node_id=i.node_id WHERE " + candidates();
        boolean search = !options.search().isEmpty();
        if (search) from += " AND (LOWER(n.name) LIKE ? ESCAPE '!' OR LOWER(COALESCE(i.filename,'')) LIKE ? ESCAPE '!'"
                + " OR LOWER(COALESCE(i.link,'')) LIKE ? ESCAPE '!')";
        if (!options.tag().isEmpty()) from += " AND " + tagPredicate(c);
        long total;
        try (var q = c.prepareStatement("SELECT COUNT(*)" + from)) {
            bindSearch(q, options, search);
            try (var rs = q.executeQuery()) { rs.next(); total = rs.getLong(1); }
        }
        String order = switch (options.sort()) {
            case "oldest" -> "COALESCE(n.ts_creation,0) ASC,i.node_id,i." + quote(c) + " ASC";
            case "node" -> "LOWER(n.name) ASC,i.node_id,i." + quote(c) + " ASC";
            case "id" -> "i.node_id,i." + quote(c) + " ASC";
            default -> "COALESCE(n.ts_creation,0) DESC,i.node_id,i." + quote(c) + " ASC";
        };
        var items = new ArrayList<Item>();
        String sql = "SELECT i.node_id,i." + quote(c) + ",n.name,n.ts_creation,i.filename,i.link,i.time,"
                + length(c) + from + " ORDER BY " + order + " LIMIT ? OFFSET ?";
        try (var q = c.prepareStatement(sql)) {
            int next = bindSearch(q, options, search);
            q.setInt(next++, PAGE_SIZE); q.setLong(next, (long) (options.page() - 1) * PAGE_SIZE);
            try (var rs = q.executeQuery()) {
                while (rs.next()) items.add(new Item(rs.getLong(1), rs.getInt(2), text(rs, 3), rs.getLong(4),
                        text(rs, 5), text(rs, 6), rs.getLong(7), rs.getLong(8)));
            }
        }
        return new Page(List.copyOf(items), total, options, catalogue(c,options.tag()));
    }
    /** Tag identity is literal and case-sensitive, even under a case-insensitive server collation. */
    static String tagPredicate(Connection c)throws SQLException{
        String product=c.getMetaData().getDatabaseProductName();
        if(product.equalsIgnoreCase("PostgreSQL"))return "STRPOS((' ' || COALESCE(n.tags,'') || ' ') COLLATE \"C\", ?) > 0";
        if(product.equalsIgnoreCase("MySQL")||product.equalsIgnoreCase("MariaDB"))return "LOCATE(CAST(? AS BINARY), CAST(CONCAT(' ', COALESCE(n.tags,''), ' ') AS BINARY)) > 0";
        return "INSTR(' ' || COALESCE(n.tags,'') || ' ', ?) > 0";
    }
    private static List<String> catalogue(Connection c,String selected)throws SQLException{
        var tags=new TreeSet<String>(com.turkerozturk.node.tags.NodeTags.ORDER);
        // Deduplicate numeric owner IDs, never tag strings under a server collation.
        try(var q=c.prepareStatement("SELECT n.tags FROM node n JOIN (SELECT DISTINCT i.node_id FROM image i WHERE "+candidates()+") owners ON owners.node_id=n.node_id WHERE LENGTH(n.tags)<=?")){
            q.setInt(1,com.turkerozturk.node.tags.NodeTags.MAX_INPUT_CHARACTERS);
            try(var rs=q.executeQuery()){while(rs.next())tags.addAll(com.turkerozturk.node.tags.NodeTags.split(rs.getString(1)));}
        }
        tags.removeIf(t->t.equalsIgnoreCase(com.turkerozturk.node.TrashSql.TAG)||t.equalsIgnoreCase(com.turkerozturk.node.audit.AuditSql.TAG));
        if(!selected.isEmpty())tags.add(selected);
        return List.copyOf(tags);
    }
    private static String quote(Connection c) throws SQLException {
        return com.turkerozturk.jdbc.JdbcIdentifiers.quote(c, "offset");
    }
    private static int bindSearch(PreparedStatement q, Options options, boolean search) throws SQLException {
        int next=1;
        if(search){
            String value = "%" + options.search().toLowerCase(Locale.ROOT).replace("!", "!!")
                    .replace("%", "!%").replace("_", "!_") + "%";
            for(int i=0;i<3;i++)q.setString(next++,value);
        }
        if(!options.tag().isEmpty())q.setString(next++," "+options.tag()+" ");
        return next;
    }
    private static String text(ResultSet rs, int col) throws SQLException {
        return Objects.requireNonNullElse(rs.getString(col), "");
    }
    static Payload payload(Connection c, long nodeId, int offset) throws SQLException {
        if (nodeId <= 0 || offset < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        String where = " FROM image i JOIN node n ON n.node_id=i.node_id WHERE i.node_id=? AND i."
                + quote(c) + "=? AND " + candidates();
        try (var q = c.prepareStatement("SELECT " + length(c) + where)) {
            q.setLong(1, nodeId); q.setInt(2, offset);
            try (var rs = q.executeQuery()) {
                if (!rs.next()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
                if (rs.getLong(1) > MAX_BYTES) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
            }
        }
        // Repeat the size predicate so a concurrent replacement cannot bypass the bound.
        try (var q = c.prepareStatement("SELECT i.png,i.filename" + where + " AND " + length(c) + "<=?")) {
            q.setLong(1, nodeId); q.setInt(2, offset); q.setInt(3, MAX_BYTES);
            try (var rs = q.executeQuery()) {
                if (!rs.next()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
                return new Payload(rs.getBytes(1), text(rs, 2));
            }
        }
    }
}
