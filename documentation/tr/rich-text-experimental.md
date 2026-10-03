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

## Önizleme düzeltmesi: anchor kayıtları ve satır sonları

Image tablosunda anchor kayıtlarının `png` alanı NULL olabilir. Image entity'sindeki
`@PostLoad` metadata hesabı artık bu kayıtları sıfır byte olarak kabul eder; binary veri
veya veritabanı değiştirilmez. Böylece tüm image-table kayıtlarını okuyan deneysel yol,
anchor'ları yüklerken durmaz. Örnekteki sekiz anchor kaydı bu durumdadır.

Önizleme HTML'inin Jsoup pretty-print işlemi kapatıldı; iki panelde de `white-space:pre-wrap`
kullanılır. XML'deki gerçek newline ve tekrarlı boşlukların korunması test edilir.
Hata izolasyon testlerinin bilerek bozuk XML/exception kullanması WARN logu üretir;
bu loglar test başarısızlığı değildir. Manuel önizlemede oluşan WARN ise gerçek sorundur.
Bu düzeltmede üç test eklenir; önceki 95 üzerine toplam 98 beklenir.

## Hizalama, bağlantılar ve codebox güncellemesi

Yeni HTML renderer aynı satırdaki farklı biçimli parçaları tek paragrafta toplar.
`right`, `center`, `fill` sırasıyla sağ/orta/iki yana yaslama olur. Link metni mavi
gösterilir. HTTP(S), sayısal node ve node+anchor bağlantıları desteklenir; dosya/folder
linkleri henüz desteklenmez. Node bağlantılarına mevcut tenant token eklenir.
Ek dosyalar ataç simgesi ve mevcut download endpoint'ine bağlantıyla gösterilir.
LaTeX özel gösterimi hâlâ kapsam dışıdır.

Önceki önizlemenin bağlantıları devre dışı bırakma kuralı güncellendi: izin verilen
bağlantılar yeni sekmede açılır, download izni verilir. Sandbox'ta script/same-origin/form
izni yoktur. Düğüm içeriğinin script, form ve event handler'ları ayıklanır. Parent sayfa,
mevcut yerel highlight.js ile detached DOM'daki codebox'ları renklendirip iframe'e koyar.
`myapp.syntax-highlighting.enabled` ayarına uyulur. Bilinmeyen lexer düz metin kalır.

Tablo/codebox'ın ardından gelen ilk newline, blok satırının sonlandırıcısı olarak işlenir;
böylece fazladan boş paragraf oluşturmaz. Birden fazla newline varsa sonraki boş satırlar
korunur. Bu davranış görsel karşılaştırmada ayrıca kontrol edilmelidir.

Beş test eklenir; önceki 98 üzerine toplam 103 beklenir. Eski parser'ın iç algoritması
değişmez; önizlemenin bağlantı politikası iki panel için güncellenir.

## Sandbox kaynakları ve aynı düğüm anchor'ları

Sandbox'ta `allow-same-origin` yoktur; iframe'in opaque origin'i nedeniyle önceki
`img-src 'self'` ve `style-src 'self'` yerel kaynakları engelleyebilirdi. Önizleme artık
highlight CSS'ini style içine, eski panelin aynı içerik düğümüne ait resimlerini PNG data
URI içine gömer. CSP yalnızca data resimlerine ve inline stillere izin verir; network
kaynaklarına veya düğüm scriptlerine izin verilmez. Parent sayfanın güvenilir highlight.js
işlemi aynı biçimde devam eder. Kaynak haritası yorumları gömülen CSS'ten kaldırılır.

Aynı içerik ID'sine yönelik node+anchor bağlantısı `about:srcdoc#anchor` ve `_self`
olarak hazırlanır; ilgili iframe içinde kaydırılır. Diğer düğüm bağlantıları yeni sekmede
kalır. Anchor nesnesi konumunda ⚓ gösterilir. Alias önizlemede karşılaştırma master
content ID üzerinden yapılır. Üç test eklenir; toplam 106 beklenir.

