# Custom lists setup

Open `/cherrytemplatetasks`. The page locates exactly one Plain Text node with the configured name anywhere in the database. It does not create or modify anything merely by opening the page. Duplicate exact names and incorrect content types are reported instead of silently choosing a node.

## Names and precedence

Defaults in `application.yml`:

```yaml
myapp:
  custom-lists:
    main-node-name: cherrytemplatetasks
    template-prefix: cherrytemplate
```

Optional per-connection properties override the global values:

```properties
custom.customListsNodeName=my-lists
custom.templatePrefix=my-template
```

Omitted, empty or invalid tenant values fall back to the global setting. Global names must be nonempty and at most 255 UTF-16 characters. A separating hyphen is added unless the prefix already ends with one. Reload connection configurations after editing tenant properties; restart after editing global YAML. Changing these settings does not rename existing nodes. The tenant wizard preserves these additional properties when an existing connection is edited.

## Definitions and creation

In the main node, write one key per line, optionally followed by a literal TAB and a display label:

```text
projects	My projects
devices	Device list
#software	Software
```

The example contains actual TAB characters. Use the new TAB toolbar button in the Plain Text editor; physical Tab keeps its normal browser focus behavior. Blank lines are ignored and lines starting with `#` (after boundary whitespace) are disabled and never searched. Duplicate active keys are explicitly marked. With defaults, `projects` locates `cherrytemplate-projects`. Name matching is exact and case-sensitive even on a case-insensitive database.

The page shows matching IDs linking to the node and a separate link to the existing template report. ADMIN users on writable connections can create a missing main/template node with confirmation. New nodes are empty Plain Text nodes at the end of the root level; fill in their content afterward. Creation rechecks the current definitions and selected connection, and rejects existing/ambiguous/disabled definitions. Readers and read-only connections cannot create nodes here.

The two in-app help pages use localized visual examples in English and Turkish, including the effective names. The existing report engine and special child-node conventions are unchanged. In particular, its legacy project selection compares the **whole `node.tags` value** with the template node name; it is not a token-based tag search. This setup work does not change field selection, missing-value handling or HTML/XLSX/PDF layouts.

## Node names and editor buttons

When names are authored in Properties, quick creation, ordinary creation or new duplication, boundary whitespace (including NBSP), control and Unicode format characters are removed. Internal spaces and visible text are preserved. Existing database names are not bulk rewritten; imports/exports retain their original names. Content-only saves do not rename nodes. Duplicating a node cleans only the new stored name, leaving the source unchanged.

The Plain Text/Markdown toolbar includes ☐, ☑, ☒, • and TAB; the visual Rich Text toolbar adds TAB beside its existing symbols. Insertions use the existing selection, undo/redo and recovery flow. They do not intercept keyboard Tab.

## Manual checks

1. With defaults, open setup with no main node, confirm creation, and verify its type/name/root position.
2. Enter the TAB-separated example, save and return: projects/devices are checked; software is disabled. Create a missing template and follow both its ID and report links.
3. Create duplicate main names, duplicate template names or repeated active keys; verify explicit explanations and no automatic choice. Use Markdown/Rich Text for the main node to verify the Plain Text warning.
4. Test global settings, then tenant overrides; check both localized help pages and the report's embedded help.
5. Verify read-only/USER access shows information without creation, and an old tab after a connection switch cannot create in the new database.
6. Insert TAB/symbols in both editors, undo/redo and save. Save a node name with leading/trailing spaces, NBSP or zero-width format characters; verify only the boundaries are removed.
