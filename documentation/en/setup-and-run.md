# SweetCherry 1.0.0 — installation and first use

SweetCherry is an open-source web companion for CherryTree CTB note databases. It provides browser views, mind maps, editing, and exports. It does not replace CherryTree. The supported starting point is an unencrypted SQLite `.ctb` file; `.ctd`, `.ctx`, and `.ctz` files are outside this guide.

## Windows 10/11, 64-bit

Open [SweetCherry Releases](https://github.com/turkerozturk/SweetCherry/releases/tag/v1.0.0). Choose one product asset:

| Asset | Use |
| --- | --- |
| `SweetCherry-1.0.0-windows-x64-setup.exe` | Folder selection wizard and optional shortcuts |
| `SweetCherry-1.0.0-windows-x64.zip` | Extract and run without an installation wizard |

Both include a Windows x64 Java 17 runtime. You do not need to install Java. The automatically generated **Source code** archives contain source, not the ready-to-run application. Do not run the installer from inside a ZIP.

### Installer

1. Download the setup EXE and run it.
2. Select a writable folder, for example `C:/Users/YourName/Documents/SweetCherry`. The default is a `SweetCherry` folder next to the installer. Administrator privileges are not required.
3. Optionally create Desktop/Start menu shortcuts, then finish and start SweetCherry.
4. The control window shows startup progress. When ready, the browser opens `http://localhost:8080`. You can also use **Open in Browser**.

The EXE is unsigned; Windows may show a reputation warning. Confirm the download came from this project's release page. `SHA256SUMS.txt` provides the published file hashes. This does not claim that the EXE has a verified publisher signature.

### Portable ZIP

Extract the entire ZIP. Open the extracted `SweetCherry` folder and double-click `SweetCherry.exe`. Keep `runtime`, the JAR, scripts, and configuration together. Removing `runtime` prevents the EXE from starting.

## First login

Use **Application Folder** in the control window. First startup creates `login-credentials.properties` in that folder.

| Account | Username | Password entry |
| --- | --- | --- |
| Administrator | `admin` | Value after `admin.password=` |
| Regular user | `user` | Value after `user.password=` |

`admin.password` is a setting key, not your username. Enter the value after the equals sign as the password. Keep this file private. To change a generated password, stop the app, edit the value, and start it again. Explicit `myapp.login.*` credentials in `application.yml` take priority over this file.

## Open the included demo

Sign in and select the demo data source. The package includes `CTBDATA/demo.ctb` and `allTenants/demo.txt`. Use the demo to explore the desktop/mobile readers, mind maps, rich-text editor, bookmarks, and PDF export. Some actions require the administrator account and a writable data source.

## Open your own CTB

SweetCherry does not upload or copy an existing CTB simply because a tenant configuration is loaded. A tenant file identifies a database accessible to the machine running SweetCherry. A server cannot access a path that exists only on your phone or another PC.

Copy the example tenant config to `allTenants/my-notes.txt` and adapt it:

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

**SweetCherry does not make automatic backups.** Back up important databases yourself and avoid simultaneous writes from SweetCherry and CherryTree. CTB files may stay outside the application folder. Copying the closed application folder does not back up those external databases.

## Stop, move, or remove SweetCherry

Closing a browser tab does not stop the server. Use **Stop SweetCherry** in the control window, the tray menu, or the administrator shutdown action in the web interface. Closing the control window offers either stopping or keeping the server running.

After stopping, you may copy or move the application folder. Recreate shortcuts after moving it. This portable installer does not create an uninstaller. To remove the app, stop it and delete the folder only after preserving any notes/configs you want to keep.

## Other systems and source builds

The bundled Windows runtime is not for Linux, Raspberry Pi, or macOS. Install a JDK 17+ to build from source. Download the tagged [v1.0.0 source ZIP](https://github.com/turkerozturk/SweetCherry/archive/refs/tags/v1.0.0.zip), extract it, and run:

- Windows: `first-run.bat`
- Linux/Raspberry Pi: `sh ./first-run.sh`
- macOS: `sh ./first-run.sh` or `first-run.command`

The first build needs internet access to download Maven/dependencies. Scripts produce `release/SweetCherry`. Later, run its `run.bat`, `run.sh`, or `run.command` from that folder; rebuilding is unnecessary. macOS has not been verified on a physical Mac.

## Troubleshooting and network access

- **Port already in use:** inspect the control-window message and `myapp.log`; the app does not stop the other program or silently change ports.
- **Second click does nothing:** the Windows launcher prevents another instance. Open the existing control window from the tray or browse to `http://localhost:8080`.
- **Database missing:** correct its tenant path; SweetCherry does not create an empty CTB in place of a missing file.
- **Cannot replace an open CTB:** close the data source first; also close CherryTree/DB Browser connections to that file.
- **Remote access:** the default is local-only. LAN/internet access requires explicit binding, firewall/router, and HTTPS/proxy configuration. Do not expose plain HTTP login to the internet. See the [network guide (Turkish)](../tr/network-access.md) and [transport security guide (Turkish)](../tr/https-and-session-security.md).

## Known limits

Raw content search operates on stored `node.txt`, including rich-text XML. Search results use POST; browser Back may require resubmission or show an expired-document message. Internal node links in CTB exports are not rewritten to new node IDs. Shared tree parent/master relationships are remapped separately and are supported. Codebox editing and broader rich-text compatibility remain future work.
