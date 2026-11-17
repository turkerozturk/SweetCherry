# Node-linked reminders

Use **Add reminder** in the node toolbar or a TODO row. Enter a date/time, time
zone and optional note. New forms use `myapp.calendar.time-zone` (`system` uses
the server zone). The instant is stored in UTC alongside the chosen zone.

**View Mode → Reminders** (`/nodes/reminders`) provides pending, upcoming, overdue,
completed, cancelled and all filters, search and pagination. Pending instants at
or before the check time are overdue. Use **Refresh / filter** to recalculate.
One-time and annual reminders are supported, along with the in-page alarm panel
described below. Past dates are allowed. DST gaps and overlaps require
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

## Annual recurrence and page alarms

Choose **Repeat → Every year**. The first reminder date anchors the month/day and
local time. February 29 uses February 28 (default) or March 1 in non-leap years,
and returns to February 29 in leap years. Completing an occurrence schedules the
first future occurrence after now or the selected occurrence if it is later.
A new anniversary replaces a missed earlier year without accumulating every
missed year. Only the last completed occurrence is retained. Reads calculate
rollover without writing; occurrence IDs reject stale annual action buttons.
Annual DST gaps move forward and overlaps select the earlier offset. Manual
ambiguous/nonexistent times still require correction or an explicit offset.

Enable the **Alarms** panel on a page for the selected CTB. **Enable / test sound**
plays two short tones and enables sound for that page. Sound must be enabled
again after navigation/reload; `/tree` content navigation keeps the same page.
Open pages poll roughly every 20 seconds, subject to browser throttling. Closed browsers, sleeping machines
and background tabs cannot guarantee timely alarms. These are in-page visual
alerts, not an OS service or desktop notification. Up to 20 recent due alerts
are shown; all reminders remain in the list.

Closing an alert acknowledges only its occurrence. Snooze persists a ten-minute
server-time delay and creates a new presentation key. The next annual occurrence
has fresh alarm state. The list can reactivate a closed/snoozed alarm. Persistent
actions require ADMIN, a writable source, CSRF and the current tenant token.
Reader/read-only dismissal is local to this browser. Missing/inactive targets do
not alarm. Web Locks and shared browser receipts suppress repeated sounds across
tabs; without Web Locks only the focused page checks. The latest 1000 receipts
are retained; unavailable browser storage limits receipts to page memory.
Different devices/browsers may alert independently. A changed source/session
stops polling and mutes sound.

Version 1 stores load as one-time records and migrate to version 2 only on a
mutation, including dismissal/snooze. GET requests do not migrate. The protected
tag and CTB schema stay unchanged. Version 2 adds recurrence, anchor/leap policy,
last completion, occurrence identity and acknowledgement/snooze timestamps.
Older SweetCherry versions reject version 2 rather than overwriting it.
