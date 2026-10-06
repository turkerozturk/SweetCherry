# Düğüm içeriği PDF dışa aktarma planı

Tarih: 2026-10-07. Başlangıç: `0bebdf6`. Bu belge hedef davranışı ve aşamaları
anlatır; aşağıdaki özellikler henüz tamamlanmış değildir.

## Mevcut yol ve karar

`src/main/java/com/turkerozturk/pdf/PdfFromHtmlController.java` düğüm içeriğini
eski NodeContentParserService ve `node/exportNodeToPdf` şablonuyla hazırlayıp
`src/main/java/com/turkerozturk/helpers/PDFFromHTMLHelper.java` içindeki OpenPDF
`HTMLWorker` parser'ine verir. Ayrı `PdfGenerator` / `FontHelper` örneği PDFBox
kullanır. Flying Saucer bağımlılığı mevcut olsa da bu düğüm yolunda kullanılmaz.
HTMLWorker'ın yerine yalnız sürüm yükselterek gelişmiş CSS yerleşimi sağlanmaz.

Yeni yol mevcut rich-text rendering hizmetini kullanıp PDF'ye özel XHTML/CSS
üretecek. İlk renderer adayı Java 17 uyumlu Flying Saucer 9.13.3 + OpenPDF 2.0.5.
PDFBox 3.0.8 üretilen PDF'in metin, sayfa ve annotation testlerinde kullanılacak.
Mevcut yardımcılar ilk aşamada korunur; yeni yol karşılaştırılarak devreye alınır.
CSS 2.1 tabanlı renderer tarayıcıdaki bütün HTML/CSS davranışlarını sağlamaz.

## İstenen seçenekler

| Özellik | Hedef |
|---|---|
| Metin ve font | Gömülü Unicode fontlar; bold/italic, boyut, renk, hizalama, alt/üst simge |
| Renk | Renkli / renksiz çıktı; renk kapalıyken metin ve arka plan okunaklı |
| Görseller | Sayfa içine sığma, içerikteki sıra/hizayı koruma, en-boy oranını koruma |
| Link | Dış URL annotation'ı, güvenli hedef; internal/anchor linkler ayrı ele alınır |
| Başlık / TOC | h1–h6 için hedefler; tıklanabilir içindekiler ve PDF outline ayrı kavramlar |
| Sarma | Uzun metin, URL ve codebox/pre; syntax rengi seçenekle uyumlu |
| Sayfa | A4 / Letter ve portrait / landscape |
| Üst / alt bilgi | Ayrı enable/disable; dosya adı ve sayfa numarası için ayrılmış marj |
| Düğüm adı | İçerik başlığını göster/gizle; dosya adı bundan bağımsız |
| İndirme | Güvenli UTF-8 dosya adı, `.pdf`, attachment disposition ve PDF MIME |

PDF bir ekran görüntüsü değildir: sayfa geçişleri, geniş tablolar ve uzun kodlar
PDF'ye özel yerleşim gerektirir. Tablo için tekrar eden başlık ve sayfa bölünmesi
kontrol edilir; çok geniş tablo davranışı gerektiğinde ayrıca seçenek olur.
“Diğer diller” tek fontla garanti edilemez: font glyph kapsamı, font fallback,
RTL/bidi ve karmaşık yazı shaping'i ayrı doğrulanır. Türkçe ilk kabul grubudur;
Latin dışı metinler fixture olarak eklenir. Eksik glyph sessizce kaybolmamalıdır.
Font ve lisansları uygulama paketinde yer almalı; işletim sistemi fontlarına veya
internet indirmesine bağımlı olunmamalıdır.

## Aşamalar

1. **Bağımlılık tabanı:** OpenPDF/fontlar 2.0.5, Flying Saucer 9.13.3; PDFBox
   3.0.8. Renderer uyumluluk testi. Bu yama yalnız bu aşamayı ve planı içerir.
2. **Temel yeni export:** Ayrı PDF servisleri, gömülü fontlar, Türkçe ve format
   testi, tek düğüm ve shared occurrence çözümleme, güvenli kaynak yükleyici,
   tıklanabilir dış linkler ve doğru download header'ı.
3. **İçerik yerleşimi:** Veritabanından resimler, tablolar, codebox ve satır
   sonları; görsel hizalama/sığdırma, uzun satır/URL sarma ve renk seçimi.
4. **Seçenek ekranı:** A4/Letter, yön, node name ve üst/alt bilgi seçenekleri;
   masaüstü/mobil PDF bağlantılarının seçenek ekranına bağlanması.
5. **TOC ve kabul:** Başlık hedefleri, tıklanabilir TOC/outline, sayfa numaraları;
   Java 17 Windows ve Raspberry Pi gerçek paketinde karşılaştırma.

## Kaynak erişimi ve hata davranışı

İçerikteki image URL'lerini backend üzerinden serbest HTTP veya file isteğine
çevirmeyin. CTB görselleri aynı aktif tenant'ın repository/service katmanından
alınır; classpath fontlar kontrollü kaynaktır. Dış linkler tıklanabilir olarak
saklanabilir ama PDF oluştururken hedefleri ziyaret edilmez. Harici resim URL'leri
başlangıçta indirilmez; kullanıcıya anlaşılır eksik görsel bilgisi gerekir.
XML entity/DTD çözümleme kapalı olur. Boyut, görsel çözünürlük ve üretim süresi
sınırları belirlenir; Raspberry Pi için sınırsız eşzamanlı üretim yapılmaz.
Başarısız üretimde boş PDF indirmek yerine yerelleştirilmiş hata gösterilir.
Tenant guard, stale-tab token ve shared/master ayrımı korunur.

