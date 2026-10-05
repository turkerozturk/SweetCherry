# CherryTree: gerçek ve paylaşımlı düğüm kontrol listesi

Bu belge yalnızca **CherryTree'nin davranışlarını** incelemek içindir. Başka bir uygulamanın davranışını tarif etmez. Amaç, ağaçtaki bir düğümün kimliği ile ortak içeriğin kimliğini ayırarak, sürüme bağlı davranışları tekrar edilebilir biçimde kaydetmektir.

Kaynak proje: https://github.com/giuspen/cherrytree

İncelenen kaynak anlık görüntüsü: `c6e626b1f4011056f21369d3343e2bae2723e34d` (commit tarihi: 2026-09-19). Bu kodun sizin kurulu sürümünüzle aynı olduğunu varsaymayın. Kaynakta görülen işlem akışı, GUI testi ve kaydedilip yeniden açılan CTB sonucu ayrı kanıtlardır. Bir fark bulunması tek başına yazılım hatası anlamına gelmez.

## Test kaydı ve hazırlık

- CherryTree sürümü: …
- İşletim sistemi: …
- Dosya türü: CTB; dosya adı: …
- Otomatik kaydetme ayarı: …
- Test tarihi: …

**Silme ve taşıma deneylerini yalnızca ayrı bir test CTB'sinde yapın.** Her bağımsız deneyden önce başlangıç dosyasının kapalı durumdaki kopyasına dönün. Bir işlemin kaydedildiğini doğrulayın; ardından dosyayı kapatıp yeniden açın. GUI'de doğru görünmesi, diskte doğru saklandığını tek başına göstermez.

Başlangıç ağacını şu şekilde hazırlayın:

| İşaret | Hazırlık |
|---|---|
| M | Gerçek rich-text düğüm. Ayırt edilebilir metin, biçim, resim, ekli dosya, tablo, codebox ve çapa ekleyin. |
| M1, M2 | M'nin gerçek alt düğümleri; M1'in de bir alt düğümü olsun. |
| P | Ayrı bir gerçek kök düğüm; en az iki gerçek alt düğümü olsun. |
| S1 | M'nin paylaşımlı düğümü; P'nin altında dursun. |
| S2 | M'nin başka paylaşımlı düğümü; kök seviyesinde dursun. |
| S3 | S1 üzerinden paylaşımlı düğüm oluşturun; hangi asıl kimliğe bağlandığını kaydedin. |
| R | Aynı paylaşım grubuna bağlı olmayan bağımsız gerçek düğüm. |

Düğüm özellikleri penceresinden her birinin gerçek sayısal kimliğini kaydedin. Adlar paylaşılabildiği için **adı kimlik yerine kullanmayın**. M, S1 ve S2'yi ayrı ayrı yer işaretlerine ekleyin. R içine M'ye, S1'e ve M içindeki bir çapaya giden bağlantılar hazırlayın.

Her satırın sonucunu `✓ / farklı / uygulanamadı` olarak, gözlenen kimlikler ve yeniden açma sonucuyla birlikte kaydedin. “Manuel” etiketli sorularda beklenen sonuç henüz belirlenmemiştir; gözlemi bir varsayıma uydurmayın.

## Kaynak kodunda görülen davranışlar

