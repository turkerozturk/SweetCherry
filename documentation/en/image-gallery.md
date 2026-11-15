# Read-only image gallery

Select a database and open **View Mode → Image gallery**, or `/image-gallery`.
Both reader and administrator accounts can use it, including on read-only databases.
No notes, image rows, timestamps or audit records are changed.

- Browse 36 image records per page, sorted by node creation date, node title or image identity.
- Search node titles, stored filenames and stored links. Search text is treated literally.
- Choose one exact node tag to filter images by their owning content node. Case, Unicode and punctuation are preserved; `work` does not match `workshop` or `Work`. The tag filter combines with text search and survives sorting/pagination. Tags are offered from image-owning nodes; shared occurrences use their content master’s tags without duplicating images.
- Open a thumbnail to inspect a larger preview. Use the previous/next buttons or Left/Right keys within the current page; Escape closes the viewer.
- Use **Fit / zoom** to switch between fitting the preview and its natural pixel size. The preview has a maximum edge of 2560 pixels; download for the original resolution.
- Show or hide image information in the gallery or viewer. This display preference is remembered in the browser.
- Follow the node-title link to its content. Metadata includes node creation date, byte size, stored filename/link and image time where present.

CherryTree stores these records in the `image` table, with the composite identity `node_id` + `offset`. There is no separate global image ID. The offset can change when node content is edited; the gallery does not introduce persistent IDs. Shared occurrences do not duplicate an image row in this list. Pictures still stored in the trash are included.

The gallery includes embedded images (empty filename) and named PNG/JPEG/GIF/BMP/WebP/TIFF attachments. Anchors, LaTeX sources, SVG and other attachments are excluded. Named attachments are selected by extension; invalid or unsupported data gets a preview-unavailable message. WebP or other formats without an installed Java ImageIO decoder may be downloadable without a preview. Animated images use the first frame.

Metadata listing does not load binary payloads. Visible thumbnails are requested through a bounded queue; the server decodes at most two previews concurrently. Thumbnails have a maximum edge of 320 pixels. Payloads over 64 MiB and previews over 100 million source pixels are rejected. These are per-image limits, not changes to upload settings. Oversized originals can still be accessed through the existing node/attachment tools, subject to their own limits.

Preview responses are re-encoded PNGs; stored links are shown as text and are never fetched. Original downloads are attachment responses with safe generated filenames and a signature-derived extension (or `.bin` for unrecognized bytes). Requests carry the selected-database token and use `no-store`; an old tab cannot silently load image data from a newly selected database.
