# Text, hex and archive attachment previews

Use **Inspect file** beside an attachment in the Attachments page or a rich-text
note. Existing Word/Excel previews and original downloads remain available.
The new page offers **Text**, **Hex** and **Archive contents**. It also works with
unknown filenames: use Text or Hex explicitly.

Known source/config/text extensions open as text, supported archive extensions
open as archive listings, and other files open as hex. Detection is based on the
filename; archive readers also validate the container. UTF-8 is the default
text encoding. The selector also offers UTF-16/LE/BE, Windows-1254, ISO-8859-9,
Windows-1252, ISO-8859-1 and US-ASCII. There is no heuristic encoding detection.
A leading decoded BOM is hidden. Malformed encoded bytes and non-printing
control characters are replaced only in the displayed text, with a warning;
choose a different encoding or use hex to inspect exact bytes.

Text is an escaped literal reading view: HTML/SVG/scripts/Markdown are not
rendered or executed. Recognized source extensions use the existing local
highlight.js bundle when application syntax highlighting is enabled. Missing
grammars or disabled JavaScript leave the text readable. Text is a bounded
prefix, not a full-file editor. Partial trailing multibyte characters at the
prefix boundary are omitted, and character clipping preserves code points.

Hex pages show 16-byte rows with hexadecimal offsets, unsigned bytes and ASCII
(printable ASCII only; other bytes use a dot). Previous/next preserve source
tokens. Text and hex read bounded bytes from a JDBC stream; hex skips to the
selected page. Some database drivers may buffer the whole individual BLOB even
when the application requests a bounded stream.

## Archive listings

Supported containers: ZIP, JAR, WAR, EAR, TAR, TAR.GZ and TGZ. ZIP-based DOCX,
XLSX, PPTX, ODT, ODS and EPUB containers can also be inspected. RAR, 7z and other
formats are not supported here; their raw bytes remain available as text/hex.

The table reports names, entry types, uncompressed and compressed byte sizes,
modified times in UTC, CRC32, ZIP method number, encryption flags and comments
where available. TAR links show their stored target as text. ZIP method 0 means
stored and 8 means deflate. Absent fields stay blank or use an em dash.
Names/comments/targets are limited to 512 code points for display.

This is metadata inspection, not archive validation or extraction. ZIP entries
are not inflated, including encrypted payloads; unreadable/encrypted directory
metadata can prevent listing. Header sizes/checksums are reported values and
do not certify contents. TAR.GZ is decompressed under a strict byte limit before
listing. Archive members are never written to disk, opened, followed, downloaded
individually, recursively inspected or executed. Duplicate or `../` member names
are shown literally and cannot create paths. Long listings are explicitly clipped.

## Configuration

Merge into the existing `myapp` section and restart after changes:

```yaml
myapp:
  file-preview:
    max-text-bytes: 262144
    max-characters: 200000
    hex-bytes-per-page: 512
    max-archive-bytes: 5242880
    max-expanded-bytes: 33554432
    max-entries: 1000
```

- Text bytes: 1024–1048576; characters: 1–1000000 Unicode code points.
- Hex bytes per page: 128–4096, a multiple of 16.
- Archive input bytes: 1024–20971520; expanded TAR.GZ bytes: 1024–67108864.
- Visible archive rows: 1–2000. The character and row limits are also available
  in System settings. Limits reject or clip with an explicit message; original
  downloads remain available.

The viewer is available to authenticated database readers, including read-only
sources. Every preview requires the current selected-source token, with
`Cache-Control: no-store` and `X-Content-Type-Options: nosniff`. Changing the
selected database invalidates old preview links. The feature does not change
attachment bytes, node dates/tags/properties or the CTB schema. It uses the
existing Apache Commons Compress and highlight.js dependencies; no new library
or external resource service is added.

## Checks

```text
mvnw.cmd -Dtest=FilePreviewServiceTest,FilePreviewTemplateTest,FilePreviewSecurityTest,WordPreviewTemplateTest,RichTextObjectHtmlRendererTest,AppearanceSettingsTest test
mvnw.cmd clean package
```

Manually try UTF-8 Turkish/emoji, Windows-1254, UTF-16, HTML/source files, unknown
binary data and empty files. Check hex pagination, a large text prefix, ZIP/TAR/
TGZ metadata and corrupt/oversized archives. Try both list and node links, mobile,
dark mode, USER/read-only sources and a stale tab after changing databases.
