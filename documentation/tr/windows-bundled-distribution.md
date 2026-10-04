# Windows x64: Java içeren EXE ve portable ZIP

Bu paket Windows 10/11 x64 içindir. 32-bit Windows ve ayrı ARM64 paketi henüz hedeflenmez. Kullanıcının Java kurması gerekmez. Geliştirme makinesinde Maven derlemesi için Java/JDK gereksinimi sürer.

## Mevcut klasör korunur

Normal `mvnw.cmd clean package`, önceki gibi `release/SweetCherry` üretir. Windows paketleme adımı **bu klasöre** `SweetCherry.exe`, `SweetCherry.ico`, `runtime/` ve `windows-bundle.json` ekler. Dış YAML yoksa paketleme Maven’in filtrelenmiş `target/classes/application.yml` dosyasını buraya kopyalar. JAR, YAML, CTBDATA, allTenants, exportedFiles ve run scriptleri aynı yerde kalır. `run.bat` varsa yanındaki runtime’ı, yoksa sistem Java’sını kullanır. Linux/macOS scriptleri değişmez.

Launch4j EXE çalışma dizinini kendi klasörüne alır ve yalnız `runtime/` altında Java arar. Ana port 8443, ek HTTP portu 8080 ve tarayıcı açılması mevcut run scriptleriyle aynıdır. EXE varsayılanları JVM sistem özellikleridir; kullanıcı komut satırından `--server.port=...` gibi Spring seçenekleriyle bunları değiştirebilir. SSL ayarı yoksa bu portlar HTTP’dir. Sistem Java’sı veya PATH değiştirilmez. GUI çalıştırıcı konsol açmaz; sorun halinde `run.bat` ile tanı koyun ve `myapp.log` dosyasına bakın. İlk giriş bilgileri mevcut `login-credentials.properties` dosyasındadır; yayımlanan paket bu dosyayı içermez, ilk çalıştırmada oluşur.

Tek EXE çalıştırıcısı aynı anda ikinci bir örnek başlatmaz (Launch4j mutex). Bu ilk sürümde ikinci tıklama tarayıcıyı tekrar açma işlevi değildir; açık tarayıcıyı kullanın veya `http://localhost:8080` açın. Uygulamayı yönetici menüsündeki kapatma işlemiyle kapatın; tarayıcı sekmesini kapatmak sunucuyu durdurmaz. `run.bat` ile paralel ikinci süreç başlatmayın. Birden fazla bağımsız kopyayı eşzamanlı farklı portlarda çalıştırma bu Windows EXE akışının hedefi değildir.

## Sihirbaz ve veri dosyaları

Inno Setup EXE’si kendi bulunduğu klasörün altındaki `SweetCherry` klasörünü önerir. Kullanıcı yazılabilir başka bir klasör seçebilir. Yönetici yükseltmesi istenmez. EXE’nin yanına yazılamıyorsa Documents/Desktop gibi bir yer seçin; Program Files altında değiştirilebilir CTB tutmak önerilmez. Masaüstü ve Başlat menüsü kısayolları isteğe bağlıdır. Taskbar’a sabitleme Windows kullanıcısının işlemidir; kurucu bunu otomatik yapmaz.

Portable dağıtım için kaldırıcı/kaldırma kaydı üretilmez. Silmek için uygulamayı kapatıp istenen dosyaları elle kaldırın; veri içeren klasörü bilinçsizce silmeyin. Klasör taşınınca kısayolları yeniden oluşturun. ZIP’in içindeki `SweetCherry` klasörünü çıkarıp EXE’ye çift tıklamak da aynı çalıştırma biçimidir.

Güncellemeden önce uygulamayı kapatın. Kurucu mevcut `application.yml`, `CTBDATA/demo.ctb` ve `allTenants/demo.txt` dosyalarını değiştirmez; başka CTB/tenant dosyalarını ve `login-credentials.properties` dosyasını silmez. Uygulama/runtime dosyaları yenilenir. Korunan eski YAML’ye yeni ayarların gerektiğinde elle birleştirilmesi gerekir. ZIP’i kullanılan klasörün üzerine topluca açmak aynı koruma güvencesini vermez; yeni klasörde açıp kullanıcı dosyalarını bilinçli aktarın.

CTB dosyaları başka konumlarda kalabilir; mevcut tenant bağlantıları kullanılabilir. Bu paket yedekleme yapmaz ve dış CTB’leri taşımaya çalışmaz. Portable kullanım bütün dosyaların aynı yerde bulunmasını zorunlu kılmaz.

## GitHub Actions ile oluşturma

