# SweetCherry 1.8.1 — installation and first use

SweetCherry is an open-source web companion for CherryTree CTB note databases. It provides browser views, mind maps, editing, and exports. It does not replace CherryTree. The supported starting point is an unencrypted SQLite `.ctb` file; `.ctd`, `.ctx`, and `.ctz` files are outside this guide.

## Windows 10/11, 64-bit

Open [SweetCherry Releases](https://github.com/turkerozturk/SweetCherry/releases/tag/v1.8.1). Choose one product asset:

| Asset | Use |
| --- | --- |
| `sweetcherry_1.8.1_all_os_portable_with_java_and_setup.exe` | Folder selection wizard and optional shortcuts |
| `sweetcherry_1.8.1_all_os_portable.zip` | All-OS scripts and application; requires system Java 17+ |

Only the setup EXE includes Java 17 (Windows x64). The all-OS ZIP has no Java; install Java 17+ matching your OS/CPU and use its run scripts. Despite its filename, the setup EXE itself runs only on Windows 10/11 x64. The automatically generated **Source code** archives contain source, not the ready-to-run application. Do not run the installer from inside a ZIP.

### Installer

1. Download the setup EXE and run it.
2. Select a writable folder, for example `C:/Users/YourName/Documents/SweetCherry`. The default is a `SweetCherry` folder next to the installer. Administrator privileges are not required.
3. Optionally create Desktop/Start menu shortcuts, then finish and start SweetCherry.
4. The control window shows startup progress. When ready, the browser opens `http://localhost:8080`. You can also use **Open in Browser**.

The EXE is unsigned; Windows may show a reputation warning. Confirm the download came from this project's release page. `sha256sums.txt` provides the published file hashes. This does not claim that the EXE has a verified publisher signature.

### Portable ZIP

Extract the entire ZIP and keep the complete `SweetCherry` folder together. With Java 17+ installed, use `run.bat` on Windows, `sh ./launcher.sh` (desktop) or `sh ./run.sh` (terminal) on Linux/Raspberry Pi, and `sh ./run.command` on macOS. The included `SweetCherry.exe` requires the Windows runtime from the setup package; use `run.bat` for the Java-free ZIP. On Unix, invoking scripts with `sh` also works when ZIP extraction does not preserve executable permissions. The setup package creates a portable folder with Java; no uninstaller is registered.

## First login

When using the Windows control window, click **Copy Admin Password**, paste into the browser password field and sign in; the administrator username is prefilled. Script users can read the generated password in the local console or credentials file. First startup also creates `login-credentials.properties` in the application folder. See [first-start help](../tr/first-start.md#english) for local console output, Linux desktop launching and configuration defaults.

| Account | Username | Password entry |
| --- | --- | --- |
| Administrator | `admin` | Value after `admin.password=` |
| Regular user | `user` | Value after `user.password=` |

`admin.password` is a setting key, not your username. Enter the value after the equals sign as the password. Keep this file private. To change a generated password, stop the app, edit the value, and start it again. Explicit `myapp.login.*` credentials in `application.yml` take priority over this file.

## Open the included demo

Sign in and select the demo data source. The package includes `CTBDATA/demo.ctb` and `allTenants/demo.txt`. Use the demo to explore the desktop/mobile readers, mind maps, rich-text editor, bookmarks, and PDF export. Some actions require the administrator account and a writable data source.

## Open your own CTB

SweetCherry does not upload or copy an existing CTB simply because a tenant configuration is loaded. A tenant file identifies a database accessible to the machine running SweetCherry. A server cannot access a path that exists only on your phone or another PC.

Administrator users can use the data-source menu's tenant configuration wizard to create/edit a connection and open the database. Choose an existing CTB on the machine running SweetCherry, give the connection a name, and start read-only. In desktop mode a file picker is available. When accessing a remote server, a path on your laptop is not a path on that server.

Existing manual tenant files and uploads remain supported. Copy the example tenant config to `allTenants/my-notes.txt` and adapt it:

```properties
name=My Notes
security-role=ADMIN
datasource.url=jdbc:sqlite:C:/Users/YourName/Documents/Notes/my-notes.ctb
datasource.driver-class-name=org.sqlite.JDBC
datasource.init-mode=always
custom.isWritable=false
custom.newNodeName=New Node
custom.newNodeTags=
```

Use a unique display name and an existing CTB file. Forward slashes make Windows properties paths easier to write. Reload the data sources and select **My Notes**. Administrator users can also upload a `.txt` tenant configuration through the data-source menu. A config may contain database credentials; do not share it publicly.

Start with `custom.isWritable=false`. Setting it to `true` enables supported write operations for authorized users. Content read-only flags are separate from tenant write permission.

**Manual and optional scheduled CTB backups are available under System → CTB backups (ADMIN).** Scheduling is disabled until configured; see [backup setup and recovery](ctb-backups.md). Keep copies on another device and avoid simultaneous writes from SweetCherry and CherryTree. CTB files may stay outside the application folder. Copying the closed application folder does not back up those external databases.

## SQLite, MariaDB and PostgreSQL connections

SweetCherry can switch between SQLite CTB files and compatible CherryTree-schema databases on MariaDB/MySQL or PostgreSQL through tenant configurations. Hibernate uses the selected tenant's JDBC database type/version. A remote database must already contain the compatible schema and content; selecting a connection is not a CTB-to-server migration. SweetCherrySync is a separate project for synchronization.

A database login must have CONNECT/schema/table permissions appropriate to its database system; `custom.isWritable=true` does not grant SQL privileges. Start read-only. If another tool synchronizes into this database, avoid simultaneous editing because later synchronization can overwrite your changes.

Old SQLite files without `children.master_id` show an explanatory warning. The supported upgrade requires explicit `custom.allowLegacySchemaUpgrade=true` and ADMIN selection. Preserve a separate backup before allowing a schema change. Arbitrary missing columns are not automatically invented.

## Application settings

ADMIN users can edit selected non-security application settings from the System menu and use the confirmed restart action. The desktop control window also offers a local YAML editor, including configuration values unavailable in the web editor. Treat these files as sensitive, review changes, and restart when required. Keep application configuration and data during upgrades.

## Upgrading an existing portable installation

Stop SweetCherry. Preserve your application folder, external CTB files, `allTenants`, external YAML files, `login-credentials.properties`, and any exports you need. Extract the new package into a separate folder, then carry over your own configuration/data deliberately. Do not overwrite your credentials or notes with demo files, and do not merge the new bundled Java runtime with the old one. Recreate shortcuts if the application location changes. Existing external YAML overrides remain effective; new defaults do not override them automatically.

## Stop, move, or remove SweetCherry

Closing a browser tab does not stop the server. Use **Stop SweetCherry** in the control window, the tray menu, or the administrator shutdown action in the web interface. Closing the control window offers either stopping or keeping the server running.

After stopping, you may copy or move the application folder. Recreate shortcuts after moving it. This portable installer does not create an uninstaller. To remove the app, stop it and delete the folder only after preserving any notes/configs you want to keep.

## Other systems and source builds

The same Java-free ZIP is intended for Windows, Linux (including Raspberry Pi ARM), and macOS with a matching Java 17+ runtime and native dependencies. macOS and 32-bit/i386 systems have not been tested; Java portability alone does not guarantee native SQLite/JDBC compatibility. The bundled Windows runtime is not for Linux, Raspberry Pi, or macOS. Install a JDK 17+ to build from source. Download the tagged [v1.8.1 source ZIP](https://github.com/turkerozturk/SweetCherry/archive/refs/tags/v1.8.1.zip), extract it, and run:

- Windows: `first-run.bat`
- Linux/Raspberry Pi: `sh ./first-run.sh`
- macOS: `sh ./first-run.sh` or `first-run.command`

The first build needs internet access to download Maven/dependencies. Scripts produce `release/SweetCherry`. Use that entire generated folder, including data, tenant definitions, scripts, documentation and licences; copying only the JAR is insufficient for the documented first-use setup. Later, run its `run.bat`, Linux `launcher.sh` (desktop) / `run.sh` (terminal), or macOS `run.command` from that folder; rebuilding is unnecessary. On Linux, `sh ./release/SweetCherry/install-desktop.sh` creates optional per-user shortcuts. macOS has not been verified on a physical Mac.

## Troubleshooting and network access

- **Port already in use:** inspect the control-window message and `myapp.log`; the app does not stop the other program or silently change ports.
- **Second click does nothing:** the Windows launcher prevents another instance. Open the existing control window from the tray or browse to `http://localhost:8080`.
- **Database missing:** correct its tenant path; SweetCherry does not create an empty CTB in place of a missing file.
- **Cannot replace an open CTB:** close the data source first; also close CherryTree/DB Browser connections to that file.
- **Remote access:** the default is local-only. LAN/internet access requires explicit binding, firewall/router, and HTTPS/proxy configuration. Do not expose plain HTTP login to the internet. See the [network guide (Turkish)](../tr/network-access.md) and [transport security guide (Turkish)](../tr/https-and-session-security.md).

## Known limits

Raw content search operates on stored `node.txt`, including rich-text XML. Search results use POST; browser Back may require resubmission or show an expired-document message. CTB exports now add a source container: supported internal links within the selected branch are remapped, and missing/excluded targets point to the container report. Shared hierarchy is preserved. Embedded codebox text and syntax can be edited; broader rich-text compatibility remains limited. LaTeX uses offline JLaTeXMath; unsupported expressions show source text rather than breaking the page.

## Bundled quick-start guides

`CTBDATA/sweetcherry-hizli-baslangic.ctb` and `CTBDATA/sweetcherry-quickstart-en.ctb` contain the Turkish and English quick-start guides. Select **SweetCherry Quick Start (TR)** or **SweetCherry Quick Start (EN)** in the data source list. Their bundled `allTenants/quickstart-tr.txt` and `quickstart-en.txt` definitions are writable in 1.8.1 so ADMIN users can try editing. To preserve a guide unchanged, set `custom.isWritable=false` or practice on a separate copy. Existing external tenant files are not overwritten by a normal source build. The guides have no images; TODO bodies are intentionally empty. They can also be opened in CherryTree.

**What is a tenant?** In SweetCherry, this technical term means a data source connection definition. It comes from the application's multitenancy architecture. Switching sources resembles opening/closing files in a desktop application. A file in `allTenants` stores the database path/connection and opening settings, not the notes themselves. Several definitions may point to one CTB; deleting a definition does not delete the CTB.

## Editor helpers

In visual rich-text editing mode, clicking `☐`, `☑` or `☒` at the start of a line (optionally preceded only by spaces or tabs) cycles through those three states. Boxes preceded by other text or objects remain unchanged. Save writes the change to the database; Undo/Redo are supported. In text/preview mode, boxes remain ordinary text.

The **33~** button in both rich-text and plain-text editors inserts 33 tildes and a line break at the cursor, replacing selected text if any. Ctrl+R retains its browser refresh function.


[Database opening and home activity](home-activity.md) — `myapp.home`


[Consistent CTB backups](ctb-backups.md)

### Changing a node's content type

In **Node Properties**, change Rich Text to Plain Text or a supported code syntax (including Markdown or HTML), then choose Save. An explicit Yes/No dialog explains the data loss. Formatting is removed; table cells, codebox text and link targets become readable text. Embedded images, attachments and anchors are removed, leaving filenames or placeholders. Back up the database and save any needed attachments first. Shared occurrences of the same node also change. This page cannot undo the conversion; conversion back to Rich Text is not supported.

If the source changes after confirmation preparation, the operation is rejected. Content and object removal are saved in one transaction. Conversion is limited to 2,000,000 source/object-text characters and 2,000 objects. For a locked node, clear the read-only setting, save and reopen Properties before changing its type. Plain Text/code-to-code changes retain the existing source text; selecting Markdown or HTML changes its interpretation rather than generating markup.

### Appearance and safe application settings

The default remains the existing light appearance. To enable dark application chrome, add or edit this in the active external `application.yml`, then restart SweetCherry:

```yaml
myapp:
  appearance:
    theme: dark # light or dark; default: light
```

The administrator can also select **Light** or **Dark** on **System → Application settings** (`/system/settings`). The theme is application-wide, including login, desktop/mobile navigation, calendars, mind maps and editors; it does not follow the browser/OS theme. Rich-text note paper, visual rich-text editing, rendered code snippets and sandboxed HTML previews retain a light content surface so stored colors, images and formatting stay readable. Stored note contents and title colors, exported documents and the desktop launcher are unchanged.

The settings page now also exposes the initial language; opening the latest modified note; home created/modified list minima (0 hides the corresponding list); initial desktop tree width; calendar week start, time zone, entry/card limits and day-preview character/line limits; folder/text export limits; and record-layout date/time format, optional time zone and template/output limits. Saved browser choices still take priority over initial language/tree defaults. Calendar `system` uses the operating-system/JVM time zone; an empty record-layout time zone does the same. All values are validated, changes preserve YAML comments/unrelated settings, and the page distinguishes saved values from the effective values used by the running process. Restart after saving; command-line/environment overrides still take priority.

Only the active external YAML can be edited here. Network listeners, proxy/TLS/login/security settings, credentials, storage paths, backup deletion/retention policy, operations profiles and arbitrary YAML keys remain outside this form. Existing dedicated backup controls and documented YAML configuration still apply.

### Focused connection map

Open **Mind Map → Connection map** on a note, or choose **Connection map** from the application view menu. The view menu starts at the smallest available tree-occurrence ID. Click a node to make it the focus; its readable content appears beside the map. Incoming internal links appear left, outgoing links right. Enable hierarchy to add parents above and children below, using dashed parent-to-child arrows. A neighbour with several relation types appears in the right-hand group. The node list provides the same navigation without relying on the canvas. Back/Forward preserve the focus history; **Open note** opens the normal reader. On mobile the preview appears below the map.

This is a read-only, offline Cytoscape.js view derived from existing CherryTree rich-text and embedded-object internal links. Plain text, Markdown/HTML source strings and shared tags do not create new graph edges. Shared occurrences keep separate IDs and show the links of their master content. Repeated source-to-target links are grouped; missing targets are omitted and counted in the warning. A missing anchor can still show an edge to its available target note. The preview opens the whole note, not the original link's anchor.

Choose 40, 100 or 200 neighbours. A warning identifies omitted neighbours, unreadable notes, broken links or a partial scan. The derived index is scoped to the selected data source and view token, cached for 30 seconds in at most eight entries, and can be rebuilt with **Rescan links** after edits or external CherryTree changes. Bounds: 10,000 node-content metadata rows, 20,000 tree occurrences, 50,000 relevant image/object rows, 10,000 rich-text notes, 2,000,000 characters per note and 32 Mi characters per scan. At most 50,000 expanded link occurrences are inspected and 10,000 retained for the map index. Exceeding metadata limits fails the map; text/link limits produce an explicitly partial map. No graph tables, saved coordinates or changes to CTB files are introduced.
