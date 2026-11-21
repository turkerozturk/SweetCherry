# Draw.io attachment viewer

Open **View diagram** for a `.drawio` file in Attachments or a rich-text attachment link. The original download remains available. Choose any page within the configured budget (512 pages by default); zoom from 2% to 400%, reset to 100%, fit the current page, scroll, or toggle fullscreen. The ordinary wheel zooms around the pointer inside the diagram; hold Shift for native scrolling. **Fit horizontally** fills the available width while tall content remains vertically scrollable. Fullscreen uses the browser API with an expanded-panel fallback; Escape closes the fallback.

Normal and raw-DEFLATE/base64 compressed mxfile pages and standalone mxGraphModel documents are supported. The official Draw.io static viewer is bundled locally at a pinned commit; neither diagrams nor external images are sent to Draw.io or another service. No Nasdanika/Java renderer or new Maven dependency is needed.

The preview is read-only. HTML labels retain safe formatting, block/line breaks, horizontal/vertical alignment and wrapping; links, executable content, custom XML stencils, external fonts/backgrounds and resource URLs are removed. Bundled Draw.io library icons are resolved locally; other images are replaced by rectangles showing their filenames (including attachment-embedded images). Ordinary vector shapes/edges, groups, geometry and safe color/line styles are preserved. Specialized/custom shapes, icon fonts, math and rich text can differ from the original. Download and open in Draw.io when exact fidelity is needed.

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

## Compatibility and diagnostic errors

Draw.io graph roots and layers may use generated identifiers instead of literal `0`/`1`. The viewer now infers the hierarchy and maps cell/parent/connector/port references to safe internal rendering IDs. UserObject wrapper IDs take precedence over inner mxCell IDs, matching Draw.io serialization. Source attachment bytes are unchanged. Empty diagram pages and empty graph models remain selectable as blank pages.

When parsing fails, the page shows a localized reason, a stable code such as `compression`, `graph_root`, `missing_parent` or `geometry_number`, the 1-based page number and XML line/column when available. The console warning includes diagnostic codes/page/location, without the filename, cell labels or raw XML. `invalid` means this viewer could not parse a structure, not proof that the file is corrupt. Limit errors still name the adjustable setting.

Regression checks include two unmodified upstream templates with generated root IDs (source and licence are recorded under `src/test/resources/drawio/UPSTREAM.md`). During development, 155 upstream diagram templates parsed successfully; the separate catalogue index is not a diagram. This verifies parsing compatibility, not complete browser rendering fidelity or the contents of any private file.

## Offline shape libraries

The bundled resources now include all 204 stencil sets from the pinned Draw.io version (9,089 unique shape identifiers), plus 1,910 original PNG/SVG library icons. The existing viewer also supplies JavaScript shapes such as Infographic. This covers Standard/Basic/Flowchart, Cabinets, EIP, networking and the other bundled families. These are viewer resources, not an editor palette or a new Java dependency. General geometric primitives remain available without a separate switch.

All 75 family switches default to **enabled**. Filter **System settings** for `drawio` or a family such as `cabinets` to enable/disable it, save and restart. The complete key list is present in `application.yml`; merge overrides into the existing mapping, for example:

```yaml
myapp:
  drawio-preview:
    shape-libraries:
      cabinets: true
      infographic: true
      eip: true
      clip-art: true
      cisco: true
      azure2: false
```

Definitions remain ready in `static/js/drawio/stencils.min.js` and `catalog.js`. The selected stencil set is decoded/registered on first use, rather than constructing all shapes for every page. Icon packs are separated by family in `static/js/drawio/icons/`; disabled icon packs are not included in the frame. All paths are fixed local resources. No drawing or resource is sent to the internet. An arbitrary external image URL does not become permitted just because icons are supported.

Missing/disabled names appear above the diagram and in the **browser developer console**, prefixed `[SweetCherry Draw.io]`. Diagnostics contain identifiers only, are deduplicated, and are capped at 100 per page render. Unsupported names use the normal rectangle fallback; other cells continue rendering. Custom inline XML stencils, separately supplied libraries, external images, newer vendor shapes and unsupported icon fonts are still outside this catalogue. This is not a guarantee of pixel-identical rendering of every Draw.io diagram.

Manual sample: attach `documentation/examples/drawio-library-shapes.drawio`, view its Cabinets/EIP/Flowchart/Infographic and local icon cells, then disable a named family, restart and reopen. The sample also intentionally includes a missing definition and an external bitmap placeholder.

