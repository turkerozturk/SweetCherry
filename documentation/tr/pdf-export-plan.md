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
