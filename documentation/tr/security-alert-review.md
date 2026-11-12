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

## 2e4047c sonrası ikinci yama — 2026-11-12

Paylaşılan 18 Dependabot ve 18 CodeQL uyarısına göre hazırlanmıştır. Uyarılar otomatik kapatılmaz veya bastırılmaz.

### Bağımlılıklar

- Jackson BOM: 2.21.4 → 2.21.7; core/databind ve ilgili modüller birlikte yönetilir. BOM'un seçtiği annotations sürümünün 2.21 olması normaldir.
- HttpClient: 5.5.2 → 5.6.3; HttpCore ve HTTP/2 modülü: 5.3.6 → 5.4.3. Bu Core sürümü yeni Client parent'ında da kullanılır.
- Log4j API ve köprü bileşenleri: 2.24.3 → 2.25.5.
- OGNL: transitif sürüm yerine dependency management ile 3.4.13.
- Spring Framework 6.2.19 değişmez. Dependabot #18 ve #19 için Boot 4 geçişi hâlâ gereklidir.

Bu sürümler listedeki Spring dışındaki 16 Dependabot uyarısını hedefler. Gerçekte hangilerinin kapandığı push ve bağımlılık grafiği yenilenmesinden sonra doğrulanır.

### Uygulama değişiklikleri

- HTML yapıştırma önce yerel/offline DOMPurify 3.4.17 ile temizlenir; ardından mevcut CTB metin/özellik/tablo dönüşümü uygulanır. Temizleyici yüklenmemişse ham HTML'ye geri dönülmez. Biçimler, güvenli bağlantılar, tablolar ve sunucudan alınan güvenilir görsel token'ları korunur. Lisans ve kaynak bilgileri dağıtıma eklenir.
- `viewMode` tercih cookie'sine HttpOnly ve SameSite=Lax eklenir; HTTPS isteğinde Secure olur. Yerel HTTP kullanımında tercih cookie'si çalışmaya devam eder. Bu cookie kimlik doğrulama bilgisi içermez. HTTPS durumu keyfî proxy başlığından değil, servlet isteğinden ve mevcut güvenilir proxy mekanizmasından gelir.
- Export, listeleme, indirme ve silme ortak klasör sınırı kullanır. Normalize edilmiş yol, tek parça dosya/klasör adı ve sembolik bağlantı kontrolleri uygulanır. Collection export sembolik bağlantıyı SQLite olarak açmadan reddeder. Bu kontroller, aynı anda dosya sistemini değiştirebilen bir yerel yöneticiyi atomik olarak izole eden bir sandbox değildir.
- Aramanın ORDER BY sütunu ve yönü sabit switch seçeneklerinden üretilir; filtre değerleri parametre olarak bağlanmaya devam eder.
- CTB seçiminde kontrol karakterleri ve aşırı uzun girdi Path oluşturulmadan reddedilir. ADMIN'in CTBDATA dışındaki mevcut bir CTB'yi seçebilmesi korunur; gerçek yol, uzantı, normal dosya ve SQLite başlık kontrolleri devam eder.

### Kalan CodeQL değerlendirmeleri

SSRF #22/#23 için önceki sürücü/URL, yetki, DNS sabitleme ve yönlendirme kontrolleri geçerlidir. Uyarının ortak yardımcıya taşınması tek başına açık bulunduğu veya yanlış alarm olduğu anlamına gelmez. Veri akışı ve kurulumun güven sınırı değerlendirilmeden uyarı kapatılmaz. ADMIN'in özel ağdaki veritabanına bağlanması ve kendi CTB dosyasını seçmesi kasıtlı özelliklerdir.

Cookie uyarısı HTTP dalında kalırsa, yalnızca görünüm tercihi içeren bu cookie ile oturum cookie'lerinin politikası ayrı değerlendirilmelidir. Güvenlik uyarılarını susturmak için yerel HTTP desteği kaldırılmaz.

### Kontrol

Java testleri ve paketleme Linux'ta çalıştırılır. Gerçek Windows ve uzak veritabanı bağlantıları ayrıca denenmelidir. Dosya sistemi/izinler sembolik bağlantı oluşturmaya izin vermiyorsa yalnızca ilgili symlink testleri açık bir varsayımla atlanır.

Chromium kontrolünde zararlı script/event/SVG/MathML/template girdileri çalışmadı; parser sırasında kaynak isteği veya canlı belgeye HTML eklenmesi gözlenmedi. Başlık, kalın yazı, bağlantı, tablo ve güvenilir görsel token'ı senaryoları geçti. Bu sonuç bütün olası girdiler için garanti değildir.

Sonraki büyük geçişten önce rich-text yapıştırma/URL import, CTB export–indirme–silme, veritabanı seçimi/oluşturma ve görünüm değiştirmeyi manuel deneyin. Push sonrası GitHub taramalarını tekrar değerlendirin. Uyarı numarası eşlemesi ve resmî kaynaklar İngilizce incelemenin ikinci aşama bölümündedir.


## 3f71f76 üzerinde Spring Boot 4 geçişi (2026-11-13)

