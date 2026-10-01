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
