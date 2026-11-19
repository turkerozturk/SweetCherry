# Reader preferences and restart

The **User** menu contains three independent confirmation switches below a separator. All default to enabled:

- Creation: sibling, child, root and shared-node creation.
- Duplication: a single node or a node with descendants.
- Deletion: the node deletion confirmation page, including move-to-trash when configured.

Preferences are saved in this browser until changed and synchronize between tabs. If browser storage is blocked, changes last only for the current page. These switches affect node-operation confirmations; export, backup, database, shutdown and unsaved-editor prompts keep their existing behavior.

With deletion confirmation disabled, the explicit Delete command visits the existing preparation page and submits its POST form automatically. The tenant token, CSRF protection, writable/admin checks and trash revision check still apply. Merely visiting an unmarked deletion URL does not automatically submit it. The temporary fast-delete URL marker is removed before submitting so refreshing that page cannot repeat the automatic submission.

**Node content background** opens a dialog with a color picker and searchable standard CSS color names. You can also type hex values (`#rgb`, `#rgba`, `#rrggbb`, `#rrggbbaa`). Apply changes immediately in the tree, mobile reader and quick reader, including linked fragments. The saved browser preference overrides the application's default; **Use application default** removes that override. Authored text colors, highlights and embedded objects are preserved. Editors and PDF exports are unaffected.

The application default is editable in **System settings**, or in the existing YAML mapping:

```yaml
myapp:
  appearance:
    node-background-color: ivory # Or a quoted hex value, such as "#fff8dc".
```

Configuration-file changes take effect after restart. Invalid configuration colors fall back to white; the settings editor rejects invalid values. The browser menu applies its override without restarting.

After **Restart SweetCherry**, the status page polls a small anonymous readiness endpoint without the expired session cookie. It waits for a different application instance to finish startup, then opens the application's home page automatically. Login may be required because restart closes sessions. During a slow or failed startup it keeps waiting and leaves the manual home link available.

Mind-map and ZIP import file fields display Browse/No file selected in the application's selected language. The native file selection dialog itself follows the operating system/browser language. Without JavaScript the original native file input remains available.

## Tree keyboard navigation

Focus a node name in `/tree` with a click or Tab. Up/Down open the previous/next visible node; Home/End open the first/last visible node. Right expands a closed branch, or visits its first child when already open. Left collapses an open branch, or visits its parent. Focus stays in the tree.

Only unmodified keys on tree node-name links are handled. Content scrolling, text selections, buttons, editors, dialogs and browser shortcuts retain their normal behavior. Existing Alt+Shift+arrow move commands are unchanged. Leading blank rich-text lines are displayed as stored.

After a successful structural move returning to `/tree`, focus returns to the moved node once its new path is visible, so arrow-key navigation can continue. Normal page loads do not force tree focus, and an input/control already focused during loading is left alone.
