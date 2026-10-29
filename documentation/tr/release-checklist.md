# SweetCherry 1.4.0 yayımlama adımları

1.3.0 sonrası uyumlu yeni özellikler için sürüm **1.4.0**, etiket **v1.4.0**.
Son özellik commit’i `39003c72e32b98ec938e8f1dbc83ddd8b2c3c044` için kullanıcı manuel kontrolleri ve 572 hatasız yerel Java testi bildirdi. Etiket workflow sonucu ayrıca kontrol edilir.
Etiket aşağıdaki **yeni sürüm hazırlığı commit’ine** konur; özellik commit’ine veya önceki sürüme konmaz.

## 1. Hazırlık yamasını uygula ve derle

```bat
git switch main
git pull --ff-only
git apply --check sweetcherry-39003c7-release-1.4.0.patch
git apply sweetcherry-39003c7-release-1.4.0.patch
git diff --check
mvnw.cmd clean package
```

Proje sürümü 1.4.0 görünmeli, testler geçmeli. Bu yama yalnız proje sürümü,
README, changelog, kurulum/yayın belgelerini günceller; uygulama kodu, bağımlılık sürümü veya yeni test içermez.
Yeni ekran görüntüsü yayın şartı değildir; mevcut görsel korunur.

IntelliJ’den `Prepare SweetCherry 1.4.0 release` mesajıyla **commit ve push** yapın.
Önceki başarılı IntelliJ kimlik doğrulama yönteminizi kullanabilirsiniz.

## 2. Yeni commit’i etiketle

Commit/push sonrasında:

```bat
git status --short
git log -1 --oneline
git tag -a v1.4.0 -m "SweetCherry 1.4.0"
```

İlk komutun çıktısı boş, son commit sürüm hazırlığı commit’i olmalı.
IntelliJ → **Git araç penceresi → Tags → v1.4.0 → sağ tık → Push to origin**.
Komut satırı kimlik doğrulaması çalışıyorsa alternatif `git push origin v1.4.0`.
İkisini birden yapmak gerekmez. Eski v1.0.0/v1.1.0/v1.2.0 etiketlerine dokunmayın.

## 3. Windows paketini etiketten üret

GitHub → **Actions → Windows x64 bundled distribution → Run workflow → v1.4.0 → Run workflow**.
Seçim **main değil v1.4.0** olmalı. Önceki main paketi hâlâ 1.3.0 sürümünü içerir; onu yüklemeyin.

Workflow Java 17 ile test/paketleme yapar ve sistem Java olmadan kurucu/launcher kontrollerini çalıştırır.
Başarılı olunca **SweetCherry-windows-x64-java17** artifact’ini indirip dış ZIP’i açın.
**Complete 1.0.0 draft license notices** görevini çalıştırmayın; eski sürüme özeldir.
Normal paketleme mevcut lisans bildirimlerini, Flexmark ve mevcut font bildirimleri dahil, zaten kopyalar.

## 4. Son paket kontrolü

Release’e dış artifact ZIP’i yerine şu beş dosyayı ekleyin:

| Dosya | Amaç |
| --- | --- |
| `dist/windows/SweetCherry-1.4.0-windows-x64-setup.exe` | Windows kurucu |
| `dist/windows/SweetCherry-1.4.0-windows-x64.zip` | Portable uygulama |
| `dist/windows/SweetCherry-1.4.0-notices.zip` | Lisans ve atıflar |
| `dist/windows/SHA256SUMS.txt` | Üç ZIP/EXE ürün dosyasının SHA-256 değerleri |
| `release/SweetCherry/windows-bundle.json` | Sürüm, commit ve Java bilgisi |

Manifest’te `applicationVersion` **1.4.0** olmalı; `commit` şu komutla aynı olmalı:

```bat
git rev-parse "v1.4.0^{commit}"
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
İsterseniz tek bir Markdown önizlemesiyle pakette yeni kütüphanenin bulunduğunu da görün.
Tüm manuel test listesini baştan tekrarlamak gerekmez. `THIRD-PARTY-NOTICES.md`, `licenses/`,
runtime ve iki hızlı başlangıç CTB’si pakette bulunmalı. Kullanılmış uygulama klasörünüzü,
kendi not/ayar/parola dosyalarınızı ZIP’leyerek yüklemeyin.

## 5. GitHub Release oluştur

GitHub → **Releases → Draft a new release**:

1. Tag: mevcut **v1.4.0**.
2. Title: **SweetCherry 1.4.0**.
3. Description: [documentation/releases/v1.4.0.md](../releases/v1.4.0.md) dosyasının Markdown içeriği.
4. Yukarıdaki beş dosyayı yükleyin; hepsinin yüklenmesini bekleyin.
5. **Set as a pre-release:** işaretlemeyin. **Set as the latest release:** seçin.
6. İsterseniz **Save draft** ile kontrol edin; hazır olunca **Publish release**.

GitHub’ın otomatik eklediği iki Source code arşiviyle toplam **yedi asset** normaldir.
README’nin `/releases/latest` bağlantısı yeni yayına gider; ek commit gerekmez.
Yayımlanan etiketi taşımayın ve aynı sürüm paketlerini farklı derlemeyle sessizce değiştirmeyin.
Sonraki uyumlu düzeltme 1.4.1, yeni özellik yayını 1.5.0 olabilir; nightly kanalı şart değildir.
