# Folder tree / text ZIP export

This implements the folder-oriented plain-text export requested in [CherryTree issue #2914](https://github.com/giuspen/cherrytree/issues/2914), independently in SweetCherry.

Open a node in either desktop or mobile reading view, choose **Export → Folder tree / text files (.zip)**, select whether descendants are included and choose **Download ZIP**. This is available to signed-in readers, including read-only database connections. Nothing is added to the source database, and no CTB schema/timestamp is changed. PDF, the four existing CTB exports and Freeplane export now share the **Export** menu; Markmap and Mermaid viewing remain in **Map**.

## Mapping nodes to files

- The selected occurrence is the ZIP root; ancestors above it are not added. Children follow the occurrence hierarchy and existing sequence. Shared occurrences read their master content but follow their own children, not the master's other children.
- With descendants enabled, any node with children becomes a directory. Its own non-empty text is written to `_content.txt` inside it. Empty branches still create their directories without an empty content file.
- Leaves use the node title as their filename. Existing extensions and dotfiles are kept; names without a dot receive `.txt`. No language-based extension is guessed. With descendants disabled, even a parent node becomes a single file.
- Plain text, source code and Markdown are written as stored, in UTF-8 without BOM or extra headings; existing text line endings are retained. Rich text is converted through the existing object-aware text converter: links include targets, tables become tab-separated rows and codeboxes include their source. Image/attachment/anchor objects become descriptions, not binary files. This is a text export, not an executable project generator or a full CTB backup.

A filesystem cannot contain both a file and a folder at the same path. For the issue's example, exporting Playbooks therefore uses:

| ZIP path | Purpose |
| --- | --- |
| `Playbooks/add_new_user.ansible` | Leaf source, original extension |
| `Playbooks/set_system_settings.ansible/_content.txt` | Source belonging to a node that also has children |
| `Playbooks/set_system_settings.ansible/files/enable_system_tweaks.conf` | Nested leaf source |
| `Playbooks/set_system_settings.ansible/files/hugepages.systemd` | Nested leaf source |

Unsafe path characters, traversal components, Windows device names and overly long components are sanitized. Unicode remains supported. Collisions are compared case-insensitively (including file-versus-folder and `_content.txt` conflicts) and resolved by an occurrence-ID suffix before the extension. Extremely long full paths are rejected rather than silently dropping nodes. Operating-system extraction/path-length restrictions may still apply.

## Limits

Add under the existing `myapp` key in external `application.yml` and restart:

```yaml
myapp:
  text-tree-export:
    max-nodes: 10000
    max-depth: 64
    max-node-characters: 8388608
    max-text-bytes: 67108864
```

Values must be positive. Depth starts at zero for the selected node. The byte budget counts total uncompressed UTF-8 file contents, not ZIP size or web-server upload size. Rich-text object text is checked as well. Components are bounded to approximately 180 UTF-8 bytes; complete archive paths are bounded to 2,048 bytes. Oversized or invalid hierarchies cancel the whole export before a download begins; the form explains which settings to inspect. A missing source, malformed rich text or missing master also cancels instead of omitting data.

One archive is prepared at a time per application. The ZIP is staged in the Java temporary directory rather than accumulated in heap memory, and deleted after download or a handled failure. Sufficient temporary disk space is required. A process crash may leave `sweetcherry-text-tree-*.zip` temporary files for OS/administrator cleanup. No attachment/image binary column is selected. Server-side tenant-view checks reject stale tabs, and POST uses normal CSRF protection.

## Manual checks

1. Export plain-text/code/Markdown nodes with extensions, dotfiles, Unicode and CRLF; check their exact text and filenames.
2. Export a parent with its own text and an empty folder node; verify `_content.txt`, nested directories and ordering.
3. Export shared occurrence 11 from the shared-tree SQL fixture; its own descendants should appear, not master 2's child 3.
4. Try duplicate/case-only names, `_content.txt`, `CON`, slashes and `..`; extraction must not overwrite files or escape the output directory.
5. Export rich text containing links, tables, codeboxes and attachments; check text representations. Test single-node scope, size limits, read-only connections, stale tabs, both languages and a narrow mobile menu.
