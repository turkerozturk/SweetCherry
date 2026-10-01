# Rich text uyumluluk değerlendirmesi ve geliştirme planı

İnceleme tarihi: 2 Ekim 2026.
SweetCherry: `dd911ed28962a95ada82d03745a0704d1fe6f7d3`. CherryTree referansı: `c6e626b1f4011056f21369d3343e2bae2723e34d`.

Bu çalışma [SweetCherry okuma zinciri](rich-text-sweetcherry.md) ile
[CherryTree storage modeli](rich-text-cherrytree.md) karşılaştırmasına dayanır.
Mevcut parser değiştirilmemiştir. GUI ile yeni CTB örnekleri üretme, bütün regresyon kümesini çalıştırma
ve karşılıklı açma/kaydetme deneyleri bu incelemede yapılmamıştır.

## Genel değerlendirme

SweetCherry'nin örnek CTB'leri gözlemleyerek geliştirilmiş renderer'ı, metin biçimlerini ve gömülü
nesneleri web ortamına taşıyan yararlı bir uygulamadır. Çalışma gereksiz değildir: genel bir XML parser'ı
CherryTree nesnelerini ve yerleşim kurallarını kendi başına HTML'e dönüştürmez.
Upstream kodun incelenmesi, bu uygulamanın kapsamını ölçülebilir biçimde geliştirmek için ek bir referans sağlar.

Kullanıcı denemelerinde çoğu içeriğin doğru görüntülenmesi gözlenmiştir. Bu belge bunu yüzdeyle ifade etmez;
biçim, nesne ve Unicode varyasyonları içeren bir test kümesi kurulmadan sayısal kapsam ölçülemez.

## Bulgular

“Kaynakta doğrulandı” yürütme/GUI testi yapılmış demek değildir. Aşağıdaki kayıtlar kod davranışını
ve ondan çıkarılan uyumluluk sonuçlarını ayırır. Dosya bağlantıları diğer iki belgenin kaynak listelerindedir.

| Konu | SweetCherry durumu | Değerlendirme |
| --- | --- | --- |
| CTB rich text türü | `syntax=custom-colors`; entity'de bit 0 da okunur | Upstream tür kimliğiyle uyumlu; çelişkili dosyada uygulanacak politika ayrıca tanımlanmalı. |
| Kalın/italik/metin rengi | CSS dönüşümleri var | Temel destek mevcut; birleşik stiller ve renk varyasyonları fixture ile test edilmeli. |
| Altı/üstü çizili | CSS string'i `textContent-decoration` üretiyor | Kaynakta doğrulanan yanlış CSS adı; `text-decoration` olmalı. |
| Anchor | Haritaya ekleniyor, aktif HTML üretimi yorum/TODO halinde | Hedef `id` oluşmadığı durumlarda düğüm+anchor bağlantısı hedefe ulaşmayabilir. |
| `indent`, `invisible` | Upstream 12 attribute listesinde var; etkin CSS dönüşümünde yok | Görsel/semantik eksik; invisible içeriğin webde gösterilme politikası ayrıca belirlenmeli. |
| Link ve scale birlikteliği | `link` ve `scale` için `if/else if` kullanılıyor | Link varken scale element dönüşümü atlanabilir; birleşik attribute testi gerekli. |
| Offset birimi | `String.length()` ve boş run için +1 | GTK karakter/child-anchor modelinden farklı; özellikle Unicode ve widget karışımı incelenmeli. |
| Aynı offset | `Map.put()` ile tek değer saklanıyor | İkinci nesne/run ilkini ezer; deterministik sıralı olay modeli gerekli. |
| Metnin ortasındaki widget | Bütün run tek seferde HTML'e ekleniyor | Run gerektiğinde widget sınırında bölünmüyor; nesne sırası hatası oluşabilecek yapı. |
| Tablo başlığı | Son satır başa taşınıyor | Upstream okuma/yazma kuralıyla doğrulandı. |
| Tablo ayrıntıları | Hücre metni gösteriliyor; boyut/kolon ayrıntılarının tamamı eşlenmiyor | İçerik desteği var; bütün masaüstü yerleşimine eşitlik iddiası yok. |
| Codebox | Metin ve syntax highlighter desteği var | Boyut, satır numarası ve diğer widget özelliklerinin tamamı aktarılmıyor. |
| Resim | Binary endpoint veya base64 | Temel görüntü var; image link/justification bütünlüğü ayrıca ele alınmalı. Base64 dalında sabit `image/jpeg` kullanımı PNG verisiyle tutarsız olabilir. |
| LaTeX | ParserType seçeneği var | Etkin parse dispatch'inde ayrı bir LaTeX render dalı görülmedi; attachment olarak görünen veri tam render desteği sayılmaz. |
| Paylaşılan çalışma durumu | Renderer DOM'u ve seçenekleri `static`; servis embedding seçeneği değişebilir | Eşzamanlı çağrılar için izolasyon gerekli; karışma ihtimali kaynak yapısından çıkarımdır, yük testiyle doğrulanmadı. |
| XML yapılandırması | İlgili DOM factory çağrılarında açık DTD/external entity kısıtları görülmedi | Güvenilmeyen CTB'ler için güvenli parse ayarları ve testleri ayrıca tamamlanmalı; burada saldırı denemesi yapılmadı. |

