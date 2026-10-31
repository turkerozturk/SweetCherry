# SweetCherry 1.5.0 yayımlama adımları

1.4.0 sonrası uyumlu yeni özellikler için sürüm **1.5.0**, etiket **v1.5.0**.
Son özellik commit’i `6b9f558491e374e125cc6c494a7d587f73eb7982` için kullanıcı 611 hatasız Java testi, bookmarks ve yeni ikonlar için başarılı manuel kontrol bildirdi. Önceki arama/gezinme geliştirmeleri de manuel olarak doğrulandı. Etiket workflow sonucu ayrıca kontrol edilir.
Etiket aşağıdaki **yeni sürüm hazırlığı commit’ine** konur; özellik commit’ine veya önceki sürüme konmaz.

## 1. Hazırlık yamasını uygula ve derle

```bat
git switch main
git pull --ff-only
git apply --check sweetcherry-6b9f558-release-1.5.0.patch
git apply sweetcherry-6b9f558-release-1.5.0.patch
git diff --check
mvnw.cmd clean package
```

Proje sürümü 1.5.0 görünmeli, testler geçmeli. Bu yama yalnız proje sürümü,
README, changelog, kurulum/yayın belgelerini günceller; uygulama kodu, bağımlılık sürümü veya yeni test içermez.
Yeni ekran görüntüsü yayın şartı değildir; mevcut görsel korunur.

IntelliJ’den `Prepare SweetCherry 1.5.0 release` mesajıyla **commit ve push** yapın.
Önceki başarılı IntelliJ kimlik doğrulama yönteminizi kullanabilirsiniz.

## 2. Yeni commit’i etiketle

Commit/push sonrasında:

```bat
git status --short
git log -1 --oneline
git tag -a v1.5.0 -m "SweetCherry 1.5.0"
```

İlk komutun çıktısı boş, son commit sürüm hazırlığı commit’i olmalı.
IntelliJ → **Git araç penceresi → Tags → v1.5.0 → sağ tık → Push to origin**.
Komut satırı kimlik doğrulaması çalışıyorsa alternatif `git push origin v1.5.0`.
İkisini birden yapmak gerekmez. Eski yayımlanmış etiketlere dokunmayın.

## 3. Windows paketini etiketten üret

GitHub → **Actions → Windows x64 bundled distribution → Run workflow → v1.5.0 → Run workflow**.
Seçim **main değil v1.5.0** olmalı. Yeni sürüm hazırlığından önce üretilmiş main paketini kullanmayın; sürüm/commit farklı olabilir.

Workflow Java 17 ile test/paketleme yapar ve sistem Java olmadan kurucu/launcher kontrollerini çalıştırır.
Başarılı olunca **SweetCherry-windows-x64-java17** artifact’ini indirip dış ZIP’i açın.
**Complete 1.0.0 draft license notices** görevini çalıştırmayın; eski sürüme özeldir.
Normal paketleme mevcut lisans bildirimlerini, mevcut kütüphane, font ve ikon bildirimleri dahil, zaten kopyalar.

## 4. Son paket kontrolü

Release’e dış artifact ZIP’i yerine şu beş dosyayı ekleyin:

| Dosya | Amaç |
| --- | --- |
| `dist/windows/SweetCherry-1.5.0-windows-x64-setup.exe` | Windows kurucu |
| `dist/windows/SweetCherry-1.5.0-windows-x64.zip` | Portable uygulama |
| `dist/windows/SweetCherry-1.5.0-notices.zip` | Lisans ve atıflar |
| `dist/windows/SHA256SUMS.txt` | Üç ZIP/EXE ürün dosyasının SHA-256 değerleri |
| `release/SweetCherry/windows-bundle.json` | Sürüm, commit ve Java bilgisi |

Manifest’te `applicationVersion` **1.5.0** olmalı; `commit` şu komutla aynı olmalı:

```bat
git rev-parse "v1.5.0^{commit}"
```

SHA-256 değerlerini karşılaştırmak için artifact’in `dist/windows` klasöründe PowerShell açın:

```powershell
Get-Content .\SHA256SUMS.txt | ForEach-Object {
    if ($_ -match '^([0-9a-fA-F]{64})\s+\*?(.+)$') {
        $expected = $matches[1]
        $file = $matches[2]
        if ((Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash -ne $expected) {
            throw "SHA-256 mismatch: $file"
        }
        Write-Host "OK: $file"
    }
}
```

Portable ZIP’i temiz bir klasöre açıp launcher, giriş, demo açma ve durdurmayı kısaca deneyin.
Hızlı geçiş, gelişmiş arama ve yeni bir ikonun görünümünü kısaca kontrol edin. Linux/Windows ilk açılış kolaylıkları için yeniden kapsamlı test listesi çıkarmak gerekmez.
Tüm manuel test listesini baştan tekrarlamak gerekmez. `THIRD-PARTY-NOTICES.md`, `licenses/`,
runtime ve iki hızlı başlangıç CTB’si pakette bulunmalı. Kullanılmış uygulama klasörünüzü,
kendi not/ayar/parola dosyalarınızı veya saved-filters klasörünüzü ZIP’leyerek yüklemeyin.

## 5. GitHub Release oluştur

GitHub → **Releases → Draft a new release**:

1. Tag: mevcut **v1.5.0**.
2. Title: **SweetCherry 1.5.0**.
3. Description: [documentation/releases/v1.5.0.md](../releases/v1.5.0.md) dosyasının Markdown içeriği.
4. Yukarıdaki beş dosyayı yükleyin; hepsinin yüklenmesini bekleyin.
5. **Set as a pre-release:** işaretlemeyin. **Set as the latest release:** seçin.
6. İsterseniz **Save draft** ile kontrol edin; hazır olunca **Publish release**.

GitHub’ın otomatik eklediği iki Source code arşiviyle toplam **yedi asset** normaldir.
README’nin `/releases/latest` bağlantısı yeni yayına gider; ek commit gerekmez.
Yayımlanan etiketi taşımayın ve aynı sürüm paketlerini farklı derlemeyle sessizce değiştirmeyin.
Sonraki uyumlu düzeltme 1.5.1, yeni özellik yayını 1.6.0 olabilir; nightly kanalı şart değildir.
