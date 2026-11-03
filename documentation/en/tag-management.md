# Database-wide tag management

As ADMIN with a writable connection, open **Tags → Rename / remove tags**. Choose one tag from the searchable list, choose rename/remove and review changes. The confirmation page shows affected stored-node count and up to 200 before/after rows. Nothing is written until confirmation; Cancel does not write.

Matching follows existing ASCII-space-delimited, case-sensitive tag semantics. `work`, `Work` and `homework` are different. New names must be one non-empty tag without spaces/control characters and differ from the source. Renaming to an existing tag merges duplicates. Changed fields are de-duplicated and joined with single spaces, as in Node Properties; unrelated raw fields remain unchanged. Remove never deletes nodes or content.

All `node` records are included, even records without a tree occurrence. Shared occurrences reflect their master tags; each stored master is updated once. The content-read-only bit does not forbid metadata changes, consistent with Node Properties, but a read-only tenant does. Created timestamps are preserved and Modified is set to the operation time for each changed stored node. Content and hierarchy remain untouched.

Confirmation is session/source-view bound, single-use and valid for ten minutes. A new preview replaces the previous one; returning to the management page cancels pending confirmation. If affected tags/names or matching records changed since preview, review again. All updates share one transaction; any failure rolls everything back. Safety limits are 50,000 affected stored nodes, 16 Mi characters of affected raw tags and 20,000 characters per resulting tag field. The first 200 preview rows do not limit the operation to those rows.

After commit, INFO logs contain one line per changed stored node with a common operation ID, actor, connection name, node ID/name, RENAME/REMOVE and old/new tag, followed by a count summary. Rolled-back transactions produce no success audit. Log controls are sanitized and long fields truncated; note bodies and connection credentials are not logged. Node names/tags are metadata that will appear in the application log.

Checks: exact/case-only matching, punctuation/Unicode tags, merging an existing destination, removal of the last tag, shared content, Cancel/replay/expiry, stale source tabs and concurrent tag edits. Verify only affected Modified timestamps change. USER and read-only tenant POSTs must fail too.
