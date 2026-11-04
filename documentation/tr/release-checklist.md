# SweetCherry 1.7.0 taslak hazırlığı

1.6.0 sonrası yeni özellikler için sürüm **1.7.0**, etiket **v1.7.0** hazırlanır. Bu aşamada yayın yapılmaz. Önceki özellik commit’i `981839e` için kullanıcı 705 başarılı Java testi ve başarılı manuel PDF kontrolü bildirdi. Son hazırlık commit’inin paketleri ayrıca doğrulanır.

## Yamayı uygula ve hazırla

```bat
git switch main
git pull --ff-only
git apply --check sweetcherry-981839e-release-1.7.0.patch
git apply sweetcherry-981839e-release-1.7.0.patch
git diff --check
mvnw.cmd clean package
```

Önceki portable-packages yamasını ayrıca uygulamayın; bu yama aynı temel commit için tüm paketleme değişikliklerini içerir. Sürüm 1.7.0 olmalı. Paketleme ve belgeler değişir; uygulama özellik kodu değişmez. IntelliJ’den commit/push yapın. Daha geliştirme yapılacaksa etiketi ve paketleri son commit’e kadar erteleyin. Mevcut yayımlanmış etiketleri taşımayın.

## Son commit’ten paket üret

Hazır olduğunuzda temiz çalışma ağacında:

```bat
git status --short
git log -1 --oneline
git tag -a v1.7.0 -m "SweetCherry 1.7.0"
```

Etiketi IntelliJ ile veya `git push origin v1.7.0` komutuyla gönderin. GitHub → Actions → **Portable distribution (Windows builder)** → Run workflow → **v1.7.0** seçin. Sadece deneme için main’den çalıştırılabilir; yayın paketi son etiketten üretilmelidir. Eski 1.0.0 notices repack görevini kullanmayın.

Workflow Java 17 ile Maven test/paketleme, Java’sız ZIP içerik karşılaştırması ve sistem Java’sı olmadan Windows setup/launcher kontrolü yapar. **sweetcherry-release-assets** artifact’ini indirip dış ZIP’i açın.

## Dört asset

| Dosya | Amaç |
| --- | --- |
| `sweetcherry_1.7.0_all_os_portable.zip` | Java’sız uygulama, tüm OS scriptleri |
| `sweetcherry_1.7.0_all_os_portable_with_java_and_setup.exe` | Windows x64 kurucu ve Java |
| `sweetcherry-1.7.0-third_party_licences.zip` | Her iki pakette de bulunan lisans belgelerinin ek kopyası |
| `sha256sums.txt` | Yukarıdaki üç dosyanın SHA-256 değerleri |

Hepsi artifact içindeki `dist/windows` klasöründedir. Ayrı JSON asset yüklenmez. GitHub’ın otomatik iki Source code bağlantısı ayrıca görünür.

ZIP içinde `runtime/` olmamalı; `build-info.json` içindeki `packageRuntimeIncluded` false olmalı. Setup’ın açtığı uygulama klasöründe runtime vardır ve aynı alan true olur. Her iki manifest’in `applicationVersion` değeri 1.7.0 ve `commit` değeri aşağıdakiyle aynı olmalı:

```bat
git rev-parse "v1.7.0^{commit}"
```

Ürün dosyalarının yanında PowerShell açıp özetleri doğrulayın:

```powershell
Get-Content .\sha256sums.txt | ForEach-Object {
    if ($_ -notmatch '^([0-9a-fA-F]{64})\s+(.+)$') { throw "Invalid checksum line: $_" }
    $expected = $matches[1]
    $file = $matches[2]
    if ((Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash -ne $expected) {
        throw "SHA-256 mismatch: $file"
    }
    Write-Host "OK: $file"
}
```

Java’sız ZIP’i temiz klasöre çıkarıp uygun Java 17+ ile Windows’ta `run.bat`, Linux/Raspberry Pi’de `sh ./run.sh` veya `sh ./launcher.sh` deneyin. ZIP’teki `SweetCherry.exe` runtime ister; Java’sız pakette run.bat kullanılır. Kurucuyu Windows x64 üzerinde ayrıca deneyin. Giriş, demo seçimi ve kapatma yeterli bir başlangıç kontrolüdür; yeni özellikleri önceki kontrollerin üstüne değişiklik yapıldıysa yeniden kontrol edin. macOS ve 32 bit/i386 için denenmedi bilgisini koruyun.

`LICENSE`, `THIRD-PARTY-NOTICES.md`, `licenses/`, örnek CTB/tenant dosyaları ve scriptler pakette olmalı. Kullanılmış uygulama klasörünüzü, özel CTB/parola/ayarlarınızı paketlemeyin.

## Draft kaydet

GitHub → Releases → Draft a new release. Mevcut son etiketi ve **SweetCherry 1.7.0** başlığını seçin. Açıklamaya ayrıca verilen kısa Markdown metnini yapıştırın; uzun [v1.7.0.md](../releases/v1.7.0.md) dosyasının tamamını yapıştırmayın. Dört asset’i ekleyip **Save draft** kullanın. Yayına kadar tarih, son değişiklikler, test bilgisi ve bağlantıların son etikete ait olduğunu tekrar kontrol edin.

Henüz etiket oluşturmak istemiyorsanız şimdilik açıklamayı hazırlayıp yerel saklayın veya GitHub’ın hedef commit/new tag alanlarını taslakta bırakın; yayın öncesi etiketi son commit’e sabitleyin. Taslaktayken main’de gelişen kod, önceden yüklenmiş paketleri kendiliğinden güncellemez. Yeni commit eklendiyse son etiketten yeniden derleyip tüm dört asset’i ve açıklamayı birlikte yenileyin. Yayımlanmış etiketi veya paketleri sessizce değiştirmeyin.
