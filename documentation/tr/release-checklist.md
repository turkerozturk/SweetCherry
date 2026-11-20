# SweetCherry 1.12.0 yayın adımları — 20 Kasım 2026

Sürüm **1.12.0**, etiket **v1.12.0**. Yama `ca19797` üzerine resim galerisinde kaynak bilgilerini ve release hazırlığını ekler. Bu dosya komutları tarif eder; kendiliğinden commit, push, etiket veya yayın oluşturmaz. Java tabanı **17** kalır.

## 1. Uygula, derle ve kontrol et

Çalışan SweetCherry örneğini durdurun. Yamayı depo dışında tutun; aşağıdaki örnekte depo klasörünün yanındadır. Temiz `main` üzerinde:

```bat
git switch main
git pull --ff-only
git apply --check ..\sweetcherry-ca19797-gallery-info-release-1.12.0.patch
git apply ..\sweetcherry-ca19797-gallery-info-release-1.12.0.patch
git diff --check
mvnw.cmd clean package
```

Node.js 24 ile Git Bash üzerinde (npm install gerekmez):

```bash
node --test src/test/js/*.test.cjs
```

Önceki feature commit’i için bildirilen 1079 test, sıfır başarısızlık/hata ve iki atlanan test bu hazırlık commit’inin veya final paketlerin testi değildir. Hazırlık ortamında önbellekteki bağımlılıklarla 27 hedefli Java testi ve 292 JavaScript testi geçti; tam Maven çalışması Maven Central DNS erişimi nedeniyle başlayamadı. Tarayıcıda manuel doğrulama yapılmadı. Bu kontroller tam Maven paketlemesinin yerine geçmez.

Kopya CTB ile resim bilgilerini kontrol edin: 1600×800 resim, 320×160 önizleme gösterilse bile 1600×800 px ve 1.28 MP yazmalı. PNG/JPEG, alfa kanalı olan/olmayan, bozuk/desteklenmeyen resim, bilgi göster/gizle, büyük görüntüleyici ve hızlı ileri/geri geçişlerini deneyin. Renk derinliği kaynak okuyucuya bağlıdır; alfa kanalı bilgisi fiilen saydam piksel bulunduğu anlamına gelmez. DPI tahmini yapılmaz. USER erişimi ve eski CTB sekmesi engelini de kontrol edin.

Ayrıca inline linked content/PDF, split view, kayıtlı arama dalları, web bookmark, başlık düzenleme, klavye gezinmesi, sıralama ve otomatik yedek zamanlamasını deneyin. Çizimde önizleme/iptal/onay, undo/redo ve özgün resmi indirme kontrolü yapın. Desteklenen bir JPEG bulunursa dönüşüm ve yön bilgisini de deneyin. Türkçe/İngilizce, dar ekran ve koyu temayı kontrol edin.

`release/SweetCherry/documentation/releases/v1.12.0.md` oluşmalı. Değişiklikleri gözden geçirip yalnız yamaya ait dosyaları stage edin; sonra:

```bat
git diff --check
git diff --cached --stat
git commit -m "Prepare SweetCherry 1.12.0 release"
git push origin main
```

CI’de hazırlık commit’inin Windows/Linux Java ve JavaScript adımlarını doğrulayın. Depo kuralları PR gerektiriyorsa değişiklikleri PR ile birleştirin.

## 2. Yeni hazırlık commit’ini etiketle

Çalışma ağacı temiz ve CI başarılı olmalı. Etiket `ca19797` yerine yeni hazırlık commit’ini göstermeli:

```bat
git status --short
git log -1 --oneline
git tag -l v1.12.0
```

Etiket henüz yoksa:

```bat
git tag -a v1.12.0 -m "SweetCherry 1.12.0"
git push origin v1.12.0
```

Yayımlanmış etiketleri taşımayın; force push kullanmayın.

## 3. Paketleri etiketten üret

Actions → **Portable distribution (Windows builder)** → **Run workflow** → **v1.12.0**. Başarılı görevden **sweetcherry-release-assets** artifact’ini indirin ve dış ZIP’i açın. Ürün asset’leri:

- `sweetcherry_1.12.0_all_os_portable.zip`
- `sweetcherry_1.12.0_all_os_portable_with_java_and_setup.exe`
- `sweetcherry-1.12.0-third_party_licences.zip`
- `sha256sums.txt`

Java’sız ZIP’te `runtime/` olmamalı; setup klasöründe Windows runtime bulunmalı. `build-info.json` içinde sürüm **1.12.0**, `packageRuntimeIncluded` ZIP için **false**, setup için **true** olmalı. Commit alanını `git rev-parse "v1.12.0^{commit}"` ile karşılaştırın. README, CHANGELOG, SECURITY.md, release/kurulum belgeleri ve lisanslar paketlerde bulunmalı. Bu yama bağımlılık eklemez; final JAR için `python packaging/update-jar-notices.py --check` yine geçmeli.

## 4. Checksum ve final paketler

Dört dosyanın olduğu klasörde PowerShell:

```powershell
Get-Content .\sha256sums.txt | ForEach-Object {
    if ($_ -match '^([0-9a-fA-F]{64})\s+(.+)$') {
        $expected = $matches[1]
        $file = $matches[2]
        if ((Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash -ne $expected) {
            throw "SHA-256 mismatch: $file"
        }
        Write-Host "OK: $file"
    } else { throw "Invalid checksum line: $_" }
}
```

İki paketi ayrı temiz, yazılabilir klasörlerde kopya/demo CTB ile açın. Java’sız paketi sistem Java 17+ ile, setup paketini kendi runtime’ıyla deneyin. Launcher, login, CTB seçimi, kapatma/yeniden başlatma, resim bilgileri ve yukarıdaki temel özellikleri kontrol edin. Yedek/geri yüklemeyi ayrı bir CTB kopyasında deneyin. Kullandığınız MariaDB/MySQL/PostgreSQL ve HTTPS/proxy bağlantılarını ayrıca kontrol edin. macOS/i386 için test edilmiş destek iddiası eklemeyin.

## 5. GitHub Release

Releases → **Draft a new release**:

- Tag: **v1.12.0**
- Title: **SweetCherry 1.12.0**
- Description: `documentation/releases/v1.12.0.md` içeriği; göreli belge bağlantılarını `https://github.com/turkerozturk/SweetCherry/blob/v1.12.0/documentation/...` adresleriyle değiştirin.
- Assets: yukarıdaki dört ürün dosyası.

Yamayı veya ayrı release-history metnini ürün asset’i olarak eklemeyin. Kontroller tamamlanınca **Set as a pre-release** kapalı, **Set as the latest release** açık → **Publish release**. Ardından indirme bağlantıları, SHA-256 ve etiket/commit eşleşmesini doğrulayın. Önceki release ve etiketleri değiştirmeyin.
