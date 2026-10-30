# İlk çalıştırma / First start

## Türkçe

### Windows: indir, başlat, giriş yap

1. Kurucuyu kullanın veya portable ZIP'in **tamamını** yazılabilir bir klasöre çıkarın. `SweetCherry.exe` kontrol penceresini açar; hazır olmasını bekleyin.
2. **Admin Şifresini Kopyala** düğmesine basın. Tarayıcıdaki giriş sayfasında yönetici kullanıcı adı hazırdır; şifre alanına yapıştırıp giriş yapın. Özel kullanıcı adı/parola ayarları varsa etkin olan değerler kullanılır.
3. Demo veya hızlı başlangıç veritabanını seçin. İlk tercih masaüstü ağaç görünümüdür. Görünüm menüsünden mobil okuyucuya geçilebilir; mevcut tarayıcı tercihi değiştirilmez.
4. İlk giriş ekranındaki **Şifrem nerede?** bölümü launcher, terminal ve uzak cihaz kullanımı için yol gösterir. Şifre HTTP sayfasında, URL'de veya JavaScript'te yayımlanmaz.

### Linux masaüstü

Kaynak arşivini açtıktan sonra Java **17 veya daha yeni JDK** gerekir. Grafik arayüz için dağıtımınızın masaüstü Java paketini kurun (yalnız `headless` paket yeterli değildir; Java 17'nin grafik araçları X11/XWayland gerektirebilir).

```sh
cd /uygulama/kaynak/klasoru
sh ./first-run.sh
```

`sh` ile çalıştırmak ilk script için bile `chmod` gerektirmez. Git checkout yürütme bitini korur; ZIP çıkarma davranışı dosya yöneticisine bağlı olabilir. Derleme ve ilk script `release/SweetCherry` altındaki çalıştırma scriptlerini yürütülebilir yapar. Masaüstü oturumunda ilk script Swing launcher'ı terminalden bağımsız başlatır. Sonraki açılışlar:

```sh
cd release/SweetCherry
sh ./launcher.sh
```

Windows'taki gibi tarayıcı açma, şifreyi kopyalama, YAML ayarları, uygulama klasörü, log ve durdurma düğmeleri vardır. Terminal kapansa da uygulama çalışır. Kontrol penceresini kapatırken arka planda çalışma veya durdurma seçilir; sistem tepsisi yoksa pencere küçültülür. Çalışan uygulamayı ikinci kez başlatmak yerine mevcut kontrol penceresini kullanın.

İsteğe bağlı, **yalnız kendi kullanıcınız için** uygulama menüsü ve masaüstü kısayolu:

```sh
sh ./install-desktop.sh
```

`sudo` kullanmayın. Masaüstünüz ayrıca **Allow Launching / Başlatmaya izin ver** isteyebilir. Klasör taşınırsa scripti yeniden çalıştırın. Kısayol oluşturmak programı sistem açılışında başlatmaz; servis kurmaz. GUI başlayamazsa `launcher.log` dosyasını kontrol edin; masaüstü Java, grafik oturumu ve `xdg-open` bulunmalı.

### Linux SSH / Raspberry Pi / grafik oturumu yok

`run.sh` mevcut terminal akışını korur. Grafik oturumu yokken `launcher.sh` de bu akışa döner:

```sh
cd ~/SweetCherry
sh ./run.sh
# Terminal kapanınca da çalışması istenirse:
nohup sh ./run.sh --myapp.openWebBrowserOnStartup=false > console.log 2>&1 < /dev/null &
```

`nohup` için `&` işareti önemlidir. `run.sh` ön planda Ctrl+C ile durur; arka plandaki uygulama web arayüzünden durdurulabilir. Bu bir sistem servisi değildir; bilgisayar yeniden başlayınca kendiliğinden başlamaz. Windows paketindeki Java/EXE Linux için değildir; Linux'ta ayrıca uygun Java kurulur ve kaynak derlemesinin ürettiği çalışma klasörü kullanılır.

### Şifre ve yerel çıktı

İlk kez `login-credentials.properties` oluşturulurken üretilmiş admin şifresi **bir kez yerel süreç çıktısında** gösterilir. Uygulamanın `myapp.log` logger'ına yazılmaz. Terminal çıktısı yönlendirildiyse o dosyada bulunur; Linux launcher için bu `launcher.log` olur. Uygulama yeniden başlatıldığında veya parola dosyada değiştirildiğinde tekrar yazılmaz. Açıkça YAML/ortam/argüman ile verilmiş yönetici parolası bu yolla yazılmaz.

Bu davranış `myapp.login.show-initial-password: false` ile kapatılabilir. İlk çıktıdan önce kapatmak için ilk açılışta `--myapp.login.show-initial-password=false` verin. Çıktı/log dosyalarını paylaşmadan önce kontrol edin. Linux launcher yeni log dosyasını yalnız kullanıcı okuyacak biçimde oluşturur. Panoya kopyalanan şifre de pano geçmişinde veya pano senkronizasyonunda kalabilir; işiniz bitince panoyu temizleyin.

Kalıcı şifreler uygulamanın çalışma klasöründeki dosyadadır. Yönetici adı varsayılan `admin`, dosya anahtarı `admin.password` şeklindedir. Kullanıcı adı olarak anahtarı yazmayın. Şifre değiştirmek için uygulamayı durdurun, ilgili değeri düzenleyip yeniden başlatın. Açıkça tanımlanmış `myapp.login.*.password` ayarları dosyaya göre önceliklidir. Mevcut parolalar değiştirilmez; dosyayı silmek yeni rastgele parolalar üretilmesine neden olur. Genel `123456` parolası veya kimlik doğrulamasız giriş eklenmemiştir.

### YAML ve varsayılanlar

Ana program Spring başlamadan önce eksik `application.yml`, `application-operations.yml`, `application-experiments.yml` dosyalarını JAR içindeki sürümlerden **çalışma klasörüne** çıkarır. Scriptler ve EXE bu klasörü JAR'ın bulunduğu klasör yapar. Doğrudan Java kullanırken önce o klasöre `cd` yapın; IDE çalıştırmasında IDE'nin çalışma klasörü kullanılır.

Mevcut aynı adlı `.yml`/`.yaml` dosyaları ve `config/` altındaki eşdeğerleri korunur. Profil dosyasının çıkarılması o profili etkinleştirmez. Geçersiz dosya otomatik düzeltilmez veya üzerine yazılmaz. Yazılamayan konumda uyarı verilir; JAR varsayılanları kullanılabilir ama ayar kaydı için yazılabilir klasör gerekir. Eski harici ayarlar yükseltmede yeni varsayılanlara dönüştürülmez. İleri düzey özel `spring.config.location/name` kullanımında Spring'in kendi arama kuralları geçerlidir.

Yeni kurulumun örnek astronomi konumu Greenwich Kraliyet Gözlemevi çevresidir: enlem `51.4779`, boylam `-0.0015`, saat dilimi `Europe/London`. Otomatik konum tespiti değildir. Eski koordinatlar korunur. Yeni ana port `8443`, ek HTTP portu `8080`; SSL yapılandırılmadıysa ikisi de HTTP'dir. Linux'ta normal kullanıcı için 443 gibi ayrıcalıklı port seçmeyin; eski YAML'da `server.port: 443` varsa bunu elle değiştirin veya `--server.port=8443` kullanın. Linux `run.sh` artık YAML portlarını sabit argümanlarla ezmez. Ağ erişimi varsayılan olarak yine yalnız yereldir; uzaktan erişim/HTTPS ayrı ve bilinçli yapılandırılır.

### Manuel kontroller

- Ayrı yazılabilir test klasöründe, harici YAML yokken başlatın: üç dosya oluşmalı, ilk girişten sonra Sistem → Uygulama Ayarları okunup kaydedilebilmeli. Restart yeni değeri yüklemeli.
- Mevcut `.yml`, `.yaml` veya `config/application.yaml` ile başlayın: dosya içeriği ve özel ayarlar değişmemeli.
- Yeni parola dosyasında ilk konsol çıktısını, sonraki açılışta çıktının olmamasını, launcher kopyala düğmesini ve özel YAML parola önceliğini kontrol edin.
- Giriş sayfasında Türkçe/İngilizce yardım ve hazır kullanıcı adı görünmeli; şifre bulunmamalı. Uzak tarayıcı için otomatik giriş veya parola gösterimi olmamalı.
- Görünüm tercihi olmayan tarayıcı ağaçla başlamalı; mobil okuyucu seçimi sonraki gezinmelerde korunmalı.
- Linux masaüstünde `launcher.sh`, terminali kapatma, kısayol, durdurma ve tepsisiz kapatma davranışını deneyin. SSH'de grafik pencere açılmamalı; foreground/`nohup` çalışmalı.

## English

On Windows, start `SweetCherry.exe`, wait until ready, click **Copy Admin Password**, paste into the browser's password field and sign in. The effective administrator username is prefilled. The login page includes first-use help but never exposes passwords. Existing credentials and custom login overrides remain effective.

On Linux with a desktop JDK 17+, run `sh ./first-run.sh` from extracted source. It builds the application, marks the distribution scripts executable and launches the Swing controls independently of the terminal. Later run `sh ./launcher.sh` from `release/SweetCherry`. Use `sh ./install-desktop.sh` there for per-user application-menu and optional desktop shortcuts; no sudo is needed. Your desktop may require **Allow Launching**. Reinstall shortcuts after moving the folder. A desktop Java package, a working graphical session (possibly XWayland) and `xdg-open` are needed. Inspect `launcher.log` if the GUI fails. No system service or automatic boot startup is installed.

For SSH/headless use, `sh ./run.sh` stays in the foreground; `launcher.sh` falls back to it when no desktop session is detected. To survive terminal closure, use `nohup sh ./run.sh --myapp.openWebBrowserOnStartup=false > console.log 2>&1 < /dev/null &`. Windows-bundled Java/EXE cannot run on Linux; use Linux Java and the source-built distribution.

When the credentials file is first generated, its initial admin password is shown once in local process output, not through the application logger. Redirected output can retain it (`launcher.log` on Linux). It is not printed again on restart or after changing it, and explicitly configured admin passwords are not printed. Disable this before first startup with `--myapp.login.show-initial-password=false`, or later in YAML. Keep output, the credentials file and clipboard history private. Change generated passwords in `login-credentials.properties` while stopped, then restart; explicit `myapp.login.*.password` settings take precedence. No shared weak password or authentication bypass is introduced.

Before Spring reads settings, missing bundled application/profile YAML files are copied into the working directory. Launch scripts/EXE select the JAR's directory; for direct Java launches, change directory there first. Existing root/config `.yml` or `.yaml` files are preserved; profiles are not activated merely by extracting their files. A read-only folder produces a warning, and settings editing requires a writable location. Existing external settings keep their values across upgrades.

New defaults use the desktop tree and the Greenwich example astronomy location (`51.4779`, `-0.0015`, `Europe/London`); saved reader preferences and existing coordinates are retained. The main port defaults to unprivileged `8443`, with additional HTTP on `8080`; neither implies HTTPS without SSL configuration. An old external YAML using port 443 must be changed or overridden explicitly on Linux. `run.sh` respects YAML port values. Remote access remains an explicit configuration choice.
