# Enable automatic backups for the current CTB

As ADMIN, select a CTB and open `/system/backups`. Beside the existing manual-backup controls, **Enable automatic backups for the selected database** adds the selected connection name to `myapp.ctb-backup.automatic.tenants` and sets `myapp.ctb-backup.automatic.enabled` to `true`.

The existing interval, retry period, retention count and other listed tenants are preserved. Enabling the shared automatic scheduler also resumes any other tenants already listed there. The confirmation describes this action. The new schedule is saved to the active external YAML configuration with its revision check, then applied to the running scheduler without a restart. The next check occurs within a minute; a backup is created when the existing schedule says it is due, rather than being forced immediately. An existing running job is left alone.

This action requires ADMIN, CSRF, the current CTB view token, an eligible local SQLite source, and a writable active external `application.yml`/`application.yaml`. Backups must be globally enabled. The button is disabled if configuration cannot be edited or the selected name is already listed and the scheduler is enabled. A failed or stale configuration save does not change the live schedule. Remove scheduled connections through the existing controls on `/tenants`.
