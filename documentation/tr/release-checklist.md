# SweetCherry 1.15.0 yayın adımları — 26 Kasım 2026

Sürüm **1.15.0**, etiket **v1.15.0**. Hazırlık yaması `9fa77d3` üzerine uygulanır. Java tabanı **17** ve CTB şeması korunur. Bu belge komutları tarif eder; commit, push, etiket veya yayın oluşturmaz.

> Mevcut `v1.15.0` etiketiyle metadata hatasını gideriyorsanız: `sweetcherry-f916655-temurin-metadata-fix.patch` düzeltmesini `main` üzerine uygulayıp commit/push edin; etiketi yeniden oluşturmayın. Aşağıdaki **3. bölümdeki main + source_ref** adımını kullanın. Eski release hazırlık yamasını ikinci kez uygulamayın.

## 1. Hazırlık yamasını uygula ve doğrula

Çalışan uygulamayı durdurun. Temiz ve güncel `main` üzerinde, yama depo klasörünün yanındaysa:

```bat
git switch main
git pull --ff-only
git apply --check ..\sweetcherry-9fa77d3-release-1.15.0.patch
git apply ..\sweetcherry-9fa77d3-release-1.15.0.patch
git diff --check
node --test src/test/js/*.test.cjs
python -m unittest discover -s packaging -p "test_*.py"
powershell -NoProfile -ExecutionPolicy Bypass -File packaging/windows/test-launcher-config.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File packaging/windows/test-runtime-policy.ps1
mvnw.cmd clean package
python packaging/verify-viewer-resources.py
python packaging/update-jar-notices.py --check
```

Python yalnız derleme/doğrulama ve isteğe bağlı eski filtre aktarımı için gerekir; uygulamayı çalıştırmak için gerekmez. Önceki feature/CI test sonuçları bu yeni hazırlık commit'inin ve son ürünlerin doğrulaması değildir.

Yerel EXE'yi de denemek için, paketleme sonrasında:

```bat
powershell -NoProfile -ExecutionPolicy Bypass -File packaging/windows/build-launcher.ps1
```

`release/SweetCherry/` altında JAR, `documentation/releases/v1.15.0.md`, editör/resim sınırları ve diğer kılavuzlar, `tools/migrate-saved-filters.py`, boş `saved-filters/` ve `backup/` klasörleri bulunmalı. Maven mevcut özel dosyaları korur. **Yayın paketi için ayrı, temiz checkout/staging kullanın:** kendi kayıtlı aramalarınızı, yedeklerinizi, kimlik bilgilerinizi veya özel resimlerinizi dağıtmayın. `python packaging/verify-viewer-resources.py --clean` yayın staging'inde geçmeli; kişisel dosyaları silerek staging temizlemeyin.

Kopya CTB ile şunları kontrol edin:

- `/tree` içinde düz metin/kod, Markdown, HTML ve rich text editörlerini açma; Save, Save and continue, Cancel ve önceki okuma düzenine dönüş.
- Her iki split bölmesinde bağımsız düzenleme; kaydetmeden düğüm değiştirme, Geri/İleri ve sağ bölmeyi kapatma onayları; kompakt düğmeler, undo/redo ve kurtarma.
- Menü alt gruplarını mouse, klavye ve dar ekranla açma; mevcut eylemlerin erişilebilirliği.
- Metin + yüklü resim + tablo/codebox seçimini kopyalama ve başka editöre yapıştırma; Allow object deletion ile Delete/Backspace, Kes, seçimin üzerine yapıştırma ve undo/redo.
- Web resimleri indirme onayını kabul/ret; WebP/GIF/JPG/JPEG/BMP/TIFF örnekleri, ilk kare/sayfanın PNG olması, 8/10'dan fazla küçük resim ekleme; atlanan resim sayısı ve hata durumunda seçimin korunması.
- Mevcut kayıtlı ekli dosya/anchor kopyalamada metin yer tutucusu ve Kes'in seçimi koruması; salt okunur ve USER yazma korumaları.
- EXE, YAML portları, sistem Java fallback ve önceki CTB okuma işlevleri için kısa kullanım kontrolü.

Java tabanı 17 olarak kalır; **paket runtime'ı en az 17.0.21.1** olmalıdır. Workflow güncel Temurin Windows x64 JRE'yi indirir, arşiv SHA-256'sını doğrular ve çıkarılan Java'yı da denetler. Bu, CVE-2026-114938 için Kasım 2026 alt sınırıdır; sistem Java'sı ve eski `runtime` klasörleri otomatik güncellenmez. [Güvenlik notlarına](../../SECURITY.md) bakın.

