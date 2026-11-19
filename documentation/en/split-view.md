# Two-node split view

On `/tree`, use **View Mode → Split view**, or the **Split view** button above the content, to open a second reading pane. On first use it opens the same node as the left pane. Later uses restore the last right-hand node, including the database home.

- Click **Left pane** or **Right pane**, or click/focus inside a pane, to make it active. A blue underline marks the active pane.
- The common tree and its arrow-key navigation target the active pane. Navigating in one pane leaves the other pane's content and scroll position in place.
- Internal links and breadcrumbs stay in their own pane. The tree highlights the active pane's node and reveals its ancestor path.
- The footer describes the active reading pane. The right pane has an **Open in new tab** button for its current node.
- Drag the separator between the content panes to resize them. Focus the separator and use arrow keys for keyboard resizing. Narrow windows stack the panes vertically.
- Use the right pane's close button, or toggle **Split view** again, to return to one pane. Closing a form page asks for confirmation so that removing the pane does not silently discard an editor.

Open/closed state, the right node, active side, and pane proportions are remembered in this browser tab's session, separately for each selected database view token. The main address continues to identify the left node; opening that address in another tab is not a portable two-node link. No node content or database schema is changed by the layout.

Each pane keeps a separate document so that existing editor, toolbar, modal, anchor and form IDs cannot collide, even when displaying the same node twice. Forms and editors opened in the right pane remain inside it. The left pane retains the application's normal full-page editing flow; returning to `/tree` restores the split. Existing write permissions, CSRF protection, tenant tokens and revision checks still apply. Editing one copy does not overwrite the other pane's open editor; revisit a node to reload its latest content.

The existing Alt+Shift+Arrow structural move shortcut targets the active pane. After a successful move initiated from the tree, the moved node can regain tree focus without a mouse click. Ordinary arrow keys remain scoped to focused tree links and do not replace content scrolling, selection or editor navigation.

The right pane is served by the same application. `X-Frame-Options: SAMEORIGIN` permits this internal embedding while rejecting framing by other origins. Embedded reading panes do not start another reminder alarm poller or sound; the containing workspace owns alarms. Invalid sessions or changed database selections retain the existing page/session guards.
