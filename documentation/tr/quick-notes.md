# Hızlı geçiş ve hızlı not / Quick switch and capture

## Türkçe

**Application/Uygulama → Başlığa göre hızlı geçiş** seçili veritabanındaki düğüm başlıklarında arar. Yazı alanı dışında `/` tuşu da diyaloğu açar; metin editörlerinde normal `/` yazımı engellenmez. Diyalogda başlığın bir kısmını yazın. Yön tuşları ve Enter veya tıklama ile düğümü açın. Mevcut masaüstü/mobil görünüm tercihi korunur. Eskiden bulunan **Go to Node ID** ayrıca kullanılabilir.

- İkon, başlık rengi/kalınlığı, ağaç ID ve paylaşımlı bilgisi gösterilir; hover açıklaması ID'li breadcrumb'dır. Aynı adlı veya aynı içeriği gösteren shared konumlar birleştirilmez. Seçilen kendi konumuna gidilir.
- Büyük/küçük harf ve aksan farkı göz ardı edilir; Türkçe `İ`, `I`, `ı`, `i` aramada eşdeğerdir. Tam başlıklar önce, ardından başlık başından ve başlık içinden eşleşenler gösterilir. `%` ve `_` özel sorgu karakteri değildir.
- Arama `node.txt` veya BLOB'ları okumaz. Taşınabilir Unicode eşleştirme Java tarafında yalnız başlık metadatasında yapılır; bu bir full-text indeks değildir. Yazma durduktan 300 ms sonra istek gider, önceki isteğin sonuçları yeni sorguyu ezmez.
- İlk 40 sonuç gösterilir. En çok 50.000 ağaç konumunun metadatası incelenir; bu sınır aşıldığında görünür uyarı vardır ve sonuçlar tam liste sayılmaz. Çok büyük veritabanları için ayrı indeks ileride düşünülebilir. CTB şeması veya mevcut gelişmiş ham içerik araması değiştirilmez.
- USER ve salt okunur kaynaklarda hızlı geçiş çalışır; veri kaynağı seçili değilse menü etkin değildir.

**Application/Uygulama → Hızlı not**, ADMIN için başlık ve metin yazarak sayfadan ayrılmadan düğüm oluşturur. Başlık zorunlu ve en çok 255, metin en çok 200.000 Java/JavaScript karakter birimidir. Başlık kenarlarındaki boşluklar kırpılır; içerik aynen saklanır. Biçim **Düz Metin** veya **Markdown** olabilir. Görsel rich text ve ekli nesneler için normal editörü kullanın.

**Üst düğüm ID = 0**, veritabanı köküne ekler. Gerçek veya paylaşımlı bir parent ID yazabilir veya **Üst düğümü başlığına göre bul** alanından seçebilirsiniz. Paylaşımlı parent'ın kendi altına eklenir; master'ın başka konumdaki ağacına eklenmez. Hedef silinmişse kayıt reddedilir; köke sessizce dönülmez. Gelen kutusu kullanmak için sıradan bir `Inbox/Gelen kutusu` düğümü oluşturup bir kez hedef olarak seçin.

Son **başarıyla kayıt yapılan** hedef ID, aynı tarayıcıda kullanıcı + bağlantı + çalışma klasörü kapsamındaki opak anahtarla hatırlanır. Bağlantı ayarları/parola tarayıcıya verilmez. Bu tercih CTB'ye yazılmaz; tarayıcı verileri temizlenince kaybolur. Bağlantı tanımı veya kurulum klasörü değişirse yeni kapsam oluşabilir. Bilinmeyen bağlantı kimliğinde kalıcı hatırlama yapılmaz.

Kayıt tek transaction'dır: yeni gerçek düğüm ve children kaydı birlikte oluşur; ad/metin ataması başarısızsa oluşturma ve parent zaman güncellemesi geri alınır. Parent'ın son çocuğu veya kökün son öğesi olur. Yeni düğümün Created/Modified değerleri aynıdır ve şimdiki zamandır. Parent varsa onun içerik master'ının Modified değeri mevcut oluşturma kurallarına göre güncellenir; Created korunur. `custom.newNodeTags` gibi mevcut oluşturma varsayılanları kullanılır. Yeni tablo veya sütun eklenmez.