## Yeni reader/workspace görünümlerinde kullanım

Yeni mobil reader ve masaüstü tree content artık `custom-colors` içeriklerini ortak
`RichTextRenderingService` üzerinden gösterir. Eski mobil/desktop görünümler, quick content
ve PDF yolu bu değişiklik kapsamına alınmadı. Plain-text ve kod düğümlerinin yolu değişmez.
Karşılaştırma sayfası aynı ortak çekirdeği kullanır ve sandbox politikasını ayrıca uygular.

Canlı görünümde PNG data URI yerine mevcut `/images/{id}/{offset}` URL'si ve tenant token
kullanılır. Böylece mevcut büyüteç/zoom/orijinal resim işlemleri korunur. Aynı düğüm anchor'ı
sayfa içi bağlantı olur. Tablo/codebox yatay kaydırma markup'ı korunur. Mobil ve masaüstü
content JS/CSS, scroll-to-top ve toolbar dosyaları değiştirilmez. Rich text düzenleme veya
veritabanına yazma işlemi eklenmez. Parser hatası sessizce eski parser'a dönülmez; hatalı
örnekler ayrıca incelenmelidir. Üç test eklenir; toplam 109 beklenir.

## XML yazıcı ve semantik round-trip

`RichTextXmlWriter` yalnızca `node.txt` XML metni üretir. Veritabanı yazmaz; node veya
nesne tablolarını güncellemez. Henüz editör veya rich text kaydetme endpoint'i yoktur.
Metin parçaları, boş parçalar ve tüm attribute değerleri korunur. Attribute sırası,
boş element gösterimi ve XML declaration biçimi değişebilir; byte-level aynılık vaat edilmez.

Yazıcı HTML'i XML'e çevirmek için kullanılmaz: girdi `RichTextDocument` modelidir.
XML-invalid kontrol karakterleri, eşleşmemiş surrogate'lar, geçersiz attribute adları ve
uyumsuz metin offset'leri reddedilir. Yeni nesne konumlarını hesaplamaz; içerik değişmeden
round-trip'te mevcut yerleşimin korunduğu test edilir. Metin düzenlenirse nesne offset'leri
ayrıca hesaplanıp ileride tek transaction'da kaydedilmelidir.

Testler proje fixture'ı, 13 boş parça, Unicode, newline/CR/tab/boşluk, birleşik biçimler,
bilinmeyen attribute, link değerleri ve nesne yerleşimini kapsar. Sekiz test eklenir;
önceki 109 üzerine toplam 117 beklenir. Sonraki aşama nesnesiz yeni rich text editörüdür.

## İlk metin/biçim editörü

Yeni reader/workspace toolbar'ındaki Rich text düğmesi, admin ve writable tenant için
nesnesiz plain-text/rich-text düğümlerde görünür. Doğrudan yol:
`/nodes/richtext/edit/{id}`. Yeni düğüm oluşturma önceki düğme üzerinden yapılır; bu editörde
kaydetmek plain-text düğümü `custom-colors` biçimine dönüştürür. GET ve İptal dönüşüm yapmaz.

Metin alanında metni seçip bold/italic/underline/strike, monospace, foreground/background,
scale ve paragraf hizalaması uygulanır. Bu ilk sürüm tam WYSIWYG değildir: metin alanı ve
biçimli önizleme ayrıdır. Önizlemenin hizalama gösterimi sınırlıdır; kayıt formatına hizalama
attribute'u yazılır. Mevcut link/bilinmeyen attribute'lar seçili metin değiştirilmedikçe korunur;
link ekleme, listeler, nesneler ve daha gelişmiş düzenleme sonraki aşamalardır.

