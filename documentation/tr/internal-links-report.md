# İç bağlantı raporu / Internal Links report

**Görünüm → İç Bağlantılar** menüsü, Son Güncellenenler seçeneğinin altında `/nodes/internal-links` sayfasını açar. Aktif veri kaynağı gerekir. USER dahil oturum açmış okuyucular ve salt okunur veri kaynakları kullanabilir; sayfa hiçbir veri yazmaz.

## Görünüm ve sayımlar

- Her satır kaynak ağaç konumu, bağlantı metni/nesne adı, hedef ad/ID ve durumu gösterir. Gerçek ve shared konumların kendi ID'leri korunur; ikon, başlık kalınlığı ve renk gösterilir.
- Ad/ID bağlantısı içeriği açar. Fare üzerinde bekletilince başlıkta breadcrumb ve yol üzerindeki ID'ler görünür. Geçerli anchor bağlantısı hedefteki anchor'a gider. Geçersiz hedef için çalışmayan gezinme bağlantısı oluşturulmaz; mevcut düğümde eksik anchor varsa düğümün kendisi yine açılır.
- Aynı içerik birkaç gerçek/shared konumdan erişiliyorsa her konum ayrı satırdır. İçerik bir kez parse edilir. Bu yüzden **bağlantı satırı** ile **bağlantılı içerik düğümü** sayıları farklı olabilir. **Kaynak ağaç konumu** yalnız ağaçta bulunan kaynak konumları sayar; ağaçsız bir içerik kaydı ayrıca işaretlenebilir.
- Bitişik, aynı hedefli rich-text parçaları biçimleri farklı olsa da bağlantı metni birleştirilerek tek metin bağlantısı sayılır. Ayrı bağlantılar ve `image.link` alanındaki nesne bağlantıları ayrı sayılır. Kod kutusunda veya düz metin düğümünde yalnız metin olarak geçen `node 123` bağlantı sayılmaz.
- Durumlar: **Geçerli**, **Hedef düğüm yok**, **Anchor yok**, **Geçersiz bağlantı**. Anchor varlığı paylaşımlı hedefin içerik master'ında kontrol edilir; hedef ID shared konumun ID'si olarak kalır. Bozuk döngülü/eksik parent yolları sonsuz döngüye girmez; breadcrumb sınırlanır ve `… ↻` / `? [ID]` göstergesi görülebilir.

## Filtreleme ve maliyet

Ad, ID, bağlantı metni veya anchor içinde arama yapılabilir. Arama büyük/küçük harf ve aksan farklarını yumuşatır; yalnız sorunlu veya geçerli bağlantılar seçilebilir. Varsayılan sayfa boyutu **50**, izin verilen aralık **1–1000** satırdır. Sayfalama ve filtreler GET kullanır. Üst istatistikler tüm taramanın sayımlarıdır; alt satır aralığı filtrelenmiş eşleşmeleri gösterir.

Rapor yalnız açıldığında, filtre/sayfa değiştiğinde veya Göster/Yenile düğmesine basıldığında taranır. Her istekte güncel veri okunur; önbellek, arka plan indeksleme ve otomatik yenileme yoktur. Büyük CTB'lerde ilk gösterim ve sayfa değişimi metin taraması nedeniyle zaman alabilir.

Zengin metin XML'i ve düğüm/konum/anchor/link metadatası okunur; resim/ek dosya BLOB'ları (`image.png`) yüklenmez. İçerik HTML'e render edilmez. Breadcrumb yalnız görüntülenecek sayfa ve uyarılardaki düğümler için hazırlanır. Veritabanına kayıt, tarih güncellemesi veya bağlantı onarımı yapılmaz.

Her XML için inceleme sınırı **2.000.000 karakterdir**. Bozuk/desteklenmeyen veya bu sınırı aşan içerikler atlanıp uyarıda gösterilir; ilk 100 uyarı kaynağı listelenir. Bu durumda sonuçlar eksik olabilir. Aynı içeriğin nesne bağlantıları ayrıca taranır. Bu sınır kayıt/export sınırlarını veya not içeriğini değiştirmez.

## Manuel kontrol

1. İki gerçek ve birkaç shared konumda metin bağlantıları, mevcut/eksik anchor, olmayan hedef ve bozuk bağlantı değeri oluşturun; durumları/adları/ID'leri karşılaştırın.
2. Aynı bağlantının kalın/italik parçalarını ve resim/ek bağlantısını kontrol edin. Kaynak ve hedef shared konumları master ID'ye dönüşmemeli; hover yolu kendi ağaç konumunu izlemeli.
3. Arama, yalnız sorunlu filtresi, satır sayısı, önceki/sonraki sayfa ve tarayıcı Geri işlemini deneyin. Üst global istatistikler filtre yüzünden değişmemeli.
4. USER ve salt okunur kaynakla açın; kaynak kapalıyken seçim ekranına dönmeli. Eski sekmeden farklı tenant token'ıyla istek veri okumadan reddedilmeli.
5. Türkçe/İngilizce ve mobil görünümü deneyin. Mobilde satırlar kaynak/metin/hedef/durum etiketli kartlardır.
6. Tarama öncesi/sonrası içerik, objeler, tarihlerin ve dosya verisinin değişmediğini doğrulayın.

## English

**View Mode → Internal Links**, immediately below Recently Modified, opens `/nodes/internal-links`. Authenticated readers, including USER and read-only sources, can inspect links without writing data. Rows show styled source/target names and occurrence IDs, link text or object filename, optional anchor and validity. Hover for breadcrumbs; valid targets are clickable, while missing targets are not. A missing anchor still allows opening the target node itself.

Each real/shared source occurrence gets its own row, while shared content is parsed once. Counts distinguish content nodes, source tree locations and expanded link rows. Adjacent runs with the same target are merged; image/attachment `image.link` metadata is inspected separately, without loading `image.png`. Plain/code text that merely mentions a node ID is not an internal link.

Search names/IDs/text/anchors, filter broken/valid links, and paginate (50 rows by default; 1–1000 allowed). Top statistics describe the whole scan; the range describes filtered results. Every page/filter request scans current data on demand; there is no background index or automatic refresh. Large databases may take time. Only the requested page's rows/breadcrumbs are retained for display.

Malformed/unsupported XML or XML over 2,000,000 characters is skipped with an explicit incomplete-results warning (first 100 affected contents listed); object links are still inspected. No notes, timestamps or link values are changed. Test real/shared targets, missing anchors/targets, styled text, object links, filters/paging, read-only/USER access, stale/closed tenant guards and mobile/translated layouts.
