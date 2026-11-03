# Düğüm etiketleri / Node tags

## Özellikler ve seçim diyaloğu

Düğüm Özellikleri sayfasındaki **Etiketler** kutusu serbest metindir. Yanındaki etiket düğmesi, tıklandığında aktif veritabanındaki mevcut etiketleri okur. Diyalogda arama ve çoklu seçim vardır; kutuya elle yazılmış yeni etiketler de seçimlere dahil edilir. Var olanlar işaretli açılır; işareti kaldırmak onları kutudan çıkarır. **Etiketleri uygula** yalnız metin kutusunu değiştirir; **Kaydet** işlemi yapılana kadar veritabanına yazılmaz. Diyalog İptal işlemi kutuyu değiştirmez.

Kaydederken yalnız ASCII boşluk (`U+0020`) ayırıcıdır. Boş parçalar ve **tam olarak aynı** tekrarlar kaldırılır, ilk karşılaşma sırası korunarak tek boşlukla birleştirilir. Büyük/küçük harf, Türkçe/diğer Unicode karakterler ve noktalama işaretleri değiştirilmez. `Tag` ve `tag` ayrıdır; `one,two`, `C++`, `#tag` tek etikettir. Sekme veya kesilmez boşluk etiket ayırıcısı sayılmaz. Etiket içinde normal boşluk bulunamaz. Formdaki metin en fazla 20.000 karakterdir; NUL karakteri kabul edilmez. Bunlar SweetCherry doğrulama sınırlarıdır; tüm CherryTree sürümlerinin karakter kurallarıyla birebir eşitlik iddiası değildir.

Veritabanı kataloğunda bir SQL `DISTINCT` yerine Java kümesi kullanılır: MariaDB/MySQL gibi sunuculardaki büyük/küçük harfe duyarsız collation, `Tag` ve `tag` değerlerinden birini kaybettirmemelidir. Okuma kayıtları değiştirmez; mevcut düğümlerin etiketleri topluca temizlenmez. Harf/işaret filtresi yoktur. CherryTree'nin incelenen özellik diyaloğunda metin doğrudan giriş kutusundan alınır: [ct_dialogs_tree.cc](https://github.com/giuspen/cherrytree/blob/master/src/ct/ct_dialogs_tree.cc).

Özellik formu küçük Bootstrap kontrolleri ve masaüstünde iki sütunla daraltıldı; mobilde tek sütundur. Kaydet/İptal üsttedir. İkon, renk, salt okunur ve düğüm tipi davranışları korunur. Özellikleri kaydetmek mevcut zaman damgası kuralı gereği modified tarihini günceller; iptal etmez. Etiket alanı göndermeyen eski istemciler/API çağrıları mevcut etiketleri silmez; boş alan göndermek etiketleri temizler.

## Etiketler görünümü

**Görünüm → Etiketler** (`/nodes/tags`) düğümleri etiketleriyle listeler. Katalog aranabilir; seçilen etiketler liste dışında da görünür. Seçim yokken etiketli tüm erişilebilir ağaç konumları gösterilir. Seçilmiş etiketlerden **herhangi biri** (OR) veya **hepsi** (AND) ile eşleşme seçilebilir. Eşleşme tam etikete göredir; `foo`, `foobar` ile eşleşmez. Katalog filtresi harf/aksan farklarını yumuşatır ama etiketin değerini değiştirmez. Filtrede gizlenen işaretli kutular seçili kalır.

Ad/ID linki düğümü açar; ikonu, başlık rengi/kalınlığı ve breadcrumb tooltip'i gösterilir. Paylaşımlı konum kendi ID/yoluyla ayrı satırdır; etiketler master içerikten gelir. Shared satırındaki **Gerçek düğüm özellikleri** düğmesi master'ın özellik sayfasına gider ve ortak özelliklerin tüm shared konumlarda değişeceğini açıklar. Shared düğümün kendisini doğrudan düzenleme kuralı değiştirilmemiştir. Geçerli gerçek master konumu yoksa özellik bağlantısı gösterilmez.

Okuyucular ve salt okunur veri kaynakları görünümü kullanabilir. Özellik düğmeleri yalnız ADMIN ve writable kaynakta gösterilir; backend yazma kontrolleri de korunur. GET filtreleri, sonraki/önceki sayfa ve tarayıcı Geri desteklenir. Varsayılan 50, aralık 1–1000 satır; bir sorguda en fazla 100 etiket seçilebilir. Etiketler URL'de ayrı parametrelerdir; virgül ve `+` etiketin parçası olarak korunur.

Katalog tüm node kayıtlarından gelir; ağaçta bulunmayan içeriklerin etiketleri de katalogda bulunabilir fakat bu içerikler için bozuk düğüm linkleri üretilmez. İçerik düğümü ve ağaç konumu sayıları bu yüzden farklı olabilir. Yalnız metadata okunur; not metni veya nesne/BLOB yüklenmez. Okuma sırasında tarih veya not değişmez.

## Manuel kontrol

1. Elle `  foo Türkçe foo Tag tag C++ one,two  ` yazıp kaydedin; `foo Türkçe Tag tag C++ one,two` olmalı. Boş alanla temizlemeyi de deneyin.
2. Diyaloğu açın, filtreleyin, birden fazla etiket seçin; gizlenen seçili kutular ve elle yazılmış yeni etiketler kaybolmamalı. İptal kutuyu değiştirmemeli.
3. Tags görünümünde `foo` tek etiket, OR/AND, Türkçe karakterli veya virgüllü/`+` içeren etiketler ve sayfalama/Geri işlemlerini deneyin.
4. Shared satırın ID ve breadcrumb'ı kendi konumunu göstermeli; özellik düğmesi açıkça gerçek düğüme gitmeli. Kaydedilen master etiketi diğer konumlarda da görünmeli.
5. USER/read-only kullanımda yazma düğmesi görünmemeli; değiştirilmiş tenant token'ı veri okumadan/yazmadan reddedilmeli.
6. 1920×1080 ve mobil görünümde varsayılan kapalı ikon listesiyle özellik formunu kontrol edin; renk, ikon, tip ve salt okunur ayarlarını tekrar deneyin.

