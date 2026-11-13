# SweetCherry 1.9.0 yayın adımları — 13 Kasım 2026

Sürüm **1.9.0**, etiket **v1.9.0**. Bu özellik sürümü hazırlığı `b56df67` üzerine uygulanır. Java tabanı 17 olarak kalır. Günlük takvim, tema/ayarlar, zengin metin dönüşümü, bağlantı haritası ve wiki bağlantıları içerir. Uygulamayı çalıştırmak için Node.js gerekmez.

## 1. Yamayı uygula ve doğrula

IntelliJ’de çalışan SweetCherry örneğini durdurun. Temiz `main` çalışma ağacında:

```bat
git switch main
git pull --ff-only
git apply --check sweetcherry-b56df67-release-1.9.0.patch
git apply sweetcherry-b56df67-release-1.9.0.patch
git diff --check
mvnw.cmd clean package
```

Yerel JavaScript kontrolü için Node.js 24 kuruluysa **Git Bash** üzerinde:

```bash
node --test src/test/js/*.test.cjs
```

GitHub görevleri Node.js'i otomatik kurar; npm install gerekmiyor. Yerel testleri başarıyla tamamladıktan sonra IntelliJ'den **Prepare SweetCherry 1.9.0 release** mesajıyla commit ve push yapın. Bu belge yayını kendi başına oluşturmaz.

GitHub → Actions → **CI** görevinin bu son commit için Windows ve Linux üzerinde başarılı olduğunu doğrulayın. Her iki görevde **Test JavaScript** adımı da başarılı olmalı. Kurulan ruleset normal push'a izin verir; force push veya yayımlanmış etiket taşıma kullanmayın.

Bağımlılıkları daha sonra değiştirirseniz, paketleme sonrası lisans envanterini yenileyip gözden geçirin:

```bat
python packaging/update-jar-notices.py
python packaging/update-jar-notices.py --check
```

Bu bakım aracı Python 3.9+ ve Maven'ın yerel bağımlılık önbelleğini kullanır; uygulamayı çalıştırmak için Python gerekmez. Yenileme, gerekli olduğunda Maven Central'dan lisans metaverisi/kaynak notlarını indirir; `--check` ağ bağlantısı veya yazma yapmaz. GitHub Windows runner'ındaki Python ile yayın öncesi kontrol otomatik çalışır.

## 2. Son hazırlık commit'ini etiketle

`git status --short` boş olmalı. `git log -1` son hazırlık commit'ini göstermeli. Önce etiketin varlığını kontrol edin:

```bat
git status --short
git log -1 --oneline
git tag -l v1.9.0
```

Etiket henüz yoksa:

```bat
git tag -a v1.9.0 -m "SweetCherry 1.9.0"
git push origin v1.9.0
```

Alternatif: IntelliJ → Git → Tags → v1.9.0 → sağ tık → Push to origin. Etiket bu yeni hazırlık commit'ini göstermeli; `b56df67` üzerinde bırakılmamalı. Etiket varsa yeniden oluşturmayın veya taşımayın; işaret ettiği commit'i kontrol edin.

## 3. Etiketten yayın paketlerini üret

GitHub → Actions → **Portable distribution (Windows builder)** → **Run workflow** → **v1.9.0** seçin. Eski **Complete 1.0.0 draft license notices** görevini kullanmayın.

Görev sırasıyla JavaScript testlerini, Java 17 Maven test/paketlemesini, güncel JAR/lisans envanterini, Windows Java runtime doğrulamasını, ZIP/setup oluşturmayı ve launcher/kurucu kontrollerini yapar. Tamamı başarılı olmadan yayımlamayın. Görev bu aşamada GitHub Release oluşturmaz.

**sweetcherry-release-assets** artifact'ini indirip dış ZIP'i açın. Şu dört dosya bulunmalı:

| Dosya | Amaç |
| --- | --- |
| `sweetcherry_1.9.0_all_os_portable.zip` | Java'sız uygulama ve tüm platform scriptleri |
| `sweetcherry_1.9.0_all_os_portable_with_java_and_setup.exe` | Windows x64 setup ve Java 17 |
| `sweetcherry-1.9.0-third_party_licences.zip` | Paketlerin içindeki lisans/notices dosyalarının ek kopyası |
| `sha256sums.txt` | Bu üç ürünün SHA-256 değerleri |

