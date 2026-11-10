# Changelog

SweetCherry'deki kullanıcıya dönük önemli değişiklikler bu dosyada kaydedilir.

## [Unreleased]

- Render HTML-syntax notes with jsoup sanitization and sandboxed readers; add an HTML shortcut in Node Properties and a source/live-preview editor with formatting tools, history and recovery. Preserve original HTML source in CTB storage.

- Import Freeplane/FreeMind MM files through a localized, reviewed upload into a new final root branch; preserve hierarchy, readable notes/details, direct title styling, dates and SweetCherry icon references, with atomic writes and explicit conversion limits.

## [1.7.0]

- Add an optional balanced two-sided Markmap layout using measured subtree geometry; preserve the default rightward layout, readable text, node actions and per-branch folding.

- Localize quick Markmap/Mermaid and quick-content pages; add an optional page-filling Markmap canvas toggle with Escape restoration.
- Stop hyperlink inheritance at the right edge in the rich-text editor, including composition input, and add one-click link dismissal with undo/redo while preserving text and formatting.

- Add hover/focus/touch child creation to Markmap with a name-only dialog, existing guarded quick-note writes, exact shared-occurrence parent IDs and refresh/reveal after confirmed save.

- Refresh bundled demo/quick-start connection defaults and icons; make quick-start examples writable for editing practice. Translate the Turkish rich-text edit menu label as Zengin Metin.


- Publish a Java-free all-OS portable ZIP, Windows x64 setup with Java, clearly named third-party licences ZIP and lowercase checksums; keep build metadata inside the application packages.

- Download custom single-record HTML layouts as offline A4 PDFs with embedded fonts/images, safe print CSS and page totals.

- Add customizable single-record HTML layouts stored as Plain Text nodes under `_views`, bounded text-node placeholders, a configurable server-rendered clock, shared field diagnostics and confirmed example creation. Render sanitized layouts in an isolated frame without changing the CTB schema or existing report routes.

- Unify ordered custom-list fields across HTML list/table, landscape PDF and XLSX; distinguish missing, empty and ambiguous child fields, diagnose repeated definitions, add record search/incomplete filtering and a Today shortcut, and replace blinking markers with static localized statuses. Preserve exact whole-field tag membership and the CTB schema.

- Add localized custom-list setup/help with global and per-connection names, explicit definition status and confirmed missing-node creation; add editor TAB/symbol buttons and trim boundary whitespace/invisible characters from authored node names.

- Add reviewed, atomic database-wide tag rename/removal for ADMIN writable connections, exact token matching, merge de-duplication, Modified tracking and commit-only per-node audit logs.

- Export a selected occurrence/subtree as a UTF-8 text ZIP with nested directories, safe collision handling, bounded temporary-file output and shared-occurrence hierarchy. Preserve code/Markdown source; flatten rich text without binary attachments. Consolidate PDF/CTB/Freeplane/text ZIP under Export in both readers.

## [1.6.0]

- Add consistent full file-based CTB snapshots using the SQLite Online Backup API, integrity checks, SHA-256, record counts, downloads and recovery into a separate read-only working copy; leave source databases unchanged.
- Add opt-in scheduled backups for explicitly selected connections, physical-source alias grouping, sequential jobs and retry backoff. Apply optional retention only after a successful automatic backup; protect manual/legacy backups and working copies.
- Add reviewed single/bulk backup deletion with short-lived, single-use session confirmation and checksum/usage protection; report deleted/skipped files explicitly.
- Add Created/Modified activity lists with styled, clickable occurrence breadcrumbs and configurable minimum counts; open the latest modified node after database selection by default in bundled YAML, with an opt-out.
- Fill desktop editor panes with the available viewport, remove nested writing-area scrolling, keep the rich-text caret visible, compact recovery controls and align Markdown source/preview. Add searchable code-language icons and a Markdown shortcut in Node Properties, retaining syntax locks.
- Resolve PDF translations through Spring messages and fall back to a generic code icon when a language-specific resource is missing.

## [1.5.0]

- Redesign advanced POST search with title/raw-content all-word, any-word and phrase matching; exact-tag ANY/ALL filters, searchable tag/icon choices, sortable SQL pagination, occurrence breadcrumbs and compact date columns. Retain raw node.txt filtering and current-page PDF export.
- Add private per-user/source saved filters outside CTB, preserving earlier presets and rejecting stale preference updates.
- Add on-demand incoming-link panels in desktop/mobile readers, distinguishing exact/shared target occurrences; bound content scans and report incomplete results. Restore independent tree-content scrolling around the panel.
- Extend CherryTree icon identities through ID 263 and include the 18 original/converted icon pairs; keep existing IDs and code-language mappings unchanged.
- Show node Created dates alongside Modified dates in bookmarks; keep mobile dates within the existing name column.
- Ignore only the three root runtime YAML copies in Git, retaining source configuration files.

