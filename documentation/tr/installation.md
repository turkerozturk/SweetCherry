# SweetCherry 1.0.0 — hazır Windows paketini kullanma

[Release sayfasından](https://github.com/turkerozturk/SweetCherry/releases/tag/v1.0.0) Windows 10/11 **64 bit** için bir ürün dosyası indirin:

- `SweetCherry-1.0.0-windows-x64-setup.exe`: klasör seçme sihirbazı ve isteğe bağlı kısayollar.
- `SweetCherry-1.0.0-windows-x64.zip`: tamamını çıkarıp `SweetCherry.exe` çalıştırabileceğiniz portable paket.

İkisinde de Java 17 vardır; ayrıca Java kurmanız gerekmez. GitHub’ın **Source code** ZIP’i hazır uygulama değildir. Kurucuyu ZIP’in içinden çalıştırmayın.

1. Kurucuda Documents/Desktop gibi yazılabilir bir klasör seçin. Varsayılan, kurucunun yanındaki `SweetCherry` klasörüdür. Yönetici yetkisi gerekmez.
2. Uygulamayı başlatın. Kontrol penceresinde başlangıç durumunu bekleyin; hazır olduğunda `http://localhost:8080` açılır. **Tarayıcıda Aç** düğmesi de kullanılabilir.
3. **Uygulama Klasörü** düğmesiyle `login-credentials.properties` dosyasını bulun. Kullanıcı adı `admin`, şifre `admin.password=` işaretinden sonraki değerdir. `user` hesabında `user.password=` değeri kullanılır. Anahtar adlarını kullanıcı adı olarak yazmayın.
4. Girişten sonra demo veri kaynağını seçin. Kendi notlarınızdan önce örnek CTB ile ekranları tanıyabilirsiniz.
5. Kapatmak için kontrol penceresindeki **SweetCherry’yi Durdur**, tepsi menüsü veya web arayüzündeki yönetici kapatma seçeneğini kullanın. Tarayıcıyı kapatmak uygulamayı durdurmaz.

EXE imzalanmamıştır; Windows uyarı gösterebilir. Dosyanın proje release sayfasından geldiğini kontrol edin. `SHA256SUMS.txt` dosya özetlerini içerir; yayıncı imzasının yerini tutmaz.

## Kendi notlarınız

`allTenants/demo.txt` tanımını ayrı bir `.txt` dosyasına kopyalayın. `name` alanını ve `datasource.url` yolunu mevcut CTB’nize göre değiştirin. Başlangıçta `custom.isWritable=false` kullanın. Menüden veri kaynaklarını yeniden yükleyip yeni tanımı seçin. CTB uygulama klasörü dışında kalabilir. Yol, SweetCherry’nin çalıştığı makinede erişilebilir olmalıdır.

**SweetCherry otomatik yedek almaz.** Önemli CTB’leri kendiniz yedekleyin. CherryTree ve SweetCherry’den aynı dosyaya eşzamanlı yazmayın. Uygulamayı kapatıp tüm klasörünü kopyalamak taşımayı kolaylaştırır; klasör dışındaki CTB’ler bu kopyaya dahil değildir.

Ayrıntılar: [tenant ayarları ve kaynak derleme](setup-and-run.md), [Windows paket yapısı](windows-bundled-distribution.md), [uzaktan erişim](network-access.md), [English installation guide](../en/setup-and-run.md).
