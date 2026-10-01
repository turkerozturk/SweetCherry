# CherryTree CTB rich text veri modeli

İnceleme tarihi: 2 Ekim 2026. İncelenen CherryTree commit'i: `c6e626b1f4011056f21369d3343e2bae2723e34d`.
Belge, bu commit'in storage, buffer ve HTML export kodlarının teknik özetidir; CherryTree projesinin
resmî format şartnamesi değildir. Kaynak proje ve katkı geçmişi:
https://github.com/giuspen/cherrytree

## Hazır Markdown veya Pango parser'ı mı?

İncelenen CTB yolu Markdown kullanmaz. CherryTree, `libxml++`/`libxml2` ile XML yapısını okur;
XML'in içerik anlamını `CtStorageXmlHelper` ve diğer CherryTree sınıfları belirler.
Düzenlenen içerik GTK `TextBuffer`, text tag ve child anchor nesneleriyle temsil edilir.
GTK/Pango font, renk ve yerleşim katmanında; GtkSourceView ise metin/kod buffer altyapısında kullanılır.

Bu kütüphaneler birbirinin yerine geçmez:

| Katman | Sorumluluk |
| --- | --- |
| Genel XML kütüphanesi | Element, attribute ve text node yapısını okumak/yazmak. |
| CherryTree storage kodu | `rich_text` parçaları, nesne kayıtları ve CTB alanlarının anlamını belirlemek. |
| GTK buffer / text tags | Düzenlenebilir metni ve biçim aralıklarını bellekte tutmak. |
| Pango | Yazı özelliklerini ve metin yerleşimini desteklemek. |
| GtkSourceView | Kod/metin buffer ve editör özelliklerini desteklemek. |
| SQLite | Düğüm ve gömülü nesne verilerini kalıcı saklamak. |

Standart Pango markup çoğunlukla `span`, `b`, `i` gibi etiketlerle açıklanır. CherryTree CTB içindeki
`node/rich_text` ve ayrı nesne tabloları bunun doğrudan karşılığı değildir.
Java DOM/StAX gibi araçlar XML katmanının karşılığını sağlayabilir; CherryTree'nin anlam ve offset
kuralları için ayrıca bir codec gerekir. Bu inceleme bütün üçüncü taraf Java projelerinin taraması değildir.

## CTB'de ne saklanır?

Rich text için `node.syntax` değeri `custom-colors` olur. `node.txt`, kökü `node` olan XML metnidir.
Metin, biçim attribute'ları taşıyan ardışık `rich_text` parçalarına ayrılabilir.
Aşağıdaki örnek açıklama için oluşturulmuştur; upstream dosyasından alıntı değildir:

```xml
<node>
  <rich_text>Normal metin </rich_text>
  <rich_text weight="heavy" foreground="#3584e4">kalın renkli metin</rich_text>
</node>
```

CTB'deki bu XML, metinle ilişkili her binary nesneyi içinde taşımaz.
`image`, `codebox` ve `grid` tablolarında node ID, offset ve nesneye özgü içerik bulunur.
`image` tablosu normal resim dışında anchor, dosya eki ve LaTeX gibi durumlar için de kullanılır;
bunlar alanlarına göre farklı widget türlerine çevrilir.
CTD/XML veya multifile biçimlerinin nesne saklama yolu CTB ile birebir aynı değildir.

Başlık bit alanının kaynakta açıklanan yerleşimi:

| `node.is_richtxt` bölümü | Anlam |
| --- | --- |
| Bit 0 | Rich text içerik işareti |
| Bit 1 | Düğüm başlığının kalınlık işareti |
| Bit 2 | Düğüm başlığında foreground rengi belirtilmiş olması |
| Bit 3–26 | Başlık renginin 24 bit RGB değeri |

Bu tablo içerikteki rich text renk/kalınlık attribute'larını açıklamaz; onlar `node.txt` XML'inde tutulur.
Bit 2, rich text tür işareti değildir. `node.is_ro` ise ayrı bir bit alanıdır.

## Okuma yolu

1. `CtStorageSqlite.get_delayed_text_buffer()` CTB'den metni ve nesne varlık işaretlerini okur.
2. Rich text için `CtStorageXmlHelper.create_buffer_no_widgets()` XML'i parse eder.
3. `_add_rich_text_from_xml()` parça metnini buffer'a ekler; tanınan attribute'lar için text tag oluşturur.
   Metni boş olan parça burada metin karakteri üretmez.
4. Codebox, grid ve image tablolarından widget'lar yüklenir.
5. Widget'lar offset sırasına dizilir ve `insertInTextBuffer()` ile child anchor olarak buffer'a yerleştirilir.

Bu sıra önemlidir: XML metni önce oluşturulur, widget'lar sonra belirtilen karakter konumlarına eklenir.
Her boş `rich_text` parçasının bir nesne olduğu şeklinde genel bir kural yoktur.

## Yazma yolu

`CtTextIterUtil.generic_process_slot()` text tag değişimlerini izleyerek metin aralıklarını dolaşır.
`save_buffer_no_widgets_to_xml()` bu aralıklardan metin ve attribute içeren `rich_text` elementleri üretir.
CTB yazımı XML'i `node.txt` alanına, widget'ları ilgili ayrı tablolara kaydeder.
İçerik değişiminde `has_codebox`, `has_table`, `has_image` ve `ts_lastsave` de güncellenir.
Mevcut nesnelerin temizlenmesi ve yeniden yazılması storage senkronizasyon akışının parçasıdır.

