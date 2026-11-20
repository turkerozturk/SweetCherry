# Draw.io attachment viewer

Open **View diagram** for a `.drawio` file in Attachments or a rich-text attachment link. The original download remains available. Choose any of up to 128 pages; zoom from 2% to 400%, reset to 100%, fit the current page, scroll, or toggle fullscreen. Ctrl/Command + wheel zooms inside the diagram; normal wheel scrolling is unchanged. Fullscreen uses the browser API with an expanded-panel fallback; Escape closes the fallback.

Normal and raw-DEFLATE/base64 compressed mxfile pages and standalone mxGraphModel documents are supported. The official Draw.io static viewer is bundled locally at a pinned commit; neither diagrams nor external images are sent to Draw.io or another service. No Nasdanika/Java renderer or new Maven dependency is needed.

The preview is read-only. HTML labels become plain text with line breaks; links, executable content, custom XML stencils, external fonts/backgrounds and resource URLs are removed. Images are replaced by rectangles showing their filenames (including embedded images). Ordinary vector shapes/edges, groups, geometry and safe color/line styles are preserved. Specialized/custom shapes, icon fonts, math and rich text can differ from the original. Download and open in Draw.io when exact fidelity is needed.

A sandboxed opaque-origin iframe renders only the sanitized graph model. Its CSP blocks connections, external images/fonts, frames, forms and navigation privileges; only nonced local scripts and data images are allowed. Parent/frame communication uses a dedicated MessageChannel; the renderer receives no session token or database access. The frame URL itself requires the authenticated session and current tenant token, as do attachment data/download requests. USER and read-only sources can view diagrams. No write endpoint is introduced.

Limits: 8 MiB source, 2 MiB per expanded/sanitized page, 16 MiB cumulative expansion/output, 128 pages, 4,000 cells per page and 20,000 cells per document. Labels, XML nesting, geometry coordinates and concurrent readers are also bounded. Invalid/oversized documents fail explicitly rather than silently dropping pages. Blank documents/pages still retain their page selection. Intentional text and attachment bytes remain unchanged in CTB.

Manual sample: attach `documentation/examples/drawio-viewer-demo.drawio` to a node; its three pages exercise normal shapes, compressed XML and an external-image placeholder.
