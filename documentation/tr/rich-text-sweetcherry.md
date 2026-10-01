# SweetCherry rich text okuma zinciri

İnceleme tarihi: 2 Ekim 2026. İncelenen SweetCherry commit'i: `dd911ed28962a95ada82d03745a0704d1fe6f7d3`.
Bu belge kaynak kod incelemesidir; görsel eşitlik veya bütün CTB sürümleri için uyumluluk sertifikası değildir.
CherryTree tarafının işleyişi [ayrı belgede](rich-text-cherrytree.md), farklar ve geliştirme planı
[karşılaştırma belgesinde](rich-text-compatibility-plan.md) açıklanır.

## Amaç ve kapsam

SweetCherry, CherryTree'nin CTB verisini webde görüntülemek için Java tarafında bir dönüştürme
katmanı içerir. Mevcut rich text zinciri XML ve ilişkili CTB nesnelerinden HTML üretir.
HTML'den CherryTree XML'ine geri yazan bir rich text içerik editörü bu zincirde bulunmaz.
Düz metin düzenleme ve düğüm özelliklerini güncelleme ayrı işlemlerdir.

Rich text seçimi `NodeContentParserService.parseNodeContent()` içinde `node.syntax == custom-colors`
ile yapılır. `Node` yükleme sonrası `is_richtxt` alanının düşük bitini de ayrıştırır;
ancak bu metotta yönlendirme doğrudan syntax üzerinden yapılır. Başlık rengi ve kalınlığı aynı
bit alanında saklanır; bunlar içerikteki XML biçimlendirmesiyle karıştırılmamalıdır.

## Etkin okuma adımları

| Adım | Sınıf / metot | İşlem |
| --- | --- | --- |
| Tür seçimi | `NodeContentParserService.parseNodeContent()` | Plain text, kod ve `custom-colors` içeriğini ayırır. |
| XML okuma | `NodeService.getRichTextsMap()` | Java DOM ile XML'i açar, `rich_text` elementlerini toplar. |
| Metin konumu | Aynı metot | Metin uzunluklarından kümülatif başlangıç offset'i üretir; boş element için 1 ekler. |
| Nesneleri birleştirme | `NodeContentParserService.parseNodeTxt()` | Anchor, resim, attachment, codebox ve grid kayıtlarını aynı haritaya ekler. |
| Sıralama | `ExampleMap.sortByKey()` | Haritayı offset sırasına dizer. |
| HTML oluşturma | `YeniXMLTransformer.parse()` | Nesne türüne ve rich text attribute'larına göre DOM elementleri üretir. |
| Tablo içeriği | `GridTxtXmlTransformer.parse()` | Tablo XML'ini HTML tabloya dönüştürür. |
| Sonuç | `NodeContentParserService` | HTML'i `txtAsHtml` ve `txtAsTransformed` alanlarına verir; kaynak `txt` değiştirilmez. |

`parseNodeContent()` başında bazı nesne haritaları ayrıca hazırlanır; aktif sonuç üretimi
`parseNodeTxt()` içinde yeniden kurulan `ExampleMap` üzerinden ilerler. Bu ayrım, sonraki
sadeleştirmede korunması gereken etkin yolu gösterir; bu incelemede kod kaldırılmamıştır.

## Görüntülenen içerik

- Metin DOM text node olarak eklenir; `white-space: pre-wrap` ile satır sonları ve boşluklar korunur.
- Foreground/background renkleri ve bazı font özellikleri CSS'e dönüştürülür.
- `scale` değerleri başlık, small, sub ve sup elementlerine yönlendirilir.
- Web, düğüm, düğüm+anchor, dosya ve klasör linkleri için dönüşüm dalları bulunur.
- Normal resim için `/images/{nodeId}/{offset}` kullanılır. İstenirse binary veri base64 olarak gömülür.
- Attachment için `/download/{nodeId}/{offset}` bağlantısı üretilir.
- Codebox, `pre/code` ve mevcut syntax highlighter yolu üzerinden gösterilir.
- Grid XML'indeki son satır başlık olarak öne taşınır, diğer satırlar sırasıyla eklenir.
- Anchor kayıtları haritaya alınır; aktif renderer'da bunlara karşılık gelen HTML anchor üretimi tamamlanmamıştır.

PDF yolu `PdfFromHtmlController` üzerinden `parseNodeTxt(node, true)` çağırır.
Webdeki CSS/JavaScript davranışı ile PDF motorunun desteği aynı değildir; yalnızca kaynak dönüştürme
zincirini paylaşmaları bütün görsel sonuçların eşit olacağı anlamına gelmez.