Vendor code and artwork terms differ. The exact pinned icon/stencil usage terms, third-party ownership/trademark notice, original asset hashes and source revision are recorded in `distribution-notices` and `static/js/drawio/SOURCE.txt`. The additional asset terms include restrictions relating to Atlassian products/marketplace; these resources are not labelled as unrestricted Apache/GPL artwork.

For maintainers, regeneration is optional and happens only at development time. Use a local Draw.io clone checked out at the revision in `SOURCE.txt`, with `src/main/webapp/js/stencils.min.js` and the complete `src/main/webapp/img/lib/` present. Run `python packaging/drawio/generate-packs.py /path/to/drawio` (Python 3 and Node.js, no extra packages). Review licence changes before changing the pinned revision. Generated packs preserve original image payload bytes; the generator rejects active/external SVG references and incomplete input. Normal SweetCherry builds and runs do not download these assets or need Python/Node.js for the viewer.

## Colors, missing names and local replacements

HTML label foreground/highlight colors now preserve safe named/hex/RGB/RGBA/HSL colors, including `background` used as a single color. Hex colors are not treated as numeric sizes. Active CSS, resource URLs and scripts remain removed. Cell-level font/label background colors are preserved as well.

**Show missing shape and image names** controls the placeholder text inside the diagram and the diagnostic summary above it. It defaults to on, and the preference persists in browser local storage across pages and visits. Hiding these names keeps the original captions and the placeholder geometry; it does not delete cells. The browser console continues to report missing identifiers. Unknown vector shapes show their shape identifier; unresolved library icons show their library path. Other unavailable images show a marked `[Image: filename]`, their original shape name/caption when available, or `missing source`. The original external image URL is never fetched. Hover a server-generated image marker to find its stable `image.<SHA-256>` mapping key; missing image-cell keys are also logged in the browser console. The hash is calculated from the exact source string, including query parameters, without exposing that URL in the sanitized model.

An administrator may provide **PNG/JPEG raster replacements** on the computer running SweetCherry. This supports missing shape/image cells; it does not install executable JavaScript, XML stencil packs, SVG overrides or images inside HTML labels.

1. Create a directory such as `C:/SweetCherry/drawio-images` and place your PNG/JPEG files there.
2. Create UTF-8 `images.properties` inside it, for example:

```properties
shape.mxgraph.example.missing=appliance.png
# For an unavailable image, copy its exact key from the marker tooltip/console:
# image.<64-character SHA-256 key>=screenshot.jpg
```

3. Set **Draw.io local PNG/JPEG mapping directory** in System settings, or merge the following into the existing YAML mapping:

```yaml
myapp:
  drawio-preview:
    local-images-directory: 'C:/SweetCherry/drawio-images'
```

4. Restart SweetCherry and reopen the diagram. Restart after changing files/mappings. An empty directory setting disables replacements. Use paths relative to this directory in `images.properties`; absolute paths, traversal and symlinks escaping it are rejected. Never put a URL in the filename field. A mapped image is embedded locally as a data image, keeping the caption and using the original cell geometry. Explicit local replacements take precedence over the matching bundled/fallback shape; they are global to this SweetCherry instance, including its selected databases. Keep replacements in an application-owned folder containing only the images you intend to make available to its readers.

The manifest is limited to 64 KiB/128 entries; each raster is at most 2 MiB and 16 million pixels; totals are capped at 8 MiB and 32 million pixels. Image signatures/dimensions are checked. Invalid entries are skipped with a bounded-key server warning; an invalid/unavailable manifest disables the pack. Files are read at startup, not downloaded or requested by arbitrary paths from a browser. The source attachment/database is never overwritten.

The `Colors and placeholders` page in `documentation/examples/drawio-library-shapes.drawio` exercises colors, unknown names, missing image sources and `shape.mxgraph.example.missing` for a local replacement.

## Privacy boundary

This SweetCherry preview uses fixed local vendor scripts/resources, a sanitized graph and an opaque sandbox. Its frame policy retains `connect-src 'none'`, `img-src data:`, `font-src 'none'` and nonced scripts. It does not include the online editor, cloud storage, AI, collaboration or server conversion. It sends no diagram content to Draw.io and does not fetch external media referenced by diagrams. This does not describe every feature of the separate online Draw.io editor: its current privacy policy lists server-processed functions such as PDF/image export, collaboration and AI (https://www.drawio.com/trust/).

The Windows CI failure in the first library tests was a CRLF fixture-reading bug: splitting `groups.txt` on only LF left a CR on each name. The fixture now handles Windows line endings. This was reproduced using an actual `core.autocrlf=true` checkout; the fix does not alter the unmodified vendor bundles.
