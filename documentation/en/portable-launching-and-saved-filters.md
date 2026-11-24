# Portable launching and saved searches

`mvn clean package` creates `release/SweetCherry/saved-filters` and
`release/SweetCherry/backup` if absent. Existing directories and their contents
are preserved. A release build accepts empty directories but rejects private
saved filters or backups; the installer creates these directories without
copying over personal files.

Rebuild `SweetCherry.exe` using the launcher guide to get the new Java search
policy: adjacent `runtime`, then `JAVA_HOME`/`PATH` and Windows registry, requiring
64-bit Java 17+. Updating the JAR alone cannot change an existing EXE's embedded
Launch4j settings. From a Windows source checkout, after Maven package, run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File packaging/windows/build-launcher.ps1
```

This creates `release/SweetCherry/SweetCherry.exe` without GitHub, Inno Setup or
a bundled runtime. Building needs Java 17+ and cached/downloadable Maven artifacts.

The run scripts prefer an adjacent compatible runtime and
otherwise use system Java. Scripts and EXE no longer supply server ports.

Spring reads the external YAML and command-line options as usual. The desktop
control and automatic browser opening use the resolved HTTP/HTTPS endpoint.
`server.port` is the primary listener, with `server.ssl.enabled` controlling TLS.
`server.http.port` remains the optional additional plain HTTP listener. Set
`server.http.enabled: false` to use only the primary listener. When desktop mode
has neither external YAML port settings nor explicit startup port/config options,
a protocol/port dialog is shown before starting Spring. HTTPS still needs valid
SSL configuration (certificate/keystore or SSL bundle); selecting HTTPS does not
create a certificate. Cancel exits without starting the server. Foreground run
scripts never ask for a port and use the JAR/external YAML defaults.

## Moving saved searches

Previously saved-search filenames included the application's absolute directory.
Editor recovery keeps that isolation, but saved searches now use a separate,
portable namespace. For ordinary SQLite files under the application folder,
the relative database path is used; other database URLs retain their explicit
identity. Login name, tenant name and database path/URL still distinguish users
and sources. Copy `saved-filters`, `allTenants` and the CTB files together,
keeping relative CTB paths and the same login/tenant names. Absolute JDBC URLs
in tenant files must still point at the database's new location.

Open the new version once **in the original folder** before copying it: existing
saved searches are copied to the new namespace automatically without deleting
the old files. Existing destination presets, including an explicitly emptied
list, are never replaced. No database schema or note content is changed.

If the old files have already been moved, their hash filenames contain no
recoverable source path. Import a specific JSON file explicitly while SweetCherry
is stopped (Python 3, standard library only). Inspect the JSON's preset names to
select the correct file, then supply the destination login, tenant and JDBC URL:

```bat
python tools\migrate-saved-filters.py "saved-filters\OLD_HASH.json" --installation "." --user "admin" --tenant "Notes" --jdbc-url "jdbc:sqlite:CTBDATA/notes.ctb"
```

The tool copies bytes to the matching portable filename. It never deletes the
original or overwrites a different destination, and never scans or merges other
users' files. Start SweetCherry and reload saved searches afterwards. Full field
validation remains in SweetCherry. From a source checkout the tool is under
`packaging/migrate-saved-filters.py`.
