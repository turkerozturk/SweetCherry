# Yer işaretlerini yönetme

Yeni masaüstü ağaç görünümünde ve mobil okuyucuda **Yer işaretine ekle** düğmesi vardır. Seçilen düğüm zaten yer işaretlerindeyse düğme pasif **Yer işaretlerinde** olarak görünür. Tekrarlanan ekleme aynı kaydı çoğaltmaz veya sırasını değiştirmez.

`/bookmarks` sayfası mevcut detayları korur; her kaydın yanında onay isteyen **Yer işaretini kaldır** düğmesi bulunur. Kaldırma sonrası aynı listeye dönülür. Eksik düğümlere ait bookmark'lar uyarıda ayrıca listelenir ve açıkça seçilerek kaldırılabilir; otomatik temizlik yapılmaz.

Yazma için admin ve `custom.isWritable=true` gerekir. İçeriği read-only olan gerçek düğüm de işaretlenebilir; bu işlem içeriği değiştirmez. POST işlemleri CSRF ve güncel `_tenantView` denetimlerine tabidir.

Bookmark `node_id`, ağaçtaki occurrence kimliğidir. Gerçek düğüm ve onun shared node'leri bağımsız olarak işaretlenebilir. Shared node'ye eklemek master'ı işaretlemez; shared bookmark'ı kaldırmak master veya diğer alias bookmark'larını kaldırmaz. Liste bağlantıları shared node'nin kendi kimliğini korur.

Yeni kayıt mevcut en büyük `sequence` değerinin sonuna eklenir. Kaldırmada diğer kayıtların sırası yeniden numaralanmaz; boşluklar kalabilir. Liste `sequence`, ardından `node_id` ile sıralanır. Metin, gömülü nesneler, `children` ve node kayıt zamanları değişmez.

## CTB kopyasında kontrol

- Gerçek düğümü ve bir shared node'yi ayrı ayrı ekleyin; listede iki kayıt görünmeli.
- Aynı kaydı yeniden ekleyin; ikinci bookmark oluşmamalı.
- Shared kaydın bağlantısı shared sayfasını açmalı; kaldırma sonrası master bookmark'ı kalmalı.
- Yer işareti kaldırma onayını iptal edin; liste değişmemeli.
- Read-only CTB ve user hesabında yazma düğmeleri görünmemeli; eski veri kaynağı sekmesinden POST reddedilmeli.
- CherryTree'de bookmark listesini ve özgün düğüm içeriğini kontrol edin.