ZIP içinde `runtime/` bulunmamalı. `build-info.json` içindeki `applicationVersion` **1.9.0**, `packageRuntimeIncluded` **false** olmalı. Setup'ın açtığı klasörde runtime bulunmalı ve bu alan **true** olmalı. İki paketin commit alanı şu komutun sonucuyla aynı olmalı:

```bat
git rev-parse "v1.9.0^{commit}"
```

JAR, README, CHANGELOG, SECURITY.md, v1.9.0 sürüm notları, lisanslar ve kurulum belgeleri pakette bulunmalı. `licenses/cytoscape-3.35.0-LICENSE.txt` de lisans arşivinde bulunmalı. Java'sız ZIP'te Windows kullanıcısı `run.bat` kullanır; `SweetCherry.exe` Windows runtime'ını ister.

## 4. Checksum ve kısa kullanım kontrolü

Ürünlerin bulunduğu klasörde PowerShell:

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

İki paketi ayrı, temiz ve yazılabilir klasörlerde deneyin. Gerçek notlar yerine kopya/demo CTB kullanın:

- Launcher açma, giriş, veritabanı seçme ve normal kapatma.
- Dil değiştirme; başka CTB seçildiğinde eski sekmenin uyarısı.
- Boş CTB oluşturma; düğüm özellikleri ve durum ikonları.
- Günlük takvimde tarih gezinmesi, filtreler, kart önizlemeleri ve nesne sayıları.
- Sistem ayarlarından koyu temayı kaydetme, yeniden başlatma ve tekrar açık temaya geçme.
- Bağlantı haritasında gelen/giden linkler, odak değiştirme, geçmiş, hiyerarşi, içerik paneli ve yeniden tarama.
- Zengin metinde `[[benzersiz ad]]`, yinelenen ad/breadcrumb seçimi, bulunamayan adın metin kalması, `[[ad|etiket]]`, escape ve Kaydet ve devam et.
- Kopya bir düğümde zengin metin tür dönüşümünü önce iptal, sonra onaylama; tablo/kod metinlerini ve kaldırılan ekleri kontrol etme.
- Zengin metne web içeriği yapıştırma ve halka açık bir web sayfasını içe aktarma; Markdown/HTML düzenleme.
- Kayıtlı filtreler, yedek oluşturma/listeleme/ayrı kopyaya geri yükleme.
- CTB/ZIP/MM içe aktarma ve dışa aktarma, oluşturulan dosyaları indirme/silme.

Kullanıyorsanız gerçek MariaDB/MySQL/PostgreSQL bağlantılarınızı ve HTTPS/proxy kurulumunuzu da deneyin. Windows Java'sız paketi uygun sistem Java 17+ ile, setup'ı kendi Java'sıyla çalıştırın. Linux'ta `sh ./run.sh` veya `sh ./launcher.sh` kullanılabilir. macOS ve 32 bit için test edilmiş destek iddiası eklemeyin.

## 5. Release'i oluştur ve yayımla

GitHub → Releases → **Draft a new release**:

- **Tag:** `v1.9.0`
- **Title:** `SweetCherry 1.9.0`
- **Description:** hazırlık arşivindeki `sweetcherry-1.9.0-release-body.md` içeriği.
- **Assets:** yukarıdaki dört dosya. GitHub'ın otomatik Source code bağlantıları ayrıca oluşur.

Ayrı JSON dosyası, hazırlık yaması veya workflow'un dış artifact ZIP'ini asset olarak yüklemeyin. İsterseniz önce **Save draft** ile kaydedin. Kontroller tamamlanınca **Set as a pre-release** kapalı, **Set as the latest release** açık olacak biçimde **Publish release** seçin.

Yayımdan sonra sayfadaki dosya adlarını, indirme bağlantılarını ve v1.9.0 etiketine giden kurulum/sürüm notu bağlantılarını kontrol edin. Son indirilen dosyaların checksum değerlerini de karşılaştırın. Önceki 1.8.1 etiketini veya paketlerini değiştirmeyin. Yeni özellikler ve wiki kontrolündeki CodeQL düzeltmesi yeni 1.9.0 paketleriyle dağıtılır.
