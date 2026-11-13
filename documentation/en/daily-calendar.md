# Daily calendar

Choose **Daily calendar** from the view menu or the day/week/month toolbar. Click a date heading in the weekly or monthly calendar to open that day. The canonical URL is `/nodes/calendar/day?date=2026-11-13`; `/calendar/day` is also accepted. The selected database and stale-tab checks still apply.

Cards follow **Created**, using `myapp.calendar.time-zone`, in ascending timestamp order, with stable node/occurrence ordering for ties. Read them left to right, then top to bottom. There are three columns on wide screens (1100 px and above), two on tablets (700–1099 px), and one on smaller screens. There are no empty hourly slots: a day with only a few notes stays compact, while long previews remain readable.

Each card shows creation time, the note's icon and title styling, and a **plain-text** excerpt. Rich-text XML is converted into text; Markdown/HTML is rendered to readable text and escaped for display. Images, files and tables are represented by icons and counts, with codebox counts when present. Counts include the selected master content for shared occurrences. Rich-text anchors are not counted as images. Embedded binary data, table bodies and codebox bodies are not fetched for previews. No remote resources are loaded by the preview.

Select a title to open the existing calendar dialog, including content and guarded creation-date editing. **Open node** opens the full note. Filters and date navigation retain the selected tags and matching mode. An empty day has an explicit message; an exceeded entry limit has an explicit warning.

## Configuration

Add or change these keys in the external `application.yml`, then restart:

```yaml
myapp:
  calendar:
    day-max-entries: 300
    day-preview-max-characters: 600
    day-preview-max-lines: 8
```

Merge the keys into the existing `myapp.calendar` section; do not create duplicate YAML sections. Existing external configuration need not contain them: defaults apply automatically.

- Characters are Unicode code points, so an emoji is not split; an ellipsis counts towards the limit. The supported range is 1–20000.
- Lines limit explicit text lines on the server and wrapped display lines in the browser. The supported range is 1–100. Browser wrapping can hide additional text; use Open node for the complete content.
- Day cards default to a maximum of 300 occurrences (range 1–2000), further bounded by the existing `max-entries`. This keeps initial page size and preview work bounded.
- At most the first 65536 source characters per distinct content node are read for the preview. Very large notes may show an abbreviated excerpt; malformed/prohibited rich-text XML shows a localized fallback instead of raw XML. Full content remains available through the existing node view and its own limits. For oversized HTML/Markdown, inline image/table counts cover only the inspected prefix and a card message makes this explicit; database attachment/image/table/codebox counts remain complete.

A day is measured between local midnights, so daylight-saving days may have 23 or 25 hours. No database schema change or note-content write is needed for this view. Monthly and weekly views continue to read metadata only; they do not fetch daily excerpts.
