package com.turkerozturk.exportmindmap;

import com.turkerozturk.jdbc.JdbcIdentifiers;
import com.turkerozturk.richtext.experimental.*;
import com.turkerozturk.richtext.experimental.RichTextLayout.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.*;
import java.util.*;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Text-only snapshots for mind-map notes; never selects image/attachment binary data. */
@Service
public class MindMapTextContent {
    @PersistenceContext private EntityManager entityManager;

    @Transactional(readOnly = true)
    public String text(long contentId, String syntax, String text) {
        if(com.turkerozturk.markdown.NodeMarkup.supports(syntax))return com.turkerozturk.markdown.NodeMarkup.text(syntax,text);
        if (!"custom-colors".equals(syntax)) return Objects.toString(text, "");
        return entityManager.unwrap(Session.class).doReturningWork(connection -> text(connection, contentId, text));
    }

    static String text(Connection connection, long id, String xml) throws SQLException {
        try {
            var objects = new LinkedHashMap<EmbeddedObject,String>();
            String offset = JdbcIdentifiers.quote(connection,"offset");
            try (var query = connection.prepareStatement("SELECT " + offset + ",anchor,filename,link,time FROM image WHERE node_id=? ORDER BY " + offset)) {
                query.setLong(1,id);
                try (var rows = query.executeQuery()) {
                    while (rows.next()) {
                        int position = rows.getInt(1);
                        String anchor = rows.getString(2), filename = rows.getString(3);
                        boolean isAnchor = anchor != null && !anchor.isEmpty();
                        boolean attachment = filename != null && !filename.isEmpty();
                        ObjectKind kind = isAnchor ? ObjectKind.ANCHOR : attachment ? ObjectKind.ATTACHMENT : ObjectKind.IMAGE;
                        String label = isAnchor ? "Anchor: " + anchor : attachment ? "Attachment: " + filename : "Image";
                        String metadata = "node: " + id + ", offset: " + position;
                        Object time = rows.getObject(5);
                        if (time != null) metadata += ", time: " + time;
                        add(objects,new EmbeddedObject(kind,id,position),"[" + label + " (" + metadata + ")]" + linkSuffix(rows.getString(4)));
                    }
                }
            }
            try (var query = connection.prepareStatement("SELECT " + offset + ",txt,syntax FROM codebox WHERE node_id=? ORDER BY " + offset)) {
                query.setLong(1,id);
                try (var rows = query.executeQuery()) {
                    while (rows.next()) add(objects,new EmbeddedObject(ObjectKind.CODEBOX,id,rows.getInt(1)),
                            "\n[Code box: " + Objects.toString(rows.getString(3),"") + "]\n" + Objects.toString(rows.getString(2),"") + "\n");
                }
            }
            var adapter = new EmbeddedContentAdapter();
            try (var query = connection.prepareStatement("SELECT " + offset + ",txt FROM grid WHERE node_id=? ORDER BY " + offset)) {
                query.setLong(1,id);
                try (var rows = query.executeQuery()) {
                    while (rows.next()) {
                        var table = adapter.readTable(rows.getString(2));
                        var value = new StringBuilder("\n[Table]\n");
                        var tableRows = table.rows();
                        // CherryTree stores the header last; display it first as in the node reader.
                        if (!tableRows.isEmpty()) {
                            appendRow(value,tableRows.get(tableRows.size()-1));
                            for (int i=0;i<tableRows.size()-1;i++) appendRow(value,tableRows.get(i));
                        }
                        add(objects,new EmbeddedObject(ObjectKind.TABLE,id,rows.getInt(1)),value.toString());
                    }
                }
            }
            var document = xml == null || xml.isBlank() ? new RichTextDocument(List.of()) : new RichTextXmlReader().read(xml);
            var layout = new RichTextLayoutBuilder().build(document,new ArrayList<>(objects.keySet()));
            var result = new StringBuilder();
            var segment = new StringBuilder();
            String link = null;
            for (var part : layout.parts()) {
                if (part instanceof TextPart text) {
                    String next = text.run().attributes().get("link");
                    if (!Objects.equals(link,next)) { appendText(result,segment,link); link=next; }
                    segment.append(text.run().text());
                } else {
                    appendText(result,segment,link); link=null;
                    result.append(objects.get(((ObjectPart)part).object()));
                }
            }
            appendText(result,segment,link);
            return result.toString();
        } catch (IllegalArgumentException failure) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Cannot convert mind-map content for source node " + id);
        }
    }

    private static void add(Map<EmbeddedObject,String> objects, EmbeddedObject ref, String value) {
        if (objects.putIfAbsent(ref,value) != null) throw new IllegalArgumentException("Duplicate object reference");
    }
    private static void appendRow(StringBuilder value,List<String> row) {
        value.append(String.join("\t",row)).append('\n');
    }
    private static void appendText(StringBuilder output,StringBuilder segment,String link) {
        if (segment.length()==0) return;
        String value=segment.toString();
        int end=value.length();
        while (end>0 && (value.charAt(end-1)=='\n' || value.charAt(end-1)=='\r')) end--;
        output.append(value,0,end).append(linkSuffix(link)).append(value.substring(end));
        segment.setLength(0);
    }
    private static String linkSuffix(String link) {
        if (link==null || link.isBlank()) return "";
        String target=link;
        if (link.startsWith("webs ")) target=link.substring(5);
        else if (link.startsWith("node ")) {
            String[] parts=link.substring(5).split(" ",2);
            target="/nodes/"+parts[0]+(parts.length==2 ? "#"+parts[1] : "");
        } else if (link.startsWith("file ") || link.startsWith("fold ")) {
            try { target=new String(Base64.getDecoder().decode(link.substring(5)),java.nio.charset.StandardCharsets.UTF_8); }
            catch (IllegalArgumentException ignored) { /* Keep the original link if legacy encoding is unknown. */ }
        }
        return " ("+target+")";
    }
}