Backend admin, tenant token, tenant writable, gerçek node, content read-only, uygun syntax
ve gerçek image/codebox/grid tablo kayıtlarını denetler. Nesne flag'leri yanlış olsa bile
nesneli kayıt reddedilir. CSRF mevcut Spring Security/Thymeleaf mekanizmasını kullanır.
Kaydetme XML'i yeniden okuyup canonical writer ile üretir; txt/syntax/rich bit ve lastsave
tek transaction'da değişir. Diğer title bits ve node properties korunur. Eski içerik/revision
ve koşullu UPDATE, başka sekmede değişmiş düğümün ezilmesini engeller. Nesne tablolarına
insert/delete/update yapılmaz. Henüz mevcut nesneli rich text düzenlenemez.

Yedi backend test eklenir; önceki 117 üzerine toplam 124 beklenir. Manuel test: yeni boş
düğüm, Türkçe/emoji/newline, seçili metin biçimlendirme, save/reopen ve CherryTree ile açma.
Read-only, nesneli düğüm, stale tab ve İptal davranışını da kontrol edin.

## Editör geçmişi ve önizleme hizalaması

Textarea'nın yerleşik undo geçmişi yalnızca metni tuttuğundan, native Ctrl+Z/Y sonrasında
biçim modeli kaybolabiliyordu. Editör artık metin parçaları, attribute'lar ve seçimi tek
snapshot içinde tutar. Ctrl+Z, Ctrl+Y, Ctrl+Shift+Z, toolbar düğmeleri ve beforeinput
historyUndo/historyRedo istekleri aynı geçmişi kullanır. Undo sonrası yeni değişiklik redo
kolunu kaldırır. Geçmiş son 200 durumu tutar; sayfa kapanınca silinir. IME composition
sırasında parçalı girişler tek geçmiş adımında toplanır. Bu ilk sürüm hâlâ textarea +
önizlemedir; tam WYSIWYG değildir.

Önizleme artık aynı satırdaki biçim parçalarını bir paragrafta toplar; left/right/center/fill
hizalamasını gösterir ve boş satırları korur. Backend XML/kayıt yolu değiştirilmez.

Gerçek editör script'ini küçük DOM test double ile çalıştıran altı regresyon testi:
`node --test src/test/js/richTextEditor.test.cjs`. Testler Unicode seçimi, format/typing
undo-redo, yeni redo kolu, toolbar/kısayol uyumu, native history isteği ve önizleme
hizalamasını sınar. Bunlar Maven/JUnit sayısına dahil değildir; Java test sayısı 124 kalır.
Tarayıcıda Ctrl+Z/Y, paste, renk/biçim, save/reopen ve mobil Geri al/Yinele ayrıca denenmelidir.

## İlk görsel düzenleme görünümü

Editörde Görsel düzenleme seçeneği varsayılan olarak açıktır. Aynı araç çubuğu artık biçimli
metin üzerinde çalışır; seçeneği kapatınca önceki textarea + önizleme görünümüne dönülür.
İki görünüm aynı model ve undo/redo geçmişini paylaşır; backend kayıt yolu değişmez.
Bu birinci görsel sürüm sadece metin/biçim içindir; nesne içeren düğüm kısıtı devam eder.

Enter/newline, emoji ve karakter silme, seçili metni değiştirme, paste/cut ve CTRL+B/I/U
model üzerinden işlenir. Paste düz metin alır; dışarıdan HTML, script veya nesne aktarmaz.
Drag-and-drop bu aşamada devre dışıdır. Native/IME düzenlemelerinde DOM metni ve korunmuş
attribute metadata'sı modele okunur. Script/HTML, kayıt formatı olarak kullanılmaz.
Bilinen/bilinmeyen mevcut attribute'lar metin parçalarıyla birlikte taşınır. Son boş paragraf
ve boş attributed span korunur. Seçimler DOM UTF-16 konumundan Unicode code point'e çevrilir.

Bu custom editör henüz olgun bir masaüstü editörünün tüm davranışlarını sağlamaz. Mobil
IME, selection, paste ve tarayıcı undo davranışı ayrıca manuel denenmelidir. Link ekleme,
liste, nesne ekleme/silme ve rich text nesnelerini düzenleme bu aşamaya dahil değildir.

