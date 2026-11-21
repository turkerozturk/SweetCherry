# SweetCherry 1.13.0 yayın adımları — 21 Kasım 2026

Sürüm **1.13.0**, etiket **v1.13.0**. Hazırlık yaması `fa1f393` üzerine uygulanır. Java tabanı **17** ve CTB şeması korunur. Bu belge komutları tarif eder; kendiliğinden commit, push, etiket veya yayın oluşturmaz.

## 1. Uygula ve doğrula

Çalışan uygulamayı durdurun. Temiz `main` üzerinde, yama depo klasörünün yanındaysa:

```bat
git switch main
git pull --ff-only
git apply --check ..\sweetcherry-fa1f393-release-1.13.0.patch
git apply ..\sweetcherry-fa1f393-release-1.13.0.patch
git diff --check
mvnw.cmd clean package
python -m unittest discover -s packaging -p test_viewer_resources.py
python packaging/verify-viewer-resources.py
python packaging/update-jar-notices.py --check
```

Kaynak/paket doğrulama komutları Python 3 standart kütüphanesini kullanır; uygulamanın çalışması için Python gerekmez.

Git Bash / Node.js 24:

```bash
node --test src/test/js/*.test.cjs
```

Önceki feature commit'i için bildirilen **1135 test, sıfır başarısızlık/hata, iki atlanan test** yeni hazırlık commit'inin veya paketlerin doğrulaması değildir. Hazırlık ortamındaki sentetik JAR kontrolleri de gerçek Maven/installer çalışmasının yerine geçmez. Önce tüm yerel/CI sonuçlarını kontrol edin.

`release/SweetCherry/` içinde uygulama JAR'ı, `documentation/releases/v1.13.0.md`, EN/TR Draw.io ve ikon kılavuzları, `documentation/examples/` altındaki iki Draw.io örneği ve `drawio-images/README.md` + `images.properties` oluşmalı. Varsayılan eşleme örneğinin tüm satırları yorumdur; hazır şekiller/ikonlar ve fontlar JAR içindedir. Yeni bir ilk çalıştırma indirmesi yoktur.

Mevcut yerel `release/SweetCherry/drawio-images/images.properties` üzerinde değişikliğiniz varsa Maven hazırlığı bunu korumalı. Normal doğrulayıcı buna izin verir; yayın için `--clean` eklenince değişmiş eşleme veya ek/özel resim reddedilir. **Özel dosyaları silerek temizlemeyin:** yedekleyip ayrı, temiz checkout/staging alanında yayın paketi üretin.

Kopya CTB ile: çok sayfalı Draw.io, şekiller/metin renkleri, sayfa/web bağlantıları, pointer zoom, yatay fit, kayıp şekil göster/gizle ve yerel PNG/JPEG eşlemesi; başlık/rich text/map/graph/listelerde ikonlar; PDF ve dosya adı temizliği; editör son imleç, yakınlaştırma, Enter boşluğu, undo/redo, nesne koruması ve taslak kurtarmayı deneyin. Türkçe/İngilizce, koyu tema ve dar ekran kontrolü yapın. Yerel resim ayarı boşsa kapalı kalmalı; ayar/eşleme değişince restart gerekir.

Yalnız bu yamaya ait dosyaları gözden geçirip stage edin:

```bat
git diff --check
git diff --cached --stat
git commit -m "Prepare SweetCherry 1.13.0 release"
git push origin main
```

Hazırlık commit'i için Windows/Linux CI başarılı olmalı. `git status --short` boş olmalı.

## 2. Yeni hazırlık commit'ini etiketle

```bat
git status --short
git log -1 --oneline
git tag -l v1.13.0
```

Etiket henüz yoksa, yeni hazırlık commit'inde:

```bat
git tag -a v1.13.0 -m "SweetCherry 1.13.0"
git push origin v1.13.0
```

Yayımlanmış etiketi taşımayın; force push kullanmayın.

## 3. Paketleri etiketten üret

Actions → **Portable distribution (Windows builder)** → **Run workflow** → **v1.13.0**. Başarılı görevin **sweetcherry-release-assets** artifact'ini indirin. Ürün asset'leri:

- `sweetcherry_1.13.0_all_os_portable.zip`
- `sweetcherry_1.13.0_all_os_portable_with_java_and_setup.exe`
- `sweetcherry-1.13.0-third_party_licences.zip`
- `sha256sums.txt`

Workflow gerçek JAR'daki yerel Draw.io/CTB/Font Awesome kaynaklarını kontrol eder; Java'sız ZIP'i açıp aynı kontrolü tekrarlar. Kurucu smoke testi mevcut resim eşlemesini ve kullanıcı resmini koruduğunu kontrol eder. Dizin/örnek dosyaları oluşturulur; düzenlenmiş eşlemeler yayın staging'ine alınmaz.

ZIP'te `runtime/` olmamalı; setup Windows Java runtime içermeli. `build-info.json` sürüm **1.13.0**, `packageRuntimeIncluded` ZIP için **false**, setup için **true** olmalı. Commit alanını `git rev-parse "v1.13.0^{commit}"` ile karşılaştırın. Lisans/atıflar iki pakette de bulunmalı; final JAR için notice inventory kontrolü geçmeli.

## 4. Checksum ve final paket denemesi

Dört asset'in olduğu klasörde PowerShell:

```powershell
Get-Content .\sha256sums.txt | ForEach-Object {
    if ($_ -match '^([0-9a-fA-F]{64})\s+(.+)$') {
        $expected = $matches[1]; $file = $matches[2]
        if ((Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash -ne $expected) {
            throw "SHA-256 mismatch: $file"
        }
        Write-Host "OK: $file"
    } else { throw "Invalid checksum line: $_" }
}
```

İki ürünü ayrı, yeni ve yazılabilir klasörlerde demo/kopya CTB ile deneyin. Login, kaynak seçimi, yeniden başlatma ve yeni özellikleri kontrol edin. ZIP'i mevcut klasöre açarken dosya korumasını ZIP aracı belirler; yeni klasöre açıp YAML, tenant, CTB, kimlik bilgileri, filtreler ve isteğe bağlı `drawio-images` dosyalarını bilinçli taşıyın. Bu klasör CTB yedeğinin parçası değildir. Hazır shape/ikon/fontlar için ayrıca klasör taşımak gerekmez; JAR ile güncellenirler.

Java'sız ürünü uygun sistem Java 17+ ile, setup ürününü kendi Windows runtime'ıyla deneyin. macOS/i386 için test edilmiş destek iddiası eklemeyin. Kullanılan uzak veritabanı ve HTTPS/proxy kurulumlarını ayrıca kontrol edin.

## 5. GitHub Release

Releases → **Draft a new release**:

- Tag: **v1.13.0**
- Title: **SweetCherry 1.13.0**
- Description: `documentation/releases/v1.13.0.md` içeriği. Göreli belge bağlantılarını `https://github.com/turkerozturk/SweetCherry/blob/v1.13.0/documentation/...` biçimine çevirin.
- Assets: yukarıdaki dört ürün dosyası; yama veya özel release-history metni ürün asset'i değildir.

Kontroller tamamlanınca **Set as a pre-release** kapalı, **Set as the latest release** açık → **Publish release**. Sonrasında indirme bağlantıları, etiket/commit eşleşmesi ve checksum'ları kontrol edin. Önceki release/etiketleri değiştirmeyin.
