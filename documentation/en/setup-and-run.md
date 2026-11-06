# SweetCherry 1.7.0 — installation and first use

SweetCherry is an open-source web companion for CherryTree CTB note databases. It provides browser views, mind maps, editing, and exports. It does not replace CherryTree. The supported starting point is an unencrypted SQLite `.ctb` file; `.ctd`, `.ctx`, and `.ctz` files are outside this guide.

## Windows 10/11, 64-bit

Open [SweetCherry Releases](https://github.com/turkerozturk/SweetCherry/releases/tag/v1.7.0). Choose one product asset:

| Asset | Use |
| --- | --- |
| `sweetcherry_1.7.0_all_os_portable_with_java_and_setup.exe` | Folder selection wizard and optional shortcuts |
| `sweetcherry_1.7.0_all_os_portable.zip` | All-OS scripts and application; requires system Java 17+ |

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

The same Java-free ZIP is intended for Windows, Linux (including Raspberry Pi ARM), and macOS with a matching Java 17+ runtime and native dependencies. macOS and 32-bit/i386 systems have not been tested; Java portability alone does not guarantee native SQLite/JDBC compatibility. The bundled Windows runtime is not for Linux, Raspberry Pi, or macOS. Install a JDK 17+ to build from source. Download the tagged [v1.7.0 source ZIP](https://github.com/turkerozturk/SweetCherry/archive/refs/tags/v1.7.0.zip), extract it, and run:

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

`CTBDATA/sweetcherry-hizli-baslangic.ctb` and `CTBDATA/sweetcherry-quickstart-en.ctb` contain the Turkish and English quick-start guides. Select **SweetCherry Quick Start (TR)** or **SweetCherry Quick Start (EN)** in the data source list. Their bundled `allTenants/quickstart-tr.txt` and `quickstart-en.txt` definitions are writable in 1.7.0 so ADMIN users can try editing. To preserve a guide unchanged, set `custom.isWritable=false` or practice on a separate copy. Existing external tenant files are not overwritten by a normal source build. The guides have no images; TODO bodies are intentionally empty. They can also be opened in CherryTree.

**What is a tenant?** In SweetCherry, this technical term means a data source connection definition. It comes from the application's multitenancy architecture. Switching sources resembles opening/closing files in a desktop application. A file in `allTenants` stores the database path/connection and opening settings, not the notes themselves. Several definitions may point to one CTB; deleting a definition does not delete the CTB.

## Editor helpers

In visual rich-text editing mode, clicking `☐`, `☑` or `☒` at the start of a line (optionally preceded only by spaces or tabs) cycles through those three states. Boxes preceded by other text or objects remain unchanged. Save writes the change to the database; Undo/Redo are supported. In text/preview mode, boxes remain ordinary text.

The **33~** button in both rich-text and plain-text editors inserts 33 tildes and a line break at the cursor, replacing selected text if any. Ctrl+R retains its browser refresh function.


[Database opening and home activity](home-activity.md) — `myapp.home`


[Consistent CTB backups](ctb-backups.md)
