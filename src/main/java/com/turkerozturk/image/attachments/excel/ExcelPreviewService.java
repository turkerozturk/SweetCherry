package com.turkerozturk.image.attachments.excel;

import com.turkerozturk.jdbc.JdbcIdentifiers;
import jakarta.persistence.*;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.zip.ZipInputStream;

/** Read-only extraction: no evaluation, external requests, workbook writes or BLOB updates. */
@Service @Transactional(readOnly = true)
public class ExcelPreviewService {
    @PersistenceContext private EntityManager manager;
    private final ExcelPreviewSettings settings;
    public ExcelPreviewService(ExcelPreviewSettings settings) { this.settings = settings; }
    public static boolean supports(String filename) {
        String name = Objects.requireNonNullElse(filename, "").toLowerCase(Locale.ROOT);
        return name.endsWith(".xlsx") || name.endsWith(".xls");
    }
    public static final class Problem extends RuntimeException {
        private final String code;
        public Problem(String code) { super(code); this.code = code; }
        public String code() { return code; }
    }
    public record SheetInfo(int index, String name, boolean hidden) {}
    public record CellValue(String text, String formula, boolean bold, boolean italic, String alignment) {}
    public record RowValue(int number, List<CellValue> cells) {}
    public record Preview(String filename, String nodeName, List<SheetInfo> sheets, int sheet,
            int page, int pages, int totalRows, List<String> columns, List<RowValue> rows,
            boolean clippedColumns, boolean clippedText) {}
    public Preview preview(long nodeId, int offset, int sheet, int page, Locale locale) {
        if (nodeId <= 0 || offset < 0 || sheet < 0 || page < 1 || page > 1048576) throw new Problem("request");
        return manager.unwrap(Session.class).doReturningWork(c -> load(c, nodeId, offset, sheet, page, locale));
    }
    Preview load(Connection c, long nodeId, int offset, int sheet, int page, Locale locale) throws SQLException {
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
                    return parse(bytes, filename, name, sheet, page, locale);
                } catch (IOException error) { throw new Problem("invalid"); }
            }
        }
    }
    Preview parse(byte[] bytes, String filename, String name, int selected, int page, Locale locale) {
        if (!supports(filename)) throw new Problem("unsupported");
        if (bytes.length > settings.maxBytes()) throw new Problem("large");
        if (selected < 0 || page < 1) throw new Problem("request");
        preflight(bytes);
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            if (selected >= workbook.getNumberOfSheets()) throw new Problem("request");
            var sheets = new ArrayList<SheetInfo>();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                if (i >= 256) throw new Problem("large");
                sheets.add(new SheetInfo(i, workbook.getSheetName(i), workbook.isSheetHidden(i) || workbook.isSheetVeryHidden(i)));
            }
            Sheet sheet = workbook.getSheetAt(selected);
            int total = sheet.getPhysicalNumberOfRows() == 0 ? 0 : sheet.getLastRowNum() + 1;
            int pages = Math.max(1, (total + settings.rows() - 1) / settings.rows());
            if (page > pages) throw new Problem("request");
            int start = (page - 1) * settings.rows(), end = Math.min(total, start + settings.rows());
            int width = 0; boolean clippedColumns = false, clippedText = false;
            for (int i = start; i < end; i++) {
                Row row = sheet.getRow(i);
                if (row != null) { width = Math.max(width, Math.max(0, row.getLastCellNum())); }
            }
            clippedColumns = width > settings.columns(); width = Math.min(width, settings.columns());
            var columns = new ArrayList<String>();
            for (int col = 0; col < width; col++) columns.add(CellReference.convertNumToColString(col));
            var formatter = new DataFormatter(locale); formatter.setUseCachedValuesForFormulaCells(true);
            var rows = new ArrayList<RowValue>();
            for (int i = start; i < end; i++) {
                Row row = sheet.getRow(i); var cells = new ArrayList<CellValue>();
                for (int col = 0; col < width; col++) {
                    Cell cell = row == null ? null : row.getCell(col);
                    String text = cell == null ? "" : formatter.formatCellValue(cell);
                    if (cell instanceof org.apache.poi.xssf.usermodel.XSSFCell xcell
                            && cell.getCellType() == CellType.FORMULA
                            && (!xcell.getCTCell().isSetV() || xcell.getCTCell().getV().isEmpty())) text = "—";
                    String formula = cell != null && cell.getCellType() == CellType.FORMULA ? "=" + cell.getCellFormula() : "";
                    if (text.codePointCount(0, text.length()) > settings.cellCharacters() || formula.length() > settings.cellCharacters()) clippedText = true;
                    CellStyle style = cell == null ? null : cell.getCellStyle();
                    Font font = style == null ? null : workbook.getFontAt(style.getFontIndex());
                    String alignment = style == null ? "left" : switch (style.getAlignment()) {
                        case RIGHT -> "right"; case CENTER -> "center"; default -> "left";
                    };
                    cells.add(new CellValue(clip(text), clip(formula), font != null && font.getBold(), font != null && font.getItalic(), alignment));
                }
                rows.add(new RowValue(i + 1, List.copyOf(cells)));
            }
            return new Preview(filename, Objects.requireNonNullElse(name, ""), List.copyOf(sheets), selected, page, pages,
                    total, List.copyOf(columns), List.copyOf(rows), clippedColumns, clippedText);
        } catch (Problem error) { throw error; }
        catch (org.apache.poi.EncryptedDocumentException error) { throw new Problem("encrypted"); }
        catch (IOException | RuntimeException error) { throw new Problem("invalid"); }
    }
    private String clip(String text) {
        int n = text.codePointCount(0, text.length());
        return n <= settings.cellCharacters() ? text : text.substring(0, text.offsetByCodePoints(0, settings.cellCharacters())) + "…";
    }
    private void preflight(byte[] bytes) {
        if (bytes.length < 8) throw new Problem("invalid");
        // Bound aggregate expanded OOXML size before POI builds an object graph. No files are extracted.
        if (bytes[0] == 'P' && bytes[1] == 'K') {
            try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
                long total = 0; int count = 0; var names = new HashSet<String>(); byte[] buffer = new byte[8192];
                for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                    if (++count > 2048 || !names.add(entry.getName())) throw new Problem("large");
                    int n; while ((n = zip.read(buffer)) != -1) {
                        total += n; if (total > settings.expandedBytes()) throw new Problem("large");
                    }
                }
            } catch (IOException error) { throw new Problem("invalid"); }
        }
    }
}
