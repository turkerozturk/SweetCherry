# Import Freeplane / FreeMind maps

Open a writable data source as ADMIN, then choose **Application → Import Freeplane / FreeMind** (also linked from Mindmap Export). Upload an `.mm` file, review the title/count and conversion notices, then confirm. The map root becomes one new root-level branch **after all existing roots**. Child order and hierarchy are preserved. Nothing is written during preview; Cancel discards it. Keep a backup before importing.

A preview is valid for ten minutes, the current session and the current data-source view. It can be confirmed only once. Switching away and back invalidates the old preview. Confirmation rechecks writable access and inserts the whole tree in one transaction. Existing notes, bookmarks and source files are unchanged. New node IDs are allocated above IDs in both node and children tables; importing the same file again intentionally creates another copy.

## Data conversion

- Node titles come from `TEXT` or HTML `richcontent TYPE="NODE"`. Empty titles become `(Untitled)`. Titles longer than 255 Unicode characters are shortened; the full title is retained in the body.
- `NOTE` and `DETAILS` become Plain Text content, with section labels when both are present. Paragraphs, line breaks, list items and link destinations remain readable. SweetCherry's exported `<pre>` note text retains its whitespace exactly.
- Direct title `COLOR`, explicit font boldness and valid Created/Modified dates are retained. Freeplane millisecond timestamps become CTB seconds. Missing/invalid dates use the import time. Inherited Freeplane styles are not resolved.
- Recognized `ctbicons/<canonical-name>.png` references restore the corresponding **Node Properties icon** without loading an image. New SweetCherry exports with icons also carry `sweetcherry.node-icon` metadata to distinguish the node icon from the root's database emblem. Older SweetCherry exports are recognized through their export comment and hook order. Keep that comment for legacy root-icon disambiguation.
- Freeplane's built-in icons are omitted. Map positions, folding, fonts/sizes, edges, clouds, reminders and scripts are not reproduced. Formulas remain literal titles. Encrypted payloads cannot be decrypted; visible content is retained with a warning.
- Node links, ordinary attributes, arrow-link destination IDs and external-object/image references are retained as inert text when available. Freeplane IDs/links are not converted to live SweetCherry internal links. No external file, map, image or URL is fetched. Styles and hooks never become child nodes.

## SweetCherry export → import

To retain note text, enable **Include note content** when exporting. Choose an icon-reference or icon-bundle mode to retain icons, and include colors to retain title colors. If an export is a ZIP, extract and upload **only the `.mm` file**; do not upload the ZIP. The `ctbicons` folder is unnecessary for import.

This restores the information actually carried by the MM: hierarchy, names, optional flattened note content, title properties, dates and exported icons. It is not a full CTB backup: the export does not carry original rich-text/Markdown/code syntax, binary objects, tags, bookmarks, read-only flags or shared-node relationships. Shared occurrences import as independent nodes. An all-database export has a synthetic database root, which is imported as the new container branch. A depth-limited export can only import the included levels. Use CTB export/import or CTB backups when those additional database details must survive.

## Limits and access

The upload is limited to 1 MiB, 10,000 nodes and 128 child levels. The servlet's upload limits may impose an additional lower bound if customized. DTDs, external entities, malformed XML and maps without exactly one direct root node are rejected before any write. ZIP extraction and arbitrary server file paths are not supported. ADMIN, CSRF and current-data-source checks apply to every import POST. Writes require transactional target tables (InnoDB for node/children on MariaDB/MySQL).

The regression fixture `freeplaneFunctions.mm` is the user-supplied Freeplane functions map: 69 actual nodes, HTML titles/notes/details, styles, icon references, links and encrypted content. Tests verify that all 69 actual nodes import and style definitions do not become notes.