Java test sayısı 124 kalır. Node testleri toplam 12 olur:
`node --test src/test/js/*.test.cjs`. Manuel test: görsel ve textarea görünümü arasında
geçiş, metin/biçim, Enter, boş satır, emoji, cut/paste, undo/redo, save/reopen ve CherryTree.


## Mevcut nesneleri koruyarak metin düzenleme

Rich text editörü artık resim, ek dosya, çapa, tablo ve codebox içeren gerçek düğümlerin
metinlerini düzenleyebilir. Admin, writable tenant ve düğümün read-only olmaması koşulları
aynı kalır. Görsel düzenlemede resimler küçük önizlemeyle, diğer nesneler adlandırılmış
korunan kutularla gösterilir. Bu aşamada nesne ekleme, silme veya yeniden sıralama yoktur.
Textarea görünümünde her nesne tek bir korunan U+FFFC karakteriyle temsil edilir.

`ProtectedRichTextCodec` metin parçaları ile nesneleri ortak Unicode code-point konumlarına
yerleştirir. Kaydetmede mevcut nesnelerin tam bir kez ve aynı sırada bulunduğunu doğrular;
nesne işaretçilerini XML'den çıkarır ve yeni CTB buffer offset'lerini hesaplar. Editöre özel
`__sweet_object` attribute'u veritabanına yazılmaz. Bilinmeyen metin attribute'ları korunur.

Metin, rich-text biti, ts_lastsave ve image/grid/codebox offset'leri aynı transaction içinde
kaydedilir. Composite key çakışmalarını önlemek için offset'ler önce geçici negatif değerlere,
sonra nihai konumlarına taşınır. Yalnızca offset sütunu güncellenir; binary payload, dosya adı,
anchor, link, tablo ve codebox verileri değiştirilmez. Bir kayıt bulunamazsa işlem geri alınır.
Revizyon kontrolü bütün nesne alanlarını da kapsar; editör açıkken dışarıdan yapılan değişiklik
eski sayfadan sessizce ezilmez. CTB nesne konumlarının geçerli ve benzersiz olması gerekir.

Yeni testlerle Java test sayısı 131, Node test sayısı 13 olur. Node testleri Maven sayısına
dahil değildir: `node --test src/test/js/*.test.cjs`.
Manuel kontrolü demo.ctb'nin bir kopyasında yapın: 53 numaralı düğümde nesnelerden önce ve
nesneler arasına metin/emoji ekleyin; biçim verin, undo/redo yapın ve kaydedin. SweetCherry
ve CherryTree'de tekrar açıp resim, dosya indirme, çapa, tablo ve codebox'ın yerlerini ve
verilerini kontrol edin. Nesne kutusunu silme girişimi engellenmelidir.


### SQLite binary okuma düzeltmesi

Nesne revizyonu için image.png alanı okunurken Hibernate'in BLOB çıkarımı SQLite JDBC'de
`SQLFeatureNotSupportedException` oluşturabiliyordu. Sorgu artık binary sütunu nullable hex
metni olarak döndürür; servis bunu byte dizisine çevirerek aynı revizyon hesabında kullanır.
NULL ve boş binary birbirinden ayrılır. Veritabanında payload değişikliği yapılmaz.
`SqliteObjectPayloadTest` gerçek SQLite JDBC bağlantısıyla binary/boş/NULL okumasını sınar;
bu ek testle beklenen Java test sayısı 132 olur.


### Karışık nesne kayıtlarında JDBC okuma

Hex sorgusu driver seviyesinde çalışsa da Hibernate'in otomatik scalar type discovery
mekanizması karışık CTB kayıtlarında bir sütunu Decimal olarak okuyabiliyordu. Nesne
okuma artık Session.doReturningWork ile mevcut tenant transaction'ının bağlantısını
kullanır. image.png getBytes ile, diğer sütunlar SQLite getObject ile okunur. Hibernate
BLOB/Decimal çıkarımı bu yolda kullanılmaz. Bağlantı servis tarafından kapatılmaz;
statement ve result set kaynakları try-with-resources ile kapatılır.