- Add title-based Quick switch with styled shared-occurrence results and a slash shortcut; add transactional Quick note capture in plain text/Markdown, searchable parent selection and per-source remembered destinations.

- Initialize missing external YAML before startup without overwriting existing configuration; default fresh readers to the desktop tree and use Greenwich astronomy defaults. Add local launcher password copying, first-use login guidance and one-time generated-password console hints. Add detached Linux desktop launching and per-user shortcuts; use an unprivileged default main port.

## [1.4.0]

- Render Markdown-syntax nodes with safe HTML across node/calendar/template/PDF views; extend the source editor with Markdown tools and preview while retaining history and recovery drafts.

- Add weekly/monthly Created-date calendars, optional tag filters, mobile day lists, content dialogs and guarded timestamp editing with Modified tracking.

- Browser-local recovery drafts for both editors; failed saves keep unsaved content open.

- Add editable node tags with an on-demand searchable multi-select picker, exact de-duplication on property save, compact property layout, and a read-only Tags view with ANY/ALL matching, shared-occurrence navigation and explicit original-property editing.

- Add a read-only Internal Links report under View Mode with source/target occurrence navigation, breadcrumb tooltips, validity/anchor diagnostics, statistics, search and pagination; inspect rich text and object links without loading image BLOBs.

- Add/edit rich-text internal links by occurrence ID or exact selected-title matches, with styled target choices, existing-anchor selection, save-time target checks and undo/redo.

## [1.3.0]

- Add and edit embedded codeboxes in the rich-text editor with searchable language icons, atomic saves and undo/redo.

- Translate object browsing pages and handle empty object lists. Render CherryTree LaTeX attachments offline with JLaTeXMath as web PNGs and PDF vector outlines, with PNG/source fallback and unchanged editor storage. Include library/font notices.

- Translate CTB export results and node-info booleans/content categories; keep the raw rich-text flag row in a Thymeleaf comment.
- Add optional plain-text content notes to Freeplane/FreeMind export, including link targets and text representations of objects without reading image/attachment BLOBs.

- Add reader-safe Go to Node ID navigation with clipboard and filtered bookmark shortcuts. Translate exported-file actions/results, provide a return link after deletion, and confirm each CTB export before submitting.

- Wrap every CTB export in a dated, bold source container. Remap internal links within the exported branch and redirect missing/excluded targets to a rich-text report, preserving copied content timestamps and shared hierarchy.

- Filter Move to bookmark shortcuts by title; compact bookmark date/order columns and headings without wrapping drag handles.
- Remember only the desktop tree-pane width across tabs, add `myapp.tree.default-width` (600 px), and expand its range to 20–1900 px within the available viewport. Other splitters retain their existing behavior.

- Choose a Move to parent from styled bookmark shortcuts, retaining each real/shared occurrence ID and requiring explicit Move confirmation.

- Add a clipboard-paste icon beside the Move to parent-ID field without widening the dialog; validate numeric clipboard text and explain manual-paste fallback when access is unavailable.

### Validation

- The maintainer confirmed the new features manually and reported successful GitHub Actions on feature commit `cd1f7f26fccad968f2c8f07ce8fcde34aff9d627` (513 Java tests in Actions; 515 locally, all passing). The count difference alone does not establish a defect; local test reports/source files must be compared to identify it.
- Relevant editor JavaScript tests and Chromium desktop/mobile viewport checks passed during implementation. Two existing duplication JavaScript test failures were reproduced against the unchanged baseline; they are not included in the passing editor subset.
- Windows release packages must be rebuilt and checked from the final `v1.3.0` tag. See `documentation/releases/v1.3.0.md` for changes, upgrade notes and limitations.

## [1.2.0]

