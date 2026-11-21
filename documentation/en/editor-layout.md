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
