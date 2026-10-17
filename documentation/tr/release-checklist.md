# SweetCherry 1.1.0 yayımlama adımları

Kaynak doğrulaması ile ürün paketi doğrulaması ayrı adımlardır. Kullanıcı son kaynakta **393 başarılı test** ve SQLite, MariaDB, PostgreSQL manuel kontrolleri bildirmiştir. Bu kayıt henüz oluşturulmamış 1.1.0 Windows installer'ının test edildiği anlamına gelmez. Bu hazırlık yeni uygulama özelliği eklemez.

## 1. Hazırlık yaması ve görüntüler

Yayın hazırlığı pom.xml sürümünü 1.1.0 yapar, changelog/kurulum/yayın/lisans açıklamalarını günceller. Lisans dosyaları normal paketin içine girer; ayrıca küçük notices ZIP oluşturulur.

```bat
git apply --check sweetcherry-4190204-release-1.1.0.patch
git apply sweetcherry-4190204-release-1.1.0.patch
git diff --check
mvnw.cmd clean package
```

1.1.0 yayınına ekran görüntüsü eklenmeyecek; bu yayın için gerekli değildir. Ekran görüntüleri isteğe bağlıdır. Eklemek istiyorsanız etiketi oluşturmadan önce [çekim listesi](../images/1.1/README.md) içindeki dosya adlarıyla ekleyin, gerçek dosyalar mevcut olduğunda README'ye bağlantı koyun. Olmayan görüntüler için bozuk link bırakmayın. Parola, kişisel not, dosya yolu ve sunucu adresi göstermeyin.

Değişiklikleri main dalına commit/push edin. Geçici yorum arşivi 1.0.0 öncesinde zaten yedeklenmişti; tekrar silinecek dosya yoktur. Eski lisans tamamlama workflow'u 1.0.0 taslağına özeldir; **1.1.0 için çalıştırmayın**.

## 2. Yayın commit'i ve etiket

Pushed yayın commit'inin kısa kimliğini bulun. Aşağıdaki yer tutucuyu gerçek commit ile değiştirin:

```bat
git tag -a v1.1.0 RELEASE_COMMIT -m "SweetCherry 1.1.0"
git push origin v1.1.0
```

Cmd Git authentication çalışmıyorsa önceki gibi IntelliJ Git araç penceresi → Tags → v1.1.0 → Push to origin kullanın. Etiketi kaynak/README/görüntüler tamamlandıktan sonra oluşturun. Yayımlanmış v1.0.0 etiketini taşımayın; 1.1.0 etiketini de yayımlandıktan sonra değiştirmeyin.

## 3. Windows paketleme

Actions → **Windows x64 bundled distribution** → Run workflow → **v1.1.0** seçin. Main'in son halinden değil, bu etiketten paketleyin. CLI alternatifi:

```bash
gh workflow run windows-bundle.yml --ref v1.1.0
```

Etiket gönderimi kendiliğinden Release yayımlamaz. Görev mevcut testleri, paketlemeyi ve Java kurulu olmadan installer/launcher smoke testini çalıştırır.

Artifact `SweetCherry-windows-x64-java17` içinden şu dosyaları alın; dış artifact ZIP'ini release asset'i olarak yüklemeyin:

| Konum | Release asset'i |
| --- | --- |
| `dist/windows/SweetCherry-1.1.0-windows-x64-setup.exe` | Installer |
| `dist/windows/SweetCherry-1.1.0-windows-x64.zip` | Portable ZIP |
| `dist/windows/SweetCherry-1.1.0-notices.zip` | Lisans/atıf ZIP'i |
| `dist/windows/SHA256SUMS.txt` | Üç ZIP/EXE ürün dosyasının checksum'ları |
| `release/SweetCherry/windows-bundle.json` veya portable paketin içindeki aynı dosya | Sürüm/runtime/commit manifest'i |

Gerekirse artifact içinde windows-bundle.json dosyasını adına göre bulun. Dosya adları 1.1.0 olmalı; SNAPSHOT veya 1.0.0 paketini karıştırmayın. Manifest'teki commit v1.1.0 commit'iyle aynı olmalı.

Son paketi ayrı, temiz bir klasörde açın. Launcher başlangıcı, giriş, demo açma, durdurma/yeniden başlatma ve ayar editörü erişimini kısaca kontrol edin. Kendi CTB'lerinizi/parolanızı yayın paketine koymayın. Pakette `THIRD-PARTY-NOTICES.md`, `licenses/`, runtime lisansları ve kılavuzların bulunduğunu doğrulayın. Checksum publisher imzası değildir; EXE'nin dijital imzası yoktur.

## 4. GitHub Release taslağı

1. Releases → Draft a new release.
2. Mevcut v1.1.0 etiketi; başlık SweetCherry 1.1.0.
3. [İngilizce release açıklaması](../releases/v1.1.0.md) içeriğini kullanın.
4. Yukarıdaki beş asset'i yükleyin. GitHub Source code ZIP/tar.gz dosyalarını kendisi ekler; toplamda yedi asset görünür.
5. Stable olarak yayımlayacağımız için prerelease işaretlemeyin; Latest seçilebilir.
6. Görev ve son paket kontrolü başarılı olduktan sonra Publish release.

Bu belge veya yama yayın işlemini otomatik yapmaz. Taslağı/paketleri görmeden yayımlanmış saymayın.

## 5. Yayın sonrası

README'deki `/releases/latest` bağlantısı son kararlı sürüme gider. İsterseniz geliştirmeye devam ederken main sürümünü 1.1.1-SNAPSHOT yapın; aynı tag/paket değiştirilmez. Henüz ayrı nightly/development download kanalı oluşturulmadı. Yeni özellikler, VPS demo ve geniş güvenlik incelemesi sonraki işlerdir; 1.1.0 için yeni feature hedefi değildir.
