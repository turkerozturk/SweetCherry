# Düğüm araç çubuğu

Yeni masaüstü ve mobil görünümler ortak araç çubuğunu kullanır:
Bilgi → Özellikler → Düzenle → Taşıma okları → PDF → CTB → Harita → Ekle → Sil.

Düzenle menüsünde mevcut metin ve zengin metin düzenleyicileri, ayrıca daha önceki tek düğüm/alt ağaç çoğaltma işlemleri korunur. Ekle menüsü kardeş düğüm, alt düğüm, yer işareti ve paylaşımlı düğüm oluşturmayı içerir. Kardeş düğüm, seçilen ağaç kaydının hemen sonrasına eklenir; paylaşımlı düğüm seçilmişse onun ağaç konumu kullanılır. Paylaşımlı bir düğümün altına düğüm ekleme sınırı korunur.

Taşıma oklarının açıklamaları Alt + Shift + yön tuşu kısayolunu gösterir. PDF alt düğümler seçeneği henüz uygulanmamıştır ve pasiftir. Yetki, salt okunur veri kaynağı ve düğüm türüne göre önceki görünürlük kuralları sürer.

## Tenant config indirme

`/tenants/config?tenant=...` doğrudan açıldığında da onay sayfası gösterir. Dosya ancak onay formu POST edildiğinde indirilir; admin yetkisi, CSRF koruması, kayıtlı dosya yolu kontrolü ve önbelleğe almama kuralları korunur. Config dosyaları açık metin bağlantı bilgileri içerebilir.

Tenant tablosu kullanıcı adı veya şifreyi göstermez. Kimlik doğrulama sütunu, kullanıcı adı veya şifre alanlarından en az birinin dolu olup olmadığını belirtir; sunucunun gerçekten kimlik doğrulama zorunlu tuttuğunu tespit eden bir bağlantı testi değildir.
