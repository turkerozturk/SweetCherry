# Word attachment preview

Open **Preview Word** next to a `.docx` or `.doc` attachment in a rich-text note or the Attachments list. The original download remains available. No Office installation, cloud upload or external document server is required.

This is a read-only reading view, not a Word-compatible page renderer or editor. The preview never writes a document, changes the attachment bytes or changes node timestamps, properties, bookmarks or database structure.

## What is shown

- DOCX body paragraphs and tables in their stored order.
- Basic DOCX run formatting: bold, italic and underline; basic paragraph alignment and common heading styles.
- Lists indicated by simple bullets, without reconstructing numbering.
- Hyperlink display text, without active document hyperlinks.
- Table cell text, including nested table text flattened inside the containing cell. Table borders are always visible and wide tables scroll horizontally.
- Legacy Word 97–2003 DOC body paragraphs and table text, without original run formatting.

Images, drawings, embedded objects, exact fonts/colors, page breaks/layout, merged table geometry, headers/footers, footnotes, comments and revision history are not reproduced. The preview should not be used to review tracked changes or establish the authoritative appearance of a document. Older Word formats and unusual features may not be supported. An empty or image-only body may have little or no visible text. Download the original for the complete document.

Macros and fields are not executed. External links, linked images and remote resources are not fetched. Untrusted document text is HTML-escaped. File type is checked using both the filename and the container format. Password-protected, corrupt, unsupported or oversized files show a localized message while keeping the original download available. Macro-enabled `.docm`, `.dotm`, `.dotx`, ODT, RTF and renamed non-Word files are not supported.

The selected-database session token is required before reading an attachment. Authenticated database readers can use the preview, including for read-only databases. Responses use `Cache-Control: no-store`.

## Configuration

Settings under `myapp.word-preview` in `application.yml`:

| Key | Default | Allowed range |
| --- | ---: | ---: |
| `max-file-bytes` | 5242880 (5 MiB) | 1024–20971520 |
| `max-expanded-bytes` | 16777216 (16 MiB) | 1024–67108864 |
| `max-characters` | 200000 | 1–1000000 |
| `max-blocks` | 1000 | 1–2000 |
| `max-table-rows` | 100 | 1–200 |
| `max-table-columns` | 20 | 1–50 |

The four display limits are also exposed in System settings. Restart the application after configuration changes. The file and expanded-size limits remain configuration-file settings.

Text limits count Unicode code points. A notice appears when body content, tables or text are clipped. Characters in table cells count toward the same document-wide text budget. Each body paragraph or table counts as one block.

File size is checked before selecting its BLOB, with a bounded read afterwards. DOCX ZIP entries are scanned without extraction before POI parsing: at most 2048 unique entry names and the configured aggregate uncompressed size are accepted. POI's own ZIP protections remain enabled. Documents are parsed per request. Conservative limits can reject valid but complex documents; download is unaffected.

This feature uses the existing POI OOXML dependency and adds the matching `poi-scratchpad` module for legacy DOC. Its license notices and dependency inventory are included in the distribution notices.
