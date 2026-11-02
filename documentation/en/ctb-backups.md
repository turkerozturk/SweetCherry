# Consistent CTB backups

Open **System → CTB backups** as an **ADMIN**. Select a file-based SQLite CTB and choose **Back up selected CTB**, then confirm. A read-only tenant can also be backed up. MariaDB, MySQL, PostgreSQL and in-memory SQLite databases are not supported by this feature.

The server uses the SQLite Online Backup API, not an ordinary copy of a live database file. It captures the main database, including committed content still in the WAL, without changing source node IDs, timestamps, hierarchy, shared references, or schema. All SQLite tables and embedded binaries are included, including tables not explicitly known to SweetCherry. Uncommitted writes and unsaved editor text are not included. Other programs' unsaved changes must be saved there first.

Only one backup runs at a time across the application, including aliases pointing to the same CTB. The source connection is captured when you confirm; switching tenant later does not switch the job to another database. Its connection remains borrowed until the job finishes, so the existing pool-close/reload protection applies. Keep SweetCherry running until completion. Other connections can continue to use the source, although heavy concurrent writes can delay a backup or cause a lock failure. No SQLite journal-mode/checkpoint changes are made to the source.

## Completion and download

1. The background job creates a private temporary snapshot, copying SQLite pages in bounded batches.
2. The snapshot must pass `PRAGMA integrity_check` and provide the six CTB tables: `node`, `children`, `bookmark`, `image`, `codebox`, and `grid`.
3. Record counts are collected, SHA-256 is calculated by streaming, and the file is flushed before publication.
4. Only completed snapshots with a matching catalog manifest appear in the list. A failed or interrupted job is not a completed backup. After a process crash, `pending-*.partial` files can remain; they are not offered for download or opening. Once SweetCherry is stopped and no job runs, those partial files can be removed manually.
5. Download the `.ctb`. The details show its checksum and record counts. SHA-256 is checked again before download and before creating a working copy. The list itself reads only small catalog files and file sizes, rather than hashing every backup on every page visit.

Completed files use `backup-<UTC date>-<UTC time>-<UUID>.ctb`, accompanied by a small `.json` catalog. The JSON stores the selected connection's display name, completion time, checksum, size and record counts, **not its JDBC URL, username or password**. Keep both files for the in-app catalog. The CTB itself remains independently usable in CherryTree. The page displays completion times in the server's local time zone. Status polling does not require the source to remain selected; download and recovery also work without an active tenant.

The SQLite data and BLOBs are streamed/copied without reading the entire CTB into the Java heap. Integrity verification and hashing still take time and disk I/O on a Raspberry Pi. Status refreshes automatically; if the session or network is lost, sign in and revisit the page. This does not cancel the server job.

## Inspect or recover safely

Choose **Open a separate working copy** and confirm. SweetCherry verifies the checksum, copies the closed backup to a new file under `working-copies/`, verifies that copy, then opens the existing database wizard. Its default is **read-only**, with schema upgrade disabled. Confirm the wizard to register and load the copy; its name can be changed first.

The original CTB and the stored backup are never overwritten. If you later make the working copy writable, edits affect only that copy. If you cancel the wizard, the copy remains for manual cleanup. To continue using recovered content, keep/select this separate database. This first version deliberately does not replace a live database file. Such a replacement would require closing every tenant pool pointing to the original file and ensuring CherryTree and other external applications also released it.

## Configuration

Under the existing `myapp` key in the external `application.yml` beside the JAR, add/edit:

```yaml
myapp:
  ctb-backup:
    enabled: true
    directory: backups/ctb
```

Restart after changes. Defaults also apply when these keys are absent. The directory is relative to the **application working directory**, or it can be an absolute path. New backup creation can be disabled with `enabled: false`; existing backup downloads and working-copy recovery remain available. No folder is created until needed. The account running SweetCherry needs access and sufficient free disk space. A working copy requires additional space roughly equal to the backup. New files/directories receive private POSIX permissions where supported; protect existing folders and Windows/network-share ACLs yourself. A custom folder inside a Git checkout should also be ignored by Git; the default `/backups/` is already ignored.

Automatic scheduling and retention are optional (see below). There is no manual backup deletion button or per-node version history. Backups do not include application YAML, login credentials, tenant config files, or files linked externally from notes. Stored snapshots contain the notes and attachments themselves; keep the folder private. A backup beside the original does not protect against loss of that disk: keep downloaded copies on another device or storage medium as well.

