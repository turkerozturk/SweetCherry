# Sürümleme ve release akışı

Uygulama sürümünün tek kaynağı `pom.xml` içindeki proje `<version>` alanıdır; Spring Boot parent sürümü değildir. `application.yml` içindeki `info.app.version: "@project.version@"` Maven resource filtering ile doldurulur. Git bilgileri ayrıca `git.properties` dosyasına yazılır. JAR dışındaki YAML’de eski bir sabit `info.app.version` varsa kaldırın; uygulama sürümü dağıtılan JAR’a ait olmalıdır.

Geliştirme sürümü `0.5.0-SNAPSHOT` olarak işaretlenir. Her commit için sürüm artırılmaz; aynı geliştirme sürümünün farklı derlemeleri commit kimliğiyle ayrılır. İlk planlanan yayın `0.5.0`dır; bu belge henüz release oluşturmaz. Önceden yayımlanmış aynı sürümü yeniden kullanmayın; varsa sıradaki uygun sürümü seçin.

`MAJOR.MINOR.PATCH` yaklaşımı kullanılır. Uyumluluk kapsamımız tenant ayarları, belgelenmiş kullanım yolları ve CTB okuma/yazma davranışıdır. `0.x` geliştirme döneminde uyumluluk garantisi verilmez; kıran değişiklikleri yine sürüm notunda açıkça belirtiriz. Hata düzeltmeleri için `0.5.1`, yeni özellikler için `0.6.0` örnektir. Aday sürüm gerekiyorsa `0.5.0-rc.1` kullanılabilir. Kaynak: https://semver.org/

## Yayın öncesinde

1. Temiz çalışma ağacında proje sürümünü `0.5.0` yapın. CHANGELOG’daki Unreleased değişikliklerini `0.5.0` ve yayın tarihi altında tamamlayın; yeni boş Unreleased bölümü bırakın.
2. Windows’ta `mvnw.cmd clean package`, Linux’ta `sh ./mvnw clean package` çalıştırın. Dağıtım klasöründeki `application.yml` ve `git.properties` sürümünü kontrol edin. Manuel kabul listesini tamamlayın.
3. Release hazırlığını commit/push edin ve aynı commit için CI’nin başarılı olduğunu doğrulayın. Sonra `git tag -a v0.5.0 -m "SweetCherry 0.5.0"` ve `git push origin v0.5.0` çalıştırın. Etiketi sonradan taşımayın.
4. Etiketli commit’ten temiz checkout ile son paketi yeniden derleyin; bu sayede paket Git bilgileri hazırlık commit’ine ve etikete aittir. `SweetCherry.jar` adı scriptlerle uyum için sabit kalır; dağıtım arşivi `SweetCherry-0.5.0.zip` olur. SHA-256 özetini ve sürüm notlarını aynı GitHub Release’e ekleyin. [Release kontrol listesi](release-checklist.md) izlenir. Mevcut CI tag push’larında otomatik release yayımlamaz; gerekirse workflow_dispatch ile etiketin derlemesini çalıştırın.
5. Yayın sonrasında main sürümünü sıradaki çalışma için örneğin `0.5.1-SNAPSHOT` yapın. Yayınlanan arşivi değiştirmeyin; düzeltme yeni sürümle yayımlanır.

Geçici `documentation/tr/comment-removal-log.md` dosyası release öncesinde yerel olarak yedeklenip depodan kaldırılmalıdır. Git geçmişi de korunur.
