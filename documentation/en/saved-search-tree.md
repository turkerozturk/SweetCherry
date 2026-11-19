# Saved searches as virtual tree branches

The desktop `/tree` view shows **Saved searches** above the database's real nodes. Save a named filter in **Advanced Search** to populate this group. It uses the existing per-user, per-source saved filters; it does not create a second kind of preset or write virtual nodes to CTB.

- Expand a saved search to run it and show matching nodes. Results remain collapsed until requested, so opening the tree does not execute every saved query.
- Each page contains at most 50 results. **Previous** and **Next** run the corresponding page on the server. A preset's saved page size is intentionally ignored in this compact view.
- The saved matching, tag, date, icon and sorting choices are reused. Dates remain absolute. Results use the same database-side search as Advanced Search, including its shared-occurrence and managed-note behavior.
- A result opens the original occurrence, with its original icon, title styling and tag marker. Its tooltip shows the location path. It is a navigation shortcut, not a copy, new parent or shared node. Subsequent editing/moving/deleting acts on the original node through the normal permissions and controls.
- Split view sends selection to the active pane. Result links also support focused arrow-key navigation without taking over content/editor keys. Virtual group headings and paging controls use normal Tab/Enter/Space interaction.
- Reopening a branch or using its refresh button reruns its first page. The group's reload button rereads the saved filter list, for example after changing presets in another tab. Reloading the list collapses the branches; ordinary node navigation keeps them open. Results are snapshots until another query/refresh is requested.

The group is enabled by default. To hide it and disable its read-only result APIs, set the following in external `application.yml`, or change **Show saved searches as virtual tree branches** on `/system/settings`, then restart:

```yaml
myapp:
  tree:
    saved-searches-enabled: false
```

Disabling this presentation does not delete filters or disable saving/applying filters in Advanced Search. Private presets stay in the existing `saved-filters/` store, scoped to the signed-in user and connection identity. Missing/stale tenant tokens and expired sessions retain the existing guards. Preference-storage errors affect the optional group rather than replacing the normal tree. Only metadata is returned as results; text/BLOB contents are not loaded into result rows. The existing search engine may scan stored text to evaluate text predicates.