Spring Boot **4.1.2** sürümüne geçildi; Java tabanı **17** olarak korundu. Spring Framework, Security, Hibernate, Jackson ve Tomcat birlikte Boot bağımlılık yönetimine bırakıldı. Önceki Tomcat 10 ve Jackson 2 BOM sabitlemeleri kaldırıldı; incelenmiş OGNL sabitlemesi korundu. Springdoc 3.2.0 ve Thymeleaf layout dialect 4.0.1 kullanılıyor. Yayınlanmış `thymeleaf-extras-springsecurity6` modülü Boot'un yönettiği sürümde kullanılmaya devam ediyor; adındaki 6, çalışan Spring Security sürümünü belirlemiyor.

Uygulamanın JSON kodları Jackson 3'e taşındı. Eski yedek kataloglarında sonradan eklenen otomatik yedekleme alanlarının bulunmaması destekleniyor; bozuk kayıtlı filtreler mevcut IO hata akışını kullanıyor. Geçiş mevcut dosyaları yeniden yazmıyor. Bazı üçüncü taraf kütüphaneler için Jackson 2'nin düzeltilmiş sürümleri ayrıca Boot tarafından yönetiliyor. Boot paketleri, environment processor kaydı, Tomcat ek bağlantı noktası ve dil çerezi API'leri uyarlandı. Betikler zaten `java -jar` kullandığından artık desteklenmeyen Maven `executable` seçeneği kaldırıldı. Security'nin aynı origin üzerinde göreli giriş yönlendirmesi korundu; HTTPS/HSTS kontrolleri sürüyor.

Dependabot #18 ve #19 için düzeltilmiş OSS Spring Framework sürümü 7.0.9'dur. Boot 4.1.2'nin yönettiği **7.0.10**, bu iki bağımlılık uyarısının etkilenen sürümlerini giderir. GitHub bağımlılık grafiği yenilenmeden uyarıların kapandığı söylenmemelidir.

Kalan CodeQL uyarıları bu sürüm yükseltmesinden ayrıdır; bastırma veya otomatik kapatma yapılmadı:

- **#23:** Web içeriği aktarımında tüm DNS yanıtları doğrulanıyor, değiştirilemez yanıtlar gerçek bağlantı yöneticisine veriliyor ve her yönlendirme yeniden doğrulanıyor. ADMIN, yazılabilir not ve CSRF kontrolleri de var. Tam veri akışı `PublicWebFetcherSecurityTest` ve `WebContentImportSecurityTest` ile birlikte incelenmeli; özel doğrulamaların analiz tarafından tanınmadığı doğrulanırsa gerekçeli false-positive kararı verilebilir.
- **#22:** ADMIN'in localhost veya özel ağdaki veritabanına bağlanabilmesi amaçlanan işlevdir. Sürücü/JDBC eşleştirmesi denetlenir ve SQLite uzak kaynakları reddedilir. ADMIN bu yetki için güvenilir kabul edilmelidir; güvenilmeyen kişilere bağlantı yapılandırması verilecekse dağıtıma özel hedef izin listesi gerekir.
- **#20:** ADMIN sunucuda CTBDATA dışındaki mevcut bir CTB'yi de seçebilir. Gerçek yol, normal dosya, `.ctb` uzantısı ve SQLite başlığı denetlenir. Normalizasyon tek başına güvenlik kanıtı değildir; tam uyarı akışı ve yetkilendirme birlikte incelenmelidir.

64 bit Linux / Corretto 17.0.21 üzerinde `clean package` başarılı: **782 Java testi**, sıfır hata/başarısızlık/atlama. **147 JavaScript testi** de geçti. Paketlenen JAR ayrı klasörde çalıştırılarak giriş, dil değiştirme, CTB seçimi, tenant sihirbazı, düğüm içeriği/özellikleri, iki mind map görünümü ve quick content kontrol edildi. Yalnızca bu denemede etkinleştirilen API dokümantasyonu geçerli OpenAPI JSON döndürdü. Bunlar Windows, 32 bit, uzak veritabanı veya CodeQL taraması sonucu değildir.

`mvnw.cmd clean package` ve `node --test src/test/js/*.test.cjs` sonrasında temiz bir deneme klasöründe giriş/çıkış, dil değiştirme, CTB açma/oluşturma, editörler, kayıtlı filtreler, yedek oluşturma/geri yükleme ve import/export işlemleri denenmelidir. Launcher ve Windows paketleme ayrıca kontrol edilmelidir. Kullanılıyorsa gerçek MariaDB/MySQL/PostgreSQL sunucuları ile HTTPS/proxy kurulumu denenmelidir. Otomatik SQLite JPA testleri bu sunucuların yerini tutmaz. **32 bit işletim sistemi/JVM desteği test edilmedi**; Java 17 uyumluluğu, yerel SQLite veya launcher desteğini tek başına garanti etmez.

Sürüm tablosu, ayrıntılı inceleme ve resmi kaynaklar için [İngilizce bölüm](../en/security-alert-review.md#spring-boot-4-migration-on-3f71f76-2026-11-13).
