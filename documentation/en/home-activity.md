# Database opening and home activity

After a database is selected, the bundled YAML now opens the most recently modified node by default. Set `open-latest-modified: false` to keep opening the virtual root. The virtual root is not a real node; neither option changes the CTB schema or node timestamps.

Add or edit this section under the existing `myapp` key in the external `application.yml` beside `SweetCherry.jar`, then restart:

```yaml
myapp:
  home:
    open-latest-modified: true
    modified-minimum: 5
    created-minimum: 5
```

* `open-latest-modified: true` opens the most recently modified tree occurrence after successful database selection, including selection through the database wizard. The existing desktop/mobile view preference is preserved. An empty database, or failure to read the latest node, falls back to home. An explicit visit to the virtual root still opens home. With tied timestamps a real occurrence is preferred, then the lowest occurrence ID.
* Each enabled home list shows **all** occurrences from today, newest first. If there are fewer than the configured minimum, the newest earlier occurrences fill the list. These settings are minimums, not maximums. Set either minimum to `0` to hide that list. Negative numbers are invalid. With fewer total nodes than the minimum, all available nodes are shown.
* Today uses `myapp.calendar.time-zone`, or the operating system time zone when it is `system`. Entries dated tomorrow or later are excluded from the home activity lists; calendar features can still use future creation dates. Dates in the lists use the same zone. Equal timestamps are ordered by occurrence ID.
* Shared occurrences use their own IDs and full hierarchy paths, with their master's name, icon, title style and timestamps. Links include the current database view token. Content text and embedded binaries are not loaded to build these lists.
* Recently Modified, Recently Added and Bookmarks remain available as buttons. Long root-page lists scroll inside the desktop content pane; mobile uses normal page scrolling.

Existing external YAML files are not overwritten. An explicit `false` in your external `application.yml` still wins: change it to `true` and restart if you want the new default. Missing keys inherit the bundled YAML values above.

## Manual checks

1. Select a database with the bundled defaults: its newest modified occurrence opens. Set the option to false and restart: home opens instead. Switch to a different database and verify its node opens, rather than one from the previous database. Read-only databases work too.
2. Open the virtual root explicitly. Verify both lists, full clickable paths, icons, bold/color styling and dates in Turkish and English, including shared occurrences under different parents.
3. With fewer than five entries today, verify older entries fill to five. With more than five today, verify all are displayed. Check a zero minimum, a different minimum, an empty database, and future creation dates.
4. Check desktop scrolling and narrow mobile layout. Confirm these reads do not change creation or modification dates.
