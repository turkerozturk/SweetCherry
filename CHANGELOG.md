# Changelog

SweetCherry'deki kullanıcıya dönük önemli değişiklikler bu dosyada kaydedilir.

## [Unreleased]

### Added

- Windows, Linux ve macOS için ilk derleme, ayrı derleme ve çalıştırma scriptleri.
- Demo CTB veritabanı ve göreli yollu örnek tenant tanımı içeren çalıştırılabilir dağıtım klasörü.
- Normal düğüm ve alt ağaç için koleksiyona veya ayrı CTB'ye aktaran dört export seçeneği.
- Normal kullanımdan ayrılmış `operations` ve `experiments` profilleri.
- Türkçe kurulum, isteğe bağlı özellikler, CTB export ve yayım hazırlık belgeleri.
- Windows ve Linux üzerinde Java 17 ile test/paketleme yapan CI iş akışı.

### Changed

- Spring Boot 3.2.4 sürümünden 3.5.16 sürümüne yükseltildi.
- Yavaş lisans, bağımlılık raporu ve Asciidoctor görevleri isteğe bağlı Maven profillerine taşındı.
- Paketleme çıktısı `release/SweetCherry` altında toplandı; kullanıcı tarafından değiştirilmiş demo verileri korunuyor.
- Uygulama varsayılan olarak yalnızca yerel bilgisayardaki `127.0.0.1` adresinde dinliyor.

### Fixed

- GitHub kaynak ZIP'inde `.git` klasörü bulunmadığında Maven derlemesinin başarısız olması önlendi.
- İlk girişte service-worker dosyasına yönlenme ve tenant seçilmeden veri tabanı gerektiren sayfalardaki teknik hatalar giderildi.
- CTB export işlemleri `POST/Redirect/GET` akışına geçirildi; tarayıcı geri/ileri hareketinin export'u tekrarlaması önlendi.
- Export klasörünün ilk işlemden önce oluşturulması ve export dosyalarının güvenli silme onayı düzeltildi.
- Image ve Anchor birleşik kimlik eşlemeleri ayrı kimlik sınıflarıyla düzeltildi.

### Known limitations

- Alias/shared node, node bağlantısı ve anchor referansları içeren exportlar deneysel durumdadır.
- macOS scriptleri hazırlanmıştır fakat henüz gerçek bir Mac üzerinde doğrulanmamıştır.
- Yerleşik hesaplar yalnızca yerel deneme amaçlıdır; uzak erişim için üretim tipi kimlik yönetimi sağlanmamaktadır.
