package com.turkerozturk.image.attachments.word;

import com.turkerozturk.jdbc.JdbcIdentifiers;
import jakarta.persistence.*;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.xwpf.usermodel.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.zip.ZipInputStream;

/** Extracts bounded text only; no document writes, embedded object decoding or external fetches. */
@Service @Transactional(readOnly=true)
public class WordPreviewService {
    @PersistenceContext private EntityManager manager;
    private final WordPreviewSettings settings;
    public WordPreviewService(WordPreviewSettings settings) { this.settings=settings; }
    public static boolean supports(String filename) {
        String name=Objects.requireNonNullElse(filename, "").toLowerCase(Locale.ROOT);
        return name.endsWith(".docx") || name.endsWith(".doc");
    }
    public static final class Problem extends RuntimeException {
        private final String code;
        public Problem(String code) { super(code); this.code=code; }
        public String code() { return code; }
    }
    public record Run(String text, boolean bold, boolean italic, boolean underline) {}
    public record Block(String kind, String alignment, List<Run> runs, List<List<String>> rows) {}
    public record Preview(String filename, String nodeName, List<Block> blocks, boolean clipped, boolean legacy) {}

    public Preview preview(long nodeId, int offset) {
        if (nodeId <= 0 || offset < 0) throw new Problem("request");
        return manager.unwrap(Session.class).doReturningWork(c -> load(c, nodeId, offset));
    }
    Preview load(Connection c, long nodeId, int offset) throws SQLException {
        String length = c.getMetaData().getDatabaseProductName().equalsIgnoreCase("PostgreSQL") ? "OCTET_LENGTH(i.png)" : "LENGTH(i.png)";
        String where = " FROM image i JOIN node n ON n.node_id=i.node_id WHERE i.node_id=? AND i."
                + JdbcIdentifiers.quote(c, "offset") + "=? AND i.png IS NOT NULL AND COALESCE(i.anchor,'')='' AND COALESCE(i.filename,'')<>'' AND i.filename<>'__ct_special.tex'";
        String filename, name;
        // Check metadata before requesting the BLOB, also on drivers which eagerly fetch binary columns.
        try (var q = c.prepareStatement("SELECT i.filename,n.name," + length + where)) {
            q.setLong(1, nodeId); q.setInt(2, offset);
            try (var rs = q.executeQuery()) {
                if (!rs.next()) throw new Problem("missing");
                filename = rs.getString(1); name = rs.getString(2);
                if (!supports(filename)) throw new Problem("unsupported");
                if (rs.getLong(3) > settings.maxBytes()) throw new Problem("large");
            }
        }
        try (var q = c.prepareStatement("SELECT i.png" + where)) {
            q.setLong(1, nodeId); q.setInt(2, offset);
            try (var rs = q.executeQuery()) {
                if (!rs.next()) throw new Problem("missing");
                try (var in = rs.getBinaryStream(1)) {
                    byte[] bytes = in.readNBytes(settings.maxBytes() + 1);
                    if (bytes.length > settings.maxBytes()) throw new Problem("large");
                    return parse(bytes, filename, name);
                } catch (IOException error) { throw new Problem("invalid"); }
            }
        }
    }


    Preview parse(byte[] bytes, String filename, String nodeName) {
        if (!supports(filename)) throw new Problem("unsupported");
        if (bytes.length>settings.maxBytes()) throw new Problem("large");
        preflight(bytes);
        var builder=new Builder();
        try {
            FileMagic magic=FileMagic.valueOf(bytes);
            if (filename.toLowerCase(Locale.ROOT).endsWith(".docx")) {
                if (magic==FileMagic.OLE2) {
                    try(var fs=new org.apache.poi.poifs.filesystem.POIFSFileSystem(new ByteArrayInputStream(bytes))) {
                        throw new Problem(fs.getRoot().hasEntry("EncryptedPackage") ? "encrypted" : "invalid");
                    }
                }
                if (magic!=FileMagic.OOXML) throw new Problem("invalid");
                try(var doc=new XWPFDocument(new ByteArrayInputStream(bytes))) {
                    for (var element:doc.getBodyElements()) {
                        if (!builder.room()) break;
                        if (element instanceof XWPFParagraph p) builder.paragraph(p);
                        else if (element instanceof XWPFTable table) builder.table(table);
                    }
                }
            } else {
                if (magic!=FileMagic.OLE2) throw new Problem("invalid");
                try(var doc=new HWPFDocument(new ByteArrayInputStream(bytes))) {
                    Range range=doc.getRange();
                    for(int i=0; i<range.numParagraphs();) {
                        if (!builder.room()) break;
                        var p=range.getParagraph(i);
                        if(p.isInTable()) {
                            var table=range.getTable(p); builder.table(table);
                            int end=table.getEndOffset(); i++;
                            while(i<range.numParagraphs() && range.getParagraph(i).getStartOffset()<end) i++;
                        } else {
                            builder.plain(WordExtractor.stripFields(p.text())); i++;
                        }
                    }
                }
            }
            return new Preview(filename,Objects.requireNonNullElse(nodeName,""),List.copyOf(builder.blocks),builder.clipped,
                    filename.toLowerCase(Locale.ROOT).endsWith(".doc"));
        } catch (Problem error) { throw error; }
        catch (org.apache.poi.EncryptedDocumentException error) { throw new Problem("encrypted"); }
        catch (IOException | RuntimeException error) { throw new Problem("invalid"); }
    }