| Kod | Bulgular | Kaynakta ilgili bölüm |
|---|---|---|
| K1 | Paylaşımlı düğümün kendi ağaç kimliği vardır. İçerik ve özellikler alınırken asıl düğümün verisine başvurulur; sıra ve ağaç kimliği ayrı tutulur. | `CtTreeStore::get_node_data`, `CtTreeIter::get_node_id_data_holder` [B] |
| K2 | Normal tek-düğüm çoğaltma, kaynak paylaşımlı olsa bile yeni düğümün paylaşım ilişkisini sıfırlar ve bağımsız içerik kopyası oluşturur. Paylaşımlı düğüm oluşturma ise kaynak zaten paylaşımlıysa aynı asıl kimliği kullanır. | `CtActions::_node_add` [A] |
| K3 | Asıl düğüm silinirken grubun silinmeyen bir üyesi varsa veri ona aktarılır; diğer kalan üyelerin asıl kimliği güncellenir. Bütün paylaşım grubunun otomatik olarak silineceği varsayılmamalıdır. | `CtActions::node_delete` [A] |
| K4 | Silme sonrasında seçim sırası: önce önceki sibling, yoksa sonraki sibling, yoksa parent, yoksa boş seçim. Yer işaretlerinden silinenler, kaldırılan ağaç kimlikleridir. | `CtActions::node_delete` [A] |
| K5 | Düğüm özelliklerini değiştirme ve read-only değiştirme işlemlerinde paylaşım grubunun diğer üyelerine de güncelleme uygulanır. | `CtActions::node_edit`, `node_toggle_read_only` [A] |
| K6 | SQLite kaydında paylaşımın asıl olmayan üyesi için hiyerarşi kaydı yazılır; o üye adına ayrıca `node` satırı yazılmaz. Tek düğüm olarak CTB dışa aktarılırken paylaşım bağlantısı kaldırılarak içerik bağımsızlaştırılır. | `CtStorageSqlite::_write_node_to_db` [C] |
| K7 | İncelenen sağa taşıma ve parent değiştirme yollarında hedefi sırf paylaşımlı olduğu için reddeden açık bir koşul görülmedi. Parent seçme ve sürükleme yollarında kendine/alt soyuna taşıma kontrolleri var. Bu, tüm GUI yollarının her durumu kabul ettiğine dair garanti değildir. | `node_right`, `node_change_father`, `node_move` [A]; manuel doğrulama gerekli |

Kaynak bağlantıları sabit commit'e aittir:

[A] https://github.com/giuspen/cherrytree/blob/c6e626b1f4011056f21369d3343e2bae2723e34d/src/ct/ct_actions_tree.cc

[B] https://github.com/giuspen/cherrytree/blob/c6e626b1f4011056f21369d3343e2bae2723e34d/src/ct/ct_treestore.cc

[C] https://github.com/giuspen/cherrytree/blob/c6e626b1f4011056f21369d3343e2bae2723e34d/src/ct/ct_storage_sqlite.cc

Aşağıdaki sorular bu bulguların kapsamını genişletmek içindir. K2'deki tek düğüm çoğaltma bulgusunu alt ağaç çoğaltmaya; K6'daki CTB bulgusunu PDF/HTML dışa aktarmaya kendiliğinden genellemeyin.

## 1. Oluşturma, kimlik ve içerik paylaşımı

| No | İşlem ve kontrol | Dayanak | Sonuç |
|---|---|---|---|
| 01 | M üzerinden S1 ve S2 oluşturun. Kimlikleri farklı mı; asıl kimlikleri M mi? Yeni paylaşımlı düğüm seçilinin hemen sonrasına mı ekleniyor? | K1, K2; yerleşimi doğrulayın | ☐ |
| 02 | S1 üzerinden S3 oluşturun. S3, S1'e zincirleme bağlanmak yerine M'ye mi bağlı? Kaydedip yeniden açın. | K2 | ☐ |
| 03 | M'de metin ve biçim değiştirin; S1/S2/S3'te kontrol edin. Sonra S1'de değiştirip M'ye dönün. | K1 | ☐ |
| 04 | Resim, ekli dosya, tablo ve codebox'ı bir paylaşım üyesinden değiştirin veya silin. Diğerlerinde içerik ve indirme/açma sonucu aynı mı? | Manuel; nesne verilerini ayrıca kontrol edin | ☐ |
| 05 | M ile plain-text ve syntax-highlighted düğümlerdeki paylaşımı ayrı ayrı deneyin. Tür değiştirmede grubun sonucu nedir? | Manuel | ☐ |

## 2. Paylaşımlı düğümün altına düğüm koyma — öncelikli

**İçerik paylaşımı ile alt ağaç paylaşımını ayrı inceleyin.** M'nin alt düğümlerinin S1 altında görünmesi, S1'in kendisine ait alt düğümlerinin olmasıyla aynı şey değildir.