## Manual checks

* Back up writable and read-only CTBs. Check content, images/attachments, tables, codeboxes, bookmarks and shared hierarchy in the downloaded CTB using CherryTree. Source timestamps must not change.
* With a source using WAL, save changes and back up while it remains open. Confirm the latest saved content is present. Unsaved editor text must not appear.
* Start a backup, then change tenant. Verify the completed backup still identifies and contains the originally selected database. A second job, including one through an alias, must be refused while the first is running.
* Create/open a working copy, confirm the wizard and inspect it. If made writable, editing it must not change either the original or the stored backup. Test catalog/download access after closing the active CTB and after restarting SweetCherry.
* Test invalid IDs, a modified backup file, disabled creation, low disk space/unwritable storage, interrupted jobs, and a non-ADMIN account. Invalid/incomplete files must never be offered as a valid recovery source.

References: [SQLite Online Backup API](https://www.sqlite.org/backup.html), [SQLite backup concurrency](https://www.sqlite.org/c3ref/backup_finish.html), [unsafe live-file copies](https://www.sqlite.org/howtocorrupt.html#_backup_or_restore_while_a_transaction_is_active).


## Optional automatic backups and retention

Automatic backups are **off by default**. Under the existing `myapp.ctb-backup` section, add:

```yaml
myapp:
  ctb-backup:
    enabled: true
    directory: backups/ctb
    automatic:
      enabled: true
      interval-minutes: 1440
      retry-minutes: 5
      keep-last: 7
      tenants:
        - "My notes"
        - "Another CTB"
```

Use exact registered tenant display names, not file paths. The default tenant list is empty; enabling the flag alone does not back up every configured database. `keep-last` defaults to **0 (unlimited)**; the example explicitly opts into keeping seven automatic backups per physical source. Intervals/retries must be positive; retention must be nonnegative. Restart after editing YAML. The unrelated experimental `myapp.scheduling.enabled` flag is not needed.

The first check is one minute after SweetCherry is ready, then once per minute. No browser login or selected database is required. Eligible sources are handled sequentially, at most one new job per check. A manual/running backup is allowed to finish first. Missed backups while the application is stopped do not accumulate as a queue; an overdue source is backed up when running again. A failed source waits `retry-minutes`, allowing other due sources to run.

Aliases, SQLite URI URLs and symbolic links to the same CTB are grouped. Source identity uses the canonical file location (stored only as a hash); renaming a tenant does not split its history. Moving/renaming the CTB itself is treated conservatively as a new source. Sources are opened through a separate read-only SQLite connection: closed tenant pools remain closed, no missing file is created, no session is changed and no schema upgrade is performed. Missing names, unsupported server databases and inaccessible paths are shown on the backups page. Refresh to see updated schedule state.

The interval starts from the latest successful keyed backup, including a new manual backup of that source. Completion times survive application restarts in the catalog. Old manifests without a source identity remain readable and protected; they cannot postpone the first automatic run because their physical source cannot be safely inferred.

Retention runs **only after a new automatic snapshot has been completed and verified**, and applies only to automatic snapshots of that same source. It always preserves the new snapshot, manual backups, legacy backups, other sources and `working-copies/`. Files registered as tenants, files with SQLite WAL/SHM/journal sidecars, or files whose checksums no longer match, are skipped. Always open a separate working copy for editing; external programs do not participate in SweetCherry’s coordination. Cleanup failures are warnings and do not invalidate the newly completed backup. With `keep-last: 0`, nothing is automatically deleted. Increasing/enabling retention never triggers an immediate deletion sweep on startup; it waits for the next successful automatic backup. Retired catalog tombstones left by an interrupted cleanup are ignored by the list. Protect the backup folder from external modification; checksums are not digital signatures.

### Additional manual checks

* Enable automatic backup for two names pointing to the same CTB and another name pointing to a different CTB. Verify one automatic source for the aliases, sequential jobs and no need to keep a browser open.
* Restart SweetCherry before the interval is due: an existing successful keyed backup should postpone the next run. Try a missing source and verify another due source still progresses after retry backoff.
* Set `keep-last: 2` and obtain three successful automatic backups. Only the two newest automatic backups of that source should remain. A manual backup and working copy must remain. With zero, all remain.
* Cause the new job to fail: no old backups may be deleted. Modify an old automatic snapshot, or register it as a tenant: cleanup must skip it and report a warning. Test English/Turkish schedule state and narrow mobile layout.
