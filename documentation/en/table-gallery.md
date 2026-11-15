# Read-only table gallery

Select a database and choose **View Mode → Table gallery**, or open `/table-gallery`.
Reader and administrator accounts can browse it, including on read-only databases.
The view changes no records, timestamps or audit history. Shared occurrences do not duplicate stored grid records; tables still retained in the trash are included.

Search node titles and sort by node creation date, node title or composite table identity (`node_id:offset`). Open a preview in the larger viewer; previous/next buttons browse the current page. Escape closes it. Arrow keys outside the scrollable table switch tables; inside it they retain normal scrolling. Node links and original XML downloads are available with the optional information panel. Its visibility preference is remembered in the browser.

CherryTree stores the table header as the last XML row. The gallery displays it first. Cell values are plain text, including preserved line breaks, and are never interpreted as HTML. Ragged rows are padded with empty cells. An invalid or unsupported table shows an error without stopping other previews.

## Display settings

Merge into the existing `myapp` section of `application.yml`, or change these settings under **System → Settings**, then restart:

```yaml
myapp:
  table-gallery:
    cell-width: 160
    cell-padding: 6
    font-size: 14
    cell-max-height: 160
    preview-height: 260
    preview-rows: 5
    preview-columns: 5
    viewer-rows: 200
    page-size: 12
```

Sizes are CSS pixels. Cell width is fixed across columns and tables; a narrow table does not stretch to fill its container, and a wide table scrolls horizontally. Long text wraps within each cell; cells exceeding the maximum height scroll internally. Every cell always has a **1 px solid border** in both themes. The stored `col_min` / `col_max` values are displayed as metadata but do not override the gallery settings. These settings affect this view only.

Allowed ranges: cell width 20–480; padding 0–20; font size 10–32; cell/preview height 20–800; preview data rows 1–20; preview columns 1–12; viewer data rows 1–1000; page size 1–36. Invalid YAML values fail startup rather than entering CSS unchecked.

## Limits and safety

Listings read metadata only. Visible previews load lazily, with at most two parsing requests running concurrently. Original XML is limited to 2,097,152 UTF-16 units per table. DTDs, external entities and nested cell markup are rejected by the existing hardened table parser.

Previews use the configured row/column limits and at most 240 characters per cell. The larger viewer shows at most 100 columns, the configured data-row limit and 10,000 cells including the header; cell text is capped at 20,000 characters. A clear notice marks shortened displays. Downloaded original XML is not truncated, even when the display is shortened. Tables exceeding the XML limit remain accessible through existing node tools, subject to their limits.

The XML download is the original `grid.txt` value, not a complete CTB export; separately stored width/placement metadata is not embedded into it. Offsets can change after editing note content, so table identities are not permanent independent IDs. Data/download requests carry the selected-database token and use `no-store`; stale tabs cannot silently browse another database.