SqliteObjectPayloadTest artık üretimde kullanılan readObjects metodunu doğrudan çağırır;
aynı düğümde NULL anchor payload, boş payload, PNG binary, attachment, binary tablo verisi
ve codebox metnini birlikte sınar. Servis testleri de gerçek bellekte SQLite bağlantısıyla
bu okuyucuyu kullanır. Test sayısı 132 kalır. Anchor ikonlarının gösterimi değiştirilmez.


## Dosyadan ve panodan resim ekleme

Rich text editöründe Resim ekle düğmesi PNG/JPEG dosyası seçer. Panoda gerçek resim binary
verisi varsa paste ile eklenebilir; web sayfasındaki uzak resim URL'leri indirilmez. Görsel ve
textarea görünümü aynı modeli kullanır. Resim kaydedene kadar yalnızca tarayıcı belleğinde
kalır; iptal veya sayfayı kapatma CTB'ye nesne eklemez. Undo/redo yeni resmin işaretçisini de
geri alır/getirir. Kayda sadece mevcut modelde kalan yeni resim payload'ları gönderilir.

Backend yeni resimleri ImageIO ile doğrulayıp PNG'ye dönüştürür. Bir resim en fazla 8 MB,
bir kayıt en fazla 10 resim ve 16 milyon pixel/resim kabul eder. Toplam JSON sınırı 16 MB,
üretilen toplam PNG sınırı 12 MB'dır. Dosya adı, HTML veya harici URL veritabanına resim
olarak yazılmaz. Yeni image kayıtlarında justification=left; anchor, filename, link boş ve
time=0 olur. node.has_image=1; txt, is_richtxt, ts_lastsave ve tüm object offset'leri aynı
transaction içinde kaydedilir. Admin/writable/real-node/read-only ve tenant/revision/CSRF
kontrolleri korunur. Mevcut nesnelerin silinmesi veya sırasının değiştirilmesi hâlâ yasaktır.

Clipboard düz metni U+FFFC yer tutucularını atar; nesneyi kopyalamadan boş OBJ karakteri
yapıştırmaz. Mevcut içerikte daha önce kaydedilmiş bağımsız U+FFFC karakterleri otomatik
silinmez. Nesnelerin iki yanında DOM caret alanları vardır; bunların yardımcı karakterleri
XML'e yazılmaz. Metin biçimini clipboard ile taşıma bu aşamaya dahil değildir.

server.tomcat.max-http-form-post-size=32MB hem ana connector hem ek HTTP connector için
uygulanır. JAR yanındaki harici application.yml kullanılıyorsa bu ayarı oraya da ekleyin;
reverse proxy daha düşük body sınırı uyguluyorsa onun da yeterli olması gerekir.

Beklenen Java test sayısı 138; Node test sayısı 16:
`node --test src/test/js/*.test.cjs`. Manuel test: bir CTB kopyasında boş ve nesneli düğüme
PNG/JPEG dosyası ekleyin; varsa panodan ekran görüntüsü yapıştırın. Resimden önce/sonra
emoji/metin ekleyin, undo/redo ve görsel/textarea geçişi yapın. Kaydedip SweetCherry ve
CherryTree'de açın; eski resim/attachment/table/codebox konumlarını kontrol edin. İptalde
kayıt eklenmediğini, read-only tenant/node ve user hesabında işlemin kapalı kaldığını sınayın.


## Attachment ekleme ve ortak tenant boyut sınırı

