# Deneysel rich text okuyucu: ilk aşama

Bu kod normal kullanımda etkin değildir. Mevcut parser, PDF yolu ve resim/dosya/tablo/codebox
browse sayfaları değişmez. Henüz CTB yazma veya rich text düzenleme özelliği sunmaz.

Paket: `com.turkerozturk.richtext.experimental`.

| Sınıf | Görev |
| --- | --- |
| `RichTextDocument` | Metin parçalarını, boş parçaları ve bilinen/bilinmeyen tüm attribute değerlerini korur. |
| `RichTextXmlReader` | CTB `node.txt` XML'ini okur; DTD ve harici entity erişimini kapatır. |
| `RichTextHtmlRenderer` | Kaçırılmış metin ve izin verilen inline biçimlerden bağımsız HTML önizlemesi üretir. |

Metin konumları Unicode code point sayısıdır; Java UTF-16 uzunluğu değildir.
Boş parça konumu artırmaz. Bu konumlar **yalnızca metin konumudur**; CTB nesnelerinin
`offset` alanlarıyla doğrudan eşleştirilmemelidir. Nesne yerleşimi sonraki aşamadır.

Önizleme bold, italic, monospace, foreground/background, underline/strikethrough ve
scale değerlerini destekler. Bağlantı ve scale birlikte korunur. HTTP(S) dış bağlantılar
etkindir. Node/anchor/file/folder bağlantıları modelde korunur fakat henüz etkin değildir.
Justification, indent ve invisible modelde korunur; bu aşamada HTML'e uygulanmaz.
Bilinmeyen attribute korunur fakat HTML/CSS içine aktarılmaz. Beklenmedik elementler
sessizce atılmak yerine hata üretir. Bu yüzden bu önizleme henüz mevcut renderer'ın yerine geçmez.

## Test verisi

`src/test/resources/richtext/demo-node-53.xml`, SweetCherry deposundaki
`external/CTBDATA/demo.ctb` dosyasının 53 numaralı düğümündeki `txt` değeridir.
Kullanıcının format araştırması için hazırladığı örnek, SQLite gerektirmeyen XML fixture'ına
ayrılmıştır. 13 boş metin parçası içerir. CTB'deki ayrı widget tabloları bu fixture'a dahil değildir.
Ek sentetik testler Unicode, birleşik biçimler, HTML kaçırma, güvenli bağlantılar ve XML
entity reddini sınar. Test sayısı tam suite çalıştırıldığında sekiz artmalıdır.

## Kaynak ve uygulama yaklaşımı

Veri formatı incelemesi için kaynak proje: https://github.com/giuspen/cherrytree

Bu paketin kodu bağımsız olarak yazılmıştır; CherryTree kaynak kodu veya test fixture'ları
kopyalanmamış/çevrilmemiştir. Yeni upstream bağımlılığı eklenmez. Mevcut lisans ve telif
bildirimleri değiştirilmez. Bir dosya formatına uyum sağlamak ile başka projenin kaynak kodunu
aktarmak ayrı konulardır; ileride kod aktarılırsa ilgili lisans koşulları ayrıca değerlendirilmelidir.
Bu açıklama mevcut deponun tamamı için hukuki uygunluk değerlendirmesi değildir.

Sonraki aşama: metin konumları ile ayrı widget kayıtlarını birleştiren yerleşim modeli ve
resim, ek dosya, anchor, codebox, table adaptörleri. Ardından XML semantik round-trip
ve sınırlı düzenleme desteği düşünülebilir.

## İkinci aşama: nesne yerleşimi

`RichTextLayoutBuilder`, metin modeli ile nesne referanslarını birleştirir.
Her nesne son buffer'da bir karakter konumu kaplar. Sıralanmış nesneler için metin konumu,
`bufferOffset - öncekiNesneSayısı` olarak hesaplanır. Örneğin `ABC` metninde 1 konumundaki
resim `A [resim] BC` yerleşimini verir. Metin parçası gerekirse bölünür; iki tarafta da
attribute değerleri korunur. Unicode surrogate pair bölünmez. Boş parçalar kaybolmaz.

`RichTextLayout` metin ve nesne slotlarını ayrı tutar. `CtbObjectReferences` mevcut
image/anchor/codebox/grid entity'lerinden tür, node ID ve offset alır; binary içerikleri
okumaz, servis veya browse sayfalarını değiştirmez. Aynı image kaydını hem `fromImage`
hem `fromAnchor` üzerinden eklemeyin: bunlar alternatif adaptör girişleridir.

Aynı final-buffer konumunda iki nesne, sınır dışı offset veya farklı düğümlerin
nesnelerini birleştirme girişimi hata üretir. Çakışma durumunda kayıt sessizce ezilmez.
Ardışık nesnelerin buffer offset'leri farklıdır; metin konumları aynı olabilir.

