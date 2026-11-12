# Güvenlik uyarıları — ilk sağlamlaştırma yaması

2026-11-12 tarihinde `996332e` temel alınmıştır. Bu yama bütün CodeQL/Dependabot uyarılarının çözüldüğü anlamına gelmez; hiçbir uyarıyı otomatik kapatmaz veya bastırmaz.

- YAML ayarlarında `SafeConstructor` açıkça kullanılır; özel sınıf/yerel etiketler ve aşırı iç içe belgeler reddedilir. Yorumları ve diğer ayarları koruyan mevcut metin güncelleme yöntemi devam eder.
- URL içerik aktarmasında DNS yanıtı kopyalanıp doğrulanır ve bağlantı boyunca sabitlenir. Aynı sunucuya yönlendirmelerde bile yeni DNS yanıtı doğrulanır. Özel IP, IPv6, DNS rebinding, yönlendirme ve yanıt sınırları ağ erişimi yapmayan testlerle kontrol edilir.
- Tenant bağlantılarında yalnızca desteklenen dört JDBC sürücüsü ve eşleşen URL önekleri kabul edilir. Kontrol eski bağlantı kapatılmadan ve dosya yazılmadan önce, ayrıca config yeniden yüklenirken yapılır. SQLite `:resource:` bağlantıları reddedilir.
- ADMIN tarafından bilerek ayarlanan localhost ve özel ağdaki PostgreSQL/MariaDB/MySQL bağlantıları korunur. Desteklenmeyen sürücü veya SQLite `:resource:` kullanan eski tenant config dosyaları düzeltilmelidir; atlanan bağlantılar uygulama logundan kontrol edilir.
- Boş CTB ve export işlemleri dosya yolu alan ortak SQLite yardımcısını kullanır; JDBC URL seçeneklerine dönüşebilecek `?`, `#` ve kontrol karakterleri kabul edilmez. Export dosya isminde tenant adındaki `#`, `_` olur. Güvenilir tenant config dosyalarındaki mevcut SQLite dosya URI'leri ve seçenekleri bu yardımcıya dönüştürülmez.
- Embedded Tomcat bileşenleri birlikte 10.1.55 → 10.1.61 olarak güncellenir.

## Sınır ve sonraki aşama

Tomcat güncellemesi Spring Framework açıklarını düzeltmez. Boot 3.5.16 son OSS bakım sürümüdür; yönetilen Framework 6.2.19 için raporlanan açıklar ayrıca değerlendirilmelidir. İncelenen kaynaklarda XsltView veya SSE uygulaması bulunmaması yalnızca bu iki kullanım koşuluna ilişkin bir ön değerlendirmedir. Boot 4 geçişi ayrı uyumluluk çalışmasıdır; Boot 3 üzerine tek tek Spring 7 modülü eklenmez. Java 17 hedefi korunabilir.

Özel SSRF korumalarını statik analiz her zaman tanımayabilir. Kalan uyarılar veri akışı ve test kanıtlarıyla değerlendirilir; sırf sayıyı azaltmak için bastırılmaz.

## Uygulama sonrası

1. `mvnw.cmd clean package` ve Node.js varsa `node --test src/test/js/*.test.cjs` çalıştırın.
2. `mvnw.cmd -Preports compile` ile `target/dependency-tree.txt` içinde Tomcat core/el/websocket sürümlerinin 10.1.61 olduğunu doğrulayın.
3. Ayar kaydetme, CTB açma/oluşturma, CTB export ve herkese açık URL'den içerik aktarmayı deneyin.
4. Kullanıyorsanız PostgreSQL/MariaDB/MySQL bağlantısını gerçek sunucuyla ayrıca deneyin; SQLite testleri uzak sunucu uyumluluğunu doğrulamaz.
5. Push sonrasında CodeQL ve dependency submission tamamlanınca kalan uyarıları inceleyin. Spring uyarılarını bu yamayla çözülmüş olarak kapatmayın.

Ayrıntılı uyarı eşlemesi ve resmî kaynaklar: [İngilizce inceleme](../en/security-alert-review.md).
