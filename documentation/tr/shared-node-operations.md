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

Kaynakta kök düğüm çoğaltma ayrı `_node_add` akışındadır ve paylaşımlı kökü bağımsız gerçek düğüme dönüştürebilir. SweetCherry’de daha önce onaylanan “paylaşımlı kopya aynı master’a bağlanır” davranışı bu adımda korunmuştur; kök düğümün bağımsızlaştırılması ayrı bir karar gerektirir. Yeni master seçiminin kimliği CherryTree’den farklı olabilir; korunması gereken içerik, dış konumlar ve ağaç ilişkileridir.

## Demo kopyasında manuel test

Her senaryoya başlangıçtaki CTB’nin ayrı kopyasıyla başlayın. SQL fixture’ında eklenen 18 ve 19 orijinal demo dosyasında bulunmayabilir.

| İşlem | Beklenen sonuç |
| --- | --- |
| 11’i alt ağacıyla çoğalt | Yeni shared kök → 2; 12, 13, 14, 15 bağımsız gerçek kopyalar; varsa 18 → 1 ve altındaki 19 kopyalanır |
| 1’in alt ağacındaki referansı çoğalt | Yeni shared referans özgün master’a bağlı kalır; yeni gerçek master kopyasına yönlenmez |
| Shared 16’yı sil | 16, 7, 8 kalkar; gerçek 6 kalır; dışarıdaki 17 yeni master olur, 8’in içeriğini korur |
| Gerçek 6’yı sil | 16 yeni master olur; 7 ve 8 onun altında kalır; nesneler ve 16 bookmark’ı korunur |
| Gerçek 1’i sil | 10 veya dışarıdaki başka bir referans yeni master olur; diğer dış referanslar ona bağlanır |
| Shared 11’i sil | Yalnız kendi alt ağacı kalkar; gerçek 2 ve onun çocuğu 3 kalır |
| Bir grubun tüm üyelerini aynı alt ağaçta sil | Grubun içerik ve nesne satırları tamamen kaldırılır |

SQLite testleri ayrıca binary/NULL korunması, döngünün yazmadan önce reddi ve promotion hatasında transaction rollback durumlarını kapsar. SweetCherry otomatik yedek almaz.