Yalnız hazırlık yamasındaki dosyaları gözden geçirip stage edin:

```bat
git diff --check
git add pom.xml README.md CHANGELOG.md .github/workflows/windows-bundle.yml packaging/windows/smoke-test.ps1
git add documentation/en/editor-layout.md documentation/en/setup-and-run.md
git add documentation/tr/installation.md documentation/tr/setup-and-run.md documentation/tr/content-zoom-and-editor-controls.md documentation/tr/release-checklist.md documentation/releases/v1.15.0.md
git diff --cached --stat
git commit -m "Prepare SweetCherry 1.15.0 release"
git push origin main
```

Windows/Linux CI ve Windows PowerShell kontrolü başarılı olmalı. `git status --short` boş olmalı.

## 2. Hazırlık commit'ini etiketle

```bat
git status --short
git log -1 --oneline
git tag -l v1.15.0
```

Etiket henüz yoksa, yeni hazırlık commit'inde:

```bat
git tag -a v1.15.0 -m "SweetCherry 1.15.0"
git push origin v1.15.0
```

Yayımlanmış etiketi taşımayın; force push kullanmayın.

## 3. Paketleri etiketten üret

`v1.15.0` etiketi zaten oluşturulduysa onu taşımayın ve tekrar oluşturmayın.
Paketleme düzeltmesini `main` üzerine commit/push edip CI'ı kontrol ettikten sonra:

Actions → **Portable distribution (Windows builder)** → **Run workflow**:

- **Use workflow from:** `main` (düzeltilmiş workflow).
- **source_ref:** `v1.15.0` (derlenecek uygulama kaynağı).

Eski başarısız görevi Re-run yapmak veya workflow kaynağı olarak eski etiketi
seçmek eski hatalı tanımı tekrar çalıştırır. Bu işlemde POM sürümü **1.15.0** ve
uygulama kaynağı mevcut `v1.15.0` etiketi olarak kalır; yalnız paketleme workflow'u
`main` üzerinden gelir. Workflow özeti kaynak commit'i ile workflow commit'ini
ayrı gösterir. `build-info.json` içindeki commit, `git rev-parse "v1.15.0^{commit}"`
ile eşleşmeli.
 Başarılı görevin **sweetcherry-release-assets** artifact'ini indirin. Ürünler:

- `sweetcherry_1.15.0_all_os_portable.zip`
- `sweetcherry_1.15.0_all_os_portable_with_java_and_setup.exe`
- `sweetcherry-1.15.0-third_party_licences.zip`
- `sha256sums.txt`

Workflow gerçek JAR'ı ve açılmış Java'sız ZIP'i doğrular; yerel viewer/font kaynaklarını, yeni kılavuzları, filtre aktarım aracını ve temiz özel dizinleri kontrol eder. Windows smoke testi gerçek kurucunun çıkardığı runtime sürümünü, kurucu dosya korumasını, runtime önceliğini, sistem Java'sıyla EXE başlatmayı ve YAML portunu kontrol eder. Workflow özeti gerçek runtime sürümünü, runtime arşiv SHA-256 değerini, kaynak ve workflow commit'lerini gösterir; bu bilgileri de kontrol edin.

ZIP'te `runtime/` olmamalı; setup Windows Java runtime içermeli. `build-info.json` sürüm **1.15.0**, `packageRuntimeIncluded` ZIP için **false**, setup için **true** olmalı. Commit alanını `git rev-parse "v1.15.0^{commit}"` ile karşılaştırın. Lisans/atıflar iki pakette de bulunmalı.

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

Çıkarılan Windows setup klasöründe ayrıca:

```bat
runtime\bin\java.exe -version
```

Çıktı en az `17.0.21.1` olmalı. `build-info.json` içindeki `java` değeri workflow'da indirilen Temurin sürümüyle eşleşmeli. Windows paketini yeniden test etmeden kaynak testlerinin geçmesini yayın onayı saymayın. Java'sız ZIP kullanıcılarının ayrıca güvenlik yamalı sistem Java'sı gerekir.

## 5. GitHub Release

Releases → **Draft a new release**:

- Tag: **v1.15.0**
- Title: **SweetCherry 1.15.0**
- Description: `documentation/releases/v1.15.0.md` içeriği; bağlantılar etikete bağlı mutlak GitHub adresleridir.
- Assets: yukarıdaki dört dosya; kaynak yaması ürün asset'i değildir.

Son kontroller tamamlanınca **Set as a pre-release** kapalı, **Set as the latest release** açık → **Publish release**. İndirme bağlantıları, etiket/commit eşleşmesi ve checksum'ları kontrol edin. Önceki release/etiketleri değiştirmeyin.