Başarıda başlık/metin temizlenir ve yeni notu ayrı sekmede açan bağlantı gösterilir. Aynı diyalogdan art arda not eklenebilir; mevcut ağaç görünümünde yeni kayıtlar için yenileme gerekir. Kaynak token'ı, CSRF, ADMIN ve yazılabilir kaynak kontrolleri korunur. Başka sekmede veritabanı değişince eski diyalog yeni kaynağa yazamaz. Kaydetme sırasında tekrar gönderme ve kapatma engellenir; kaydedilmemiş metinle kapatırken/ayrılırken uyarı vardır. Hata veya giriş sayfasına yönlendirme metni temizlemez. **Ağ zaman aşımında kayıt tamamlanmış olabilir; tekrar denemeden önce son düğümleri kontrol edin.**

Bu küçük diyalogun içeriği kalıcı kurtarma taslağı değildir; normal editörlerin IndexedDB kurtarma özelliği buraya eklenmemiştir. Tarayıcı çökmesi veya zorla kapatma durumunda kaydedilmemiş hızlı not kaybolabilir. Hassas veya uzun notlarda normal editörü kullanın. Kaydetme CTB yedeği veya çok kullanıcılı düzenleme desteği oluşturmaz.

### Manuel kontroller

1. USER/salt okunur kaynakta başlık arayın; ID, ikon, renk/kalınlık, breadcrumb ve normal/shared konumlara gidişi kontrol edin. Markdown/kod içeriği arama sonucu üretmemeli; yalnız başlık eşleşmeli.
2. Aynı adlı düğümler, Türkçe harfler, `%_`, `<b>metin</b>` gibi başlıkları deneyin. HTML olarak çalışmamalı. Hızlı yazıp silerken eski sonuç gelmemeli. `/` editörde metin olarak yazılmalı.
3. Hızlı notu köke, gerçek parent'a ve shared parent'a ekleyin. Hiyerarşi, son sıra, kaynak metin ve tarihleri SQLite üzerinde kontrol edin; MariaDB/PostgreSQL'de de deneyin.
4. Notu kaydettikten sonra hedef hatırlanmalı. Başka kullanıcı/veritabanında aynı hedef otomatik kullanılmamalı. Silinmiş parent, geçersiz ID ve uzun içerik kayıt oluşturmamalı.
5. İkinci sekmede veri kaynağını değiştirin; eski diyalogdan kayıt reddedilmeli ve metin korunmalı. Sunucuyu durdurup kaydetmeyi deneyin; metin kaybolmamalı. Tekrar denemeden son kayıtları kontrol edin.
6. Başlık/metin değişmişken Kapat/Esc ve sayfadan ayrılma uyarılarını, masaüstü/mobil görünümü, klavye seçimlerini ve Türkçe/İngilizce metinleri kontrol edin.

## English

**Application → Quick switch** searches node titles in the active database; `/` opens it outside editing controls. Type part of a title, then use arrows/Enter or click a result. The selected desktop/reader mode is retained. Results include icons, bold/color, occurrence IDs and breadcrumb tooltips; shared occurrences keep their own positions. Exact matches precede prefixes and other substrings. Search is case/accent insensitive, treating Turkish I variants equivalently, and `%`/`_` are literal characters.

Only metadata is read, not content or image/attachment BLOBs. Unicode matching happens in Java after a 300 ms input debounce; it is not a full-text index. Results are capped at 40; metadata scanning is capped at 50,000 positions with an explicit incomplete-results warning. The existing advanced raw-content search and CTB schema are unchanged.

**Quick note** lets ADMIN users create a plain-text or Markdown node without leaving the page. A title is required (255-character maximum); text is limited to 200,000 UTF-16 units and stored unchanged. Parent `0` means the database root; another real/shared occurrence ID appends a child there. A title-search picker can select the parent. Create an ordinary Inbox node and select it once if desired. Missing parents are rejected rather than silently replaced by root.

The last successfully used parent is remembered locally per user/source/installation using an opaque scope; no JDBC credentials are exposed or CTB metadata added. Creation and content assignment share a transaction. Existing default tags, hierarchy and parent Modified rules are retained; new Created/Modified timestamps are equal to now. Success clears the title/text, allows another capture and offers a new-tab link. Refresh the tree to show additions.

ADMIN, writable-source, CSRF and stale-source checks protect creation. Errors keep entered content; closing/navigation warns about unsaved changes. Saves are not automatically retried. A timed-out request may have committed, so check recent notes before retrying. The quick dialog does not have durable recovery drafts; normal editors retain their separate recovery feature. No backup or collaborative-editing guarantee is added.