Dosya ekle düğmesi herhangi bir dosyayı attachment olarak alır; dosyanın MIME tipi onun
resim olarak yorumlanmasına neden olmaz. Editörde ataç ve dosya adıyla korunan kutu görünür.
Dosya adı yalnızca basename olarak saklanır; path ve kontrol karakterleri kabul edilmez.
Binary veri decode sonrası aynen image.png alanına, ad image.filename alanına yazılır.
anchor ve link boş; justification=left; time kaydetme anındaki Unix saniyesidir. Normal
okuma görünümü mevcut /download/{node_id}/{offset} bağlantısıyla indirmeyi sağlar.
Attachment içeriği çalıştırılmaz, parse edilmez veya uzak bir URL'den indirilmez.

Yeni resim ve dosyalar aynı protected-slot/Unicode offset, undo/redo ve transaction
akışını paylaşır. Yeni dosya undo ile kaldırılırsa Save isteğine dahil edilmez; redo ile
geri gelir. Sayfa kapatma veya Cancel kayıt oluşturmaz. Kaydetmede tüm eski nesnelerin
aynı sırada ve tam bir kez bulunması şartı sürer. Nesne silme kilidi henüz eklenmez.

Tenant TXT ayarı custom.maxEmbeddedFileSizeMB=9 resim ve attachment için ortaktır.
MB decimal olarak yorumlanır. Geçerli aralık 1-20 tam sayıdır; eksik/geçersiz ayar 9 MB'a
döner. Kaydetmede ayar yeniden okunur; frontend sınırı değiştirilerek aşılamaz. Resim
kaynak verisi ve normalize PNG ayrı ayrı limite uymalıdır. Dosyanın binary verisi üzerinde
boyut kontrolü uygulanır. Bir Save işleminde toplam en fazla 10 yeni nesne, 20 MB binary
ve iki payload JSON alanı birlikte 30 milyon karakter kabul edilir. HTTP/proxy form
boyutu limitleri ayrıca geçerlidir; base64/form kodlaması binary dosyadan daha büyüktür.
Bu sınırlar SweetCherry uygulama politikasıdır; SQLite/CherryTree'nin kesin sınırı değildir.

Beklenen Java test sayısı 147; Node test sayısı 18:
`node --test src/test/js/*.test.cjs`.
Manuel test: CTB kopyasında PDF, TXT ve binary dosya ekleyin; emoji/metin ve mevcut
nesneler arasında konumlandırın. Undo/redo ve Cancel ardından Save davranışını kontrol
edin. SweetCherry'den indirip orijinal dosyayla byte/hash karşılaştırın; CherryTree'de de
attachment görünümünü ve açılmasını kontrol edin. Tenant sınırını 1 yapıp veri kaynaklarını
yeniden yükleyin; 1 MB üstü dosya ve resmin reddini deneyin. Sonra ayarı geri alın.


## External link hedeflerini düzenleme

Araç çubuğunda Dış bağlantı düğmesi seçili metne HTTP/HTTPS hedefi ekler. Görsel veya
textarea görünümünde mevcut bir external link'in içine imleç koyup düğmeye basınca aynı
hedefe sahip bitişik metin parçaları birlikte seçilir. Farklı weight/style/color parçalarının
biçimleri korunur. Metnin bir kısmı özellikle seçilmişse işlem o seçime uygulanır.
Link kaldırma metni ve biçimini silmez; yalnızca external link attribute'unu kaldırır.
İptal/Escape içerik değişikliği yapmaz. Uygulama ve kaldırma aynı undo/redo modelini kullanır.

CTB'de attribute formatı link="webs https://..." olarak saklanır. URL tarayıcıda normalize
edilir (örneğin Unicode domain/karakterler); XML writer attribute'u güvenle escape eder.
Editör bağlantıyı tıklanabilir yapmaz; alt çizgi, varsayılan mavi renk ve hover hedefiyle
belirtir. Açıkça seçilmiş foreground rengi korunur. Okuma görünümündeki mevcut link renderer
bağlantıyı açar. Metin düzenleme hedef URL'yi değiştirmez; hedef bu diyalogdan düzenlenir.

