# Changelog

SweetCherry'deki kullanıcıya dönük önemli değişiklikler bu dosyada kaydedilir.

## [Unreleased]

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
