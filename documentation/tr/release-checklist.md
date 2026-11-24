# SweetCherry 1.14.0 yayın adımları — 25 Kasım 2026

Sürüm **1.14.0**, etiket **v1.14.0**. Hazırlık yaması `4de6d34` üzerine uygulanır. Java tabanı **17** ve CTB şeması korunur. Bu belge komutları tarif eder; commit, push, etiket veya yayın oluşturmaz.

## 1. Hazırlık yamasını uygula ve doğrula

Çalışan uygulamayı durdurun. Temiz ve güncel `main` üzerinde, yama depo klasörünün yanındaysa:

```bat
git switch main
git pull --ff-only
git apply --check ..\sweetcherry-4de6d34-release-1.14.0.patch
git apply ..\sweetcherry-4de6d34-release-1.14.0.patch
git diff --check
node --test src/test/js/*.test.cjs
python -m unittest discover -s packaging -p "test_*.py"
powershell -NoProfile -ExecutionPolicy Bypass -File packaging/windows/test-launcher-config.ps1
mvnw.cmd clean package
python packaging/verify-viewer-resources.py
python packaging/update-jar-notices.py --check
```

Python yalnız derleme/doğrulama ve isteğe bağlı eski filtre aktarımı için gerekir; uygulamayı çalıştırmak için gerekmez. Önceki feature/CI test sonuçları bu yeni hazırlık commit'inin ve son ürünlerin doğrulaması değildir.

Yerel EXE'yi de denemek için, paketleme sonrasında:

```bat
powershell -NoProfile -ExecutionPolicy Bypass -File packaging/windows/build-launcher.ps1
```

`release/SweetCherry/` altında JAR, yeni release notu, okuma düzeni/DTT/taşınabilirlik kılavuzları, `tools/migrate-saved-filters.py`, boş `saved-filters/` ve `backup/` klasörleri bulunmalı. Maven mevcut özel dosyaları korur. **Yayın paketi için ayrı, temiz checkout/staging kullanın:** kendi kayıtlı aramalarınızı, yedeklerinizi, kimlik bilgilerinizi veya özel resimlerinizi dağıtmayın. `python packaging/verify-viewer-resources.py --clean` yayın staging'inde geçmeli; kişisel dosyaları silerek staging temizlemeyin.

Kopya CTB ile şunları kontrol edin:

- Okuma düzeninde kompakt düğmeler ve ana menü/başlık/breadcrumbs/backlinks/toolbar/split kontrolleri; gizlenen alanlara erişim ve sayfa değiştirince tercihlerin korunması.
- Varsayılanı kapalı ağaç kimlik/tarih önekleri, padding ve created/modified biçimleri; hızlı ad+ikon kaydetme/iptal; shared/master ayrımı ve panoya kopyalama.
- Kopya veride tüm çöp kutusunu boşaltma ve dışarıda paylaşılan içeriklerin korunması.
- İsteğe bağlı DTT bağlantı testleri, tarih seçimi, durum/not kaydetme/silme, konu detayları, eksik konu ve tam katalog aktarımı; USER/ADMIN ve yazılabilirlik sınırları.
- Kayıtlı aramaları önce özgün konumda yeni sürümle açıp taşıma veya açık eski JSON aktarımı; mevcut hedef filtrelerin korunması.
- EXE'yi önce kendi runtime'ıyla, sonra runtime'sız ve uygun sistem Java 17+ ile başlatma; özel YAML HTTP portu ve yapılandırılmış HTTPS; port yoksa masaüstü formu.

Yalnız hazırlık yamasındaki dosyaları gözden geçirip stage edin:

```bat
git diff --check
git diff --cached --stat
git commit -m "Prepare SweetCherry 1.14.0 release"
git push origin main
```

Windows/Linux CI ve Windows PowerShell kontrolü başarılı olmalı. `git status --short` boş olmalı.

## 2. Hazırlık commit'ini etiketle

```bat
git status --short
git log -1 --oneline
git tag -l v1.14.0
```

Etiket henüz yoksa, yeni hazırlık commit'inde:

```bat
git tag -a v1.14.0 -m "SweetCherry 1.14.0"
git push origin v1.14.0
```

Yayımlanmış etiketi taşımayın; force push kullanmayın.

## 3. Paketleri etiketten üret

Actions → **Portable distribution (Windows builder)** → **Run workflow** → **v1.14.0**. Başarılı görevin **sweetcherry-release-assets** artifact'ini indirin. Ürünler:

- `sweetcherry_1.14.0_all_os_portable.zip`
- `sweetcherry_1.14.0_all_os_portable_with_java_and_setup.exe`
- `sweetcherry-1.14.0-third_party_licences.zip`
- `sha256sums.txt`

Workflow gerçek JAR'ı ve açılmış Java'sız ZIP'i doğrular; yerel viewer/font kaynaklarını, yeni kılavuzları, filtre aktarım aracını ve temiz özel dizinleri kontrol eder. Windows smoke testi kurucu dosya korumasını, runtime önceliğini, sistem Java'sıyla EXE başlatmayı ve YAML portunu kontrol eder. DTT ayrı uygulamadır; token/gerçek DTT adreslerini release staging'ine eklemeyin.

ZIP'te `runtime/` olmamalı; setup Windows Java runtime içermeli. `build-info.json` sürüm **1.14.0**, `packageRuntimeIncluded` ZIP için **false**, setup için **true** olmalı. Commit alanını `git rev-parse "v1.14.0^{commit}"` ile karşılaştırın. Lisans/atıflar iki pakette de bulunmalı.

## 4. Checksum ve son paket denemesi

Dört asset'in bulunduğu klasörde PowerShell:

```powershell
Get-Content .\sha256sums.txt | ForEach-Object {
    if ($_ -match '^([0-9a-fA-F]{64})\s+(.+)$') {
        $expected = $matches[1]; $file = $matches[2]
        if ((Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash -ne $expected) { throw "SHA-256 mismatch: $file" }
        Write-Host "OK: $file"
    } else { throw "Invalid checksum line: $_" }
}
```

İki ürünü ayrı, yeni ve yazılabilir klasörlerde demo/kopya CTB ile deneyin. Login, kaynak seçimi, yeniden başlatma ve yeni özellikleri kontrol edin. Aktarımda aynı kullanıcı/tenant ve göreli CTB yolu korunmalı; mutlak JDBC yollarını yeni konuma göre güncelleyin. Eski hash isimli filtre dosyalarının taşınması için [taşınabilirlik kılavuzunu](../en/portable-launching-and-saved-filters.md) izleyin.

Java'sız EXE'nin sistem Java'sıyla çalışması için bu release'in yeni launcher'ı gerekir. Linux/macOS için uygun script ve yerel Java kullanın. macOS/i386 için test edilmiş destek iddiası eklemeyin.

## 5. GitHub Release

Releases → **Draft a new release**:

- Tag: **v1.14.0**
- Title: **SweetCherry 1.14.0**
- Description: `documentation/releases/v1.14.0.md` içeriği; bağlantılar etikete bağlı mutlak GitHub adresleridir.
- Assets: yukarıdaki dört dosya; kaynak yaması ürün asset'i değildir.

Son kontroller tamamlanınca **Set as a pre-release** kapalı, **Set as the latest release** açık → **Publish release**. İndirme bağlantıları, etiket/commit eşleşmesi ve checksum'ları kontrol edin. Önceki release/etiketleri değiştirmeyin.
