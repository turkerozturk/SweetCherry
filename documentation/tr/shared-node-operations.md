# Paylaşımlı alt ağaç: çoğaltma ve silme

## Çoğaltma

Gerçek ve paylaşımlı konumlarda **Alt düğümlerle çoğalt** kullanılabilir. `father_id` ile bağlı kendi alt ağaç kopyalanır; master’ın başka yerdeki çocukları ödünç alınmaz. Her konum yeni bir `children.node_id` alır. Gerçek düğümler `node`, `image`, `grid`, `codebox` içerikleriyle bağımsız kopyalanır. Paylaşımlı konumlar yalnız yeni referans satırı alır, özgün master’a bağlı kalır. Master da bu işlemde kopyalanmış olsa bile referans ona yönlendirilmez.

Kök olarak seçilmiş paylaşımlı düğümde SweetCherry’nin mevcut tek-kopya sözleşmesi korunur: yeni kök yine aynı master’a referanstır. Alt ağaç seçeneği onun kendi çocuklarını da çoğaltır. Bookmark’lar ve metin içindeki internal link kimlikleri kopyaya uyarlanmaz.

## Silme ve yeni master

Silme işleminin kapsamı seçilen konum ve `father_id` ile altında bulunan tüm konumlardır. Paylaşımlı düğümün çocukları da bu kapsama dahildir. Dışarıdaki paylaşımlı başvurular otomatik olarak silinmez.

Silinen gerçek düğümün dışarıda referansı kalıyorsa, en küçük kimlikli kalan referans yeni master seçilir:

1. `node`, `image`, `grid`, `codebox` satırlarının `node_id` alanı yeni master kimliğine taşınır. Diğer sütunlar, SQL NULL, binary veriler ve zaman damgaları aynen kalır.
2. Yeni master’ın `master_id` değeri `0` olur. Diğer referanslar ona bağlanır.
3. Yeni master’ın ebeveyni, kendi çocukları ve bookmark’ı korunur.
4. Silinen alt ağacın konumları ve bookmark’ları kaldırılır. Hiçbir referansı kalmayan içerik grubu tamamen silinir.

Hiyerarşi ve içerik referansları önce doğrulanır. Bütün yazmalar aynı tenant transaction’ında yapılır; yazma hatasında geri alınır. İçerik read-only bayrağıyla tenant yazma yetkisi ayrı konulardır; mevcut admin, writable tenant, CSRF ve tenant token denetimleri korunmuştur. Silme confirmation sayfası bu davranışı açıklamaktadır.

## Kaynak ve doğrulama sınırı

CherryTree kaynağı: https://github.com/giuspen/cherrytree/blob/master/src/ct/ct_actions_tree.cc

2026-10-08 incelemesinde `node_subnodes_paste2` alt düğüm verilerini yeni kimliklerle kopyalarken shared master bilgisini koruyor. `node_delete`, yalnız seçilen alt ağacın kimliklerini topluyor; silinen master’ın kalan bir grup üyesine içeriğini taşıyor ve diğer başvuruları ona bağlıyor. SweetCherry bu davranışları bağımsız SQL işlemleriyle uygular; kaynak kod kopyalanmamıştır.

**Bilinçli fark: CherryTree’nin `_node_add` akışı paylaşımlı kökü bağımsız gerçek düğüme dönüştürebilir. SweetCherry’de paylaşımlı kökün kopyası aynı özgün master’a bağlanır; bu tercih korunmaktadır. Bu, geçerli bir paylaşımlı kayıt oluşturur ve veritabanı yapısını bozmaz; kopyanın içeriği bağımsız değildir.**

**Silmede seçilen yeni master’ın kimliği CherryTree’den farklı olabilir. Korunan içerik, dış konumlar ve ağaç ilişkileri esas alınır.**

## Silme sonrası seçim

Silinen konumun sıralamadaki önceki kardeşi seçilir. Önceki kardeş yoksa sonraki kardeş, kardeş yoksa ebeveyn seçilir. Son kök silinirse sanal kök (`0`) gösterilir. Gerçek ve paylaşımlı konumlar arasında ayrım yapılmaz; seçim `children.sequence` sırasına göredir.

## SQL’den manuel test CTB’si oluşturma

Kaynak dosya: [`src/test/resources/fixtures/shared-node-tree.sql`](../../src/test/resources/fixtures/shared-node-tree.sql). Test veritabanı bu SQL’den üretilir; oluşturulan CTB depoya eklenmez.