| No | İşlem ve kontrol | Dayanak | Sonuç |
|---|---|---|---|
| 06 | S1 seçiliyken “alt düğüm ekle” ile gerçek bir düğüm oluşturun. Menü etkin mi? Yeni düğümün parent kimliği S1 mi? M ve S2 altında da görünüyor mu? | Manuel | ☐ |
| 07 | Gerçek R'yi S1'in hemen sonraki sibling'i yapın; sağa taşıma kısayoluyla S1 altına alın. Yerleşimi ve yeniden açmayı kontrol edin. | K7 | ☐ |
| 08 | Aynı işlemi parent seçme diyaloğu ve sürükle-bırak ile ayrı ayrı yapın. Üç yöntemde kabul/ret ve sonuç aynı mı? | K7; GUI yolları ayrı test edilir | ☐ |
| 09 | Başka bir grubun paylaşımlı düğümünü S1 altına taşıyın. Sonra aynı grubun S2 üyesini S1 altına taşıyın. Gerçek hiyerarşi nasıl saklanıyor? | Manuel | ☐ |
| 10 | M'yi S1 altına taşımayı deneyin; ayrıca S1'i M altına taşımayı deneyin. Ağaç kimlikleri açısından döngü ile paylaşım ilişkisi açısından döngü farklı olabilir. Kabul/ret ve yeniden açma sonucunu kaydedin. | Manuel; test dosyasında | ☐ |
| 11 | S1'e eklenen gerçek çocuğu çoğaltın, dışarı taşıyın, sola çıkarın ve silin. M'nin kendi alt ağacı etkileniyor mu? | Manuel | ☐ |

## 3. Sıra, parent, fold ve read-only

| No | İşlem ve kontrol | Dayanak | Sonuç |
|---|---|---|---|
| 12 | M ve S1'i ayrı ayrı yukarı/aşağı taşıyın. Kimlikler ve ortak içerik korunurken sadece seçilenin sırası değişiyor mu? | K1; hiyerarşi kontrolü | ☐ |
| 13 | S1'i başka parent altına taşıyın. M ve S2'nin yerleri, S1'in varsa kendi alt ağacı değişiyor mu? | Manuel | ☐ |
| 14 | İlk sibling'de yukarı/sağa; son sibling'de aşağı; kökte sola taşıma deneyin. Ret veya etkisiz işlem veri kaybına yol açıyor mu? | Manuel | ☐ |
| 15 | Gerçek düğümü kendisinin veya gerçek alt soyunun altına parent diyaloğu/sürüklemeyle taşımayı deneyin. | K7; reddedilmesi beklenir | ☐ |
| 16 | M/S1 üzerinde ayrı fold/unfold ve seçim yapın. Durumları ve imleç/scroll konumları ortak mı, ayrı mı? Yeniden açınca ne korunuyor? | Manuel | ☐ |
| 17 | Ad, tags, ikon, ad rengi ve kalınlık değişikliklerini M ve S1'den ayrı ayrı deneyin. Hangi özellikler bütün gruba yansıyor? | K5 | ☐ |
| 18 | S1'i read-only yapın; M ve S2'de metin düzenleme ve kilit simgesini kontrol edin. Tek üyeden kaldırınca grubun durumu nedir? | K5 | ☐ |
| 19 | Read-only üye üzerinde taşıma, çoğaltma, paylaşım oluşturma, alt düğüm ekleme ve silmeyi ayrı ayrı deneyin. İçerik kilidini hiyerarşi kilidi saymayın; her sonucu kaydedin. | Manuel | ☐ |
| 20 | Read-only parent altında yazılabilir çocuğa aynı işlemleri uygulayın. Parent kilidi alt ağaca otomatik uygulanıyor mu? | Manuel | ☐ |

## 4. Silme, yeni asıl düğüm ve seçim — öncelikli