    private final class Builder {
        final List<Block> blocks=new ArrayList<>(); int remaining=settings.characters(); boolean clipped;
        boolean room() {
            if(remaining<=0 || blocks.size()>=settings.blocks()) { clipped=true; return false; }
            return true;
        }
        String text(String value) {
            String clean=Objects.requireNonNullElse(value,"").replace('\u000b','\n').replace('\f','\n').codePoints()
                    .filter(c->c=='\n'||c=='\t'||c=='\r'||!Character.isISOControl(c))
                    .collect(StringBuilder::new,StringBuilder::appendCodePoint,StringBuilder::append).toString();
            int count=clean.codePointCount(0,clean.length()), take=Math.min(count,remaining);
            remaining-=take;
            if(take<count) { clipped=true; return clean.substring(0,clean.offsetByCodePoints(0,take))+"…"; }
            return clean;
        }
        void plain(String value) {
            blocks.add(new Block("paragraph","left",List.of(new Run(text(value).stripTrailing(),false,false,false)),List.of()));
        }
        void paragraph(XWPFParagraph p) {
            var runs=new ArrayList<Run>();
            String style=Objects.requireNonNullElse(p.getStyle(),"").toLowerCase(Locale.ROOT);
            if(p.getNumID()!=null || style.startsWith("listbullet") || style.startsWith("listnumber")) runs.add(new Run(text("• "),false,false,false));
            for(var item:p.getIRuns()) {
                if(remaining<=0) { clipped=true; break; }
                if(item instanceof XWPFRun run) runs.add(new Run(text(run.text()),run.isBold(),run.isItalic(),run.getUnderline()!=UnderlinePatterns.NONE));
                else if(item instanceof XWPFSDT sdt) runs.add(new Run(text(sdt.getContent().getText()),false,false,false));
            }
            if(runs.isEmpty()) runs.add(new Run(text(p.getText()),false,false,false));
            String kind=style.startsWith("heading")||style.equals("title")?"heading":"paragraph";
            String alignment=switch(p.getAlignment()) { case RIGHT->"right"; case CENTER->"center"; default->"left"; };
            blocks.add(new Block(kind,alignment,List.copyOf(runs),List.of()));
        }
        void table(XWPFTable table) {
            var rows=new ArrayList<List<String>>();
            int count=Math.min(table.getNumberOfRows(),settings.tableRows());
            if(count<table.getNumberOfRows()) clipped=true;
            for(int r=0;r<count && remaining>0;r++) {
                var row=table.getRow(r); var cells=new ArrayList<String>();
                int columns=Math.min(row.getTableCells().size(),settings.tableColumns());
                if(columns<row.getTableCells().size()) clipped=true;
                for(int c=0;c<columns && remaining>0;c++) cells.add(text(row.getCell(c).getTextRecursively()));
                rows.add(List.copyOf(cells));
            }
            if(remaining<=0) clipped=true;
            blocks.add(new Block("table","left",List.of(),List.copyOf(rows)));
        }
        void table(org.apache.poi.hwpf.usermodel.Table table) {
            var rows=new ArrayList<List<String>>();
            int count=Math.min(table.numRows(),settings.tableRows()); if(count<table.numRows()) clipped=true;
            for(int r=0;r<count && remaining>0;r++) {
                var row=table.getRow(r); var cells=new ArrayList<String>();
                int columns=Math.min(row.numCells(),settings.tableColumns()); if(columns<row.numCells()) clipped=true;
                for(int c=0;c<columns && remaining>0;c++) cells.add(text(WordExtractor.stripFields(row.getCell(c).text())).stripTrailing());
                rows.add(List.copyOf(cells));
            }
            if(remaining<=0) clipped=true;
            blocks.add(new Block("table","left",List.of(),List.copyOf(rows)));
        }
    }

    private void preflight(byte[] bytes) {
        if(bytes.length<8) throw new Problem("invalid");
        if(bytes[0]=='P' && bytes[1]=='K') {
            try(var zip=new ZipInputStream(new ByteArrayInputStream(bytes))) {
                long total=0; int count=0; var names=new HashSet<String>(); byte[] buffer=new byte[8192];
                for(var entry=zip.getNextEntry();entry!=null;entry=zip.getNextEntry()) {
                    if(++count>2048 || !names.add(entry.getName())) throw new Problem("large");
                    int n;while((n=zip.read(buffer))!=-1) { total+=n; if(total>settings.expandedBytes()) throw new Problem("large"); }
                }
            } catch(IOException error) { throw new Problem("invalid"); }
        }
    }
}
