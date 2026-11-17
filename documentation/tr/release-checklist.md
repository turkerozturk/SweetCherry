# SweetCherry 1.11.0 yayın adımları — 17 Kasım 2026

Sürüm **1.11.0**, etiket **v1.11.0**. Hazırlık yaması `6bc2027` üzerine uygulanır; sürüm numarası, README, CHANGELOG, kurulum ve yayın belgelerini günceller. Java tabanı **17** kalır. Bu adımlar kendi başına commit, etiket veya GitHub Release oluşturmaz.

## 1. Yamayı uygula ve test et

IntelliJ’de çalışan SweetCherry örneğini durdurun. Arşivi **deponun dışında**, repo klasörünün yanındaki `sweetcherry-1.11.0-release-preparation` klasörüne açın. Böylece hazırlık yaması ve kısa release açıklaması yanlışlıkla kaynak depoya eklenmez. Aşağıdaki komutları repo kökünde, temiz `main` üzerinde çalıştırın:

```bat
git switch main
git pull --ff-only
git apply --check ..\sweetcherry-1.11.0-release-preparation\sweetcherry-6bc2027-release-1.11.0.patch
git apply ..\sweetcherry-1.11.0-release-preparation\sweetcherry-6bc2027-release-1.11.0.patch
git diff --check
mvnw.cmd clean package
```

Hazırlık ortamında 211 JavaScript testi geçti; Maven deposu DNS erişimi sağlanamadığından Java derlemesi/paketleme çalışmadı. Aşağıdaki yerel/CI kontrollerini tamamlayın.

Node.js 24 ile **Git Bash** üzerinde JavaScript testlerini de çalıştırın (npm install gerekmez):

```bash
node --test src/test/js/*.test.cjs
```

`release/SweetCherry/documentation/releases/v1.11.0.md` dosyasının oluştuğunu ve `release/SweetCherry` klasörünün tamamının kullanılabildiğini kontrol edin. Kopya CTB ile aşağıdaki manuel kontrolleri uygulayın. Son feature commit’i için bildirdiğiniz sonuç 993 Java testi, sıfır hata/başarısızlık ve iki atlanan testtir; bu sonuç hazırlık commit’inin veya son paketlerin testi yerine geçmez.

Başarılı test ve kısa manuel kontrolden sonra yalnız bu hazırlığın dosyalarını commit/push yapın (aşağıdaki dosyalar dışında değişiklik varsa ayrıca inceleyin):

```bat
git add pom.xml README.md CHANGELOG.md documentation/en/setup-and-run.md documentation/tr/installation.md documentation/en/todo.md documentation/tr/todo.md documentation/releases/v1.11.0.md documentation/tr/release-checklist.md
git commit -m "Prepare SweetCherry 1.11.0 release"
git push origin main
```

GitHub Actions → **CI** üzerinde bu hazırlık commit’inin Windows/Linux Java ve JavaScript adımlarının başarılı olduğunu doğrulayın. Depo kurallarınız PR gerektiriyorsa aynı hazırlık değişikliklerini PR üzerinden birleştirin.

## 2. Hazırlık commit’ini etiketle

CI başarılı olmalı. Çalışma ağacı temiz olmalı; etiket yamadan önceki `6bc2027` yerine yeni hazırlık commit’ini göstermeli:

```bat
git status --short
git log -1 --oneline
git tag -l v1.11.0
```

Etiket henüz yoksa:

```bat
git tag -a v1.11.0 -m "SweetCherry 1.11.0"
git push origin v1.11.0
```

Alternatif: IntelliJ → Git → Tags → **v1.11.0** → sağ tık → **Push to origin**. Var olan/yayımlanmış etiketi taşımayın veya force push kullanmayın.

## 3. Yayın paketlerini etiketten üret

GitHub → Actions → **Portable distribution (Windows builder)** → **Run workflow** → **v1.11.0**.

Görev JavaScript testlerini, Java 17 Maven paketlemesini, paketlenmiş JAR/lisans envanterini, runtime checksum kontrolünü ve Windows paket/launcher kontrollerini yapar. Tamamı başarılı olduktan sonra **sweetcherry-release-assets** artifact’ini indirip dış ZIP’i açın. GitHub’ın indirme ZIP’i ayrıca bir ürün asset’i değildir.

Dört ürün dosyası:

- `sweetcherry_1.11.0_all_os_portable.zip`
- `sweetcherry_1.11.0_all_os_portable_with_java_and_setup.exe`
- `sweetcherry-1.11.0-third_party_licences.zip`
- `sha256sums.txt`

Java’sız ZIP’te `runtime/` bulunmamalı. Setup’ın açtığı klasörde Windows runtime bulunmalı. `build-info.json` içinde `applicationVersion` **1.11.0**, ZIP için `packageRuntimeIncluded` **false**, setup klasöründe **true** olmalı. Commit alanını şu çıktıyla karşılaştırın:

