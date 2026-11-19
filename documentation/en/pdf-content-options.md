# PDF content and layout

The node PDF options page, including export with descendants, offers two independent selections:

- **Internal links:** show the original link labels (default), or include the linked node contents at each link's position.
- **Node layout:** continue on the next line (default), or start each node on a new page. This also applies to included linked contents. Ordinary page overflow still creates new pages.

Options are remembered in the current session after a successful download and shared between single-node and subtree PDF settings. They are independent of the browser's linked-content toggle. A selected table of contents remains separate from the content pages; its links and the PDF outline work with either layout.

Included text starts inline at the original link position, without a generated source name or "Open source" link. Actual line breaks and blank lines in the target remain. For example, a linked filename can directly complete `Get-FileHash -Algorithm SHA256 .\`. There is no additional source-visibility option for PDF; this source-free representation is always used when including linked contents. Tables, lists and codeboxes keep their structured layout, and explicitly selecting a new page per node still starts included nodes on new pages.

Referenced rich-text contents retain the selected images, codeboxes, tables and LaTeX objects. Plain text, code, Markdown and sanitized HTML targets can be included, but only rich-text targets are traversed for more links. A link with a target anchor includes the whole note; same-node anchor navigation stays local. Each copy has separate anchor IDs.

The existing `myapp.node-fragments` depth, inclusion, source, HTML and object limits and visibility checks also apply to PDF inclusions. Inclusion attempts and the 4,194,304-character inclusion budget are shared across an entire PDF job, including all subtree nodes. The existing `myapp.pdf` limits also apply. Cycles (including aliases), missing or protected helper targets and exceeded inclusion limits leave the original label in the PDF. Internal links outside the exported document retain the existing PDF text-only representation. No external resources are fetched and no note content is changed.
