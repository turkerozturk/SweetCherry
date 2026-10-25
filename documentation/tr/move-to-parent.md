# ID ile üst düğüme taşıma / Move to parent by ID

Düğüm içeriğinin araç çubuğunda **Sil** düğmesinin sağındaki **Şuraya taşı / Move to** düğmesi,
masaüstü ağaç görünümünde ve mobil okuyucuda aynı şekilde çalışır. ADMIN oturumu ve yazılabilir
veri kaynağı gerekir.

1. Taşınacak düğümü açın ve **Şuraya taşı** düğmesine basın.
2. Yeni üst düğümün ağaç ID’sini girin. Paylaşımlı düğüm hedeflenecekse master’ın değil,
   o paylaşımlı konumun ID’sini kullanın. `0`, veritabanının sanal kökünü belirtir.
3. **Taşı** ile onaylayın. Düğüm, mevcut alt ağacıyla beraber hedefin en son çocuğu olur;
   aynı düğüm seçili kalır ve ağaç/navigasyon yenilenir. **İptal** veya Escape değişiklik yapmaz.

İçerikler, yer işaretleri, düğüm kimlikleri ve master bağlantıları korunur. Eski ve yeni üst
konumdaki kardeşlerin sıra numaraları düzenlenir. Taşınan gerçek düğümün (paylaşımlıysa master’ının)
modified tarihi güncellenir; created tarihi ve alt düğümlerin tarihleri değişmez. Hedef mevcut
üst düğümse işlem yapılmaz: sıra ve tarihler korunur.

Kendi içine veya alt ağacındaki herhangi bir konuma taşıma reddedilir. Bu kontrol `children`
ağaç ilişkisine göredir; ortak içeriğe sahip farklı konumlar tek başına döngü sayılmaz.
Olmayan/geçersiz ID ve eski ağaç anlık görüntüsü veri değiştirmez. Ağaç değişti uyarısında
pencereyi kapatıp yeniden açın; veri kaynağı değiştiyse sayfayı da yenileyin. Ağ hatasında
sonuç belirsiz olabilir: tekrar denemeden önce sayfayı yenileyip konumu kontrol edin.

## Manuel kontrol

- Bir düğümü başka daldaki gerçek üst düğüme taşıyın; alt ağacı ve içeriği korunsun, son sıraya gelsin.
- Aynısını paylaşımlı kaynak ve paylaşımlı hedefle deneyin; master bağlantıları korunsun.
- `0` ile köke taşıyın. Mevcut üst düğüm ID’siyle işlem yapınca sıra ve tarih değişmesin.
- Kendi ID’si, bir alt düğümün ID’si, olmayan/negatif/metin/çok büyük ID ile kayıt değişmesin.
- Pencere açıkken başka sekmeden ağacı veya veri kaynağını değiştirin; eski işlem reddedilsin.
- Salt okunur veri kaynağında veya USER oturumunda düğme görünmesin; doğrudan POST da reddedilsin.

## English

**Move to**, immediately after **Delete**, opens a parent-ID dialog in both desktop and mobile
readers. An ADMIN session and a writable database are required. Enter a real or shared tree
occurrence ID, or `0` for the database root. The selected occurrence and its subtree are appended
to that parent's children; content, IDs, bookmarks and shared/master references are preserved.
The moved occurrence's content/master modified timestamp is updated. Selecting its existing
parent is a no-op, retaining order and timestamps. Self/descendant destinations and stale tree
snapshots are rejected before writing. Cancel and Escape do not move anything. After a network
failure, reload and check the location before retrying.
