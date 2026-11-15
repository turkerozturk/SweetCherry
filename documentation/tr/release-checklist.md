# SweetCherry 1.10.0 yayın adımları — 15 Kasım 2026

Sürüm **1.10.0**, etiket **v1.10.0**. Hazırlık yaması `988ebd7` üzerine uygulanır; resim galerisi etiket filtresi ve sürüm hazırlığını birlikte içerir. Java tabanı **17** kalır. Bu adımlar kendi başına commit, etiket veya GitHub Release oluşturmaz.

## 1. Yamayı uygula ve test et

IntelliJ’de çalışan SweetCherry örneğini durdurun. Arşivi **deponun dışında**, repo klasörünün yanındaki `sweetcherry-1.10.0-release-preparation` klasörüne açın. Böylece hazırlık yaması ve kısa release açıklaması yanlışlıkla kaynak depoya eklenmez. Aşağıdaki komutları repo kökünde, temiz `main` üzerinde çalıştırın:

```bat
git switch main
git pull --ff-only
git apply --check ..\sweetcherry-1.10.0-release-preparation\sweetcherry-988ebd7-release-1.10.0.patch
git apply ..\sweetcherry-1.10.0-release-preparation\sweetcherry-988ebd7-release-1.10.0.patch
git diff --check
mvnw.cmd clean package
```

Node.js 24 kuruluysa **Git Bash** üzerinde JavaScript testlerini de çalıştırabilirsiniz (npm install gerekmez):

```bash
node --test src/test/js/*.test.cjs
```

`release/SweetCherry/documentation/releases/v1.10.0.md` dosyasının oluştuğunu ve `release/SweetCherry` klasörünün tamamının kullanılabildiğini kontrol edin. Kendi CTB dosyanızın kopyasında resim etiket filtresini deneyin; `work`, `Work` ve `workshop` farklı eşleşmelerdir. Filtre, arama/sıralama ve sonraki sayfaya geçişte korunmalı; tüm etiketler seçilince kaldırılmalıdır.

Başarılı test ve kısa manuel kontrolden sonra **Prepare SweetCherry 1.10.0 release** mesajıyla commit ve push yapın. GitHub Actions → **CI** üzerinde bu hazırlık commit’inin Windows/Linux Java ve JavaScript adımlarının başarılı olduğunu doğrulayın. Depo kurallarınız PR gerektiriyorsa aynı hazırlık değişikliklerini PR üzerinden birleştirin.

## 2. Hazırlık commit’ini etiketle

Çalışma ağacı temiz olmalı; etiket yamadan önceki `988ebd7` yerine yeni hazırlık commit’ini göstermeli:

```bat
git status --short
git log -1 --oneline
git tag -l v1.10.0
```

Etiket henüz yoksa:

```bat
git tag -a v1.10.0 -m "SweetCherry 1.10.0"
git push origin v1.10.0
```

Alternatif: IntelliJ → Git → Tags → **v1.10.0** → sağ tık → **Push to origin**. Var olan/yayımlanmış etiketi taşımayın veya force push kullanmayın.

## 3. Yayın paketlerini etiketten üret

GitHub → Actions → **Portable distribution (Windows builder)** → **Run workflow** → **v1.10.0**.

Görev JavaScript testlerini, Java 17 Maven paketlemesini, paketlenmiş JAR/lisans envanterini, runtime checksum kontrolünü ve Windows paket/launcher kontrollerini yapar. Tamamı başarılı olduktan sonra **sweetcherry-release-assets** artifact’ini indirip dış ZIP’i açın. GitHub’ın indirme ZIP’i ayrıca bir ürün asset’i değildir.

Dört ürün dosyası:

- `sweetcherry_1.10.0_all_os_portable.zip`
- `sweetcherry_1.10.0_all_os_portable_with_java_and_setup.exe`
- `sweetcherry-1.10.0-third_party_licences.zip`
- `sha256sums.txt`

Java’sız ZIP’te `runtime/` bulunmamalı. Setup’ın açtığı klasörde Windows runtime bulunmalı. `build-info.json` içinde `applicationVersion` **1.10.0**, ZIP için `packageRuntimeIncluded` **false**, setup klasöründe **true** olmalı. Commit alanını şu çıktıyla karşılaştırın:

