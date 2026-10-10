# Changelog

SweetCherry'deki kullanıcıya dönük önemli değişiklikler bu dosyada kaydedilir.

## [Unreleased]

### Added

- Masaüstü kontrol penceresinde tüm yerel YAML ayarları için tablo/metin editörü; JAR varsayılanından gerektiğinde harici dosya oluşturma, gizli alan maskeleme ve mevcut yeniden başlatma akışı.

- ADMIN için seçilmiş güvenlik dışı YAML ayarlarını düzenleme formu ve onaylı, aynı JVM içinde yeniden başlatma.
- ADMIN için tenant config oluşturma/düzenleme sihirbazı: sunucunun yerel veya ağ paylaşımı CTB dosyasını Gözat ile seçme, ayarları formdan düzenleme ve kaydedince mevcut seçim akışıyla açma. CTB kopyalanmaz; bilinmeyen config alanları korunur.

### Fixed

- Eski SQLite CTB şemaları seçimden önce kontrol edilir. Eksik `children.master_id` alanı yalnız `custom.allowLegacySchemaUpgrade=true` (veya `1`) izniyle ve yönetici seçiminde, varsayılan `0` ile transaction içinde eklenebilir. Desteklenmeyen eksikler dosya değiştirilmeden açıklanır.

## [1.0.0] - 2026-10-08

### Added

- Java 17 içeren Windows x64 installer ve portable ZIP; masaüstü kontrol penceresi, tepsi menüsü, başlangıç durumu ve kapatma kontrolleri.
- İngilizce ve Türkçe son kullanıcı kurulum kılavuzları; HTTP/HTTPS, Caddy/proxy ve yerel/uzak erişim belgeleri.
- Masaüstü ve mobil okuma, yer işaretleri, düğüm taşıma/çoğaltma ve gerçek/paylaşımlı düğüm altında kardeş/alt düğüm oluşturma.
- Zengin metin düzenleme; resim ve dosya yükleme/panodan resim, dış bağlantı ve tablo düzenleme, web sayfasından biçimli yapıştırma, nesne silme.
- Markmap/Mermaid haritaları; Freeplane/FreeMind ve isteğe bağlı CherryTree ikon ZIP'i.
- Düğüm/alt ağaç PDF; gömülü fontlar, bağlantılar, hiyerarşik PDF navigasyonu, içerik tablosu ve oturumda hatırlanan sayfa/nesne seçenekleri. Şablon tablosu PDF çıktısı.
- Düğüm ve alt ağaç için ortak veya yeni CTB'ye dört aktarma seçeneği; paylaşımlı konumların hiyerarşisi ve desteklenen master eşlemeleri.
- Tenant yapılandırması indirme/silme onayları; dosya yokken SQLite'ın boş veritabanı oluşturmasını engelleme ve bağlantı havuzlarını kapatma.
- İsteğe bağlı çevrimdışı astronomi widget'ları ve konum/zaman ayarları.

### Changed

- Uygulama adı SweetCherry; sürüm 1.0.0. Varsayılan dinleme adresi 127.0.0.1.
- Java 17 tabanı ve güncellenmiş bağımlılıklar; Windows dağıtımı GitHub Actions ile oluşturulur.
- Yavaş bağımlılık/lisans raporları ayrı Maven profillerine taşındı.
- Otomatik yedek alınmadığı açıklandı; ilk hesap parolaları uygulama klasöründeki dosyada üretilir.

### Fixed

- Paylaşımlı düğümlerin gösterim, taşıma, çoğaltma, silme, oluşturma ve export hiyerarşileri.
- Düğüm silindikten sonra komşu düğüme geçiş; eksik düğümün oturum süresi dolmuş gibi gösterilmesi.
- Veri kaynağı kapanırken dosya kilidinin bırakılması; silinmiş/eski tenant seçiminde anlaşılır dönüş.
- Mobil/masaüstü toolbar düzeni, bookmark konumları, resimli ikon seçimi ve arama formunda yinelenen filtreler.
- Güvenilir proxy üzerinden HTTPS/çerez davranışı, hassas ayar değerlerinin gösterilmemesi ve sade hata/404 sayfaları.

### Known limitations

- SweetCherry otomatik yedek almaz. İki uygulamadan aynı CTB'ye eşzamanlı yazmayın.
- İçerik araması saklanan raw metinde çalışır; POST aramasından sonra tarayıcı geri hareketi formun yeniden gönderilmesini isteyebilir.
- CTB export sırasında metindeki düğüm bağlantılarının kimlikleri yeniden yazılmaz.
- Kod kutusu düzenleme ve ek editör iyileştirmeleri sonraki çalışmalardır.
- macOS scriptleri fiziksel Mac üzerinde doğrulanmamıştır.
- Üçüncü taraf lisans/atıf envanterinin tamamlanması ayrı yayın hazırlık maddesidir.