1. Bu dosyaları push edin. Normal CI önceki gibi çalışır; yeni paketleme her push’ta çalışmaz.
2. GitHub → Actions → **Windows x64 bundled distribution** → **Run workflow**. Geliştirme denemesinde main’i, release paketinde sürüm etiketini seçin. Dosya adının sürümü checkout’taki `pom.xml` sürümünden alınır.
3. Windows runner normal Maven `clean package` ile testleri çalıştırır. Temurin’in en güncel Java 17 Windows x64 JRE paketini resmi Adoptium API üzerinden çözer; yayımlanan SHA-256 ile indirilen dosyayı doğrular. Tam runtime’ın lisans/legal dosyaları korunur.
4. Launch4j EXE, `windows-launcher` Maven profilindeki `launch4j-maven-plugin:2.7.0` ile oluşturulur. Plugin Launch4j 3.50 core ve platform araçlarını Maven repository üzerinden çözer; SourceForge ZIP indirmesi yapılmaz. Normal `clean package` bu plugini çalıştırmaz; bundle scripti yalnız ilgili Maven goal’u çağırır. Runner’da Inno Setup 6 bulunmalıdır; yoksa açık hata ile durur. Araç ve runtime sürümleri `windows-bundle.json` içinde kaydedilir.
5. Sihirbaz geçici klasöre sessiz kurulup varolan kullanıcı dosyalarını koruduğu kontrol edilir. Boşluk ve Türkçe karakterli klasörde, farklı çalışma dizininden, sistem JAVA_HOME/PATH olmadan **üretilen EXE** başlatılır ve localhost giriş sayfasının HTTP 200 dönmesi ve giriş alanlarını içermesi doğrulanır; boş tenant fixture’ı için genel sağlık yanıtı ayrıca tanı amacıyla loglanır. Sadece CI test süreci test sonunda kapatılır; kullanıcı süreçlerine uygulanmaz.
6. Başarılı artifact’ten şunları indirin: `SweetCherry-<version>-windows-x64-setup.exe`, `SweetCherry-<version>-windows-x64.zip`, `SHA256SUMS.txt`, `windows-bundle.json`. Artifact 14 gün tutulur; kalıcı dağıtım için GitHub Release’e dosyaları yükleyin. İş akışı release veya tag oluşturmaz/yayımlamaz.

Artifact indirmesi GitHub’ın dış ZIP’ini verir; bunun içindeki **ürün ZIP’i** portable dağıtımdır. Kaynak kod ZIP’i Java içeren ürün değildir. GitHub Releases birden fazla asset kabul eder; dosya başına sınır 2 GiB’dir. JAR/runtime sıkıştırılsa da kesin boyut derleme sonucuyla ölçülür; `jlink` küçültmesi şimdilik yoktur.

Runtime en güncel Java 17 olarak seçildiğinden aynı commit farklı tarihlerde farklı runtime alabilir. Yayımlanan `windows-bundle.json` sürüm ve checksum’ı kaydeder; tam tekrar üretim için o belirli runtime arşivini kullanın. Java güvenlik güncellemeleri sonrasında yeni dağıtım yayımlanmalıdır; kullanıcının sistem Java’sını güncellemesi bu runtime’ı güncellemez.

## Yerel paketleme (isteğe bağlı)

Windows x64 JDK 17, Maven wrapper ve Inno Setup 6.3+ ve doğrulanmış Temurin Java 17 x64 JRE ZIP’i gerekir. Temiz bir checkout kullanın; gerçek kullanılan release klasörünüzden paket oluşturmayın. `packaging/windows/build-bundle.ps1` Maven’den sonra Inno yolu, runtime ZIP’i, resmi SHA-256 ve sürüm adı parametreleriyle çalıştırılır. Parametre örneği:

```powershell
.\mvnw.cmd clean package
.\packaging\windows\build-bundle.ps1 -IsccPath 'C:\Program Files (x86)\Inno Setup 6\ISCC.exe' -RuntimeArchive 'C:\Downloads\temurin-jre17.zip' -RuntimeSha256 'RESMI_SHA256_DEGERI' -RuntimeVersion 'RESMI_RELEASE_ADI'
```

Örnek checksum/sürüm değerlerini gerçek paket bilgileriyle değiştirin; örnek komutu değişmeden çalıştırmayın. PowerShell script çalıştırma politikanız engelliyorsa kurumsal politikanızı devre dışı bırakmadan uygun yerel çalıştırma yöntemini belirleyin. Çıktı `dist/windows` altında oluşur ve Git’e eklenmez. Paketleme araçları son kullanıcıya dağıtılmaz.

## İlk paket için manuel kabul

- Java kurulu olmayan gerçek Windows 10/11 x64’te EXE/ZIP ile açılış, ilk giriş ve Demo CTB yükleme.
- Desktop/Documents altında boşluk/Türkçe karakterli yol; hedef klasör seçimi ve kısayoldan başlatma.
- Tarayıcı açılması, kapatma ve tekrar başlatma; port dolu olduğunda `run.bat`/log tanısı.
- Demo CTB, YAML, tenant ve parola dosyaları değiştirilmiş kurulumda güncellemenin koruması.
- Belgelenen lisans/atıf envanterinin dağıtımda tamamlanması; Windows EXE’lerinin imzalanması ayrıca değerlendirilmeli. Bu ilk paket imzalanmaz, SmartScreen uyarısı görülebilir.

## Kaynaklar

- https://launch4j.sourceforge.net/docs.html
- https://sourceforge.net/projects/launch4j/files/launch4j-3/3.50/
- https://jrsoftware.org/ishelp/
- https://adoptium.net/temurin/releases/
- https://docs.github.com/en/repositories/releasing-projects-on-github/about-releases

Launch4j plugin kaynakları: https://github.com/orphan-oss/launch4j-maven-plugin/tree/launch4j-maven-plugin-2.7.0
Plugin uygulamanın runtime bağımlılığı değildir ve SweetCherry.jar içine eklenmez. Maven cache araç indirmelerini saklar; yeni bir runner ilk çalışmada paketleri indirir.