ExternalRichTextLinks kaydetmeden önce yeni/değişmiş link attribute'larını doğrular:
absolute HTTP(S), host, en fazla 4096 karakter ve kontrol karakteri/kullanıcı bilgisi olmaması.
Mevcut internal/file/anchor attribute değerleri korunur. Internal/anchor link hedeflerini
oluşturma veya düzenleme bu yamaya dahil değildir. Korunan object slot'larına link attribute'u
uygulanmaz. Tenant, writable, admin, real node, read-only, revision ve CSRF kontrolleri sürer.

Beklenen Java test sayısı 153; Node test sayısı 22:
`node --test src/test/js/*.test.cjs`.
Manuel test: kalın/renkli bir metin seçip dış bağlantı ekleyin; metnin bir kısmını farklı biçimde
formatlayın; caret ile aynı link'in hedefini değiştirin. Link kaldırma ve undo/redo deneyin.
Save/reopen ve CherryTree'de hedefi kontrol edin. Query string (&), fragment (#), Unicode,
Cancel/Escape ve geçersiz javascript/file hedeflerini de deneyin. Nesneli düğümde diğer
object offset ve payload'larının değişmediğini gözle kontrol edin.


## Tablo ekleme ve hücre düzenleme

Tablo ekle düğmesi ayrı bir hücre diyaloğu açar. Yeni tabloda satır sayısı başlık dahil
1-100, sütun sayısı 1-20 olabilir. Ölçüleri değiştirince Boyutu uygula düğmesine basılır.
Hücreler düz metindir; newline, emoji ve XML özel karakterleri korunur. İlk görünen satır
başlıktır; CTB grid.txt XML'inde başlık satırı en sona taşınır. Yeni tablonun col_widths
attribute'u her sütun için 0; justification=left; col_min=col_max=200 olur.

