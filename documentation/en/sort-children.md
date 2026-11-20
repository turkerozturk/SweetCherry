# Sort immediate children

Display a node, then choose **Application → Sort Children…**. The expandable submenu offers Created, Modified and Name, first descending (↓), then ascending (↑). Confirm the node name, child count and requested order in the popup. This confirmation always applies independently of creation/duplication/deletion preferences.

Only direct child occurrences are reordered. Descendant order, node text, images, bookmarks, master references and the children's timestamps remain unchanged. Shared children use their master's name/dates as sort keys while keeping their own occurrences. A shared parent can have its own children sorted. Dates use stored creation/last-save values (missing values count as zero); name sorting ignores case and is lexicographic, without numeric or locale-specific collation. Equal keys retain their existing sibling order.

ADMIN access, a writable connection, the current tenant token and CSRF are required. Locked parents, protected helper containers and their descendants are rejected. A database root or a page without a selected content node cannot be sorted. Close open editors before sorting. Fewer than two children or an already-sorted list is a no-op. A changed list/name/date invalidates an open confirmation; reload and retry.

The changed order is written to existing `children.sequence` values in one transaction, without changing the schema. Only the parent's modified time is touched after an actual order change. CTBLOGS records `tree.sort` when enabled. The selected content and navigation refresh after success; in split view, the active reading pane supplies the selected node. This writes the order into CTB, so CherryTree sees it too.
