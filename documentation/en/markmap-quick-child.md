# Add a child from Markmap

On `/markmap-quick-mind-map/<nodeId>` or the all-roots map, hover over a node label to reveal **+** immediately. Keyboard users can Tab to the button and press Enter/Space; touchscreens show it without hover.

Click **+**, enter a name, and choose **OK** (or press Enter). **Cancel** or Escape closes the dialog without writing. The new node is appended as the last child, with empty Plain Text content and the connection's normal new-node tag defaults. You can edit its content later through the existing node reader/editor.

The name still navigates the map, and the icon still opens content in another tab. The + button is a separate action. It is available only for ADMIN on a writable connection; server-side write, CSRF and selected-database checks still apply. A content read-only flag does not lock the entire subtree; the existing child-creation rules are used.

After a confirmed save, the current map refreshes, preserving its root, icon/color/chat settings and saved zoom scale. If needed, the depth increases just enough to include the new child. A folded chat parent is opened for this refresh. Creation under a shared/alias node uses that occurrence's ID, so the child belongs to that place in the tree, not the shared content's master. The virtual database root has no + button; use the ordinary top-level creation action for that.

If saving is rejected, the name stays in the dialog. A validation error can be corrected. If the database changes or the response is uncertain, saving is disabled until the page is reopened/refreshed; check the map before retrying, since a lost response can follow a successful write. The client does not retry creation automatically.

## Manual check

1. As ADMIN on a writable demo connection, hover a real node and a shared occurrence. Add a named child and confirm the intended parent in the tree; inspect that the new content is empty Plain Text.
2. Repeat at the current depth limit and beneath a folded `chat` node. The refreshed map should expose the new child. Confirm name navigation and icon content links still work.
3. Open the dialog with Tab + Enter/Space, submit with Enter, and cancel another entry with Escape. On a touchscreen, check that + is visible without hover.
4. As USER or on a read-only connection, creation controls should be absent. Switch databases in another tab before submitting an old dialog: it must not create in the newly selected database.

## Page-filling map

The corner expand icon hides the toolbar and fills the web page with the map, without browser fullscreen. Click it again or press Escape to restore the normal view. Open dialogs keep their own Escape action. Normal view is the default on each load; changing the view does not reset the map zoom or its data.
