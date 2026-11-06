# SweetCherry 1.7.0 yayın adımları — 6 Kasım 2026

Sürüm **1.7.0**, etiket **v1.7.0**. Son temel commit `23027be` (demo/quick-start tenant ayarları); son özellik commit’i `9a5b715`. Kullanıcı 710 başarılı Java testi ve yeni özellikler için başarılı manuel kontroller bildirdi. Bu yama Türkçe Zengin Metin etiketini ve release/kurulum belgelerini günceller; sürüm zaten 1.7.0’dır.

## Yamayı uygula ve hazırla

```bat
git switch main
git pull --ff-only
git apply --check sweetcherry-23027be-release-1.7.0-final.patch
git apply sweetcherry-23027be-release-1.7.0-final.patch
git diff --check
mvnw.cmd clean package
```

Eski hazırlık yamalarını tekrar uygulamayın. `git diff --check` ve derleme başarılı olmalı. Türkçe dilde düğüm içerik düzenleme menüsünde **Zengin Metin**, editör başlığında **Zengin Metin düzenle** göründüğünü kontrol edin. İngilizce karşılıkları değişmez.

IntelliJ’den **Finalize SweetCherry 1.7.0 release notes and Turkish rich-text label** mesajıyla commit/push yapın. Aşağıdaki etiket **bu yeni son commit’e** konmalı; `23027be` veya eski bir özellik commit’ine konmamalı.

## Son commit’ten paket üret

Hazır olduğunuzda temiz çalışma ağacında aşağıdakileri kontrol edin. `git status --short` boş olmalı, `git log -1` az önce push edilen son hazırlığı göstermeli. `git tag -l v1.7.0` doluysa aşağıdaki tag oluşturma komutunu çalıştırmayın: mevcut etiketin commit’ini kontrol edin, yayımlanmış etiketi taşımayın.

```bat
git tag -l v1.7.0
```

Etiket henüz yoksa:

```bat
git status --short
git log -1 --oneline
git tag -a v1.7.0 -m "SweetCherry 1.7.0"
```

Etiketi IntelliJ ile veya `git push origin v1.7.0` komutuyla gönderin. GitHub → Actions → **Portable distribution (Windows builder)** → Run workflow → **v1.7.0** seçin. Sadece deneme için main’den çalıştırılabilir; yayın paketi son etiketten üretilmelidir. Eski 1.0.0 notices repack görevini kullanmayın.

Workflow’un başarılı bittiğini doğrulayın; yalnız normal CI başarısı paketleme doğrulaması değildir. Workflow Java 17 ile Maven test/paketleme, Java’sız ZIP içerik karşılaştırması ve sistem Java’sı olmadan Windows setup/launcher kontrolü yapar. **sweetcherry-release-assets** artifact’ini indirip dış ZIP’i açın.

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

## Taslak kaydet ve son kontrol

GitHub → Releases → Draft a new release. Mevcut son etiketi ve **SweetCherry 1.7.0** başlığını seçin. Açıklamaya ayrıca verilen kısa Markdown metnini yapıştırın; uzun [v1.7.0.md](../releases/v1.7.0.md) dosyasının tamamını yapıştırmayın. Dört asset’i ekleyip **Save draft** kullanın. Yayına kadar tarih, son değişiklikler, test bilgisi ve bağlantıların son etikete ait olduğunu tekrar kontrol edin.

Henüz etiket oluşturmak istemiyorsanız şimdilik açıklamayı hazırlayıp yerel saklayın veya GitHub’ın hedef commit/new tag alanlarını taslakta bırakın; yayın öncesi etiketi son commit’e sabitleyin. Taslaktayken main’de gelişen kod, önceden yüklenmiş paketleri kendiliğinden güncellemez. Yeni commit eklendiyse son etiketten yeniden derleyip tüm dört asset’i ve açıklamayı birlikte yenileyin. Yayımlanmış etiketi veya paketleri sessizce değiştirmeyin.

## Yayımla

Son paketler ve taslak kontrol edildikten sonra GitHub Release ekranında:

1. Tag **v1.7.0**, başlık **SweetCherry 1.7.0** olmalı; kısa açıklamanın tarihini fiili yayın gününe göre güncelleyin.
2. Dört yüklenen asset doğru sürüm/commit’e ait olmalı. Ayrı `build-info.json` veya workflow’un dış artifact ZIP’ini yüklemeyin.
3. **Set as a pre-release** işaretli olmasın. Kararlı yayın olarak **Set as the latest release** seçin.
4. Hazır olduğunuzda **Publish release** seçin. Yayın sayfasındaki kılavuz ve ayrıntılı sürüm notu bağlantılarını açıp kontrol edin.

Bu hazırlık kendi başına tag veya release oluşturmaz. Yayımlanan etiketi taşımayın; sonraki değişiklikleri yeni sürümde yayımlayın.
