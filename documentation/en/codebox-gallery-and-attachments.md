# Codebox gallery and attachment browser

These views require a selected database and a signed-in reader or administrator. They also work with read-only database connections. They do not update note/object records, timestamps or audit history. Shared occurrences do not duplicate stored object rows; objects still retained in the trash are included.

## Codebox gallery

Open **View Mode → Codebox gallery**, or `/codebox-gallery`. Search node titles, optionally enter a stored syntax value, and sort by node creation date, node title, syntax or composite codebox identity. The syntax field offers up to 200 observed values as suggestions; an unlisted value may still be entered.

Cards contain bounded text previews, syntax, character counts, node links and optional identity/date metadata. The larger viewer supports previous/next within the current page, line wrapping, text selection, copying the displayed code and downloading the complete original source. Escape closes the viewer. Arrow keys outside the code/input area navigate; inside code they retain normal scrolling and selection. Metadata/wrapping preferences are remembered locally in the browser.

Code is always text and is never executed or treated as rendered HTML. Highlighting uses the existing bundled language allowlist and respects `myapp.syntax-highlighting.enabled`. Unknown syntaxes stay plain text. Viewer content over 20,000 characters stays plain text to avoid expensive highlighting. Copying retains original displayed text rather than highlighted markup.

Merge these defaults under the existing `myapp` section of `application.yml`, or change them in **System → Settings**, then restart:

```yaml
myapp:
  codebox-gallery:
    font-size: 14
    preview-max-characters: 2400
    preview-max-lines: 20
    viewer-max-characters: 200000
    page-size: 12
```

Allowed ranges: font size 10–32 CSS pixels; preview characters 1–10,000; preview lines 1–100; viewer characters 1–1,000,000; page size 1–36. Display clipping is marked explicitly and does not alter storage. Previews select only a bounded substring, not whole codebox bodies. Viewing/downloading a complete source has a fixed 2,097,152 UTF-16-unit limit. Downloaded source below that limit is exact and untruncated, including line breaks. Codebox IDs are `node_id:offset`, and offsets may change after editing their containing note.

## Attachments

The existing `/attachments` page now supports literal filename filtering, ascending/descending filename and numeric byte-size sorting, and newest-first ordering. Sorting/filtering applies to all matches before 50-row pagination. Filename/size column headings also toggle their sort direction. Filters and source tokens survive pagination. Desktop and mobile layouts use the same query/results; extra metadata columns are hidden on narrow displays, with node links retained below filenames.

An attachment is a named, non-anchor `image` row with a binary payload, excluding the special `__ct_special.tex` LaTeX object. A named PNG/JPEG file attachment is still an attachment; ordinary embedded pictures have an empty filename and are excluded. No file-type guessing or binary decoding is used for this classification.

The list selects filename/date/node/byte-length metadata only, never all BLOBs. New list download links use `/attachments/{nodeId}/{offset}/download`. A single file is read and copied to the response inside the routed read transaction; JDBC drivers may still buffer that individual payload. No new attachment-size limit is imposed by the list. Downloads use `application/octet-stream`, attachment disposition and `nosniff`. Stored path components/control characters are removed from the suggested download filename, with UTF-8 filename encoding. Existing node-content download routes remain unchanged.

Both views carry selected-database tokens in data/download links and use `no-store`. Stale object requests return a conflict instead of silently reading a newly selected database. Stored code, filenames and node titles are escaped/displayed as text; no remote source URLs are fetched.