DB Browser for SQLite’da yeni, boş bir `shared-node-tree.ctb` dosyası oluşturun. Tablo oluşturma penceresini iptal edin. **Execute SQL** sekmesinde kaynak SQL dosyasını açıp tamamını çalıştırın; değişiklikleri kaydedip bağlantıyı kapatın. Var olan not veritabanınızda bu script’i çalıştırmayın.

Alternatif olarak SQLite CLI kuruluysa depo kökünde Windows CMD’den:

```bat
mkdir target\manual-tests
sqlite3 target/manual-tests/shared-node-tree.ctb ".read src/test/resources/fixtures/shared-node-tree.sql"
```

Hedef CTB önceden mevcut olmamalıdır. Mevcut demo tenant tanımını ayrı bir config dosyasına kopyalayıp `name` ve SQLite bağlantı yolunu yeni CTB’ye göre değiştirin; test için `custom.isWritable=true` kullanın. Diğer ayarları koruyun. Veri kaynaklarını yeniden yükleyip bu tanımı seçin. CTB’yi yeniden oluşturmak veya üzerine yazmak için önce SweetCherry’de veri kaynağını, CherryTree ve DB Browser’da dosyayı kapatın.

Fixture düz metin ve ağaç ilişkilerini içerir. Nesne ve bookmark korumasını manuel kontrol etmek için başlangıç kopyasına ayrıca resim, tablo, kod kutusu ve yer işareti ekleyin. Otomatik testler binary/NULL verilerini ayrıca oluşturur.

## Demo kopyasında manuel test

Her senaryoya yukarıdaki SQL’den üretilen CTB’nin ayrı başlangıç kopyasıyla başlayın. Bu fixture 18 ve 19 dahil test konumlarını içerir.

| İşlem | Beklenen sonuç |
| --- | --- |
| 11’i alt ağacıyla çoğalt | Yeni shared kök → 2; 12, 13, 14, 15 bağımsız gerçek kopyalar; 18 → 1 ve altındaki 19 kopyalanır |
| 1’in alt ağacındaki referansı çoğalt | Yeni shared referans özgün master’a bağlı kalır; yeni gerçek master kopyasına yönlenmez |
| Shared 16’yı sil | 16, 7, 8 kalkar; gerçek 6 kalır; dışarıdaki 17 yeni master olur, 8’in içeriğini korur |
| Gerçek 6’yı sil | 16 yeni master olur; 7 ve 8 onun altında kalır; nesneler ve 16 bookmark’ı korunur |
| Gerçek 1’i sil | 10 veya dışarıdaki başka bir referans yeni master olur; diğer dış referanslar ona bağlanır |
| Shared 11’i sil | Yalnız kendi alt ağacı kalkar; gerçek 2 ve onun çocuğu 3 kalır |
| Bir grubun tüm üyelerini aynı alt ağaçta sil | Grubun içerik ve nesne satırları tamamen kaldırılır |

SQLite testleri ayrıca binary/NULL korunması, döngünün yazmadan önce reddi ve promotion hatasında transaction rollback durumlarını kapsar. SweetCherry otomatik yedek almaz.

## Kullanım ve eski kayıtlar

Release öncesi denemeleri demo veya ayrı CTB kopyalarıyla yapın; diğer tenant tanımlarında `custom.isWritable=false` kullanabilirsiniz. Silme onayında seçilen ağaç konumunun kimliği ile içerik master’ının kimliğini ayrı kontrol edin. Paylaşımlı bağlantılar kendi `children.node_id` değerini korur.

Eski sürümlerden kalmış, silinmiş düğümlere ait bookmark kayıtları okuma sayfasında bildirilir; sırf liste görüntülendi diye otomatik temizlenmez.

## Ayrı doğrulanacak işler

- Paylaşımlı konumun altında yeni alt düğüm oluşturma: mevcut oluşturma servisi hâlâ gerçek parent ister; gösterim, taşıma ve çoğaltma desteği bu kısıtı kendiliğinden kaldırmaz.
- Ortak/yeni CTB’ye kopyalama: dışarıda kalan master’a referans veren bir alt ağacın hedef CTB’de geçerli içerik bağlantıları kurduğu ayrıca doğrulanmalıdır.
- Metin içindeki internal linkler: silinen kimlikler yeni master kimliğine otomatik çevrilmez. Bookmark temizliği ile metin bağlantılarının korunması ayrı konulardır.