Mevcut desteklenen tablo kutusunda Tabloyu düzenle düğmesi görünür. Bu ilk sürüm yalnızca
hücre metinlerini düzenler; mevcut satır/sütun sayısını değiştirmez. Root table attribute'ları
(col_widths ve bilinmeyen attribute'lar dahil), justification, col_min ve col_max aynen korunur.
XML'e yerleştirilmiş biçimli/nested cell content, geçersiz UTF-8 veya sınır dışı tablo varsa
hücre editörü açılmaz; nesne korunan kutu olarak kalır ve normal metin kaydı ona dokunmaz.

Yeni tablo boyutu değiştirilirken mevcut hücreler korunur. Küçültmede dolu hücrelerin dışarıda
kalması engellenir; kullanıcı önce bu hücreleri temizlemelidir. Nesne silme bu yamada yoktur.
Tablo değişiklikleri metinden ayrı payload'ta, aynı undo/redo snapshot'ında tutulur. Undo yeni
tabloyu kaldırırsa Save isteğine dahil edilmez. Cancel/Escape yalnızca diyaloğu kapatır.

RichTextTableCodec yalnızca rectangular string arrays alır; key, XML karakterleri ve boyut
kontrollerini yapar. Hücre başına 5000 karakter, toplam tablo JSON alanına 2 milyon karakter
ve en fazla 20 tablo edit payload'ı sınırı vardır. Yeni resim/dosya/tablo nesnelerinin toplamı
bir Save'de en fazla 10 olur. Mevcut tabloların nesne sırası ve tüm object offset'leri aynı
koruma kurallarına tabidir. Yeni tabloda node.has_table=1 olur. txt/ts_lastsave, offset'ler ve
grid kayıtları tek tenant transaction'ında yazılır; metadata değişmez ve kaydı bulunamayan
nesne işlem hatasına neden olur. Admin/writable/real node/read-only/revision/CSRF sürer.

Beklenen Java test sayısı 162; Node test sayısı 25:
`node --test src/test/js/*.test.cjs`.
Manuel test: CTB kopyasında yeni tablo ekleyip header/body metinleri, emoji, & ve newline
yazın; ölçüleri değiştirin ve undo/redo deneyin. Demo 15 ve 53'te mevcut tabloların hücrelerini
düzenleyin; Save/reopen ve CherryTree'de header sırası ve sütun genişliklerini kontrol edin.
Metin eklenince tablo ve diğer nesnelerin offset'lerinin doğru yerde kaldığını kontrol edin.


## İlk biçimli web/clipboard yapıştırma

RichTextClipboard clipboard HTML'yi inert template içinde parse edip desteklenen CTB
metin attribute'larına ve tablo payload'larına dönüştürür. Kaynak HTML editör DOM'una
aktarılmaz. Bold/italic/underline/strikethrough, monospace, h1-h6/small/sub/sup, desteklenen
hex/RGB renkler, inline text-align, newline ve HTTP(S) linkler aktarılır. Web sitesinin class
adları, harici CSS'i, özel fontları veya sayfa tasarımı kopyalanmaz. Script/style/iframe,
SVG/MathML ve event attribute'ları aktarılmaz; URL'ler mevcut external link kurallarına uyar.

Rectangular düz hücreli HTML tablolar grid payload'ına dönüşür. İlk satır başlık sayılır;
CTB kaydında başlık yine en sonda tutulur. Birleştirilmiş hücreli/nested/uyumsuz tablolar
metin olarak aktarılır; satır/sütun metni korunur, colspan/rowspan yapısı bu sürümde yoktur.
Tablo hücresi içindeki biçimlendirme düz metne dönüşür. Yeni tablo nesneleri protected
slot'larla gösterilir ve önceki tablo diyaloğundan düzenlenebilir.

Uzak web img adresi indirilmez: varsa alt metni, yoksa URL'si HTTP(S) bağlantı olarak
aktarılır. Bu ilk aşama uzak görselin binary verisini CTB'ye eklemez. HTML'de veri üretmeyen
bir resim yapıştırmasında, panoda gerçek PNG/JPEG file varsa mevcut image ekleme yolu
kullanılır. Sadece data-URI HTML olup binary file olmayan resimler bu sürümde import edilmez.
İstenilen web görselleri ayrıca panodan/dosyadan eklenebilir; otomatik resim importu ayrıdır.

SweetCherry içindeki copy/cut, düz metne ek olarak güvenli HTML ve özel metin/biçim clipboard
metadata'sı sağlar. Aynı editöre paste biçimleri taşır. Desteklenen text attribute'ları ve
external linkler korunur; bilinmeyen attribute/internal-link kopyalama bu aşamada yoktur.
Nesne işaretçileri kopyalanmaz; image/attachment/table/codebox nesnelerinin kendisini copy ile
çoğaltmak bu aşamaya dahil değildir. Korunan nesneyi içeren cut seçimi silinemez.

Paste tek undo/redo adımıdır; tablo payload'ları ve biçimli metin birlikte geri alınır/getirilir.
Clipboard HTML/metadata en fazla 2 milyon karakter, metin/hücre içeriği 200 bin karakter,
DOM traversal 10 bin node ve 64 derinlikle sınırlıdır. Limit aşıldığında keserek kaydetmek
yerine işlem yapılmaz ve bilgi gösterilir. Mevcut yeni nesne/tablo sınırları da geçerlidir.
Backend XML/link/table/revision/tenant/role/read-only kontrolleri değişmez.

Java test sayısı 162 kalır; Node test sayısı 35:
`node --test src/test/js/*.test.cjs`.
Manuel test: web sayfasından başlık, kalın/renkli metin, link ve basit tablo seçip yapıştırın.
Editör içinde biçimli metin copy/cut/paste deneyin. Undo/redo ardından Save/reopen ve
CherryTree'de metin/biçim, link hedefi ve tablo başlığını kontrol edin. Uzak resimlerin
bu aşamada görüntü yerine link olarak geldiğini, gerçek pano PNG/JPEG'nin hâlâ eklendiğini
kontrol edin. Browser/pano HTML kalitesi farklı olabilir; görsel karşılaştırma gereklidir.
