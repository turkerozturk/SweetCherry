# Paylaşımlı düğümlerin ağaç yapısı

Bir paylaşımlı düğümün de kendi alt düğümleri olabilir. `children.node_id` ağaçtaki konumu, `father_id` ebeveyni, `sequence` kardeş sırasını belirler. `master_id` yalnız ad, içerik ve içerikle birlikte kullanılan özellikler için gerçek düğümü gösterir. Paylaşımlı düğüm altında master’ın çocukları otomatik gösterilmez.

Masaüstü ve mobil okuyucular, breadcrumb, Mermaid, Markmap, Freeplane ve alt ağaç PDF aktarımı bu ayrımı izler. PDF’de aynı içeriğin farklı konumları ayrı başlıklardır; paylaşımlı düğümün çocukları PDF gezinme panelinde onun altında yer alır.

## CherryTree kaynak incelemesi

Kaynak: https://github.com/giuspen/cherrytree/blob/master/src/ct/ct_storage_sqlite.cc

2026-10-07 tarihinde incelenen SQLite yükleme akışında, `f_nodes_from_db` düğümü `(node_id, master_id)` ile yükler ve çocuklarını `_get_children_node_ids_from_db(node_id)` ile alır. Bu sorgu `father_id` üzerinden ve `sequence` sırasıyla çalışır. `_node_from_db` ise paylaşımlı düğümün içerik özelliklerini master üzerinden okur. Bu, içerik referansı ile ebeveyn ilişkisinin ayrı olduğunu doğrular. SweetCherry değişikliği bu davranışın bağımsız uygulamasıdır; CherryTree kodu kopyalanmamıştır.

Bu inceleme silme, taşıma veya çoğaltmanın tüm kurallarını doğrulamaz. Bu yamada bu işlemler değiştirilmemiştir. Paylaşımlı düğüm altında oluşturma/taşıma, CTB kopyalama ve silme korumaları sonraki uyumluluk adımında ele alınacaktır. Özellikle alt düğümü bulunan paylaşımlı düğümü silme işlemini henüz uyumlu kabul etmeyin.

## Okunabilir test verisi

`src/test/resources/fixtures/shared-node-tree.sql`, kullanıcı tarafından hazırlanan `nodetreetest.ctb` ağacının İngilizce, içerikleri sadeleştirilmiş test karşılığıdır. Orijinal CTB değiştirilmemiştir. SQL, boş SQLite veritabanında çalıştırılır; var olan not veritabanında çalıştırılmamalıdır. JDBC testleri bunu bellekte yükler. Üretim verisi veya tenant kaydı oluşturmaz.

| Konum | Beklenen gösterim |
| --- | --- |
| Gerçek 2 | Kendi çocuğu 3 |
| Paylaşımlı 11 → 2 | Kendi çocukları 12, 13, 18; 3 burada görünmez |
| Gerçek 13, ebeveyn 11 | Çocukları 14, 15; breadcrumb 11 → 13 |
| Paylaşımlı 16 → 6 | Kendi çocukları 7, 8 |
| Paylaşımlı 17 → 8, ebeveyn 9 | 8’in adı/içeriği, 9’un altında 17 kimliği |
| Paylaşımlı 18 → 1, ebeveyn 11 | Kendi çocuğu 19; iki paylaşımlı ebeveyn içeren yol |
| Paylaşımlı 10 → 1 | Çocuksuz referans |

Kontrol için 11 ve 16’yı masaüstü ağacında açın; mobilde alt düğüme ve geri ebeveyne gidin. 19’un breadcrumb bağlantıları 11 ve 18’e gitmeli. Üç haritada ve 11’in alt ağaç PDF’sinde 12, 13, 14, 15, 18, 19 görünmeli; master 2’nin çocuğu 3 görünmemeli.