```bat
git rev-parse "v1.10.0^{commit}"
```

README, CHANGELOG, SECURITY.md, `documentation/releases/v1.10.0.md`, lisanslar ve kurulum belgeleri iki uygulama paketinde de bulunmalı. Bu sürüm yeni bağımlılık eklemez. Envanter kontrolü hata verirse körlemesine geçmeyin; bağımlılık/lisans değişikliğini inceleyin. Gerekirse `python packaging/update-jar-notices.py` ile yenileyip `--check` ile doğrulayın; Python yalnızca bu bakım kontrolü içindir.

## 4. Checksum ve son paket kontrolü

Dört dosyanın bulunduğu klasörde PowerShell:

```powershell
Get-Content .\sha256sums.txt | ForEach-Object {
    if ($_ -match '^([0-9a-fA-F]{64})\s+(.+)$') {
        $expected = $matches[1]
        $file = $matches[2]
        if ((Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash -ne $expected) {
            throw "SHA-256 mismatch: $file"
        }
        Write-Host "OK: $file"
    } else {
        throw "Invalid checksum line: $_"
    }
}
```

İki paketi ayrı, temiz ve yazılabilir klasörlerde; gerçek notlar yerine **kopya/demo CTB** ile deneyin:

- Launcher, giriş, veri kaynağı seçme, normal kapatma; Java’sız paketi sistem Java 17+ ve setup’ı kendi runtime’ıyla çalıştırma.
- Etiket sayfasında hızlı düzenleme, parent/tarih sütunları ve sıralama; gelişmiş aramada toplu etiket ve toplu taşıma.
- Çoklu resim seçimi ve zengin metne ekleme; resim galerisinde etiket + metin araması, sıralama, ikinci sayfa, görüntüleyici ve indirme.
- Tablo/codebox galerileri, geniş tablo kaydırması, 1 px kenarlık, kod kopyalama/indirme; ek dosyalarda ad filtresi ve sayısal boyut sıralaması.
- Türkçe/İngilizce, dar ekran ve koyu tema; başka CTB seçildiğinde eski sekmeden resim/kod/tablo/dosya getirilmesinin engellenmesi.
- İsteğe bağlı Trash Bin’i kopyada etkinleştirip yeniden başlatma; silme, geri alma ve shared/bookmark/ID bilgilerini kontrol etme. Kalıcı silmeyi yalnızca atılabilir örnekte deneme.
- İsteğe bağlı CTBLOGS’u kopyada etkinleştirip yeniden başlatma; içerik/etiket/taşıma/yer imi değişikliklerini ve önceki metin indirmesini kontrol etme. Audit’in binary yedek tutmadığını ve tüm yazma yollarını kapsamadığını hatırlama.
- Normal okuma/düzenleme, yedek oluşturma ve ayrı kopyaya geri yükleme; kullandığınız import/export ve senkronizasyon yollarını kontrol etme.

Trash ve audit varsayılan kapalı kalmalı. Mevcut yardımcı düğümleri başka CTB’ye import/merge etmeyin; bütün CTB yedeği/kopyası kullanın. Kullanıyorsanız MariaDB/MySQL/PostgreSQL ve HTTPS/proxy bağlantılarını ayrıca deneyin. macOS ve 32-bit/i386 için test edilmiş destek iddiası eklemeyin.

## 5. Release’i oluştur

GitHub → Releases → **Draft a new release**:

- **Tag:** `v1.10.0`
- **Title:** `SweetCherry 1.10.0`
- **Description:** hazırlık arşivindeki `sweetcherry-1.10.0-release-body.md` içeriği.
- **Assets:** yukarıdaki dört dosya. GitHub otomatik Source code bağlantılarını ayrıca ekler.

Hazırlık yaması/ZIP’i, release body dosyasını veya ayrı build-info JSON’u ürün asset’i olarak yüklemeyin. İsterseniz **Save draft** ile bekletin. Kontroller tamamlanınca **Set as a pre-release** kapalı, **Set as the latest release** açık → **Publish release**.

Yayımdan sonra indirme bağlantıları, checksum dosyası, sürüm/kurulum bağlantıları ve etiketin doğru commit’e gittiğini kontrol edin. Önceki `v1.9.0` etiketini veya paketlerini değiştirmeyin.
