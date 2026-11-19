# Düğüm başlığı ve araç çubuğu

`/tree` sayfasında düğüm başlığı üzerine gelince kalem görünür; Tab ile de ulaşılabilir. Dokunmatik cihazlarda kalem görünür kalır. Yazılabilir bağlantıdaki ADMIN kullanıcı gerçek düğümü yerinde yeniden adlandırabilir. Enter/Kaydet adı kaydeder; Escape/Vazgeç değiştirmeden kapatır. Shared konumlarda mevcut properties kısıtları korunur.

Başlık endpoint’i güncel tenant belirteci, CSRF koruması ve revision kontrolü kullanır. Eşzamanlı yeniden adlandırma veya değişiklikte vazgeçip editörü yeniden açmak gerekir. Yalnız ad ve değiştirilme zamanı değişir; başlık rengi, kalınlık, ikon, etiket ve içerik korunur. CTBLOGS etkinse properties güncellemesi olarak kaydedilir. Kaydetme sonrasında başlık, breadcrumb ve ağaç mevcut AJAX gezinmesiyle güncellenir.

Normal düğüm araç sırası mevcut izin/ayarlara bağlı olarak Tags, Edit, Properties, dört taşıma kontrolü, Move to, Delete, Add, Map, Export, CTBLOGS history ve Add reminder biçimindedir. Bilgi düğmesi başlığın yanındadır. Tek editör varsa Edit uygun ikonla doğrudan açar; düz metin ve rich text editörlerinin ikisi de varsa açılır menü korunur. Çoğaltma ve alt düğümlerle çoğaltma Add menüsünde, ayraçla ayrılan Create Shared Node seçeneğinin üstündedir. Mevcut oluşturma/çoğaltma onay tercihleri geçerlidir.