```bat
git rev-parse "v1.11.0^{commit}"
```

README, CHANGELOG, SECURITY.md, `documentation/releases/v1.11.0.md`, lisanslar ve kurulum belgeleri iki uygulama paketinde de bulunmalı. Bu hazırlık yaması bağımlılık eklemez. 1.10.0 sonrasındaki Word özelliği `poi-scratchpad` eklemiş ve lisans/JAR envanterini güncellemiştir; son paket üzerinden envanter kontrolü yine gereklidir. Envanter kontrolü hata verirse körlemesine geçmeyin; bağımlılık/lisans değişikliğini inceleyin. Gerekirse `python packaging/update-jar-notices.py` ile yenileyip `--check` ile doğrulayın; Python yalnızca bu bakım kontrolü içindir.

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
- Word DOCX/DOC ve Excel XLSX/XLS eklerinde önizleme, orijinal indirme, Excel sayfa seçimi ve sayfalama; formül sonucunun dosyada saklanmış değer olması.
- Datasources → Veritabanı bilgileri: istatistikler, eksik hedef/çapa raporu, filtre/sayfalama ve bir bağlantıyı içerikten düzelttikten sonra yeniden tarama.
- `/tree`: etiketli düğümde `#`, hover’da gerçek etiketler ve footer’da seçili düğüm etiketleri; paylaşılan düğüm örneği.
- TODO: tam `todo` etiketi, `done` ekleyerek tamamlama ve kaldırarak yeniden açma; diğer etiketlerin korunması.
- Yakın zamana hatırlatıcı ekleme, yaklaşan/geciken filtreleri, düzenleme/iptal/tamamlama; yıllık tekrar ve 29 Şubat seçimi.
- Alarmın görünmesi ve sesi, kapatma, 10 dakika erteleme, sayfa geçişinde ses tercihi ve iki sekmede ses tekrarının önlenmesi. Tarayıcı otomatik sesi engellerse Sesi aç / dene ile tekrar kontrol.
- `/system/settings` metin filtresi; açıklama/anahtar/değer eşleşmesi, Türkçe harfler, Temizle/Escape ve filtrelenmiş halde diğer ayarların da korunarak kaydedilmesi.
- Hatırlatıcı ana ayarı kapalı + yeniden başlatma: bağlantılar ve bağımsız alarm paneli gizli; kayıtlar korunuyor. Yalnız alarm kapalıysa liste/form açık, panel yok; yalnız ses kapalıysa görsel panel açık, ses düğmesi yok. Sonra üç ayarı tekrar açıp yeniden başlatma.
- USER/salt okunur erişim; başka CTB seçilince eski sekme isteklerinin engellenmesi; Türkçe/İngilizce, dar ekran ve koyu tema.
- Normal okuma/düzenleme, yedek oluşturma ve ayrı kopyaya geri yükleme; kullandığınız import/export ve senkronizasyon yollarını kontrol etme.

Hatırlatıcı, alarm ve ses varsayılan açık; Trash ve audit varsayılan kapalı kalmalı. Yeni kayıt yokken hatırlatıcı sayfasına bakmak REMINDERS düğümü oluşturmamalı. Mevcut REMINDERS kayıtları kapat/aç sonrasında korunmalı; v2 formatına geçen kayıtları eski sürüm düzenleyemez. Mevcut yardımcı düğümleri başka CTB’ye import/merge etmeyin; bütün CTB yedeği/kopyası kullanın. Kullanıyorsanız MariaDB/MySQL/PostgreSQL ve HTTPS/proxy bağlantılarını ayrıca deneyin. macOS ve 32-bit/i386 için test edilmiş destek iddiası eklemeyin.

## 5. Release’i oluştur

GitHub → Releases → **Draft a new release**:

- **Tag:** `v1.11.0`
- **Title:** `SweetCherry 1.11.0`
- **Description:** hazırlık arşivindeki `sweetcherry-1.11.0-release-body.md` içeriği.
- **Assets:** yukarıdaki dört dosya. GitHub otomatik Source code bağlantılarını ayrıca ekler.

Hazırlık yaması/ZIP’i, release body dosyasını veya ayrı build-info JSON’u ürün asset’i olarak yüklemeyin. İsterseniz **Save draft** ile bekletin. Kontroller tamamlanınca **Set as a pre-release** kapalı, **Set as the latest release** açık → **Publish release**.

Yayımdan sonra indirme bağlantıları, checksum dosyası, sürüm/kurulum bağlantıları ve etiketin doğru commit’e gittiğini kontrol edin. Önceki `v1.10.0` etiketini veya paketlerini değiştirmeyin.