| No | İşlem ve kontrol | Dayanak | Sonuç |
|---|---|---|---|
| 21 | Çocuksuz S1'i silin. M/S2/S3 kalıyor mu; yalnız S1'in yer işareti kaldırılıyor mu? | K4; ortak içerik korunmasını kontrol edin | ☐ |
| 22 | Gerçek çocukları olan S1'i silin. Silme onayı hangi alt düğümleri kapsıyor? S1'in çocukları ve M'nin kendi çocukları ayrı nasıl etkileniyor? | Manuel | ☐ |
| 23 | M'yi silin; S1 ve S2 başka dallarda kalsın. Hangi üye yeni asıl oluyor? Kalanların kimlikleri, yerleri ve paylaşım ilişkileri korunuyor mu? | **K3** | ☐ |
| 24 | 23 sonrası metin, biçim, resim, dosya indirme, tablo, codebox ve çapa çalışıyor mu? Kaydedip kapatın; yeniden açıp yine düzenleyin. | K3; veri aktarımının bütünlüğü | ☐ |
| 25 | M'nin alt ağacında bir paylaşımlı üye, başka dalda bir üye olsun. M'yi silin. Yeni asıl silinen alt ağaçtan değil, hayatta kalan üyeden mi seçiliyor? | K3 | ☐ |
| 26 | Bir parent silinerek M ve bazı üyeler birlikte kaldırılsın; en az bir üye dışarıda kalsın. Sonra tüm grubun aynı silinen alt ağaçta olduğu ayrı senaryoyu deneyin. | K3; iki bağımsız deney | ☐ |
| 27 | Yeni asılı tekrar silin, sonra grubun son üyesini silin. Her aşamada içerik ve yer işaretleri nasıl değişiyor? | K3, K4 | ☐ |
| 28 | Önceki sibling olan/olmayan, yalnız sonraki sibling olan, yalnız parent olan ve son kök düğüm durumlarında silin. Son seçimi kaydedin. | K4 | ☐ |
| 29 | Silmeyi onaylamayın. Sonra destekleniyorsa silme sonrasında undo/redo deneyin. İçerik undo'suyla ağaç işlemlerinin undo'sunu karıştırmayın. | Manuel | ☐ |

## 5. Çoğaltma ve alt ağaç kopyalama

| No | İşlem ve kontrol | Dayanak | Sonuç |
|---|---|---|---|
| 30 | M'yi normal tek düğüm olarak çoğaltın. Yeni kopyayı düzenleyin; M etkilenmemeli. Nesnelerin içeriğini kontrol edin. | K2 | ☐ |
| 31 | S1'i normal tek düğüm olarak çoğaltın. Kopya **bağımsız gerçek düğüm** mü? Kopyayı düzenleyince M/S1 değişiyor mu? | **K2** | ☐ |
| 32 | M ve paylaşımlı üyelerinin aynı alt ağaçta olduğu dalı, alt düğümleriyle çoğaltın. Yeni grup kendi içinde mi paylaşım yapıyor, eski gruba mı bağlı? | Manuel; tek düğüm çoğaltmadan farklı yol | ☐ |
| 33 | Yalnız paylaşımlı üyeyi içeren, asılı dışarıda olan dalı alt düğümleriyle çoğaltın. Yeni kopyanın asıl kimliği ve bağımsızlık durumu nedir? | Manuel | ☐ |
| 34 | Aynı alt ağaç içinde iki farklı paylaşım grubu, gerçek çocuklar ve farklı derinliklerde üyeler olsun. Çoğaltma sonrası tüm parent ve asıl kimliklerini karşılaştırın. | Manuel | ☐ |
| 35 | Düğüm/alt ağaç kopyala-yapıştır seçeneklerini aynı belge içinde ve başka test CTB'sine uygulayın. Çoğaltma ile aynı davranışı göstermek zorunda olduğunu varsaymayın. | Manuel | ☐ |

## 6. Yer işaretleri, bağlantılar ve arama

| No | İşlem ve kontrol | Dayanak | Sonuç |
|---|---|---|---|
| 36 | M, S1, S2'ye ayrı yer işareti ekleyin; her birine gidin. Ortak içeriğin yanında doğru ağaç konumu seçiliyor mu? Birini kaldırınca diğerleri kalıyor mu? | K1, K4; kimliğe göre kontrol | ☐ |
| 37 | M silinip yeni asıl seçildiğinde kalan üyelerin yer işaretlerini kontrol edin. Silinen M'nin yer işaretine ne oluyor? | K3, K4 | ☐ |
| 38 | R'deki M ve S1 bağlantılarına tıklayın. Hangi ağaç kimliği seçiliyor? S1 taşındıktan sonra tekrar deneyin. | Manuel | ☐ |
| 39 | Bir üyenin içindeki çapaya bağlantı hazırlayın. Başka üyeden tıklayın; seçilen kimliği ve scroll hedefini kaydedin. M silinip yeni asıl seçilince tekrar deneyin. | Manuel | ☐ |
| 40 | Silinen üyeye giden eski bağlantıya tıklayın. Uyarı, yönlendirme veya etkisiz işlem nedir? Yanlış kimliğe gitmediğini kontrol edin. | Manuel | ☐ |
| 41 | Ortak içerikteki tek ayırt edici kelimeyi tüm ağaçta arayın. Sonuç sayısı paylaşım üyesi sayısı kadar mı, bir mi? Sonuçlara tıklayınca hangi konumlar seçiliyor? | Manuel | ☐ |
| 42 | “Bu düğümü arama dışında tut” ve “alt düğümleri arama dışında tut” ayarlarını S1'den değiştirin. Özellik paylaşımı ve gerçek alt ağaç kapsamını ayrı gözleyin. | K5; arama etkisi manuel | ☐ |

