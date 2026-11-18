# Linked-node content fragments (transclusion)

Open **View Mode → Show linked node contents**, beside the content zoom controls.
The default remains ordinary links. The toggle applies to rich-text node content
in the tree workspace, mobile reader and ordinary node reader. The choice is
remembered in this browser across node navigation/reloads until toggled again;
other tabs receive preference changes. If browser storage is unavailable, it
works for the current page only.

For example, keep a repeated software-installation step and its screenshots in
one note. Link to that note from several manuals. In this reading mode, each
link is replaced at its position by the referenced note's content. A small
**Open source** link identifies the original and opens it separately for normal
navigation/editing. Disabling the toggle restores the original links and layout.
No text, tags, timestamps, link targets or database schema are modified.

Rich-text source notes retain stored images, tables and codeboxes using the
existing rendering pipeline. Referenced plain text, Markdown, sanitized HTML
and code notes are also readable. Only rich-text contents are traversed for
further node links; plain/code/Markdown/HTML targets are terminal fragments.
External/file/download links keep their existing behavior. A link to a node
anchor includes the whole target note, not a substring starting at the anchor.
Same-node anchor navigation remains a link. Copied anchors are given unique IDs
so repeated fragments do not interfere with each other or their host.

The feature is a browser reading preference. Editors, PDF generation, CTB/text
exports, mind maps and stored links keep their normal representation. Newly
included images support the reader's original-image opening controls.

## Updating the source

Edit the source note normally. Its updated content appears when the host is
reopened/reloaded, or when this view is toggled off and on. There is no background
live synchronization while editing another tab. Repeated references share a
fetch only within the currently displayed note; no persistent fragment cache
is kept in browser storage. The current source token accompanies fragment,
image and attachment URLs; old-source requests are rejected after a database
switch. Disabling the feature or navigating away ignores late responses.

## Bounds and fallback

Circular references are detected by actual content ID, including shared-node
aliases. Missing/corrupt targets, managed REMINDERS/audit/trash branches and
oversized fragments keep the original clickable link, with a short reason.
A branch with a broken/cyclic ancestor path is not included. Embedded forms,
scripts, frames and remote images are not included. A failed inclusion never
changes or silently removes the stored link.

```yaml
myapp:
  node-fragments:
    max-depth: 3
    max-inclusions: 20
    max-source-characters: 250000
    max-html-characters: 1048576
    max-object-bytes: 16777216
    max-objects: 200
```

Merge into the existing `myapp` mapping and restart after changing limits.
Depth is 1–5; inclusions 1–50 per displayed note. Repeated references count
separately. The source budget (1–1000000 characters) includes note, table and
codebox text. HTML is limited to 1024–2097152 characters per response, plus a
fixed 4,194,304-character inclusion budget per displayed note. Image/attachment
payloads are checked in metadata before rendering (1024–33554432 bytes total
per target); all embedded object types count toward 1–500 objects. A request
times out after ten seconds. These limits are independent of content zoom.

Readers and read-only connections can use the feature. The endpoint is a
current-tenant, authenticated GET with `no-store` responses; there are no new
write operations. Normal visibility rules for protected/trashed helper branches
are enforced before reading note bodies or binary payloads.

## Checks

```text
mvnw.cmd -Dtest=NodeFragmentServiceTest,NodeFragmentTemplateTest,NodeFragmentSecurityTest,ReminderTemplateTest test
node --test src/test/js/*.test.cjs
mvnw.cmd clean package
```

Manually check two links in one formatted paragraph, table cells and lists;
image/table/codebox targets; the same fragment twice with local anchors; a
shared alias and A → B → A recursion; missing and oversized targets. Toggle off
and verify original surrounding formatting/links, navigate between notes in
both readers, edit a source and reopen the host, and switch databases while a
request is pending. Verify image opening with both pointer and keyboard.
