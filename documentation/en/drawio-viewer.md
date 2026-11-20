# Draw.io attachment viewer

Open **View diagram** for a `.drawio` file in Attachments or a rich-text attachment link. The original download remains available. Choose any page within the configured budget (512 pages by default); zoom from 2% to 400%, reset to 100%, fit the current page, scroll, or toggle fullscreen. The ordinary wheel zooms inside the diagram; hold Shift for native scrolling. **Fit horizontally** fills the available width while tall content remains vertically scrollable. Fullscreen uses the browser API with an expanded-panel fallback; Escape closes the fallback.

Normal and raw-DEFLATE/base64 compressed mxfile pages and standalone mxGraphModel documents are supported. The official Draw.io static viewer is bundled locally at a pinned commit; neither diagrams nor external images are sent to Draw.io or another service. No Nasdanika/Java renderer or new Maven dependency is needed.

The preview is read-only. HTML labels retain safe formatting, block/line breaks, horizontal/vertical alignment and wrapping; links, executable content, custom XML stencils, external fonts/backgrounds and resource URLs are removed. Images are replaced by rectangles showing their filenames (including embedded images). Ordinary vector shapes/edges, groups, geometry and safe color/line styles are preserved. Specialized/custom shapes, icon fonts, math and rich text can differ from the original. Download and open in Draw.io when exact fidelity is needed.

A sandboxed opaque-origin iframe renders only the sanitized graph model. Its CSP blocks connections, external images/fonts, frames, forms and navigation privileges; only nonced local scripts and data images are allowed. Parent/frame communication uses a dedicated MessageChannel; the renderer receives no session token or database access. The frame URL itself requires the authenticated session and current tenant token, as do attachment data/download requests. USER and read-only sources can view diagrams. No write endpoint is introduced.

Resource budgets are configured under `myapp.drawio-preview` in YAML and **System settings**; changing settings requires a restart. The defaults below replace the former fixed budgets (8 MiB source, 2 MiB/page, 128 pages, 4,000 cells/page). Existing external YAML overrides bundled defaults. A limit error names the exact setting; adjust it and restart. URI-encoded inflation is allowed up to three times the decoded page budget, then the actual UTF-8 page size is checked; totals count each page once rather than summing input and output twice. Structural validation and active-content protections remain in force.

Manual sample: attach `documentation/examples/drawio-viewer-demo.drawio` to a node; its three pages exercise normal shapes, compressed XML and an external-image placeholder.

Merge these keys into the existing `myapp` mapping; do not add a duplicate section.

```yaml
myapp:
  drawio-preview:
    max-file-bytes: 16777216
    max-page-bytes: 16777216
    max-total-bytes: 134217728
    max-pages: 512
    max-cells-per-page: 20000
    max-total-cells: 200000
    max-label-characters: 100000
    max-xml-elements: 500000
    max-xml-attributes: 128
    max-xml-depth: 128
    max-parent-depth: 256
    max-geometry-depth: 16
    max-geometry-points: 10000
    max-coordinate: 1000000
```

The shipped defaults permit 16 MiB sources, 16 MiB per expanded page, 128 MiB total, 512 pages, 20,000 cells/page and 200,000 cells/document. The browser receives the configured page budget; its previous 128-page/2-MiB checks no longer reject valid server output. Upper bounds shown in System settings prevent accidental unbounded allocations. Unsupported document structures may still fail independently of file size.
