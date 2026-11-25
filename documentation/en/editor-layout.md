# Editor layout and language selection

The rich-text writing surface fills the available pane without a second outer content scrollbar. Its label remains available to assistive technology without taking visible space. The restored caret is kept visible after visual editing; the tools retain their independent scrollbar.

On desktops at least 900 pixels wide and 500 pixels high, plain-text and Markdown editors use the viewport space remaining below the page header and above the footer. Recovery controls consume their actual height. The source/preview surfaces scroll internally and start at the same vertical position. Smaller screens keep normal page scrolling and textarea resizing; rich-text tools still use the mobile drawer.

Node Properties offers a searchable, alphabetical code-language list with bundled icons and a Markdown shortcut. The native select remains the submitted value and the fallback without JavaScript. Existing rich-text/content-read-only locks and server-side validation still apply. Choosing a language does not save until Save is pressed.

Manual checks: type many lines, use Enter, paste, undo/redo and formatting; expand/collapse recovery controls and resize the window. Toggle Markdown preview. Search for a language and use the Markdown shortcut. Check locked nodes and the mobile tools drawer/selection.

## Starting position and editor text zoom

Content editors (plain text/code, Markdown, HTML source and rich-text visual/source modes) open focused at the end of the last line. After layout settles, the writing surface and page scroll to the end. This initial scroll yields if you move the selection, open a dialog or restore a recovery draft. Save and continue opens the saved document at the end again. Embedded codebox text also opens at its end.

The minus / percentage / plus controls immediately after Save, Save and continue and Cancel resize editor text from 70% to 200% in 10-point steps; click the percentage to reset to 100%. They share a remembered preference across editor types on the same browser/site (`sweetcherry.editorTextZoom`), independently of the reader zoom. They preserve the current selection, add no undo step and never change stored text or rich-text font formatting. Without browser storage the preference lasts for the page.

Bottom padding leaves approximately 35% of the writing surface below the final line (bounded to 32–240 pixels). This is CSS presentation space, not inserted blank paragraphs. Typing/Enter at the end keeps that room visible; moving to earlier text does not force you back down.

The rich editor keeps Undo/Redo to the right of the WYSIWYG switch, uses four alignment buttons beside the style selector, and sizes tools to their captions instead of stretching every control. The object-deletion checkbox and Manage objects share a separate row above Dismiss link. The backup reminder is last in the tools pane.

## Editing inside the tree workspace

On `/tree`, **Edit** opens the existing editor inside the selected reading pane.
Plain text, code, Markdown, HTML and rich text keep their existing save, undo/redo,
zoom, recovery and object protection behavior. The reader toolbar and content are
replaced for the duration of editing; the main tree and split controls stay available.

- **Save** returns to reading and reloads the saved node at the previous reading scroll position.
- **Save and continue** reloads the editor with the saved revision and remains in the same pane.
- **Cancel** restores the original reader without reloading it, unless an earlier Save and continue changed the node.
- Leaving a dirty editor, changing nodes or closing its split pane requires confirmation.
  Loading and saving must finish before a pane can be removed.
- Reading layout preferences remain unchanged. Compact buttons apply to editor actions
  with icons too. Rich text tools use their existing resizable sidebar at wide pane
  widths and their existing Controls drawer at narrow widths.
- Ctrl/middle-clicking an Edit link still opens the standalone editor. Editors opened
  from other pages keep their existing standalone behavior.

Editors run in designated same-origin frames so their scripts and styles do not
conflict with reading or another editor. Native write endpoints, permissions, CSRF,
tenant checks, rich text revisions and JSON save receipts remain authoritative.
If the selected database or session becomes stale, the draft remains visible and
recovery download remains available; the frame does not bypass server checks.

Manual regression checks: edit and cancel every syntax; save and Save and continue;
try node changes and browser Back/Forward with unsaved changes; edit independently
in both split panes and try closing the right pane; toggle compact layout; check
rich text undo/redo, object dialogs, tools resizing and recovery after a failed save.

## Selections containing objects

Rich text Copy now takes snapshots of loaded images, tables and codeboxes in their
text positions. Paste creates fresh objects, including when pasting into another
rich text editor. PNG snapshots contain image bytes, not references to database
rows; tables and codeboxes remain editable. Newly added, unsaved file attachments
can also be copied. Existing saved attachments and anchors use explicit text
placeholders; Cut does not remove a selection if any object could not be fully
copied. Wait for an image to finish loading before copying it. Browser clipboard
format restrictions and existing upload/object limits still apply.

Enable **Allow object deletion** to use Delete/Backspace, Cut, or paste-over-selection
on a selection containing objects. Removal is staged in the same undo history as
text and only reaches the database on Save. Manage objects remains available.

When pasted HTML contains external images, a confirmation offers to download its
public PNG/JPEG images through the existing web import policy. Declining keeps
text and links. Downloads carry no browser cookies and private/local addresses
remain blocked. Text, images and tables keep their selection order. Images that
cannot be fetched are counted and replaced by their alternative text. If import
fails or the editor changes while loading, the selection is not overwritten.
