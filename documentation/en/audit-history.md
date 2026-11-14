# Optional CTBLOGS history

CTBLOGS records selected SweetCherry changes in the same CTB, without adding tables or columns. It is off by default and is not a replacement for verified database backups.

## Enable and review

Set these values under the existing `myapp` section of `application.yml`, or use **System → Settings**, then restart:

```yaml
myapp:
  audit:
    enabled: true
    max-event-characters: 16777216
```

The character budget may be set from 1,000,000 to 67,108,864. If a snapshot/record exceeds the budget or history cannot be written, the covered edit fails and its entire transaction rolls back. Text is never silently truncated in storage. Large batches may need a larger budget or smaller selections.

Open **View Mode → CTBLOGS history**. When recording is enabled, node toolbars also have a history link. Filter by UTC day and node/occurrence ID; use 0 for all nodes. For a current shared occurrence, filtering also includes its current master ID. An ID that no longer exists matches its recorded ID literally. Historical master promotions are not reconstructed automatically.

The list shows 100 events per page. Inspect an event for before/after metadata and text, original stored source, and embedded-object manifests. Text/object previews are capped at 20,000 characters; metadata sections show the first 200 changed rows. Download full readable text, full raw source, or the complete event JSON to inspect everything recorded. The original source of rich text is CTB XML; Markdown/HTML/code/plain-text source is preserved as stored. No automatic restore or reapplication of old XML is performed.

Disabling new recording does not remove or hide existing history. The history page is ADMIN-only, including for read-only database connections. Log nodes themselves are ordinary CTB notes: anyone who can read the database through CherryTree, another tool, or SweetCherry's normal tree can read them. Old text, including subsequently removed sensitive text, remains in the history.

## Initial coverage

Covered successful operations:

- Rich-text saves, including formatting, wiki links and explicitly confirmed embedded-object deletion.
- Plain/code/Markdown/HTML saves through the maintained content editor.
- Node-property and node-type changes, including rich-text conversion.
- Quick tag edits, batch additive tagging, and global tag rename/removal.
- Single and batch moves, including sibling-order changes and shared occurrences.
- Bookmark add, remove, individual reorder and full-list reorder through `BookmarkWriteService`.

There is one event per successful outer operation, including a whole batch. An identical save or a modification-timestamp-only write does not add an event. Failed validation, stale revisions and rolled-back changes do not appear as successful events. Helper updates do not recursively audit themselves.

**Not yet covered:** node creation, duplication/sharing creation, imports/exports, calendar date changes, trash/restore/purge, permanent deletion, configuration edits, external CherryTree changes, direct SQL, or other write paths absent from the operation registry. The coverage list on the history page is deliberate; this is not a universal database trigger audit. Existing verified backups remain necessary for those paths.

## Storage layout and fidelity

`CTBLOGS` is created at the end of the database roots only on the first effective covered change. It contains UTC daily nodes (`YYYY-MM-DD (UTC)`). Small event records are appended together to `Events 0001`, `Events 0002`, and so on, rotating around 64 KiB of characters. A small change does not require its own node.

Large source/event records use a detail container named with time, operation and node summary. Source chunks hold at most 100,000 UTF-16 units, without splitting surrogate pairs. References retain their order; missing or mismatched chunks cause an error rather than returning partial text. Plain text that equals raw source shares the same chunks. SQL NULL is distinguished from an empty source value.

Events contain a UUID, UTC timestamp, user, action, selected occurrence IDs, changed node metadata, changed `children` placement, changed bookmark rows, before/after source snapshots and object manifests. Shared edits retain both the selected occurrence identity and the changed content-master identity. Tree records include the old/new parent name and ID. The daily log and helper timestamps update, while original note creation dates remain unchanged. Normal note modification timestamps keep the existing write-service rules.

Binary images/attachments are **not copied** into history. Manifests retain filenames, object offsets, relevant row metadata, byte counts and SHA-256 fingerprints. Table XML and codebox source text are retained. Offset changes alone must not be interpreted as object deletion; rich-text records separately list explicitly requested deletions. Lost binary payloads still require a backup.

Managed nodes are read-only, carry search-exclusion flags and the reserved `__sweetcherry_audit_v1__` tag. Normal editing, moves, copying, child creation and deletion/trash paths protect these nodes. Do not modify their content/tags in another program or merge/import the managed log tree into another CTB: event references require the original identities. Whole-database backups/copies preserve them. A pre-existing unmanaged note named CTBLOGS is never adopted or overwritten.

Managed log notes are excluded from the home activity/latest-note selection, maintained created/modified timelines, advanced search and calendar lists. They remain visible in the normal tree and can appear in tree/mind-map views. No retention, automatic purge or export/migration of the managed history is provided in this first version. Database size grows as history is retained.

## Transaction and maintenance design

The operation registry is `AuditConfiguration`. Its infrastructure advisor runs after method authorization and wraps supported write services in a REQUIRED transaction. JDBC snapshots, the original write and ledger/source writes use the same routed tenant transaction. The existing service validation, optimistic revisions and permissions still apply. Exceptions from audit writing roll back the original change too. Captures lock existing metadata rows on server databases and use SQLite's transactional snapshot/locking behavior. Unrelated new rows from uncovered concurrent creation paths are not attributed to the audited operation.

Adding a new operation requires a registry entry, appropriate scoped text/object capture, a coverage-list update and transaction/rollback tests. The log is inspectable recovery information, not a cryptographically authenticated or tamper-proof security journal. Direct external writes and corruption remain outside its guarantees. Periodic verified backups and full synchronization/integrity checks remain useful alongside it.
