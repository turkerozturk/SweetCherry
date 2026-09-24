# SweetCherry kurulum ve kullanım kılavuzu

> Bu belge, henüz resmi bir SweetCherry sürümü yayımlanmadan önce `main` dalındaki kaynak kodu denemek isteyenler içindir. `main` dalı geliştirme aşamasındadır; özellikler ve ekranlar değişebilir.

## SweetCherry nedir?

SweetCherry, [CherryTree](https://www.giuspen.net/cherrytree/) ile oluşturulmuş **CTB (SQLite)** not veritabanlarını web tarayıcısında görüntüleyen bir yardımcı uygulamadır. CherryTree'nin yerine geçmez: notları oluşturmak ve düzenlemek için CherryTree, aynı CTB içeriğini SweetCherry'nin farklı web görünümleriyle incelemek için SweetCherry kullanılır.

Başlangıç için ikisini aynı bilgisayarda yan yana çalıştırabilirsiniz:

1. CherryTree'de birkaç düğüm oluşturup dosyayı `.ctb` biçiminde kaydedin.
2. SweetCherry'yi başlatın ve aşağıda anlatıldığı gibi CTB dosyasını veri kaynağı olarak tanıtın.
3. Tarayıcıdan giriş yapıp veri kaynağını seçin.

Önemli: Aynı CTB dosyasını CherryTree ve SweetCherry ile kullanırken önce yedek alın. Bu ilk denemede SweetCherry'yi yalnızca görüntüleme amacıyla kullanmanız önerilir. CherryTree'de açık olan dosyayı değiştirirken iki uygulamadan eşzamanlı yazma işlemi yapmayın.

## Gereksinimler

- Windows 10/11, güncel bir Linux dağıtımı veya macOS
- **64 bit JDK 17 veya daha yeni bir JDK** (Java 21 LTS de uygundur)
- İlk derleme sırasında internet bağlantısı
- Denemek için CherryTree ve bir `.ctb` dosyası

Ayrıca Maven kurmanız gerekmez. Projedeki Maven Wrapper, gereken Maven sürümünü ve bağımlılıkları ilk derlemede indirir.

Java'yı kontrol etmek için Terminal, PowerShell veya Komut İstemi'nde şunu çalıştırın:

```text
java -version
```

Komut bulunamazsa veya sürüm 17'den küçükse bir JDK kurup terminali yeniden açın. Örneğin [Eclipse Temurin](https://adoptium.net/temurin/releases/) veya [Microsoft Build of OpenJDK](https://learn.microsoft.com/java/openjdk/download) kullanılabilir. Scriptler de Java'yı ve ana sürüm numarasını kontrol ederek anlaşılır bir hata verir.

## CherryTree'yi hazırlama

CherryTree'nin Windows kurucusu/portable paketi, Linux paketleri ve macOS paketi resmi [CherryTree indirme sayfasında](https://www.giuspen.net/cherrytree/#downl) bulunur. CherryTree'yi kurun, yeni bir not ağacı oluşturun ve **SQLite tabanlı CTB** biçiminde kaydedin. Örneğin:

```text
C:/Users/kullanici/Documents/CTB/mydatabase.ctb
```

SweetCherry'nin doğrudan çalıştığı biçim CTB'dir. CTD veya şifreli CTX/CTZ gibi başka CherryTree biçimleri bu başlangıç akışının kapsamında değildir.

## Kaynak kodu indirme

1. [SweetCherry main.zip](https://github.com/turkerozturk/SweetCherry/archive/refs/heads/main.zip) dosyasını indirin.
2. ZIP dosyasının tamamını yazma izniniz olan bir klasöre çıkarın. Örneğin Windows'ta `C:/SweetCherry-main`.
3. Scriptleri ZIP'in içinden çalıştırmayın; önce mutlaka klasöre çıkarın.

Klasör yolunda boşluk bulunması desteklenir.

## En kolay ilk çalıştırma

### Windows

Çıkarılan proje klasöründeki `first-run.bat` dosyasına çift tıklayın. Script:

1. Java 17+ bulunduğunu denetler.
2. Maven Wrapper ile `target/SweetCherry.jar` dosyasını derler.
3. SweetCherry'yi başlatır.
4. Varsayılan tarayıcıda `http://localhost:8080` adresini açar.

Windows bilinmeyen bir `.bat` dosyası için uyarı gösterirse dosyanın GitHub'dan indirdiğiniz bu proje klasörüne ait olduğunu doğrulayın. Pencereyi kapatmak veya `Ctrl+C` tuşlarına basmak uygulamayı durdurur.

### Linux

Proje klasöründe terminal açıp çalıştırın:

```bash
sh ./first-run.sh
```

Dosya yöneticisinden çift tıklama davranışı masaüstü ortamına göre değiştiği için terminal komutu daha güvenilirdir. İsterseniz bir kez `chmod +x first-run.sh build.sh run.sh` çalıştırdıktan sonra `./first-run.sh` da kullanabilirsiniz.

### macOS

Finder'da `first-run.command` dosyasına çift tıklayın. macOS dosyayı ilk seferde engellerse Terminal'de proje klasörüne geçip şunları çalıştırın:

```bash
chmod +x first-run.command run.command
./first-run.command
```

Alternatif olarak her zaman şu komut kullanılabilir:

```bash
sh ./first-run.sh
```

Not: macOS akışı script düzeyinde hazırlanmıştır ancak henüz gerçek bir Mac üzerinde denenmemiştir.

## Derleme ve çalıştırmayı ayrı yapmak

Yalnızca derlemek için:

| Sistem | Komut veya dosya |
|---|---|
| Windows | `build.bat` |
| Linux/macOS | `sh ./build.sh` |

Derleme başarılı olduğunda çalıştırılabilir dosya `target/SweetCherry.jar` olur. Ayrıca Maven, doğrudan çalıştırılabilecek aşağıdaki klasör yapısını hazırlar:

```text
release/
├── SweetCherry.jar
├── CTBDATA/
│   └── demo.ctb
└── allTenants/
    └── demo.txt
```

`SweetCherry.jar` her derlemede güncellenir. `demo.ctb` ve `demo.txt` yalnızca hedefte yoksa kopyalanır; `release` altında kullanıcı tarafından değiştirilmiş demo dosyalarının üzerine yazılmaz.

Hazır demo ile denemek için terminalde `release` klasörüne geçip uygulamayı bu klasörü çalışma dizini yaparak başlatın:

```bash
cd release
java -jar SweetCherry.jar --server.port=8443 --server.http.port=8080 --myapp.openWebBrowserOnStartup=true
```

Giriş yaptıktan sonra veri kaynağı listesinde **Demo Database** görünür. Bunun nedeni `allTenants/demo.txt` içindeki `jdbc:sqlite:CTBDATA/demo.ctb` göreli yolunun `release` klasöründen çözülmesidir.

Daha sonraki çalıştırmalarda yeniden derlemek gerekmez:

| Sistem | Komut veya dosya |
|---|---|
| Windows | `run.bat` |
| Linux | `sh ./run.sh` |
| macOS | `run.command` veya `sh ./run.sh` |

Script kullanmadan eşdeğer komut:

```bash
java -jar target/SweetCherry.jar --server.port=8443 --server.http.port=8080
```

SweetCherry açık kaldığı sürece komut penceresi/terminal de açık kalmalıdır. Uygulamayı durdurmak için `Ctrl+C` kullanın.

### Kullanılan portlar

- Tarayıcı adresi: **`http://localhost:8080`**
- Dahili ikinci HTTP bağlantı noktası: `8443`

Mevcut kaynak ayarındaki ana port `443` olduğundan scriptler, Linux/macOS'ta yönetici yetkisi gerektirmemesi ve çakışma riskini azaltması için onu `8443` olarak değiştirerek başlatır. SSL yapılandırılmadığı için bu yerel başlangıç senaryosunda adres `http://` ile açılır; `https://` kullanmayın.

## Giriş yapma

Giriş ekranında iki yerleşik hesap vardır:

| Yetki | Kullanıcı adı | Şifre |
|---|---|---|
| Yönetici | `admin` | `adminPassword` |
| Normal kullanıcı | `user` | `password` |

İlk denemede yönetici hesabıyla giriş yapabilirsiniz. Bu bilgiler `src/main/resources/application.yml` içinde tanımlıdır ve derlemeden sonra JAR'ın içinde bulunur. Bunlar yalnızca yerel geliştirme/deneme varsayılanlarıdır; uygulamayı başka cihazların erişimine açmadan önce değiştirilmelidir.

## CTB dosyasını SweetCherry'ye tanıtma

SweetCherry, **çalıştırıldığı klasördeki** `allTenants` dizinini kullanır. Verilen scriptler uygulamayı proje kökünden çalıştırdığı için beklenen konum şöyledir:

```text
SweetCherry-main/
├── allTenants/
├── target/
│   └── SweetCherry.jar
└── run.bat, run.sh, ...
```

`allTenants` klasörü yoksa uygulama ilk açılışta otomatik oluşturur. Uygulamayı bir kez başlatıp durdurabilir veya klasörü kendiniz oluşturabilirsiniz.

Her CTB dosyası için `allTenants` içinde ayrı bir düz metin dosyası oluşturun. Dosya adı ve uzantısı uygulama açısından önemli değildir; açıklık için örneğin `mydatabase.txt` kullanabilirsiniz:

```properties
name=My Database
security-role=ADMIN
datasource.url=jdbc:sqlite:C:/Users/kullanici/Documents/CTB/mydatabase.ctb
datasource.driver-class-name=org.sqlite.JDBC
datasource.init-mode=always
```

Dikkat edilmesi gerekenler:

- `name`, SweetCherry'de listede görünecek addır; boşluk içerebilir ve her veri kaynağı için benzersiz olmalıdır.
- Windows yolunda `/` kullanmak en kolay ve güvenli yazımdır. Ters eğik çizgi kullanacaksanız Java properties biçimi nedeniyle `\\` yazmanız gerekir.
- Linux örneği: `datasource.url=jdbc:sqlite:/home/kullanici/Notlar/mydatabase.ctb`
- macOS örneği: `datasource.url=jdbc:sqlite:/Users/kullanici/Documents/mydatabase.ctb`
- CTB dosyası gerçekten mevcut olmalı ve SweetCherry'yi çalıştıran kullanıcı tarafından okunabilmelidir.
- `security-role` mevcut örnek ekranlarıyla uyumluluk için gösterilmiştir; güncel kaynak kodda giriş hesabının rolünü veya veri kaynağı yetkisini değiştirmez.
- `datasource.init-mode` de güncel çoklu veri kaynağı yükleyicisinde ayrıca işlenmez; örnek dosyalarla uyumluluk amacıyla bırakılabilir.
- Silme/yazma özelliğini ayrıca etkinleştiren ayar `custom.isWritable=true` değeridir. İlk denemede ve özgün CTB dosyanızda **bu satırı eklemeyin**; böylece SweetCherry'nin düğüm silme servisi yazılabilir sayılmaz.

Birden fazla CTB kullanacaksanız her biri için ayrı bir metin dosyası ekleyin.

## Veri kaynağını yükleme ve not ağacını görüntüleme

1. SweetCherry çalışırken `http://localhost:8080` adresini açın ve giriş yapın.
2. Yeni ayar dosyasını uygulama çalışırken eklediyseniz arayüzdeki **Yenile / Reload** düğmesine basın. Görünmezse uygulamayı durdurup yeniden başlatın.
3. Veri kaynağı açılır listesinden `name` alanında verdiğiniz adı seçin. Seçim değiştiğinde form otomatik gönderilir; gerekirse yanındaki **Yükle** düğmesine basın.
4. Ana sayfaya dönüldüğünde CTB içindeki kök düğümleri ve not ağacını görüntüleyebilirsiniz.

Oturum zaman aşımına uğrarsa veya çıkış yaparsanız yeniden giriş yapın. Veri kaynağı seçimi oturuma bağlı olduğundan listeden CTB'yi tekrar seçmeniz gerekebilir.

Yönetici hesabı veri kaynakları, ayarlar, bazı pano ve silme bağlantıları gibi yöneticiye ayrılmış ekranları gösterebilir. Ancak CTB üzerinde düğüm silme işleminin uygulanabilmesi ayrıca ilgili dosyada `custom.isWritable=true` olmasına bağlıdır. Özgün not arşiviniz üzerinde bu seçeneği kullanmadan önce mutlaka yedek alın.

## Çalışırken oluşan dosya ve klasörler

Bu yollar SweetCherry'nin **çalıştırıldığı klasöre** göre oluşur:

- `myapp.log`: Uygulama ilk çalıştırıldığında oluşturulur ve uygulama loglarını içerir. Sorun bildirirken konsol çıktısıyla birlikte incelenebilir.
- `allTenants/`: Yoksa ilk açılışta boş olarak oluşturulur. CTB bağlantı tanımlarının bulunduğu metin dosyaları buraya konur.
- `exportedFiles/`: İlgili dışa aktarma işlemi kullanıldığında oluşturulur. Düğümleri CTB olarak dışa aktaran akış `exportedFiles/exportednodes.ctb` dosyasını kullanır.

Not: Kaynak kodda bulunan bazı eski/alternatif dışa aktarma akışları `exportednodes.ctb` dosyasını doğrudan çalışma klasöründe oluşturabilir. Bu bölüm ileride dışa aktarma ekranları topluca gözden geçirilirken sadeleştirilecektir.

## Sık karşılaşılan sorunlar

### `java` komutu bulunamadı

JDK 17+ kurun, terminali/Komut İstemi'ni kapatıp yeniden açın ve `java -version` ile doğrulayın. Yalnızca eski bir JRE yerine JDK kurulması önerilir.

### İlk derleme uzun sürüyor

İlk çalıştırmada Maven ve proje bağımlılıkları indirilir. Sonraki derlemeler yerel önbelleği kullanır ve genellikle daha hızlıdır.

### `http://localhost:8080` açılmıyor

- Script penceresinin hâlâ açık ve uygulamanın çalışıyor olduğundan emin olun.
- Konsolda `APPLICATION FAILED TO START` veya port kullanım hatası arayın.
- 8080 ya da 8443 portunu başka bir uygulama kullanıyorsa komut satırında boş iki port seçebilirsiniz. Örneğin:

  ```bash
  java -jar target/SweetCherry.jar --server.port=18443 --server.http.port=18080
  ```

  Bu durumda tarayıcıdan `http://localhost:18080` adresini açın.

### CTB listede görünmüyor

- Ayar dosyasının `allTenants` klasöründe olduğunu kontrol edin.
- Uygulamayı proje kökünden, verilen scriptlerle başlatın.
- `name`, `datasource.url` ve `datasource.driver-class-name` satırlarının dolu olduğundan emin olun.
- `datasource.url` içindeki CTB yolunu ve dosya izinlerini kontrol edin.
- Arayüzde **Yenile / Reload** düğmesine basın veya uygulamayı yeniden başlatın.

### CherryTree'de yaptığım son değişiklik görünmüyor

Önce CherryTree'de CTB dosyasını kaydedin. SweetCherry tarafında sayfayı yenileyin; gerekirse veri kaynağını kapatıp yeniden seçin veya uygulamayı yeniden başlatın. Aynı dosyaya iki uygulamadan eşzamanlı yazmayın.

## Şimdilik kapsam dışında kalanlar

- Release paketi ve otomatik güncelleme
- Uygulamayı internete açma, HTTPS ve ters vekil yapılandırması
- CherrySync ve uzak MySQL/PostgreSQL veri kaynakları
- Üretim ortamı kullanıcı/şifre yönetimi
- macOS üzerinde doğrulanmış kurulum testi

Geliştirme/operasyon profilleri, scheduled task denemeleri ve uzak veritabanı sürücüleri için [isteğe bağlı özellikler](optional-features.md) belgesine bakın.
