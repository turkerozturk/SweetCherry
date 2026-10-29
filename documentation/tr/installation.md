# SweetCherry 1.4.0 — hazır Windows paketini kullanma

[Release sayfasından](https://github.com/turkerozturk/SweetCherry/releases/tag/v1.4.0) Windows 10/11 **64 bit** için bir ürün dosyası indirin:

- `SweetCherry-1.4.0-windows-x64-setup.exe`: klasör seçme sihirbazı ve isteğe bağlı kısayollar.
- `SweetCherry-1.4.0-windows-x64.zip`: tamamını çıkarıp `SweetCherry.exe` çalıştırabileceğiniz portable paket.

İkisinde de Java 17 vardır; ayrıca Java kurmanız gerekmez. GitHub’ın **Source code** ZIP’i hazır uygulama değildir. Kurucuyu ZIP’in içinden çalıştırmayın.

1. Kurucuda Documents/Desktop gibi yazılabilir bir klasör seçin. Varsayılan, kurucunun yanındaki `SweetCherry` klasörüdür. Yönetici yetkisi gerekmez.
2. Uygulamayı başlatın. Kontrol penceresinde başlangıç durumunu bekleyin; hazır olduğunda `http://localhost:8080` açılır. **Tarayıcıda Aç** düğmesi de kullanılabilir.
3. **Uygulama Klasörü** düğmesiyle `login-credentials.properties` dosyasını bulun. Kullanıcı adı `admin`, şifre `admin.password=` işaretinden sonraki değerdir. `user` hesabında `user.password=` değeri kullanılır. Anahtar adlarını kullanıcı adı olarak yazmayın.
4. Girişten sonra demo veri kaynağını seçin. Kendi notlarınızdan önce örnek CTB ile ekranları tanıyabilirsiniz.
5. Kapatmak için kontrol penceresindeki **SweetCherry’yi Durdur**, tepsi menüsü veya web arayüzündeki yönetici kapatma seçeneğini kullanın. Tarayıcıyı kapatmak uygulamayı durdurmaz.

EXE imzalanmamıştır; Windows uyarı gösterebilir. Dosyanın proje release sayfasından geldiğini kontrol edin. `SHA256SUMS.txt` dosya özetlerini içerir; yayıncı imzasının yerini tutmaz.

## Kendi notlarınız

Yönetici olarak veri kaynağı menüsündeki tenant sihirbazını kullanabilirsiniz. Masaüstü modunda mevcut yerel/ağ paylaşımı CTB dosyasını Gözat ile seçin; dosya kopyalanmaz. Uzak SweetCherry sunucusu laptopunuzdaki dosya yoluna kendiliğinden erişemez.

Manuel yöntem de korunur: `allTenants/demo.txt` tanımını ayrı bir `.txt` dosyasına kopyalayın. `name` alanını ve `datasource.url` yolunu mevcut CTB’nize göre değiştirin. Başlangıçta `custom.isWritable=false` kullanın. Menüden veri kaynaklarını yeniden yükleyip yeni tanımı seçin. CTB uygulama klasörü dışında kalabilir. Yol, SweetCherry’nin çalıştığı makinede erişilebilir olmalıdır.

**SweetCherry otomatik yedek almaz.** Önemli CTB’leri kendiniz yedekleyin. CherryTree ve SweetCherry’den aynı dosyaya eşzamanlı yazmayın. Uygulamayı kapatıp tüm klasörünü kopyalamak taşımayı kolaylaştırır; klasör dışındaki CTB’ler bu kopyaya dahil değildir.

Ayrıntılar: [tenant ayarları ve kaynak derleme](setup-and-run.md), [Windows paket yapısı](windows-bundled-distribution.md), [uzaktan erişim](network-access.md), [English installation guide](../en/setup-and-run.md).

## Sunucu veritabanları ve ayarlar

SQLite, MariaDB/MySQL ve PostgreSQL tenant'ları arasında geçiş için global application.yml dialect değişimi gerekmez. Sunucu veritabanında uyumlu CherryTree şeması/içerik önceden bulunmalıdır. Tenant seçmek SQLite'tan sunucuya aktarım yapmaz; SweetCherrySync ayrı projedir. `custom.isWritable=true` SQL yetkisi kazandırmaz. Senkronizasyon hedefindeki düzenlemeler sonraki aktarımda üzerine yazılabilir.

System menüsünde seçilmiş güvenlik dışı uygulama ayarları ve onaylı yeniden başlatma vardır. Masaüstü kontrol penceresindeki yerel YAML editörü daha geniş ayarları yönetebilir. Güvenlik ve ağ ayarlarını ne yaptığınızı bilerek değiştirin.

## 1.0.0'dan yükseltme

SweetCherry'yi durdurun. Uygulama klasörünüzü ve klasör dışındaki CTB'leri koruyun; `allTenants`, harici YAML, `login-credentials.properties` ve gerekli export dosyalarını saklayın. Yeni paketi ayrı bir klasöre açın; kendi ayar/verilerinizi seçerek taşıyın. Demo dosyalarıyla kendi verinizi veya parolanızı değiştirmeyin; yeni runtime klasörünü eski Java dosyalarıyla karıştırmayın. Yol değiştiyse kısayolları yeniden oluşturun. Eski harici YAML override'ları yeni varsayılanlardan önce uygulanmaya devam eder.

Eski CTB'nin desteklenen şema güncellemesi dosyayı değiştirir; izin vermeden önce ayrıca yedek alın. Daha fazla bilgi: [veritabanı bağlantı sorunları](database-connection-troubleshooting.md).

## Dağıtımdaki hızlı başlangıç kılavuzları

`CTBDATA/sweetcherry-hizli-baslangic.ctb` ve `CTBDATA/sweetcherry-quickstart-en.ctb`, Türkçe ve İngilizce hızlı başlangıç kılavuzlarıdır. Veri kaynağı listesinden **SweetCherry Quick Start (TR)** veya **SweetCherry Quick Start (EN)** seçilebilir. `allTenants/quickstart-tr.txt` ve `quickstart-en.txt` bağlantıları salt okunurdur. Kılavuzlar resimsizdir; TODO içerikleri sonraki kullanım için boş bırakılmıştır. CherryTree ile de açılabilirler.

**Tenant ne demek?** SweetCherry'de kullanıcıya sunulan bir veri kaynağı bağlantı tanımıdır. Teknik ad, uygulamanın çoklu veritabanı (multitenancy) yapısından gelir. Masaüstü uygulamasında dosya açma/kapatmaya benzer biçimde veritabanları arasında geçiş yaparsınız. `allTenants` içindeki dosya notların kendisini değil, veritabanının yolunu/bağlantısını ve açılma ayarlarını tutar. Bir CTB'ye birden fazla tanımla bağlanılabilir; tanımı silmek CTB'yi silmez.

## Editörde küçük yardımcılar

Zengin metnin görsel düzenleme modunda, satır başında yalnız boşluk veya tab sonrasında bulunan `☐`, `☑`, `☒` işaretlerine tıklamak bu üç durumu sırayla değiştirir. Önünde başka metin veya nesne olan kutular değişmez. Değişiklik Kaydet ile veritabanına yazılır; Geri Al/Yinele desteklenir. Metin/önizleme modunda kutular normal metindir.

Zengin metin ve düz metin editörlerindeki **33~** düğmesi imlece 33 tilde ve satır sonu ekler; seçili metin varsa onun yerine ekler. Ctrl+R tarayıcının sayfayı yenileme kısayolu olarak korunur.