Boş run ve offset konusundaki eski notlar pratik gözlemleri kaydeder; upstream serializer'ın bütün
olası çıktıları için şartname sayılmaz. Upstream boş run okurken karakter eklemez, widget'ları ayrı
konumlara yerleştirir. Yeni model bu iki kaynağı bir arada ele almalıdır.

### Upstream fixture üzerinde ek kontrol

CherryTree referans commit'indeki `tests/data_данные/test_документ.ctb` SQLite read-only modunda
incelendi. Üç `custom-colors` düğümde yedi boş `rich_text` parçası bulundu. SweetCherry'nin
`String.length()` / boş run +1 hesabı Python'da UTF-16 uzunluğu ile modellenerek nesne offset'leriyle
karşılaştırıldı: yedi widget kaydı boş run başlangıcına denk geldi; dolu run başlangıcına çakışma,
hesaplanan run'ın içindeki widget veya UTF-16/code point farkı bu küçük örnekte bulunmadı.

Bu kontrol Java renderer'ın çalıştırılması veya görsel eşitlik testi değildir. Boş run +1 yaklaşımının
bu fixture'da neden işe yarayabileceğini açıklar; bütün serializer çıktılarında aynı yapının zorunlu
olduğunu kanıtlamaz. Map çakışmasının burada boş parçayı değiştirmesi, tek başına metin kaybı demek değildir.

