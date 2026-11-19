# Node title and toolbar

In `/tree`, hover over the node title to reveal the pencil, or reach it with Tab. On touch devices the pencil remains visible. ADMIN users with a writable connection can rename real nodes in place. Enter/Save submits the new name; Escape/Cancel leaves it unchanged. Shared occurrences keep their existing properties restrictions.

The title endpoint uses the current tenant token, CSRF protection and a revision check. A concurrent rename or edit requires cancelling and reopening the editor. Only the name and modification timestamp change; title colors, boldness, icons, tags and content are preserved. Enabled CTBLOGS records this as a properties update. After saving, the workspace refreshes the title, breadcrumbs and tree through its existing AJAX navigation.

The ordinary-node toolbar order is Tags, Edit, Properties, the four move controls, Move to, Delete, Add, Map, Export, CTBLOGS history and Add reminder, subject to existing permissions/settings. The information button is next to the title. A single available editor opens directly from Edit with the corresponding icon; when plain-text and rich-text editors are both available, Edit retains a dropdown. Copy and copy-with-descendants commands are in Add, above the separated Create Shared Node entry. Existing creation/duplication confirmation preferences still apply.
