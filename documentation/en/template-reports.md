# Template reports — ordered fields and diagnostics

The four existing routes remain available:

| Route | Output |
| --- | --- |
| `/cherrytemplatenode/<id>` | Detailed HTML list |
| `/cherrytemplate-type-1/<id>` | Comparative HTML table |
| `/cherrytemplatenode/pdf/<id>` | A4 landscape PDF with template header and page totals |
| `/cherrytemplatenode/xlsx/<id>` | XLSX table |

No CTB schema, stored notes, tags or timestamps are changed by reading/exporting these reports. [Custom single-record layouts and placeholders](record-layouts.md) are also available.

## Membership and fields

Project records still use exact equality of the **whole `node.tags` field** with the template node name. This does not become token-based tag membership, and shared record occurrences are not added as extra project rows in this step. Records sort by name, then ID; ISO `yyyy-mm-dd` titles therefore remain chronological.

Template text defines one variable per line: `key<TAB>display label`. The label is optional; a missing/empty label uses the key. Fields keep their definition order in all four outputs. Empty lines and `#` comments are ignored. For compatibility, lines whose **first character is an ASCII space** remain disabled as in the old template parser. This legacy rule concerns template field definitions; the main setup-node parser retains its own documented behavior.

An empty key is invalid. Repeated active keys are excluded entirely (including their first occurrence), rather than choosing one label silently. HTML/PDF show their line numbers and keys; XLSX adds a Definitions sheet. The database content is not rewritten.

Values come only from immediate children of the record whose names exactly match the variable key. Other children are omitted. Hierarchy and links use occurrence IDs from `children`; content comes from the real master for a shared child. Two shared children of the same master are still two matching occurrences and therefore ambiguous.

| Status | Meaning | Output |
| --- | --- | --- |
| Value | Exactly one matching child with nonempty content | Rendered HTML/PDF, plain text in XLSX |
| Missing field | No matching child | Localized status |
| Empty content | One matching child, empty/whitespace content | Separate localized status |
| Multiple matching fields | More than one matching child | Status and occurrence IDs; no arbitrary value |

PDF retains the existing column proportions (5% number, 20% name, 75% variables; half the page for values), two-word wrapping for names/labels, and page totals. Missing/empty cells reserve writing space and have a small status caption. XLSX uses display labels, preserves newlines, stores literal strings rather than formulas, freezes identifier columns/header, enables filters and highlights unresolved values. Its usual Excel row/column/cell limits still apply; notes are not silently clipped.

HTML rich text uses the existing reader renderer; Markdown is rendered; plain/code content is escaped and preserves line breaks. XLSX uses the shared text conversion (including object descriptions), not an XML parser applied to HTML. Attachment links in HTML carry the current connection token.

## Screen controls

Both HTML layouts have multiword record search (all words), an incomplete/ambiguous-only checkbox and a Go to today button. Search checks the displayed row text, including breadcrumb/ID/value text. These are local screen filters: PDF/XLSX downloads always include **all records**. This is stated next to the controls.

For an exact `yyyy-mm-dd` record title matching the browser's local date, the first visit in the tab/connection/layout scrolls to the record; returning or reloading does not repeatedly jump. The explicit button can reveal it after filtering. TODO/FOCUS color emphasis remains static. Existing `dailyalarm` fields on dated records for the next 90 days are available in a collapsible list instead of an automatic overlay.

## Manual checks

1. Define `z<TAB>Zed`, then `a<TAB>Alpha`; check order in both HTML layouts, PDF and XLSX.
2. Add `#disabled`, a leading-space legacy comment, a blank line and a repeated `z`; check comments disappear, repeated `z` is excluded and diagnostics are visible in every output.
3. Create records with absent `a`, empty `a`, populated `a`, and two `a` children. Verify four distinct states, clickable duplicate IDs and no chosen duplicate content. Repeat with shared children of one master.
4. Use rich text with objects, Markdown, multiline plain/code text, `<script>` as literal plain text and `=1+1` as note text. Verify formatting/escaping and that XLSX stores the last example as text.
5. Search multiple words and use incomplete-only in both layouts. Download files and confirm they contain all records. Create a dated record for today and verify the automatic/manual scroll rules.
6. Check EN/TR labels, PDF write-in space/header/page totals, XLSX labels/frozen panes and report access as USER/read-only. Close the source or switch it in another tab: stale marked links must not query the new source.

Normal PDF images are bounded to the value column and placed on separate lines to avoid renderer overlap. Inline LaTeX and attachment icons retain their existing flow.

[Custom single-record HTML layouts](record-layouts.md)