## Mevcut notlar ve geliştirme izi

Kaynakta açıklamalar ve örnekler vardır; bilgiler tek bir format şartnamesinde toplanmamıştır.

| Yer | Mevcut kayıt |
| --- | --- |
| `NodeContentParserService` | 20 Nisan 2024 öncesi ve sonrası parser ayrımı; eski çağrı yorum satırındadır. |
| `NodeTxtXmlTransformer` | Önceki dönüşüm yaklaşımı ve stil/offset notları. |
| `Test20240331Parser` | Sabit XML örneği ve nesne offset'leriyle elle deneme kodu; JUnit regresyon testi değildir. |
| `ParserController` | Benzer harita kurma ve XML deneme akışı; normal node sayfasının giriş noktası değildir. |
| `YeniXMLTransformer.image()` | Endpoint üzerinden resim gösterme ile base64 gömme arasındaki farkın açıklaması. |
| `YeniXMLTransformer.parseRest()` | Boşluk/satır sonu seçenekleri ve anchor TODO'ları. |
| `YeniXMLTransformer.cherryToCSSColor()` | 12 hex haneli renk değerini CSS renk değerine dönüştürme açıklaması. |
| `ExampleMap` ve servis yorumları | Aynı offset'te kayıt üzerine yazma ihtimaline ilişkin deneme notları. |
| `YeniXMLTransformerLinkTest` | Dış linklerin izin verilen şemaları için mevcut otomatik test. |

Bazı yorumlarda “Pango XML” adı kullanılmıştır. Attribute adlarının GTK/Pango biçimlendirmesine
benzemesi anlaşılır; fakat CTB içeriğinin tamamını standart Pango markup olarak tanımlamak doğru değildir.
İncelenen CherryTree kodu bu XML'i kendi storage helper'larıyla yorumlar.

## İncelemenin sınırları

Kaynakta metin biçimlendirme, offset, Unicode ve nesne yerleşiminin tamamını kapsayan bir otomatik
uyumluluk test kümesi görülmemiştir. Kullanım sırasında iyi sonuç alınması değerlidir; fakat bundan
sayısal bir başarı yüzdesi çıkarılmamıştır. Belirlenen farklar karşılaştırma belgesinde ayrı ayrı listelenir.

## Kaynaklar

- [NodeContentParserService](https://github.com/turkerozturk/SweetCherry/blob/dd911ed28962a95ada82d03745a0704d1fe6f7d3/src/main/java/com/turkerozturk/node/NodeContentParserService.java)
- [NodeService](https://github.com/turkerozturk/SweetCherry/blob/dd911ed28962a95ada82d03745a0704d1fe6f7d3/src/main/java/com/turkerozturk/node/NodeService.java) ve [Node](https://github.com/turkerozturk/SweetCherry/blob/dd911ed28962a95ada82d03745a0704d1fe6f7d3/src/main/java/com/turkerozturk/node/Node.java)
- [YeniXMLTransformer](https://github.com/turkerozturk/SweetCherry/blob/dd911ed28962a95ada82d03745a0704d1fe6f7d3/src/main/java/com/turkerozturk/yenixmlparser/YeniXMLTransformer.java)
- [ExampleMap](https://github.com/turkerozturk/SweetCherry/blob/dd911ed28962a95ada82d03745a0704d1fe6f7d3/src/main/java/com/turkerozturk/yenixmlparser/ExampleMap.java)
- [GridTxtXmlTransformer](https://github.com/turkerozturk/SweetCherry/blob/dd911ed28962a95ada82d03745a0704d1fe6f7d3/src/main/java/com/turkerozturk/cherryxml/GridTxtXmlTransformer.java)
- [Önceki transformer](https://github.com/turkerozturk/SweetCherry/blob/dd911ed28962a95ada82d03745a0704d1fe6f7d3/src/main/java/com/turkerozturk/cherryxml/NodeTxtXmlTransformer.java) ve [2024 deneme örneği](https://github.com/turkerozturk/SweetCherry/blob/dd911ed28962a95ada82d03745a0704d1fe6f7d3/src/main/java/com/turkerozturk/cherryxml/Test20240331Parser.java)
- [ParserController](https://github.com/turkerozturk/SweetCherry/blob/dd911ed28962a95ada82d03745a0704d1fe6f7d3/src/main/java/com/turkerozturk/yenixmlparser/ParserController.java)
- [PDF çağrısı](https://github.com/turkerozturk/SweetCherry/blob/dd911ed28962a95ada82d03745a0704d1fe6f7d3/src/main/java/com/turkerozturk/pdf/PdfFromHtmlController.java)
