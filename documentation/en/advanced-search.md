# Advanced search

`/nodesadvancedwithbinding` retains POST submission and database-side raw `node.txt` search. It does not render contents, change the CTB schema, or update note timestamps. Desktop and mobile share the same form.

- **Search titles and raw content:** All words requires every term somewhere in the title or raw content; terms may span the two fields. Any word requires at least one. Exact phrase requires a contiguous phrase within either field. Word matching uses substrings, not word boundaries.
- **Tags:** Type space-separated tags or select existing tags from a searchable list, then Search. All/Any matches whole tags (`java` does not match `javascript`). Contains text retains the former substring behavior.
- **More filters:** title-only/raw-content-only search, title prefix, ID/date ranges and searchable icon choices. Separate fields combine with AND. Word mode also applies to title-only/content-only fields; prefix always matches the complete title phrase. Empty ranges impose no limit, including future calendar entries; end dates include the entire day.
- **Sorting:** Click Name, ID, Created, Modified, Tags or Content Type to sort; click again to reverse. Dropdown controls offer the same options. Occurrence ID breaks ties. Filter/sort changes reset to the first page; pagination retains all fields. Default page size 25, maximum 100000. PDF exports the current page with the current filters/order.
- **Results:** Real/shared tree occurrences have their own IDs, links and breadcrumb paths under their names. Shared contents may appear in several positions. Nodes with no tree occurrence are excluded. Missing/cyclic ancestry is bounded and marked. Date columns are compact; timestamps appear on hover. Mobile tables scroll within their container.

Saved filters include the new query, matching modes and sorting. Existing files are compatible: records lacking new fields use phrase matching, legacy tag substring matching and Modified descending order. Absolute date ranges remain absolute.

Text fields allow 2000 characters and at most 32 terms/tags per field; syntax allows 100 characters. `%`, `_` and `!` are literal, escaped characters. Phrase mode does not split terms. There is no regex, quoted-expression or exclusion syntax. Case matching follows the database’s LOWER/LIKE and locale behavior; SQLite does not fold every Turkish case pair.

Search, count, ordering and pagination run in SQL using bound parameters and whitelisted sort keys. Result loading omits text and image BLOBs; breadcrumbs read only hierarchy/name metadata. Substring searches still scan large text datasets; no full-text index is added.

Manual checks: all/any/phrase across title/raw XML, tag boundaries and wildcard characters, sorting/pagination/current-page PDF, shared occurrence paths, old/new saved filters, invalid bounds, desktop/mobile and SQLite/MariaDB/PostgreSQL. Searches must leave note timestamps unchanged.

Date ranges and displayed timestamps use the server time zone.


Rows per page is editable (1–100000), retaining the previous 10000/100000 choices. Larger values produce larger pages and PDFs.