## English

Node Properties now has a free tags field and an on-demand, searchable multi-select catalogue. Typed new tags join the dialogue choices. Apply updates the field only; the main Save commits it. Cancel leaves it unchanged. Saving splits on ASCII space, removes empty/exact duplicate tokens, preserves first-occurrence order and joins with one space. Case, Unicode and punctuation are retained; `Tag`/`tag` differ, and comma/plus are literal tag characters. Limits: 20,000 input characters, no NUL. Older callers omitting tags preserve existing data. Property-save timestamp rules are unchanged.

View Mode → Tags lists styled real/shared occurrence names/IDs with breadcrumb tooltips. Choose ANY or ALL exact tags; no selection shows all tagged tree locations. Search filters choices without clearing hidden selections. Shared occurrences keep their IDs; their explicitly labelled Original properties button edits the real master, affecting shared properties/tags. Only ADMIN with a writable source sees property-edit links; backend protections remain enforced.

The view/catalogue reads metadata, not note text or BLOBs. Catalogue uniqueness is computed in Java to avoid case-insensitive SQL DISTINCT losing distinct tag values. Paging defaults to 50 (1–1000 permitted), with up to 100 selected tags. Repeated query parameters preserve commas and plus signs. Test normalization, cancellation, arbitrary Unicode/punctuation, ANY/ALL, shared-to-master editing, permissions, stale tenant tokens, paging/Back and compact desktop/mobile layout.

## Veritabanı genelinde yeniden adlandırma ve kaldırma

`/nodes/tags` sayfasındaki **Etiketleri yeniden adlandır / kaldır** bağlantısı yalnız ADMIN ve yazılabilir bağlantıda görünür. Yönetim ekranında arama filtreli listeden tek etiket seçin. Yeni ad girip veya kaldırmayı seçip **Değişiklikleri incele** düğmesine basın. Sonraki ekranda işlem, gerçek kayıt sayısı ve önceki/sonraki etiketler gösterilir; hiçbir veri henüz değiştirilmemiştir. **Tüm değişiklikleri onayla** işlemi uygular. İptal yazmaz.

Eşleşme yalnız ASCII boşlukla ayrılan tam etikete göre, büyük/küçük harfe duyarlı yapılır. `work`, `Work` ve `homework` farklıdır. Yeni etiket boş, aynı ad, boşluklu veya kontrol karakterli olamaz. Var olan hedefe yeniden adlandırma birleştirme sayılır; tekrar oluşmaz. Değişen kayıtların etiket alanı mevcut özellik editörü gibi tek boşlukla birleştirilip tekilleştirilir; etkilenmeyen alanlar aynen kalır. Kaldırma yalnız etiketi kaldırır, düğümü veya içeriğini silmez.

Tüm `node` kayıtları taranır; ağaçta görünmeyen kayıtlar da kapsam içindedir. Bir master gerçek kayıt olarak bir kez güncellenir; shared konumları aynı etiketi gösterir. Düğümün içerik salt okunur bayrağı metadata düzenlemesini engellemez; tenant bağlantısının yazılabilir olması zorunludur. Oluşturulma korunur; etkilenen her gerçek kaydın `ts_lastsave` değeri işlem zamanına güncellenir. İçerik ve ağaç değiştirilmez.

Onay oturuma, veritabanı görünümüne ve işlem bilgisine bağlı, on dakika geçerli ve tek kullanımlıktır. Aynı oturumda yeni önizleme öncekinin yerini alır; yönetim sayfasını yeniden açmak/iptal etmek bekleyen onayı kaldırır. Önizlemeden sonra etkilenen etiketler, adlar veya eşleşen kayıtlar değişirse işlem yazmadan reddedilir. Güncellemeler tek transaction içindedir; bir kayıt başarısızsa tamamı geri alınır. Önizlemede en çok 200 kayıt gösterilir; toplam sayı kapsamın tamamını belirtir. Bir işlem 50.000 eşleşme, 16 Mi ham etiket karakteri ve sonuç alanı başına 20.000 karakter ile sınırlıdır.

Başarılı commit sonrasında her değişen gerçek düğüm için INFO logunda ortak `operationId`, kullanıcı, bağlantı adı, `nodeId`, `nodeName`, RENAME/REMOVE ve eski/yeni etiket bulunur; ayrıca toplam kayıt sayısı yazılır. Geri alınan transaction için başarı logu yazılmaz. Kontrol karakterleri temizlenir, uzun log alanları kısaltılır; loglara not içeriği veya bağlantı parolası eklenmez. Düğüm adları ve etiketler loglarda yer alacağından normal log gizliliği geçerlidir.

Manuel kontrol: büyük/küçük harf ayrımı, virgüllü/C++/Türkçe etiketler, zaten var olan hedefle birleştirme, tek etiketi kaldırınca boş alan, shared master, iptal, çift onay, başka sekmede kaynak değişimi ve önizleme sırasında özellik düzenlemesi. Güncelleme tarihinin yalnız etkilenen gerçek kayıtlarda değiştiğini kontrol edin. USER ve salt okunur tenant doğrudan POST yaparak da değiştirememelidir.