## Kabul kanıtları

Otomatik: PDFBox ile Türkçe metin çıkarma, page MediaBox ölçüsü, sayfa sayısı,
link URI annotation'ı, TOC/outline hedefleri, font embedding, PDF dosya başlığı,
resim kaynak politikası ve invalid seçenekler. Metin çıkarma tek başına görsel
kanıt değildir; glyph/font/shaping ve sayfa taşmaları ayrıca gözle kontrol edilir.

Manuel: demo node 53, boş düğüm, plain text, shared düğüm; uzun URL, çok uzun kod,
geniş ve çok sayfalı tablo, farklı görsel hizaları, Türkçe ve Latin dışı metin.
Üretilen PDF browser ve ayrı PDF okuyucuda açılır; linkler/TOC tıklanır, renk ve
sayfa seçenekleri karşılaştırılır. Yeni yol kabul edilene kadar mevcut yolun
çalıştığı sürümle karşılaştırma yapılır.


## 2026-10-07 — İlk düğüm PDF yolu

Başlangıç commit'i `cd268fe`. `/export-node-to-pdf/{nodeId}` artık
`NodePdfExportService` → `NodePdfDocument` → `NodePdfRenderer` zincirini kullanır.
Rich-text için mevcut `RichTextRenderingService.render` çağrılır; plain text ve
syntax düğümleri bu aşamada kaçışlanmış düz metin olarak alınır. Shared ağaç ID'si
master içerik ID'sine çözülür. Tenant guard ve yeni mobil/masaüstü linklerinin
`_tenantView` token'ı uygulanır. Template PDF ve diğer eski PDF endpoint'leri
bu aşamada değiştirilmez.

Seçeneksiz varsayılan: A4 dikey, 18 mm marj, renkli metin ve düğüm adı başlık.
Fontlar mevcut `openpdf-fonts-extra:2.0.5` içindeki Liberation Sans / Mono'nun
regular, bold, italic ve bold-italic dosyalarından PDF'ye gömülür. Yeni TTF kopyası,
sistem font taraması veya internetten font indirme yoktur. Font kaynak ve lisansı
aynı JAR'ın `liberation/README`, `liberation/LICENSE` ve `META-INF/LICENSES.md`
dosyalarındadır. Türkçe/Latin ilk kabul kapsamıdır; CJK, emoji ve karmaşık yazı
shaping'i bu fontlarla garanti edilmez.

Satır sonları açık `br` olarak hazırlanır; format, paragraf hizası ve güvenli
HTTP(S) linkler korunur. PNG'ler parser'ın veritabanı snapshot'ından alınır,
en-boy oranıyla kullanılabilir sayfa alanına sığacak açık ölçüler verilir.
Temel tablo kenarlıkları ve monospace codebox yerleşimi vardır. Sayfa içi anchor
hedefleri korunur; dekoratif çapa ve ataç karakterleri PDF'ye eklenmez. Attachment
adı metin olarak kalır; tenant token'lı dosya indirme adresleri PDF'ye taşınmaz.

Kaynak erişimi: yalnız gömülü PNG, izin listesindeki sekiz Liberation fontu ve
renderer'ın kendi classpath XHTML varsayılan CSS'i açılır. Başka URL/file/CSS
kaynağı açılmaz; dış bağlantı hedefi PDF üretirken ziyaret edilmez. XML güvenli
DOM olarak hazırlanır. Harici görsel kaynakları metin işaretiyle belirtilir.
Tek uygulamada bir eşzamanlı PDF; fazlası 429. Metin 8 Mi karakter, hazırlanmış
HTML 64 Mi karakter, toplam PNG 48 MiB, tek resim 40 milyon piksel üst sınırı.
Bu ilk sınırlar henüz ayar ekranına bağlı değildir. Oluşturma hatası boş PDF
indirmek yerine hata yanıtıyla sonuçlanır. Dosya adı UTF-8 attachment `.pdf` ve
`Cache-Control: no-store` ile indirilir.

Henüz tamamlanmayanlar: seçenek ekranı, geniş tablo/çok uzun kesintisiz kod ve URL
sarma, codebox syntax rengi, ayrıntılı görsel hizası, TOC/outline, header/footer,
font fallback ve Latin dışı diller. Normal sözcük aralarında satır sarma vardır.
Bunlar sonraki aşamalarda örnek içerikle karşılaştırılacaktır.

Kabul: `mvnw.cmd clean package`; desktop/mobile düğüm PDF, shared düğüm, boş/plain
text, demo 53 ve araya metin giren çoklu resim. Türkçe harfleri, format birleşimini,
link tıklamayı, satır sonlarını ve resimlerin üst üste binmediğini kontrol edin.
`NodePdfExportTest` bu yolun temel üretim, font embedding, Unicode, bağlantı,
resim ayrımı, kaynak kısıtı, shared çözümleme ve download header kontrolleridir.
Burada tam Maven çalıştırılmadı; iki renderer sınıfı Java 17/ECJ ile derlenerek
ayrı harness'te PDF üretimi doğrulandı. Üretilen örnekte Türkçe metin, gömülü
fontlar, URI annotation'ı ve birbirinden ayrı iki resim alanı incelendi.
