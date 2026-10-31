# Saved filters and backlinks

Advanced Search can save named combinations of its existing fields. Choose New filter, enter a name, and Save / update. Select a saved filter to update/delete it or Apply and search. Applying preserves the existing POST/raw node.txt search and resets pagination. Dates are absolute; icon selections and page size are included.

Preferences live outside CTB in the working directory’s private `saved-filters/` folder, scoped to user, connection name, JDBC URL and installation path. The same account/connection can access them across devices, including on read-only sources. Renaming/moving a connection or installation changes scope. Limits: 50 filters, 80-character names, 1 MiB per scope. Keep the folder private: saved search text is stored there. Do not distribute it. Concurrent tabs cannot overwrite a newer revision; reload first.

Open Backlinks in the node reader to scan incoming CherryTree rich-text and object `node ID [anchor]` links. Markdown URLs are excluded. Exact occurrence is the default; Include all shared occurrences includes targets sharing its master content while preserving actual target IDs. Shared sources link to their own occurrence. Names retain icons, bold/color and breadcrumb tooltips. Counts mean links / source occurrences. Wide desktop content shows a sidebar; smaller screens show a collapsible block. The database and timestamps are never modified.

Scanning happens only on opening/refreshing the panel: 20 links/page, at most 10,000 rich-text contents and 32 Mi characters per scan, with 2 million characters per content. Image BLOBs are not read. Unreadable/oversized content or scan limits display an incomplete-results warning. Old database/session tokens are rejected.

Manual checks: save Unicode/punctuation and multiple icons, alter fields then apply without duplicated values; check persistence, user/source isolation, update/delete and stale-tab conflicts. Test exact/shared-family backlinks, source occurrence IDs, anchor/object links, empty results, mobile layout and source switching.
