# Yedek sorumluluğu ve tenant oluşturma planı

**SweetCherry otomatik yedek almaz.** Yazılabilir tenant ile yapılan düzenleme, taşıma, çoğaltma ve silme işlemleri doğrudan seçilen veritabanına uygulanır. CTB export, bütün veritabanının otomatik geri yüklenebilir yedeği olarak değerlendirilmemelidir. Veri kaynağı seçimi ve düzenleme ekranlarında bu bilgi gösterilir.

`custom.isWritable=false` ile başlayın. Yazma iznini açmadan önce ayrı yedek oluşturun ve geri yükleyebildiğinizi doğrulayın. Aynı CTB üzerinde iki uygulamadan eşzamanlı yazmayın. SQLite dosyasını başka bir süreç yazarken yalnız ana `.ctb` dosyasını kopyalamak güvenli bir yedek oluşturmayabilir: bütün yazan uygulamaları kapatın veya SQLite-aware bir yedek yöntemi kullanın. Yedekleme SweetCherry’nin özelliği olmayacaktır.

SweetCherrySync ayrı bir projedir: https://github.com/turkerozturk/SweetCherrySync . Yedekleme/senkronizasyon amacıyla ayrıca incelenecektir; mevcut SweetCherry dağıtımı bu projeyi otomatik çalıştırmaz veya yedek oluşturduğunu doğrulamaz. Tenant yapılandırmalarındaki uzak veritabanı bilgileri ve ilgili JDBC sürücüleri ayrı uzak veritabanı iş akışlarıyla ilişkilidir; bunlar otomatik yedek güvencesi değildir.

## Sonraki feature: tenant/veritabanı oluşturma sihirbazı

- Admin için, mevcut CTB’yi tanıtan tenant formu ve ayrı bir yeni boş CTB oluşturma seçeneği.
- Boş CTB `CTBDATA` altında doğrulanmış şemayla oluşturulmalı; demo verisini kopyalamamalı. Örnek demo tenant yalnız ayar şablonu olmalı, onun bağlantı adresi yeni dosyaya dönüştürülmeli. İlk izin salt okunur olmalı; yazma izni ayrıca açıklanmalı.
- Kullanıcıya keyfi sunucu yolu yazdırılmamalı; dosya adı/yol doğrulaması, üzerine yazmama ve aynı DB’ye birden fazla alias konusu açıkça tanımlanmalı.
- Tenant TXT ve CTB oluşturma başarısızlıklarında yarım dosyalar temizlenmeli; yeni tenant reload sonrası yüklenebilmeli veya açık sonuç mesajı verilmeli.
- Yapılandırılabilir maksimum tenant tanımı ve ayrı maksimum CTB dosyası sayısı; alias’lar nedeniyle bunlar aynı değildir. Mevcut yükleme ve sihirbaz aynı limite uymalı, eşzamanlı istekler limiti aşamamalı. Limit mevcut tanımları silmemeli.
- Yükleme boyutu, toplam disk bütçesi, bağlantı havuzu kaynakları, CSRF, admin rolü ve güvenli dosya oluşturma birlikte ele alınmalı.

Bunlar plan maddeleridir; bu belgede anlatılan sihirbaz ve limitler henüz uygulanmamıştır.
