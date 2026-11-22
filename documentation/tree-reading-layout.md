# Tree reading layout and metadata

Open **Reading layout** using the sliders button at the bottom left of `/tree`.
It stays accessible when the main menu or node buttons are hidden, when the tree
is scrolled, and when its width is reduced. The original layout remains the default.

- Compact icon buttons keep action names as tooltips and accessible labels.
- Main menu, breadcrumbs, backlinks, node buttons and split pane controls can be
  shown or hidden independently. The node title row can also be hidden (visible by default).
- Split View can still be toggled and either pane activated from this dialog
  when the pane controls are hidden. Reading preferences also apply to the right pane.
- IDs and dates can be shown independently before node names. They do not change
  node names, stored content, exports, or keyboard navigation.
- Preferences are remembered in this browser across navigation and restarts.
  **Reset layout** restores the original layout and the configured metadata defaults.

Initial metadata defaults and formatting are configured in `application.yml`
(or System → Settings); restart SweetCherry after changing these settings:

```yaml
myapp:
  tree:
    metadata:
      enabled: false
      id-digits: 6
      date-source: created
      date-format: yyyy-MM-dd
```

`enabled` supplies the initial default for both ID/date toggles; explicit browser
choices take precedence. `id-digits` is a minimum from 1 to 19; IDs are displayed
as `[000042]` and longer IDs are never truncated. `date-source` is `created` or
`modified`. The format uses Java DateTimeFormatter patterns (`MM` for month,
`mm` for minutes), for example `dd/MM/yyyy HH:mm`, in the server's system time
zone. Shared nodes show their own occurrence ID and their master's timestamps.
Missing timestamps are left blank.

# Empty trash

The Trash Bin page has an **Empty trash…** action covering every entry, including
entries on other pages. The review page shows entry/node counts and shared
locations outside the trash, and requires explicit permanent deletion confirmation.
Changes to the tree or trashed content invalidate that confirmation. The operation
uses the existing shared-content deletion rules in one transaction: a failure
rolls back all deletions. The empty trash container remains available. Individual
review, restore and purge behavior is unchanged. ADMIN, writable database, current
tenant and CSRF protections also apply to emptying the trash. Existing trash can
be cleaned up even when creating new trash entries is disabled.

## Quick name and icon editing

Hover over the node title and use the pencil to edit its name and CherryTree
icon together. The icon button opens a searchable local icon chooser; the smiley
button inserts inline symbols into the name. Save applies both changes; Cancel
discards both. Compact layout shows Save/Cancel as icons with accessible labels
and tooltips. The full Properties page remains available for bold, color,
read-only and other properties. Quick icon editing preserves the read-only bit,
other title flags and content. A concurrent icon/lock change invalidates an open
quick edit, even when the name and modification timestamp have not changed.