Kaynak: [upstream CTB fixture](https://github.com/giuspen/cherrytree/blob/c6e626b1f4011056f21369d3343e2bae2723e34d/tests/data_данные/test_документ.ctb).
Bu fixture dokümantasyon yamasına kopyalanmamıştır.

### Açıklayıcı örnekler

Bu örnekler sentetiktir; GUI'de üretilip doğrulanmış CTB fixture'ları olarak sunulmaz.

- Tek text run `ABC` ve final buffer offset'i 1 olan widget için hedef sıra `A`, widget, `BC` olur.
  Bütün `ABC` run'ını offset 0'da, widget'ı offset 1'de sıralamak `ABC`, widget üretebilir.
- Text run ve widget aynı map anahtarına konursa Java `Map.put()` önceki değeri değiştirir.
  Çözüm, tesadüfen farklı anahtar üretmek yerine run aralıklarını ve widget olaylarını ayrı saklamaktır.
- `A😀B` metninde Java uzunluğu 4, code point sayısı 3'tür. Widget konumu UTF-16 indeksine
  doğrudan çevrilirse sonraki parçaların konumu kayabilir. Birleşik emoji/grapheme örnekleri ayrıca test edilmelidir.

## Önerilen model

HTML'i geri parse ederek CTB'ye kaydetmek yerine kaynak veriyi kayıpsız taşıyan bir ara model önerilir:

- `RichTextDocument`: sıralı metin run'ları, attribute'lar, widget'lar ve bilinmeyen alanlar.
- `TextRun`: metin, biçim attribute'ları ve Unicode karakter aralığı.
- `Widget`: tür, final buffer offset'i, özgün CTB alanları ve binary içerik referansı.
- `CherryTreeRichTextCodec`: XML/CTB → model ve model → XML/CTB dönüşümü.
- `RichTextHtmlRenderer`: model → güvenli HTML; codec'ten ayrı tutulur.
- Editör adaptörü: desteklenen web editör modelini ara modele çevirir.

Bu isimler öneridir; henüz eklenmiş sınıflar değildir.
DOM, StAX veya başka XML araçlarının seçimi ikinci plandadır; nesne konumları ve anlamın korunması
ilk tasarım konusudur. Bilinmeyen attribute/widget, renderer desteklemese bile modelde korunmalı;
bunları güvenle geri yazamayan editör ilgili içeriği düzenlemeye açmamalıdır.

## Aşamalı plan

1. **Fixture ve karakterizasyon:** CherryTree'de oluşturulan küçük CTB'ler ile mevcut SweetCherry
   çıktısını kaydet. Düzeltmelerin eski çalışan içeriği bozmaması için karşılaştırılabilir beklenen sonuç üret.
2. **Reader düzeltmeleri:** Önce bağımsız CSS hatası ve anchor hedefleri; sonra Unicode, boş run,
   widget konumu ve map çakışması. Yeni modeller çağrıya özel olmalı; static DOM state kaldırılmalı.
3. **Kayıpsız codec:** Okunan model tekrar serialize edilince text, attribute ve widget anlamını korumalı.
   XML byte sırasının aynı olması şart değildir; semantik eşitlik ve ayrı tablo bütünlüğü gereklidir.
4. **Yeni basit rich text node:** İlk yazma kapsamı, widget içermeyen yeni düğümlerde düz metin,
   satır sonu, kalın/italik/altı çizili gibi açıkça desteklenen biçimlerle sınırlandırılabilir.
   XML standart serializer ile escape edilmeli, `syntax=custom-colors`, rich bit ve zaman alanları
   uygun kaydedilmeli. Bu aşama yeni veri oluşturur; eski karmaşık notlar yeniden yazılmaz.
5. **Mevcut basit notları düzenleme:** Destek dışı attribute veya widget yoksa editöre aç; varsa
   desteklenmediğini belirt ve mevcut read-only görüntülemeyi koru.
6. **Widget'lı notlar:** Resim, tablo, codebox, attachment, anchor ve LaTeX için ayrı round-trip
   testleri geçtikten sonra kapsamı genişlet. Text düzenlemesi bütün widget offset'lerini yeniden hesaplamalı.

Her yazma aşamasında mevcut admin/writable/readonly/tenant/CSRF kontrolleri kullanılmalı.
Text ve widget tablo değişimleri tek işlem bütünlüğünde yapılmalı; `has_*` ve `ts_lastsave` güncellenmeli.
Başlık renginin/kalınlığının bitleri içerik editörü tarafından silinmemeli.
Shared node düzenleme politikası mevcut korumalarla tutarlı olmalı.
CherryTree ile aynı CTB'ye eşzamanlı yazma mevcut kullanım önerisine uygun şekilde önlenmeli.

## Editör seçimi

Java karşılığı aranacak şey Markdown parser'ı değildir. WYSIWYG editör kullanıcı etkileşimini çözer;
CherryTree formatını kendiliğinden üretmez. Editörün HTML veya JSON çıktısı için explicit bir adaptör gerekir.
Bu yüzden editör kütüphanesini, codec'in ilk desteklenen biçim listesi belirlendikten sonra seçmek uygundur.
Mevcut HTML'i düzenletip doğrudan `node.txt` alanına yazmak `custom-colors` uyumluluğunu bozabilir.

`{sweetcherry}` işaretli plain text içinde HTML saklama fikri ayrı bir uygulama formatı seçeneğidir.
CherryTree'nin kendi rich text'iyle uyumluluk hedefini karşılamaz ve bu çalışmanın yerine geçirilmemiştir.

## Kabul testleri

| Grup | Asgari örnekler |
| --- | --- |
| Metin | Boş içerik, boş run, CR/LF, tab, ardışık boşluk, XML özel karakterleri |
| Unicode | Türkçe, Kiril, CJK, BMP dışı emoji, birleştirici işaret, birleşik emoji |
| Biçim | 12 upstream attribute; link+scale, foreground+background, underline+strikethrough |
| Nesne konumu | Başta/ortada/sonda widget, art arda widget, aynı sınırda run ve widget |
| Nesne türü | PNG, linkli resim, attachment, anchor, codebox, tablo ve LaTeX |
| CTB davranışı | Alias kimliği, readonly, writable kapalı, ts_lastsave, bütün has_* işaretleri |
| Round-trip | SweetCherry yazımı → CherryTree açma/kaydetme → SweetCherry tekrar okuma |
| İzolasyon | Aynı anda iki CTB ve web/PDF render çağrıları |

Upstream `tests/tests_read_write.cpp` okuma/kaydetme/tekrar açma ve buffer attribute kontrollerine
örnek sağlar. Mevcut upstream testlerinin varlığı SweetCherry uyumluluğunun otomatik doğrulandığı
anlamına gelmez; her uygulamanın kendi adaptasyon testleri gerekir.

## Sonraki adım

İlk uygulama yaması için sınırlı bir hedef önerilir: underline/strikethrough CSS hatasını örneklerle
sabitlemek ve reader için küçük bir fixture kümesi kurmak. Offset/model değişikliği bundan sonra,
eski başarılı örnekler de regresyon kümesine alınarak ilerlemelidir. Rich text yazma henüz açılmamalıdır.

## İlk uygulama adımı

Ayrı, henüz etkin olmayan okuyucu/model/HTML önizlemesi ve proje içi XML fixture'ı
hazırlandı. Uygulama kapsamı ve sınırları: [Deneysel rich text okuyucu](rich-text-experimental.md).
