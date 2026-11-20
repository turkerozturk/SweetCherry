# Live web bookmark nodes

A node behaves as a live web bookmark when all of these are true:

- Its **node Read Only** flag is set. A read-only database connection by itself does not qualify.
- Its complete name is a valid `http://` or `https://` URL.
- Its content is empty, with no embedded images/attachments, tables or codeboxes. Empty rich-text markup counts as empty.

Open the node in `/tree`, split view, or the mobile reader. The application fetches the public page on the server and shows a static preview. Reopening the node issues a new request; **Reload web page** fetches it again without changing the selection. The **CT_LINK_WEBSITE** icon at the far right of the title opens the original URL in a new browser tab. Preview links are plain text and cannot be followed. Nothing is saved into the node, so this is not an offline archive or import. PDF exports and linked-content fragments do not fetch web bookmark pages.

The preview reuses the existing public web importer and its checked/pinned DNS resolution, redirect validation, cookie/authentication isolation, time/size limits and two-job concurrency bound. Private, loopback, link-local and reserved addresses are blocked on every connection, including image downloads and redirects. Only standard HTTP/HTTPS ports are accepted. The service sends its own requests; the browser's website cookies, login state and scripts are not used.

The imported page is cleaned again using a formatting-only allowlist. Scripts, event handlers, remote styles, links, forms, frames and active media are removed. A limited number of validated, server-fetched images can be included as embedded PNG data. The result is shown in an opaque sandbox iframe with no script permissions and a Content Security Policy that blocks external resources. The original-site icon is outside this sandbox and is the explicit way to visit the website.

This is a reader for public static content, not a complete browser. Login-only, JavaScript-rendered, bot-protected, compressed-only, oversized or unavailable pages can fail or show incomplete content. A localized message leaves the original-site icon available. Content updates, redirects and website availability remain controlled by the website.

The preview requires an authenticated session and the current CTB view token; the endpoint accepts a node ID, never an arbitrary URL supplied by a client. It rechecks the node's current conditions before fetching. Shared occurrences resolve through their real content node. Changing the node to nonempty or removing Read Only returns it to normal content display.

The feature is enabled by default. To disable the preview and website icon, use `/system/settings` or set the following in external `application.yml`, then restart:

```yaml
myapp:
  web-bookmarks-enabled: false
```

The separate `myapp.web-import-enabled` setting still controls editor imports only.