- Import exported CTBs into the active writable SQLite, PostgreSQL or InnoDB MySQL/MariaDB database after a count preview and confirmation. Remap tree/content/object/bookmark IDs and internal node links; preserve hierarchy, shared groups and timestamps; roll back failed writes.
- Make CTB import byte/record limits configurable. Show measured sizes/counts and offending internal links with source IDs, titles, fields and missing target IDs before writing.
- Add **Move to** after Delete in desktop/mobile readers: choose a parent occurrence ID, retain the subtree, append it last, and reject self/descendant/stale destinations. `0` selects the database root.
- Add Save and continue to both editors; reload the saved revision before further editing.
- Add configurable database icons across trees, connection selectors and applicable mind-map roots; fix wizard binding and add a searchable illustrated icon picker.
- Sort CTB selection choices by newest filesystem modification time, followed by other sources alphabetically. Sort the tenant table by display name and show CTB modification dates.
- Place the node title/icon below breadcrumbs and buttons; move rich-text tools into a resizable desktop sidebar and a mobile drawer while preserving selection. Make rendered table borders visible.
- Decode Windows SQLite file URIs correctly, including encoded spaces and Turkish characters; remove excess space above node content.

- Use browser language unless an initial language is configured; complete node-properties translations, add mobile navigation help and a distinct empty-content notice.
- Fix object IDs on HTTP clients without randomUUID; recognize PNG/JPEG cloud files with missing MIME types and explain upload failures separately.
- Prefer HTML in the explicit clipboard action where supported, handle mobile beforeinput paste data, and explain HTTP/plain-text clipboard limitations.

- Read clipboard PNG/JPEG file items as well as files; add an explicit permission-based clipboard image button.
- Add a separate, reviewed public-URL rich-text import with bounded PNG/JPEG downloads, plain tables, atomic undo/redo, and no CTB write before Save. Restrict network targets and preserve the existing HTML paste flow.

- Add persistent node-content zoom controls to View Mode.
- Group rich-text tools, add list characters and selected-URL linking, and allow resizing the editor.
- Add plain-text undo/redo and unsaved-change protection to both editors; move Save/Cancel above editing controls.

- Add recent-node navigation, configurable timeline row counts, node icons and metadata drawers; share desktop/mobile database welcome links.
- Add touch/mouse/keyboard bookmark ordering with stale-page protection and modification-date updates.
- Resolve Actuator app.version from artifact build metadata even when external YAML contains an unresolved Maven token.

- Make node modification dates consistent across content editing, property saves, moves, creation, bookmarks and shared-node operations; preserve timestamps on no-op content saves and CTB exports. See `documentation/tr/node-timestamps.md` for the rules and manual checks.

### Validation

- Maintainer reported 471 passing Java tests and manual move/subtree checks on the final feature commit. Relevant move-control JavaScript tests and Chromium desktop/mobile viewport checks also passed during implementation.
- Windows packaging is rebuilt from the release tag before publication. Existing limitations and upgrade precautions are listed in `documentation/releases/v1.2.0.md`.

## [1.1.0]

### Added

- Bundled unchanged Turkish/English CTB quick-start guides with read-only connection definitions.
- Clickable line-leading checkbox cycle in visual rich-text editing and a 33-tilde separator button in both text editors.

- Tenant configuration wizard for creating/editing connections and opening an existing CTB in place. Desktop users can select a local/network-share file on the machine running SweetCherry; no CTB upload or copy is implied.
- ADMIN web editor for selected non-security application settings, plus confirmed restart.
- Desktop YAML editor for local configuration files, masked secret fields, and creating an external override when needed.
- Tenant-specific Hibernate routing and SQL compatibility for SQLite, MariaDB/MySQL and PostgreSQL; database changes no longer require a global YAML dialect change.
- Searchable, illustrated icon selectors with selected-icon summaries, combined no-icon filters, and translated title-color names.

### Fixed

- Open the application folder directly with Explorer on Windows, bypassing shell file associations.

- Detect MariaDB behind MySQL Connector/J, preserve its version and avoid incompatible optional keyword metadata queries; enable portable identifier quoting.
- Use qualified object columns when reading rich-text objects and numeric 0/1 flags after deleting objects, including PostgreSQL.
- Give node creation UNION subqueries an alias required by MariaDB; include shared-only IDs when reserving a new node ID.
- Handle intentionally closed/unstarted tenant pools in health checks without opening them. Failures in running pools still report DOWN.
- Show safe, translated connection-failure explanations and concise logs while retaining the previous tenant selection.
- Redirect database-dependent list pages to source selection before querying when no tenant is selected; validate malformed, negative and oversized parent IDs.
- Accept safe export filenames containing tenant display-name punctuation. Copy into an active collection CTB through the existing transaction connection to avoid self-export locking.
- Check old SQLite CTB schemas before selection. Add missing children.master_id only with explicit custom.allowLegacySchemaUpgrade permission and ADMIN selection; unsupported changes are not performed.
- Highlight active tenants and writable/authentication settings with translated labels; add missing System-menu icons and remove a spurious background-image request.
- Keep routine search/table HTML diagnostics behind myapp.debug; preserve POST/binding search and raw node.txt filtering.

