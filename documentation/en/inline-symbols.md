# Unicode characters and inline icons

Use **User → Symbols / icons…** to browse/filter and copy symbols. In the in-place node title form, Node Properties name field and rich-text editor toolbar, the same button can also **Insert** at the original cursor/selection. The picker never saves automatically; save the title/properties/content normally. Closing/Escape restores the editing selection.

The catalogue contains 236 curated Unicode characters (emoji, arrows, status, math and other common symbols), 373 existing CTBICONS PNGs, and 2,157 Free Font Awesome style/icon combinations from the installed 7.3.0 WebJar (Solid/Regular/Brands). Filter by English/Turkish keywords, names, class name or a Unicode `U+` code; select a group or page through the bounded results. The Unicode collection is a practical selection, not the whole Unicode standard. A glyph's appearance/availability depends on the operating system's fonts.

## Stored representation

Unicode is inserted/copied as the actual character, for example `🙂`, `✓` or `→`. Icons use plain-text display markers:

```text
Screenshot {{fa-solid fa-image}}
Favourite {{fa-regular fa-heart}}
Source {{fa-brands fa-github}}
Camera {{ctb:ct_camera}}
```

You may type these by hand or select/copy/insert them with the picker. The short Font Awesome prefixes `fas`, `far`, `fab` and catalogue-provided name aliases are also recognized. CTB names are resolved case-insensitively to actual packaged filenames (e.g. `{{ctb:CT_CAMERA}}`). Only exact installed names/families are accepted; arbitrary classes, paths, URLs and HTML are not supported. Unknown expressions remain literal text. These `{{...}}` forms do not activate the rich-text editor's separate `[[node name]]` internal-link resolver; other template expressions are not interpreted. Triple/nested brace expressions are left alone.

Icons are expanded in web node-name displays: tree/mobile readers, breadcrumbs, bookmarks, advanced search, attachments/galleries, calendars, TODO/reminders, reports and node-target pickers. The standalone Mermaid and Markmap maps expand names before layout; the link graph uses local canvas label images. Updates follow asynchronous navigation and title changes. Normal DOM icons inherit text size; Font Awesome inherits text color, CTB PNGs keep their own colors. Native browser tab titles, tooltips and select options cannot embed icons, so they use a readable `[Icon label]` instead of the marker. Input/source fields keep the stored text.

Node, subtree, report and filtered-list PDFs expand the same allowlisted markers. Solid/Regular/Brands glyphs use embedded local fonts; CTB icons use embedded PNGs. Subtree headings and the contents list are included. Code/preformatted examples and literal rich grids remain unchanged; explicit node-name cells in tables are decorated. Unicode characters remain text and still depend on the available browser/PDF fonts; this change does not add an emoji artwork collection.

Rendering never edits the database: no image-table rows or embedded objects are created. Known icon markers are removed **only when a node title becomes a filesystem/download filename**, including text-tree ZIP folders/files, PDF filenames and mind-map download filenames. Existing empty-name fallbacks and collision handling still apply. Real Unicode and unrelated `{{expressions}}` remain intact. The CTB contents, XML, text/mind-map contents, search/sort semantics and CherryTree desktop representation preserve the stored names and markers.

Scriptless record-view frames use embedded local PNGs for name placeholders; their existing data-only CSP is retained. Node names inserted by a record placeholder are not evaluated again as report template expressions.

## Editing and literal examples

While editing, markers stay visible as text so model offsets, formatting, selection and existing content remain stable. The picker preview shows the actual icon. Rich-text insertion uses the normal run/undo model and is one undo/redo step; protected embedded objects and changed/busy content cannot be overwritten through the picker. A title insertion that would exceed the existing 255 UTF-16-unit name limit is rejected.

Keep a marker consistently formatted (adjacent identical spans are supported): splitting it across bold/color/link spans can prevent recognition. Reinsert it with the picker if necessary. Plain/code/Markdown/HTML-node content is not opted in; code/preformatted/monospace spans and table/grid cells inside rich content are also kept literal. Use a code/monospace span when writing an example marker that should not become an icon. Editing surfaces and the picker itself are never decorated.

Copy is available to readers as a utility; actual saving remains subject to the existing ADMIN/writable/current-tenant checks. Copy uses the browser clipboard API when available, otherwise attempts the normal copy command; if unavailable it selects the value for Ctrl+C. The picker does not read the clipboard, fetch outside resources or load external icon/font services. Existing local WebJar fonts and CTB images are reused. The source and licence notices remain in `distribution-notices` and `static/js/symbols/SOURCE.txt`.

## Manual checks

1. Edit a node title, place the caret or select text, insert a Unicode symbol or icon marker and save; check tree/header/mobile names.
2. In rich-text editing, insert each marker above and a Unicode character; undo/redo, save and inspect the reading view.
3. Use Copy from User menu in a read-only session, and test filtering/groups/previous/next and Escape.
4. Put the same marker in a code box or monospace/code example: it should remain text. Unknown names and ordinary template expressions should remain unchanged.
5. Click an inline CTB icon inside a content link: follow the link normally, without opening the PNG as an attachment.

Regeneration is a development-only step: `python packaging/symbols/generate-catalog.py /path/to/font-awesome-7.3.0.jar`. It uses the version pinned in `pom.xml` and existing packaged CTB filenames, requires only Python 3, and performs no downloads. It also writes the server allowlist. Add `--pdf-fonts` to regenerate the three plain TrueType copies from the pinned WebJar WOFF2 fonts; that development-only conversion requires `fonttools[woff]` (Brotli). Normal application builds/runs use the committed catalogue.

Additional checks: inspect node-name markers in both mind maps, graph, bookmarks, search and attachments; export a node/subtree/list PDF with each style and a CTB marker; export text-tree ZIP and mind-map files from an icon-only name and from colliding cleaned names. PDF inline icons are limited to 5,000 per document.
