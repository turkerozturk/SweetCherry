# Node-linked reminders

Use **Add reminder** in the node toolbar or a TODO row. Enter a date/time, time
zone and optional note. New forms use `myapp.calendar.time-zone` (`system` uses
the server zone). The instant is stored in UTC alongside the chosen zone.

**View Mode → Reminders** (`/nodes/reminders`) provides pending, upcoming, overdue,
completed, cancelled and all filters, search and pagination. Pending instants at
or before the check time are overdue. Use **Refresh / filter** to recalculate.
This stage stores one-time reminders; there are no automatic notifications,
alarms or recurrence yet. Past dates are allowed. DST gaps and overlaps require
another time or an explicit UTC offset instead of silently shifting the time.

Multiple reminders may refer to one content node. Real and shared placements
resolve to the same content. A surviving active location is used if the preferred
one disappears or is trashed. If no location is available, the reminder remains
and can be completed/cancelled; editing and reopening require an active target.
Edits keep the existing completion/cancellation state. Reminder completion does
not change TODO tags. Source content, tags and timestamps are preserved.

Readers and read-only sources may view the list. Editing requires ADMIN, a
writable source, CSRF and a current tenant token. The entire store revision is
checked; another tab's change rejects stale forms. Invalid form values are kept.

The first successful save creates a protected, read-only, search-excluded root
called `REMINDERS`, identified by `__sweetcherry_reminders_v1__` and a versioned
header. It contains bounded JSON (5000 records / 2,097,152 Java characters).
Reading an empty store creates nothing. No CTB schema changes are made. Full CTB
copies/backups retain reminders; subtree imports/exports do not automatically
transfer or remap reminder references.

Normal editing, retagging, child creation, moving, copying and deletion of the
helper are blocked. It is excluded from tag catalogues, search, calendar and
recent activity. Corrupt, duplicate, shared, relocated or oversized metadata is
rejected without overwriting it. Restore a valid backup or inspect it in CherryTree.
Node/tree writes share one transaction; stale or repeated submissions cannot
silently replace records. Reminder operations are not separate CTBLOGS events
in this stage; record creation/update instants are retained in the store and the
helper is excluded from ordinary content history.

Focused checks:

```text
mvnw.cmd -Dtest=ReminderTimeTest,ReminderServiceTest,ReminderControllerTest,ReminderSecurityTest,ReminderTemplateTest,AuditIntegrationTest test
mvnw.cmd clean package
```