### Validation and limitations

- Maintainer reported 393 passing tests and manual checks on SQLite, MariaDB and PostgreSQL for browsing, editing, shared-node operations, exports and the changes above.
- Windows bundled-package validation is performed separately on the final release tag; source test success does not certify a not-yet-built installer.
- Automatic backups are not provided. A synchronization target can be overwritten by subsequent synchronization; avoid editing it while another application writes to it.
- An isolated MariaDB 1020 record-change conflict remains a follow-up observation if it recurs; no automatic retry or isolation weakening was added.

## [1.0.0] - 2026-10-08

### Added

- Java 17 içeren Windows x64 installer ve portable ZIP; masaüstü kontrol penceresi, tepsi menüsü, başlangıç durumu ve kapatma kontrolleri.
- İngilizce ve Türkçe son kullanıcı kurulum kılavuzları; HTTP/HTTPS, Caddy/proxy ve yerel/uzak erişim belgeleri.
- Masaüstü ve mobil okuma, yer işaretleri, düğüm taşıma/çoğaltma ve gerçek/paylaşımlı düğüm altında kardeş/alt düğüm oluşturma.
- Zengin metin düzenleme; resim ve dosya yükleme/panodan resim, dış bağlantı ve tablo düzenleme, web sayfasından biçimli yapıştırma, nesne silme.
- Markmap/Mermaid haritaları; Freeplane/FreeMind ve isteğe bağlı CherryTree ikon ZIP'i.
- Düğüm/alt ağaç PDF; gömülü fontlar, bağlantılar, hiyerarşik PDF navigasyonu, içerik tablosu ve oturumda hatırlanan sayfa/nesne seçenekleri. Şablon tablosu PDF çıktısı.
- Düğüm ve alt ağaç için ortak veya yeni CTB'ye dört aktarma seçeneği; paylaşımlı konumların hiyerarşisi ve desteklenen master eşlemeleri.
- Tenant yapılandırması indirme/silme onayları; dosya yokken SQLite'ın boş veritabanı oluşturmasını engelleme ve bağlantı havuzlarını kapatma.
- İsteğe bağlı çevrimdışı astronomi widget'ları ve konum/zaman ayarları.

### Changed

- Uygulama adı SweetCherry; sürüm 1.0.0. Varsayılan dinleme adresi 127.0.0.1.
- Java 17 tabanı ve güncellenmiş bağımlılıklar; Windows dağıtımı GitHub Actions ile oluşturulur.
- Yavaş bağımlılık/lisans raporları ayrı Maven profillerine taşındı.
- Otomatik yedek alınmadığı açıklandı; ilk hesap parolaları uygulama klasöründeki dosyada üretilir.

### Fixed

- Paylaşımlı düğümlerin gösterim, taşıma, çoğaltma, silme, oluşturma ve export hiyerarşileri.
- Düğüm silindikten sonra komşu düğüme geçiş; eksik düğümün oturum süresi dolmuş gibi gösterilmesi.
- Veri kaynağı kapanırken dosya kilidinin bırakılması; silinmiş/eski tenant seçiminde anlaşılır dönüş.
- Mobil/masaüstü toolbar düzeni, bookmark konumları, resimli ikon seçimi ve arama formunda yinelenen filtreler.
- Güvenilir proxy üzerinden HTTPS/çerez davranışı, hassas ayar değerlerinin gösterilmemesi ve sade hata/404 sayfaları.

### Known limitations

- SweetCherry otomatik yedek almaz. İki uygulamadan aynı CTB'ye eşzamanlı yazmayın.
- İçerik araması saklanan raw metinde çalışır; POST aramasından sonra tarayıcı geri hareketi formun yeniden gönderilmesini isteyebilir.
- CTB export sırasında metindeki düğüm bağlantılarının kimlikleri yeniden yazılmaz.
- Kod kutusu düzenleme ve ek editör iyileştirmeleri sonraki çalışmalardır.
- macOS scriptleri fiziksel Mac üzerinde doğrulanmamıştır.
- Üçüncü taraf lisans/atıf envanterinin tamamlanması ayrı yayın hazırlık maddesidir.