XML serializer'ın aldığı GTK `get_text()` sonucu gömülü nesneleri metin olarak içermez.
Buffer offset'leri ile yalnızca XML'deki metni sayarak elde edilen offset'ler aynı olmayabilir.

## Biçimlendirme ve offset semantiği

`CtConst.TAG_PROPERTIES`, incelenen kaynakta şu 12 özelliği tanımlar:
`weight`, `foreground`, `background`, `style`, `underline`, `strikethrough`, `scale`,
`invisible`, `family`, `justification`, `link`, `indent`.
Bunlar reader tarafından metin tag'lerine, gerektiğinde GTK/Pango özelliklerine çevrilir.
Bazı ölçek stilleri uygulama ayarlarına bağlıdır; bu nedenle HTML başlıklarının varsayılan CSS'iyle
masaüstü görünümün piksel düzeyinde aynı olması beklenmemelidir.

Offset bir SQLite byte konumu veya Java UTF-16 indeksinden ziyade GTK buffer karakter konumudur.
UTF-8 byte uzunluğu, Unicode code point sayısı, Java `String.length()` ve kullanıcının tek sembol olarak
gördüğü grapheme sayısı ayrı kavramlardır. Uyumluluk katmanında dönüşüm açıkça yapılmalıdır.

Örneğin `A😀B` üç Unicode code point, dört UTF-16 code unit içerir.
GTK child anchor'ın da buffer konumunda yeri vardır; yazılan XML'de bu nesne metin olarak bulunmaz.
Bu nedenle metin run'larını ve widget offset'lerini bir araya getirmek yalnızca XML sıralama işlemi değildir.

## Tablo ve HTML export açısından yararlı referanslar

`CtTableHeavy.write_strings_matrix()` tablo başlığını XML'de en sona yazar.
`CtStorageXmlHelper.populate_table_matrix()` son satırı başa alır.
SweetCherry'nin benzer satır taşıması bu kaynak davranışıyla uyumludur.

`CtExport2Html._html_get_from_treestore_node()` widget konumlarında metin aralıkları çıkarır;
`html_process_slot()` ve liste işleme yardımcıları HTML üretir.
SweetCherry için yalnızca storage kodu değil, bu HTML export yolu da karşılaştırma referansıdır.
GTK buffer'a bağlı olması nedeniyle bütün sınıfı Java'ya doğrudan taşımak yerine kuralları ayrı modele
uyarlamak daha uygun bir tasarım seçeneğidir.

## Kaynaklar

- [CTB storage](https://github.com/giuspen/cherrytree/blob/c6e626b1f4011056f21369d3343e2bae2723e34d/src/ct/ct_storage_sqlite.cc)
- [XML helper / reader / serializer](https://github.com/giuspen/cherrytree/blob/c6e626b1f4011056f21369d3343e2bae2723e34d/src/ct/ct_storage_xml.cc)
- [Text tag ve ölçek işlemleri](https://github.com/giuspen/cherrytree/blob/c6e626b1f4011056f21369d3343e2bae2723e34d/src/ct/ct_main_win_buffer.cc)
- [Attribute listesi](https://github.com/giuspen/cherrytree/blob/c6e626b1f4011056f21369d3343e2bae2723e34d/src/ct/ct_const.h)
- [Metin aralıklarını dolaşma](https://github.com/giuspen/cherrytree/blob/c6e626b1f4011056f21369d3343e2bae2723e34d/src/ct/ct_misc_utils.cc)
- [Child anchor ekleme](https://github.com/giuspen/cherrytree/blob/c6e626b1f4011056f21369d3343e2bae2723e34d/src/ct/ct_widgets.cc)
- [Tablo yazımı](https://github.com/giuspen/cherrytree/blob/c6e626b1f4011056f21369d3343e2bae2723e34d/src/ct/ct_table.cc)
- [HTML export](https://github.com/giuspen/cherrytree/blob/c6e626b1f4011056f21369d3343e2bae2723e34d/src/ct/ct_export2html.cc)
- [Okuma/yazma testleri](https://github.com/giuspen/cherrytree/blob/c6e626b1f4011056f21369d3343e2bae2723e34d/tests/tests_read_write.cpp)
- [Bağımlılıklar](https://github.com/giuspen/cherrytree/blob/c6e626b1f4011056f21369d3343e2bae2723e34d/CMakeLists.txt) ve [proje tanıtımı](https://github.com/giuspen/cherrytree/blob/c6e626b1f4011056f21369d3343e2bae2723e34d/README.md)
- [GTK karakter offset'i](https://docs.gtk.org/gtk3/method.TextIter.get_offset.html)
- [GTK get_text ve gömülü nesneler](https://docs.gtk.org/gtk3/method.TextIter.get_text.html)
- [Pango markup](https://docs.gtk.org/Pango/pango_markup.html)

## Kaynak kullanımı

Bu belge upstream tasarımını açıklayıcı biçimde özetler; upstream uygulama kodu kopyalanmamıştır.
CherryTree kaynak bildirimi GPL-3.0-or-later, SweetCherry kaynak başlıkları da GPL v3 veya sonrası belirtir.
Kod aktarımı yapılacaksa ilgili dosyanın telif/lisans bildirimi ve kaynak bağlantısı korunmalı;
Java'ya uyarlanan upstream algoritma ile bağımsız geliştirilen kodun kökeni belgelenmelidir.
Bu not bir lisans uygunluk incelemesi yerine geçmez.
