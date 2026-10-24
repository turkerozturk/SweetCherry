# Bellek, HTTP ve dosya sınırları

## Raspberry Pi: `OutOfMemoryError: Java heap space`

Bu hata JVM heap sınırının dolduğunu bildirir. CTB/HTTP limitini artırmak Java’ya
RAM ayırmaz; tersine daha büyük iş yüklerine izin verebilir. Zengin metin kaydı
sırasında XML, JSON/Base64, ikili veriler ve açılmış resimler aynı anda bulunabilir.
Örneğin 4000×4000 boyutlu bir resim, PNG/JPEG dosyası küçük olsa da yalnız açılmış
4 kanallı piksel verisi için yaklaşık 61 MiB gerektirir; işlem kopyaları eklenir.

Önce Pi’de kullanılabilir belleğe ve Java’nın varsayılan heap sınırına bakın:

```sh
free -h
java -XshowSettings:vm -version
```

Aynı Pi’de MariaDB/PostgreSQL ve başka servisler çalışıyorsa onların belleğini de
hesaba katın. **Yeterli RAM varsa** örneğin 384 MiB heap ile başlatabilirsiniz:

```sh
cd ~/SweetCherry
JAVA_TOOL_OPTIONS='-Xms64m -Xmx384m' nohup ./run.sh > console-nohup.log 2>&1 < /dev/null &
```

Önce çalışan SweetCherry’yi arayüzden durdurun. `JAVA_TOOL_OPTIONS` burada yalnız
bu komut ve onun başlattığı süreçlere verilir; sonraki normal çalıştırmaları
değiştirmez. Aynı JVM içindeki uygulama restart’ında heap değişmez. Yeni heap
seçeneği için Java sürecinin kapatılıp bu komutla yeniden açılması gerekir.
Bellek uygunsa daha yüksek bir değer seçilebilir; 1 GB Pi’de kontrol etmeden
`-Xmx1g` vermeyin. Xmx sadece heap’tir; Java’nın toplam RAM kullanımı daha fazladır.
Heap başlangıçta belirlenir; `application.yml` içinden sonradan değiştirilemez.

Daha küçük çözünürlüklü resimler kullanmak, çok resimli içeriği birkaç düğüme
bölmek ve eklemeler arasında kaydetmek tepe bellek ihtiyacını azaltabilir. Bir
hata sonrasında bütünlüğü doğrulamak için uygulamayı yeniden başlatıp kaydı
kontrol edin. Sürerse hata yığınının ilk uygulama sınıfı/satırı ve heap sınırı,
sorunun hangi işlemde oluştuğunu belirlemeye yardımcı olur. Bu yama olası bütün
heap hatalarını çözmüş sayılmaz; mevcut bellek kullanımını kökten değiştirmez.

## Limitleri birbirinden ayırın

| İşlem | İlgili sınır |
| --- | --- |
| Sunucudaki CTB’yi içe aktarma | `myapp.ctb-import.max-bytes`, `max-records`; HTTP upload değildir |
| Rich text PNG/JPEG veya dosya ekleme | Tenant `custom.maxEmbeddedFileSizeMB`: varsayılan 9 **ondalık MB**, 1–20 aralığı |
| Rich text tek kayıt | En fazla 10 yeni nesne; resim/dosyalar birlikte 20.000.000 bayt; mevcut JSON ve resim piksel sınırları da uygulanır |
| Rich text resmin açılması | Boyutlar en fazla 8192×8192 ve toplam 16.000.000 piksel; dosya boyutu heap ihtiyacını göstermez |
| Rich text HTTP formu | `server.tomcat.max-http-form-post-size`: mevcut varsayılan `32MB`; iki Tomcat connector’a uygulanır |
| Multipart dosya yükleme | `spring.servlet.multipart.max-file-size` ve `max-request-size`; rich text JSON/Base64 formuyla aynı akış değildir |
| Reverse proxy | Proxy’nin body/request limiti ayrıca uygulanır |
| Java heap | JVM `-Xmx`; yukarıdaki limitlerden bağımsızdır |

Base64 ikili veriye göre yaklaşık üçte bir ek boyut üretir; form URL kodlaması,
XML/JSON ve diğer alanlar da isteği büyütür. Bu nedenle uygulama veri sınırıyla
HTTP form sınırının aynı sayı olması gerekmez. `32MB`, her olası 20 MB içeriğin
kodlandıktan sonra mutlaka sığacağı anlamına gelmez. HTTP sınırı aşılıyorsa ihtiyaca
göre artırın; heap hatasını çözmek için artırmayın. Çok büyük rich text işlemlerini
özellikle düşük bellekli Pi’de bölerek yapmak daha uygundur.

## Forwarded headers

Doğru konum `server.forward-headers-strategy`’dir; `server.ssl` altında değildir.
Depodaki ayar korunmaktadır:

```yaml
server:
  forward-headers-strategy: none
```

`none`, Spring’in her istemciden gelen `Forwarded`/`X-Forwarded-*` başlıklarını
kendiliğinden güvenilir saymasını engeller. SweetCherry’nin mevcut güvenilir proxy
HTTPS ayarları ayrı yapılandırılır; HTTP upload veya Java heap ayarıyla ilişkisi yoktur.

Kaynaklar:
- [Java 17: java komutu ve -Xmx](https://docs.oracle.com/en/java/javase/17/docs/specs/man/java.html)
- [JAVA_TOOL_OPTIONS](https://docs.oracle.com/javase/8/docs/technotes/guides/troubleshoot/envvars002.html)
- [Spring Boot 3.5 uygulama özellikleri](https://docs.spring.io/spring-boot/3.5/appendix/application-properties/)