## 7. Dışa aktarma, içe aktarma ve kalıcılık

| No | İşlem ve kontrol | Dayanak | Sonuç |
|---|---|---|---|
| 43 | Yalnız S1'i yeni CTB olarak dışa aktarın. Yeni dosyada bağımsız gerçek içerik, nesneler ve çalışır indirmeler var mı? | K6 | ☐ |
| 44 | Asılı dışarıda kalan bir veya birkaç paylaşım üyesini içeren dalı CTB olarak dışa aktarın. Yeni dosyada asıl kimlikleri geçerli mi? | Manuel; dışa aktarımda yeniden eşleme kontrolü | ☐ |
| 45 | Asıl ve üyelerin birlikte bulunduğu dalı CTB olarak dışa aktarın. Paylaşım grubu ve her üyenin kendi gerçek çocukları nasıl korunuyor? | Manuel | ☐ |
| 46 | 44/45 dosyalarını dolu başka test CTB'sine içe aktarın. Kimlikler yeniden verilince paylaşım ve iç bağlantılar doğru mu? | Manuel | ☐ |
| 47 | PDF/HTML dışa aktarımında ortak içerik, üyelerin alt ağaçları ve bağlantılar nasıl gösteriliyor? CTB dışa aktarma kuralını bu biçimlere genellemeyin. | Manuel | ☐ |
| 48 | Metin değiştirme, taşıma, çoğaltma, yeni asıl seçimi sonrası her seferinde kaydedip yeniden açın. Özellikler, tarihler, nesneler ve parent/sıra kaydı korunuyor mu? | Kalıcılık kontrolü | ☐ |

## İsteğe bağlı: kapalı test dosyasının SQLite kaydını inceleme

CherryTree'de kaydedip kapattığınız test dosyasının **kopyasını** DB Browser for SQLite ile açın. Yalnızca okuma sorguları kullanın. Önce şemayı kontrol edin; farklı sürümlerde sütun adları değişebilir.

```sql
PRAGMA table_info(children);
PRAGMA table_info(node);
SELECT * FROM children ORDER BY father_id, sequence, node_id;
SELECT node_id, name, syntax FROM node ORDER BY node_id;
```

Ağaçtaki her görünümün `children.node_id` kaydı ile içerik sahibi `node.node_id` kaydını karşılaştırın. `master_id` sütunu varsa paylaşımlı üyelerin hangi içerik sahibine işaret ettiğini kaydedin. Özellikle 23, 31, 32 ve 44 numaralı deneylerde önce/sonra kayıtlarını karşılaştırın. Asıl düğümün çocuklarıyla paylaşımlı üyenin çocuklarını `father_id` üzerinden ayırın.

## Bulguyu kaydetme şablonu

- Test numarası ve sürüm:
- Başlangıç kimlikleri / parent / asıl kimlikleri:
- Kullanılan işlem yolu (kısayol, menü, sürükleme):
- Onay veya hata mesajı:
- GUI'de gözlenen sonuç:
- Kaydedip yeniden açtıktan sonraki sonuç:
- Varsa SQLite kayıtlarının sonucu:
- Kaynak beklentisiyle farklılık ve ekran görüntüsü:

İlk tur için 06–10, 23–26, 31–34 ve 36–39 önceliklidir. Bunlar içerik paylaşımı, bağımsız hiyerarşi, silme ve kimlik davranışlarını belirler. Diğer kontroller ikinci turda tamamlanabilir.
