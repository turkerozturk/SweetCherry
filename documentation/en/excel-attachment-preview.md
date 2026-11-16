# Excel attachment preview

Open **Preview Excel** next to an Excel attachment in a rich-text note or in the Attachments list. The original download link remains available. No desktop office installation, external document server or cloud upload is required.

Supported filenames: `.xlsx` and `.xls` (case-insensitive). Macro-enabled `.xlsm`, `.xlsb`, OpenDocument spreadsheets and password-protected workbooks are not supported.

Select a worksheet and click **Show sheet**. Switching sheets starts at page one. Hidden sheets are listed with a label and can be inspected by the same database readers. Row numbers and column letters preserve the workbook coordinates; empty rows inside the used range are retained. Previous/next links preserve the selected sheet. Large unused ranges may therefore produce mostly empty pages.

The read-only grid shows saved cell values and basic number/date formatting, bold, italic and alignment. Formula cells have an expandable **Formula** section. Results are the cached values last saved by the workbook's author: they can be absent or outdated. An XLSX formula without a saved result is shown as an em dash (—). Formulas are never evaluated, macros are never executed, and workbook hyperlinks, external sources and images are not fetched. Charts, pictures, comments, merged regions, exact fonts/colors and full Excel layout are not rendered. Download the original to see the complete workbook.

All cell and file text is HTML-escaped. The preview is not an editor or a converter and never writes a workbook or changes attachment bytes, timestamps, node properties, bookmarks or database structure. The selected-database session token is checked before reading attachment data; authenticated readers can use the preview for read-only databases too. Responses are marked `no-store`.

## Limits

`myapp.excel-preview` in `application.yml`:

| Key | Default | Allowed range |
| --- | ---: | ---: |
| `max-file-bytes` | 5242880 (5 MiB) | 1024–20971520 |
| `max-expanded-bytes` | 16777216 (16 MiB) | 1024–67108864 |
| `rows-per-page` | 100 | 1–200 |
| `max-columns` | 50 | 1–100 |
| `max-cell-characters` | 2000 | 1–4000 |

The last three display settings are also available under System settings. Configuration changes take effect after restarting the application. The size limits remain configuration-file settings.

File size is checked before selecting the BLOB, with a bounded read afterwards. XLSX ZIP contents are scanned in memory before POI parsing: total uncompressed data is bounded, duplicate entry names are rejected, and at most 2048 entries are accepted. POI's own ZIP protections remain enabled. Workbooks are parsed per request; sheet/page selection reparses the file. At most 256 worksheets are accepted. These conservative limits can reject otherwise valid workbooks; the original download remains available.

Only the configured first columns of the current row page are rendered; long cell text and formulas are shortened. A notice appears when content on that page is clipped. Workbook styles and hidden content are not an exact Office-compatible presentation.

No dependency or database schema change is required: this feature uses the Apache POI already present in SweetCherry.
