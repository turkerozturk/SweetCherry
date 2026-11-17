# Tasks (TODO): first stage

Open **View Mode → Tasks (TODO)** or `/nodes/todo`. Add the exact lowercase tag
`todo` to an existing node using Properties or the quick tag editor.

An open task has `todo` without `done`. **Mark completed** keeps `todo` and adds
`done`; **Reopen** removes `done`. Other tags are retained using the existing
tag normalization rules. Remove `todo` to remove the node from this list.
`done` alone, `TODO`, and `todo-later` do not create tasks.

The page offers open/completed/all filters, name/tag/ID search, stable name
ordering and pagination. Summary counts cover all tasks regardless of filters.
Shared placements count as one task; the active location count is shown. Links
prefer the real node, otherwise an active shared location. Trash/history
branches, orphaned content and invalid/cyclic paths are excluded. A task remains
visible if another active shared location exists. Completion applies to all
locations of the same content.

Readers and read-only sources can view tasks. Changes require ADMIN, a writable
source, CSRF and a current tenant token. Revision checks reject stale tag edits.
Only tags and modification time change; note content, objects and creation time
are preserved. When CTBLOGS is enabled, changes are audited transactionally as
`todo.update`.

This stage has no due dates, recurrence or alarms. The next stage is node-linked
reminder records and upcoming/overdue lists, followed by yearly recurrence,
calendar views and visual/audio alerts.
