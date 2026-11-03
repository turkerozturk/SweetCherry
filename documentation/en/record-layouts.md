# Custom single-record HTML layouts

A layout displays one matching record using HTML stored in the CTB itself. The existing list, table, PDF and XLSX views remain available. This step adds HTML layouts; export of these layouts to PDF is a later step.

## Start from an example

1. Open the existing template list or table and click **Record layouts**. The link beside a record preselects it.
2. On an ADMIN, writable connection, click **Create example layout** and confirm. This creates `_views` under the template if absent, then a Plain Text child named `Example layout`. Existing notes are not overwritten. Duplicate `_views` folders are reported instead of selecting one. A second submission cannot silently create another `Example layout`.
3. Select a record and click **Open layout** beside the design. Use **Edit HTML source** to edit its ordinary Plain Text node with the existing editor, undo/recovery and Save and continue controls.
4. Add additional Plain Text child nodes under the same `_views` folder for more designs. Their names are displayed in the chooser; IDs distinguish designs with the same name. Rich Text/Markdown/code nodes are listed but cannot be used as HTML source.

```text
cherrytemplate-artifacts                 ← template definition, existing variable list stays here
    _views                              ← exact, case-sensitive reserved name
        Example layout                  ← Plain Text HTML source
        Public report                   ← another Plain Text HTML source

Example application                     ← record elsewhere; tags equals cherrytemplate-artifacts
    application                         ← value of application
    findings                            ← value of findings
```

The `_views` node belongs to the chosen template occurrence. A shared folder/layout may be used, but its own `children` rows determine its place in the hierarchy; source text is read from its master. No schema columns/tables or external design files are added. Creation changes dates using the ordinary node-creation rules; reading/refreshing layouts does not change notes or their timestamps.

## Placeholders

| Placeholder | Value |
| --- | --- |
| `{{record.name}}` | Record title, escaped as text |
| `{{record.id}}` | Tree occurrence ID |
| `{{record.breadcrumbs}}` | Text breadcrumb |
| `{{template.name}}`, `{{template.id}}` | Template name and chosen occurrence ID |
| `{{view.name}}` | Design name |
| `{{now}}` | Server render time |
| `{{field.application}}` | Rendered content of the defined field named application |

Placeholders are case-sensitive and work in **text content**, not HTML attributes or CSS. The field key after `field.` must exactly match a valid variable in the template. Keys may contain spaces, dots or Unicode; braces are not supported and the complete placeholder token is limited to 256 characters. Use `<section>{{field.application}}</section>` rather than putting block content inside a heading/paragraph. Note content inserted into a field is never scanned again for placeholders: a note literally containing `{{now}}` keeps that text.

```html
<article style="max-width:900px;margin:auto">
  <h1>{{record.name}}</h1>
  <p>{{record.id}} · {{now}}</p>
  <p>{{record.breadcrumbs}}</p>
  <div style="display:grid;grid-template-columns:1fr 2fr;gap:12px">
    <strong>Application</strong>
    <section>{{field.application}}</section>
    <strong>Findings</strong>
    <section>{{field.findings}}</section>
  </div>
</article>
```

Project membership still compares the **whole tags value** to the template name. Field values come only from immediate matching child names. The ordered field model, missing/empty/multiple-match markers and repeated-definition diagnostics from [template reports](template-reports.md) are shared. A single-record request resolves only that record, rather than rendering every project. Shared record occurrences can be addressed directly by occurrence ID; the normal chooser retains the existing real-record list.

## Dynamic time settings

```yaml
myapp:
  custom-lists:
    views:
      date-time-format: "yyyy-MM-dd HH:mm:ss"
      time-zone: ""  # JVM/system zone; alternatively Europe/Istanbul or UTC
      max-design-characters: 131072
      max-rendered-characters: 16777216
```

`MM` means month and `mm` minute; `HH` is the 24-hour clock. These are Java DateTimeFormatter patterns, not JavaScript/PHP formats. Invalid patterns/zones and out-of-range limits fail configuration validation rather than changing the meaning silently. Restart after changing YAML. `{{now}}` is recalculated on **Refresh view/time** or page reload; it is not a continuously ticking browser clock and it never edits Created/Modified dates.

## HTML, links and limits

Supported HTML includes headings, sections, ordinary text formatting, lists, tables, links, embedded PNG/JPEG images, styles and inline CSS. Scripts, event attributes, forms, frames, objects and template expressions such as Thymeleaf/SpEL are not executed. The sanitized result is displayed in a sandboxed iframe with no script, same-origin or form privileges. A restrictive content policy blocks automatic external requests; remote images are omitted. Embedded CTB images/LaTeX remain available through field rendering. HTTP/HTTPS and internal node links open in a separate tab; unsupported URLs are stripped.

There is no script engine in this step: `{{now}}` is a named server value. A malformed or unsupported placeholder is shown as an explanatory marker instead of evaluating code. The source is never overwritten with the sanitized result. Design size defaults to 128 Ki characters, generated HTML to 16 Mi characters, and at most 64 design children can be listed. Over-limit requests stop instead of truncating content. The YAML maxima are 1 Mi design characters and 64 Mi output characters. These limits count Java string characters, not upload bytes. Database selection/tenant guards apply to all routes; only ADMIN plus writable plus a current CSRF-protected view can create the example.

## Manual checks

1. Create the example with confirmation; verify `_views` and `Example layout` are normal Plain Text CTB nodes. Open the same CTB in CherryTree to inspect them.
2. Edit the HTML to move field placeholders into two columns, add a second design, and open two different records. Verify record IDs/names and object content are correct.
3. Use missing/empty/duplicate fields and an unknown placeholder. Verify localized notices and no arbitrary duplicate value. Put `{{now}}` inside a note and verify it stays literal.
4. Change the date pattern/zone, restart and refresh. Confirm note Created/Modified dates are unchanged.
5. Add script/form/remote-image attempts to a test layout. They must not execute or fetch outside resources; supported links should open separately. The HTML source remains unchanged after display.
6. Try USER/read-only access, wrong template/layout/record combinations, duplicate `_views` folders and a stale tab after changing connections. Reads must work only for the selected source; unauthorized creation must not occur.
