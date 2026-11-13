# HTML notes

In **Node Properties**, select **HTML** next to the Markdown shortcut and save. This selects the `html` code syntax; Rich Text conversion requires explicit confirmation of formatting and embedded-object loss. Locked nodes must be unlocked and saved first. Only the exact `html` syntax uses this view. Other code syntaxes and HTML inside codeboxes remain displayed as code.

Open **Edit → HTML** to edit the source with a live rendered preview. On larger screens the panes are side by side; on narrow screens they are stacked. The toolbar inserts HTML for bold, italic, headings, paragraphs, lists, code, links and tables around the selection. The source is the editable document; the preview is not a WYSIWYG editor.

Preview updates automatically after a short pause, or manually using **Refresh** when automatic preview is off. Hide the preview to give the source more room. Undo/redo, browser-local draft recovery, Save and Save and continue use the existing text-editor workflow. Editing requires an administrator and a writable database/node. Previewing changes does not save them.

The original source is stored unchanged in the CTB. Both HTML fragments and complete HTML documents can be used. Display uses the bundled jsoup library and an isolated browser frame. Supported content includes headings, paragraphs, emphasis, lists, tables, links and embedded PNG/JPEG/GIF/WebP images. Basic inline styling such as colors, text alignment, font sizes, margins and table borders is supported. The reader, quick reader and calendar show rendered HTML; PDF and report exports use sanitized HTML fragments, and mind-map exports with note content use readable text. The files-and-folders text ZIP preserves HTML source.

This is a note renderer, not a complete website host: scripts, event handlers, forms, frames, SVG, stylesheets and external resources are omitted from the display. External image URLs and relative file paths are not loaded; use embedded base64 raster images. Safe HTTP/HTTPS and mail links open in a new tab. Internal links of the form `/nodes/123` keep the database-view token. Source is preserved even when some of it cannot be displayed. The HTML source limit for preview, rendering and saving is 2,000,000 characters.

Example:

```html
<h1>Project notes</h1>
<p style="color:#3584e4">This paragraph is blue.</p>
<ul><li>Try editing the source.</li><li>Watch the preview update.</li></ul>
<table><tr><th>Task</th><th>Status</th></tr><tr><td>Example</td><td>Ready</td></tr></table>
```