Bu aşama nesnelerin HTML gösterimini henüz uygulamaz. Yerleşim modelini eski renderer'a
bağlamaz ve CTB yazmaz. Sonraki adım her nesne türünün veri/HTML adaptörü ve bağlantı
çözümlemesidir. Bu yamada dokuz test daha eklendi; önceki 68 test üzerine toplam 77 beklenir.
53 numaralı düğümün 14 nesne konumu, metni kaybetmeden birleştirme testinde kullanılır.
Bu test image tablosu kayıtlarının türlerini değil konumlarını sınar; tür ayrımı ayrı adaptör testindedir.

## Üçüncü aşama: nesne içerikleri ve HTML

`EmbeddedContentAdapter` mevcut entity'lerden request-local veri kopyaları çıkarır.
`EmbeddedContent` metin/codebox/table/anchor/attachment bilgileri ile PNG byte dizisini
entity'lerden ayrı tutar. PNG dizisi girişte ve çıkışta kopyalanır. Dosya ve anchor
kayıtlarını işlerken binary içerik yüklenmez. Layout referansları ile payload'lar aynı
`EmbeddedObject` anahtarıyla eşleştirilmelidir.

`RichTextObjectHtmlRenderer` metin slotları arasına nesne HTML'lerini yerleştirir:

| Tür | İlk gösterim |
| --- | --- |
| Resim | PNG data URI, ekran genişliğiyle sınırlı görsel. |
| Ek dosya | Kaçırılmış dosya adı; tenant-view token verilirse mevcut download yoluna bağlantı. |
| Anchor | Kaçırılmış anchor adıyla HTML ID. |
| Codebox | Kaçırılmış kod ve `data-language`, yatay kaydırma alanı. |
| Tablo | Son storage satırı başlık; diğer satırlar gövde, yatay kaydırma alanı. |

Token yoksa ek dosya bağlantısı etkin değildir. Renderer token'ı doğrulayan güvenlik
katmanı değildir; mevcut endpoint/interceptor denetimleri yine gereklidir. Bu sınıflar
henüz controller veya Spring bean olarak kullanılmaz. Resim verisi HTML'e gömüldüğü için
preview boyutu artabilir. PNG imzası kontrolü tam resim doğrulaması değildir.

Tablonun `col_widths` ve diğer kök attribute değerleri modelde saklanır; henüz yerleşime
uygulanmaz. Codebox boyut/line-number/highlight ayarları henüz modellenmez. Resmin link ve
justification değerleri saklanır fakat uygulanmaz. Latex eklerinin özel gösterimi, node ve
anchor bağlantılarının çözümlenmesi, paragraf/list hizalama ve resim büyütme sonraki aşamalardır.
Codebox bu aşamada renkli değil, düz ve kaçırılmış kod olarak gösterilir.

Eksik payload veya tür uyuşmazlığı sessizce atlanmaz; hata üretir. Tablo XML'i güvenli
DOM okuyucuyla parse edilir; beklenmeyen elementler reddedilir. Normal parser, PDF ve
bağımsız browse sayfaları değişmez. CTB yazma yoktur.

`demo-node-53-table.xml` aynı proje CTB'sindeki 53 numaralı düğümün grid kaydından
çıkarılmıştır. Bu yamada 10 test eklenir; önceki 77 üzerine toplam 87 beklenir.

## Dördüncü aşama: yan yana önizleme

Veritabanı seçildikten sonra admin olarak aşağıdaki yolu açın:

`/nodes/richtext-preview/53`

Doğrudan ziyaret, mevcut `_tenantView` token'ıyla aynı sayfaya yönlenir. Sonraki ziyaretlerde
mevcut tenant interceptor'ı eski token'ı reddeder. Oturum/veritabanı yoksa ana sayfaya dönülür.
Sayfa yalnızca `custom-colors` düğümleri içindir. Alias ID verilirse içerik master'dan okunur;
geri dönüşte alias'ın ağaç ID'si korunur. Endpoint `@RequiresTenant` ve admin method security
ile korunur. Yazma, export, kaydetme veya mevcut görünümü değiştirme işlemi yoktur.

İki çıktı ayrı sandbox iframe içinde gösterilir. Script, form ve üst sayfaya gezinme izinleri
verilmez. Bu paneller etkileşimli düğüm sayfası değil, görsel karşılaştırmadır. Parser'lardan
biri hata üretirse diğeri gösterilmeye devam eder; ayrıntı uygulama loguna yazılır. Resimlerin
eski panelde mevcut endpoint üzerinden, yeni panelde data URI üzerinden gelmesi mümkündür.

Yeni yol tek read-only transaction içinde entity ve lazy koleksiyonları okur. Mevcut parser'ın
bilinen thread-safety sorunları bu yamada düzeltilmez; karşılaştırma geliştirme amaçlıdır.
Normal kullanımdaki renderer tercihi değişmez. İlk denemede 53 numaralı düğümde başlık/renk,
boş satır, link metni, resim, ek dosya, anchor, tablo ve codebox sırası karşılaştırılmalıdır.
Yeni parser'ın henüz desteklemediği biçimler önceki bölümlerde listelenmiştir.

Bu yamada sekiz test eklenir; önceki 87 üzerine toplam 95 beklenir.
