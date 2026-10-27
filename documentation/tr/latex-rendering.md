# LaTeX nesnelerini görüntüleme

CherryTree, LaTeX nesnesini `image` tablosunda `filename=__ct_special.tex` ve
`png` alanında UTF-8 TeX metni olarak saklar. Bu alanın adı `png` olsa da veri
resim değildir. `external/CTBDATA/demo.ctb` içindeki **53** numaralı içerik
bu biçime örnektir; aynı formülün küçük bir metin fixture’ı testlere eklenmiştir.

## Web görünümü

Rich-text okuyucu, bu özel eki JLaTeXMath 1.0.7 ile bellekte PNG’ye çevirir.
Resim doğal boyutun iki katı çözünürlükte üretilip normal boyutta gösterilir.
Harici TeX kurulumu, tarayıcı eklentisi veya internet bağlantısı gerekmez.
Diğer ek dosyalar, resimler ve anchor’lar mevcut davranışlarını korur.

Tam belgedeki preamble (`documentclass`, `usepackage` vb.) çalıştırılmaz;
yalnız document gövdesi matematik olarak işlenir. `align*` / `align`, `aligned`
biçimine uyarlanır. Bu bir tam TeX dağıtımı değildir: desteklenmeyen formülde
sayfa bozulmaz, kaynak metin escaped kod olarak gösterilir. Bozuk UTF-8 veri
normal ek dosya davranışında kalır.

Dosya/ağ okuyan komutlar, dinamik eklentiler ve kullanıcı makro tanımları
çalıştırılmaz. Kaynak 16.384 karakter, komut sayısı 2.048, süslü parantez
iç içeliği 64 ile sınırlıdır. Çizim boyutu ve küçük önbellek de sınırlıdır;
ayarlara yeni bir sistem TeX yolu eklenmez.

## PDF

LaTeX seçeneği açıksa varsayılan renkli PDF’de glyph outline’ları vektör
Form XObject olarak çizilir. Bu yol kullanılamazsa aynı PNG kullanılır.
Renksiz çıktı mevcut grayscale PNG yolunu kullanır. Resimleri kapatmak,
LaTeX seçeneği açıkken formülleri kapatmaz. LaTeX kapatılırsa önceki gibi
`<latex object: nodeId:offset>` yer tutucusu yazılır.

PDF içinde denklem tekrar düzenlenebilir TeX metni veya MathML değildir;
vektör çizimdir. Kaynak CTB’deki TeX baytları değişmez.

## Rich-text editörünün korunması

Editörün veri akışı değiştirilmedi. LaTeX nesnesi editörde korumalı
`__ct_special.tex` eki olarak kalır. Yalnız okuyucu/PDF görüntüleme için
ayrı bir snapshot dönüştürülür; dönüştürülen PNG veritabanına yazılmaz.
Değişikliksiz Kaydet işlemi BLOB, dosya adı, offset veya düğüm tarihini
bu görüntüleme özelliği nedeniyle değiştirmemelidir.

## Manuel kontrol

1. Demo CTB’de 53’ü masaüstü ve mobil okuyucuda açın. Üç satırlı formüldeki
   kare, kesir ve integrali kontrol edin. Gerçek resimler, anchor’lar ve PDF eki
   eskisi gibi kalmalı.
2. Rich-text editörünü açın: özel ek korumalı nesne olarak görünmeli.
   Değişikliksiz kaydedip aynı kaydın `png`, `filename`, `offset` değerlerini
   karşılaştırın. Normal bir metin değişikliğinde de nesne baytları korunmalı.
3. Düğüm ve alt ağaç PDF’sini indirin; formülün boyutu, konumu ve yakınlaştırma
   kalitesini kontrol edin. LaTeX’i kapatın; sonra yalnız resimleri kapatıp
   LaTeX’i açık bırakın. Renksiz çıktıyı da deneyin.
4. Test CTB’sinde geçersiz/unsupported bir formül kullanın: kaynak metin
   gösterilmeli, diğer içerik kaybolmamalı. Kaynak dosya otomatik onarılmaz.
5. `/codeboxes/53`, `/grids/53`, `/images/53` sayfalarını Türkçe/İngilizce açın.
   Başlık, sütun ve hizalama/Yes-No değerlerini; nesnesiz bir düğümde boş durum
   açıklamasını kontrol edin.

Bağımlılık: https://github.com/opencollab/jlatexmath
Lisanslar: `distribution-notices/licenses/jlatexmath-1.0.7/`.

## English

CherryTree LaTeX objects are UTF-8 `__ct_special.tex` attachments. The reader
renders a display-only PNG using JLaTeXMath; color PDFs prefer vector glyph
outlines with PNG fallback, and monochrome PDFs retain the grayscale PNG path.
Unsupported expressions show escaped source instead of breaking the page.
No TeX executable, filesystem/network resource inclusion or user macro definition
is used. The editor keeps its protected attachment representation and original
stored bytes. Demo CTB node 53 and the corresponding formula fixture cover the
full-document/aligned example.
