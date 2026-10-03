# SweetCherry’ye yerel ağdan ve internetten erişim

Bu rehber JAR dışında tutulan ayarları, sertifikaları, proxy ve ağ kurallarını birlikte açıklar. Kurulumdan sonra gerçek değerlerinizi özel bir kurulum notunda saklayın; şifreleri, özel anahtarları, DNS tokenlarını ve oturum çerezlerini Git’e veya destek mesajlarına eklemeyin. [Kurulum rehberi](setup-and-run.md) ve [oturum güvenliği](https-and-session-security.md) tamamlayıcı belgelerdir.

## Ortak örnek adresler

Bütün ağ örneklerinde aşağıdaki adresler kullanılır. Bunlar sizin cihazlarınızın adresleri değildir; her örnekte kendi karşılıklarını değiştirin. LAN adresleri özel ağ adresleridir; `203.0.113.10` dokümantasyon için ayrılmış, gerçek bağlantıda kullanılmayan bir WAN adresidir. `notes.example.org` örnek alan adıdır, sertifika almak için sahip olduğunuz alan adı gerekir.

| Bileşen | Örnek değer |
|---|---|
| Modem/yönlendirici LAN adresi | `192.168.0.1` |
| Proxy hostu | `192.168.0.2` |
| SweetCherry hostu (PC veya Raspberry Pi) | `192.168.0.5` |
| LAN istemcisi | `192.168.0.10` |
| Örnek WAN IPv4 | `203.0.113.10` |
| HTTPS alan adı | `notes.example.org` |
| Dinamik DNS alternatifi | `YOUR_SUBDOMAIN.duckdns.org` |
| Backend HTTP | `8080` |
| Ana uygulama portu | `8443` (run scriptleri ile) |
| İnternet HTTPS / HTTP | `443` / `80` |

Proxy ve SweetCherry aynı hosttaysa backend için `127.0.0.1` kullanılabilir. Ayrı Docker container’ları için `localhost` diğer container değildir. Docker ağları TCP peer adresini değiştirebilir; güvenilir proxy ayarını aşağıdaki gibi gerçekten görülen peer’a göre yapın.

## 1. Ayar dosyası ve iki portun anlamı

Dağıtım klasöründeki `SweetCherry.jar`, `application.yml`, `run.sh` veya `run.bat` birlikte bulunur. Scriptler kendi klasörüne geçer; Spring Boot buradaki dış `application.yml` dosyasını okur. Genel olarak dış ayarlar JAR içindekilerin üzerine gelir. Başka klasörden doğrudan `java -jar` çalıştırırsanız çalışma dizini farklı olur ve beklediğiniz ayar dosyası okunmayabilir. Gerekirse açıkça belirtin:

```sh
java -jar SweetCherry.jar --spring.config.additional-location=file:./application.yml --server.port=8443 --server.http.port=8080
```

Bu örnekte de `./` çalışma dizinidir. Dış dosya zorunlu olduğundan bulunamazsa hata verir. `spring.config.location` varsayılan arama konumlarını değiştirebilir; yalnız eklemek için `additional-location` kullanın. Aynı konumda `.properties` ve `.yml` birlikteyse `.properties` önceliklidir; eski çakışan dosyaları kontrol edin.

YAML parçalarını **mevcut `server`, `myapp` bölümlerine birleştirin**. Aynı anahtarı ikinci kez eklemeyin; girintide boşluk kullanın. Diğer ayarları silmeyin. Değişiklikten sonra SweetCherry yeniden başlatılmalıdır.

- Kaynak dosyada `server.port: 443` vardır. `run.sh`, `run.bat` ve `run.command` bunu komut satırından `8443` yapar; komut satırı dış YAML’den önceliklidir. Burada örnekler scriptlerin portlarını kullanır.
- `server.port`, **SSL yapılandırılmadığında HTTP** çalışır. `8443` veya `443` sayısı tek başına HTTPS açmaz.
- `server.http.port: 8080` kodda eklenen ikinci connector’dır; her zaman HTTP’dir. İki portu aynı yapmayın. Mevcut sürümde bu connector’ı kaldıran ayrı bir ayar yoktur; istenmeyen erişimi firewall ile sınırlandırın.
- `server.address: 127.0.0.1` iki connector’ı yalnız aynı hosttan erişilebilir yapar. `0.0.0.0` IPv4 arayüzlerinde dinler; bu **erişim izni veya güvenlik duvarı kuralı değildir**.
- Mevcut uygulama HTTP’yi genel olarak HTTPS’e zorlamaz. Native TLS açılınca ek HTTP portu kapanmaz. Caddy’nin dış `80 → 443` yönlendirmesi ayrı bir davranıştır.

## 2. HTTP: localhost ve LAN

Yalnız aynı bilgisayarda:

```yaml
server:
  address: 127.0.0.1
  ssl:
    enabled: false
  forward-headers-strategy: none
myapp:
  login:
    trusted-proxy-address: ""
  security:
    proxy-https-enabled: false
    hsts-enabled: false
```

Script ile başlatın; `http://localhost:8080` açın. LAN’dan erişmek için yalnız `server.address` değerini `0.0.0.0` yapın ve host firewall’da gerekli LAN istemcilerinin TCP 8080 erişimine izin verin. Diğer cihazdan `http://192.168.0.5:8080` açın. Telefonda `localhost` telefonun kendisidir.

HTTP’de şifre ve içerik ağ üzerinde şifrelenmez. LAN’a güvenmek bu gerçeği değiştirmez. Bu kurulumda modemden internete port yönlendirmeyin. Proxy yoksa proxy ayarları kapalı ve trusted peer boş kalmalıdır.

## 3. Uygulamanın kendi HTTPS’i: self-signed

Kaynak YAML’de PEM sertifika/anahtar ve PKCS12 örnekleri yorum olarak bulunur. Sertifika üretmek için daha önce kullanılan özel BAT dosyası güncel depoda bulunmuyor. Aşağıdaki işlem JDK’nin `keytool` aracıyla alternatif bir PKCS12 dosyası üretir; eski BAT’ın aynısı olduğu iddia edilmez.

Dağıtım klasöründe `certs` klasörü oluşturun. Windows CMD’de aşağıdaki komutu **tek satır** çalıştırın (Linux/macOS’ta da keytool ile çalışır):

```text
keytool -genkeypair -alias sweetcherry -keyalg RSA -keysize 3072 -validity 365 -storetype PKCS12 -keystore certs/sweetcherry.p12 -dname "CN=notes.example.org" -ext "SAN=dns:notes.example.org,dns:localhost,ip:127.0.0.1,ip:192.168.0.5"
```

Gerçek hostname ve LAN adresini SAN listesine yazın; tarayıcı adresi ile sertifika eşleşmelidir. Araç keystore şifresini etkileşimli sorar. Özel anahtar içeren `.p12` dosyasını yayımlamayın.

```yaml
server:
  address: 0.0.0.0
  ssl:
    enabled: true
    key-store: file:./certs/sweetcherry.p12
    key-store-type: PKCS12
    key-store-password: ${SWEETCHERRY_KEYSTORE_PASSWORD}
    key-alias: sweetcherry
  forward-headers-strategy: none
myapp:
  login:
    trusted-proxy-address: ""
  security:
    proxy-https-enabled: false
    hsts-enabled: false
```

`SWEETCHERRY_KEYSTORE_PASSWORD` ortam değişkenini uygulamayı başlatan süreç için tanımlayın. Scriptle `https://192.168.0.5:8443` veya sertifikadaki hostname’i açın. Host firewall’da gereken istemcilere TCP 8443 izni verin. Uygulama hâlâ HTTP 8080 dinler; istemiyorsanız erişimini engelleyin.

Self-signed sertifika tarayıcı tarafından kendiliğinden güvenilir sayılmaz. Sertifika kimliğini doğrulayıp kendi cihazınıza güvenilir biçimde kurmanız gerekir; uyarıyı alışkanlıkla atlamak çözüm değildir. Android/tarayıcı güven deposu davranışı farklı olabilir. WAN için otomatik yenilenen, güvenilir sertifikalı proxy daha kolaydır. Self-signed kurulumda HSTS kapalı kalsın.

PEM alternatifinde PKCS12 satırları yerine şunlar kullanılabilir:

```yaml
server:
  ssl:
    enabled: true
    certificate: file:./certs/certificate.crt
    certificate-private-key: file:./certs/private.key
```

`file:` disk dosyasıdır; göreli yol çalışma dizinine göredir. `classpath:` paketlenmiş uygulama kaynağıdır; JAR yanına dosya koymak onu classpath’e eklemez. PEM ve keystore yöntemlerini aynı SSL bölümünde karıştırmayın.

## 4. Uygulamanın kendi HTTPS’i: Let’s Encrypt ve dinamik DNS

Dinamik DNS yalnız değişen WAN adresini hostname’e bağlar; NAT/CGNAT, firewall veya sertifika sorunlarını çözmez. DuckDNS kullanılıyorsa tabloda `notes.example.org` geçen **bütün** yerleri sahip olduğunuz `YOUR_SUBDOMAIN.duckdns.org` ile değiştirin. Modemde veya sürekli çalışan hostta sağlayıcının güncelleme istemcisini kurun. Tokenı özel tutun; DNS kaydı LAN adresine değil, dış IPv4 adresine gitmelidir. IPv6 kaydı varsa onun erişimini de ayrıca doğrulayın.

Linux için Certbot’un işletim sisteminize uygun resmi kurulumunu izleyin. Aşağıdaki standalone örneğinde dış TCP 80 modemden SweetCherry hostuna yönlendirilmiş, host firewall izin vermiş ve 80 portu başka süreç tarafından kullanılmıyor olmalıdır:

```sh
sudo certbot certonly --standalone -d notes.example.org
```

HTTP-01 doğrulaması dış port **80** gerektirir; onu 8080’e taşımak çözüm değildir. Port 80 açılamıyorsa DNS-01 mümkündür: DNS sağlayıcısında TXT kaydı oluşturulur. DuckDNS TXT API sunar; otomasyon için seçtiğiniz ACME aracının uygun sağlayıcı entegrasyonu gerekir. Elle yapılan DNS doğrulaması tek başına otomatik yenileme sağlamaz. DNS-01 sertifika verir ama CGNAT arkasındaki uygulamayı erişilebilir yapmaz.

Certbot’un ürettiği `fullchain.pem` ve `privkey.pem` dosyalarını uygulamanın kullanıcı hesabının okuyabileceği **korumalı** konuma dağıtın; `/etc/letsencrypt` izinlerini herkese açmayın. Örneğin `/opt/sweetcherry/certs/`:

```yaml
server:
  address: 0.0.0.0
  ssl:
    enabled: true
    certificate: file:/opt/sweetcherry/certs/fullchain.pem
    certificate-private-key: file:/opt/sweetcherry/certs/privkey.pem
  forward-headers-strategy: none
myapp:
  login:
    trusted-proxy-address: ""
  security:
    proxy-https-enabled: false
    hsts-enabled: true
```

Modemde dış TCP **443 → 192.168.0.5:8443**, sertifika doğrulaması için TCP **80 → 192.168.0.5:80** olur. 8080’i internete açmayın. `https://notes.example.org` kullanın. Bu örnek HTTP yönlendirme sunucusu kurmaz; Certbot standalone yalnız doğrulama sırasında çalışır.

Yenileme zorunludur: Certbot’un timer/cron durumunu ve `sudo certbot renew --dry-run` sonucunu kontrol edin. Başarılı yenilemede bir deploy hook sertifikaları korumalı hedefe dağıtmalı ve SweetCherry servisinin yeni dosyaları yüklemesi için yeniden başlatmalıdır. Bu rehberdeki basit PEM ayarlarında kendiliğinden hot reload varsaymayın. PKCS12 kullanırsanız her yenilemede yeniden üretip dağıtmanız gerekir. Yenileme işlemini ve servis adını kendi kurulum notunuza yazın. HSTS’yi ancak güvenilir sertifikayı sürekli yenileyebileceğiniz hostname’de açın.

## 5. Caddy HTTPS proxy (ayrı host)

Örnek trafik yolu: telefon → DNS/WAN → modem → `192.168.0.2:443` Caddy → `192.168.0.5:8080` SweetCherry.

SweetCherry’nin **JAR dışındaki** dosyasına birleştirin:

```yaml
server:
  address: 0.0.0.0
  ssl:
    enabled: false
  forward-headers-strategy: none
  servlet:
    session:
      tracking-modes: cookie
      cookie:
        http-only: true
        same-site: lax
myapp:
  login:
    trusted-proxy-address: 192.168.0.2
  security:
    proxy-https-enabled: true
    proxy-https-port: 443
    hsts-enabled: true
```

`cookie.secure: false` eski dış dosyada varsa kaldırın. Burada `secure` sabitlenmez: HTTPS olarak doğrulanan istekte Secure üretilir; doğrudan yerel HTTP oturumu da çalışabilir. Yalnız HTTPS kullanacaksanız `secure: true` seçilebilir, HTTP oturumunun çalışmasını beklemeyin.

Caddyfile:

```caddyfile
notes.example.org {
    reverse_proxy 192.168.0.5:8080 {
        header_up X-SweetCherry-Client-IP {remote_host}
        header_up X-SweetCherry-Forwarded-Proto {scheme}
    }
}
```

Caddy kurulumu mevcut kabul edilir. Yapılandırmayı kendi host/container ortamınızda `caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile` ile doğrulayın; ardından kendi servis/container yönteminizle yeniden yükleyin. SweetCherry’yi de yeniden başlatın. Caddy hostname’in DNS kaydı ve erişilebilir 80/443 üzerinden sertifikayı otomatik alır/yeniler ve HTTP’yi HTTPS’e yönlendirir. Caddy veri dizini kalıcı ve yazılabilir olmalıdır; container silinince sertifika verisini kaybetmemek için kalıcı volume kullanın.

Modemde TCP 80/443 yalnız **proxy hostuna** gider. Docker kullanılıyorsa bu portlar hosta yayımlanmalıdır; yalnız `expose` WAN erişimi sağlamaz. Host firewall ve Docker’ın oluşturduğu yönlendirme kuralları birlikte kontrol edilmelidir. UDP 443 HTTP/3 için isteğe bağlıdır; TCP HTTPS için şart değildir.

Backend host firewall’da TCP 8080’e proxy hostundan izin verin. LAN istemcilerinin doğrudan HTTP erişimi isteniyorsa onlar için de dar kapsamlı izin ekleyin. Ana 8443 portunu dışa açmayın. Proxy ile backend farklı güvenilmeyen ağlardaysa aralarına TLS veya güvenli tünel gerekir.

`trusted-proxy-address` internetteki telefon IP’si değildir; SweetCherry’ye TCP bağlantısını kuran proxy peer’ıdır. Docker/NAT nedeniyle bu adres değişebilir. Kayıtlardan veya sunucu bağlantı gözleminden doğrulayın. Eşleşmiyorsa bütün LAN’a güven vermeyin; tek doğru peer’ı bulun. Aynı hostta proxy varsa, Docker ayrımı yoksa `127.0.0.1` kullanılabilir.

## 6. Başka bir reverse proxy

Caddy zorunlu değildir. Proxy’ye şu sözleşmeyi uygulayın:

1. Dış bağlantıda güvenilir HTTPS sertifikası ve yenileme kullanın; HTTP’den HTTPS’e yönlendirmeyi proxy’de yapın.
2. Backend’e istemcinin gönderdiği aynı adlı başlıkları **geçirmeyin/eklemeyin; üzerine yazın**: `X-SweetCherry-Client-IP` doğrulanmış istemci IP’si, `X-SweetCherry-Forwarded-Proto` dış bağlantı HTTPS ise tam olarak `https` olmalıdır.
3. Backend HTTP’de dış hostname’i `Host` başlığında koruyun. TLS şemasını backend bağlantısının HTTP olmasına göre üretmeyin.
4. SweetCherry’de tek gerçek proxy peer’ını, `proxy-https-enabled: true`, dış HTTPS portunu ve `forward-headers-strategy: none` ayarını kullanın.
5. Standart `X-Forwarded-For` veya `X-Forwarded-Proto` tek başına bu uygulamanın özel sözleşmesinin yerine geçmez. Genel forwarded-header filtresini ayrıca açmayın.

Proxy önünde başka CDN/proxy varsa istemci IP’si zincirini güvenilir upstream listesiyle o proxy’de çözmek gerekir. İstemcinin kendi verdiği bir IP’yi doğru kabul etmek giriş sınırlamasını aşılabilir hale getirir. Ayar sözdizimi ürün/sürüme göre değiştiği için rastgele bir hazır parçayı kopyalamayın; ürünün resmi belgesine göre aynı sözleşmeyi kurun.

## 7. Firewall, NAT ve içeriden/dışarıdan erişim

| Kurulum | Modem WAN yönlendirmesi | SweetCherry hostunda gerekli giriş |
|---|---|---|
| Yalnız localhost | Yok | LAN/WAN izni yok |
| LAN HTTP | Yok | Gerekli LAN istemcileri → TCP 8080 |
| LAN native TLS | Yok | Gerekli LAN istemcileri → TCP 8443 |
| Native TLS + HTTP-01 WAN | TCP 443 → `.5:8443`; TCP 80 → `.5:80` | TLS 8443 ve doğrulama 80 |
| HTTPS proxy WAN | TCP 80/443 → `.2:80/443` | Proxy `.2` → TCP 8080; istenirse ayrıca LAN |

- Modem NAT yönlendirmesi, modem firewall ve host firewall farklı katmanlardır. Uygulamanın dinlemesi, bu katmanların izin verdiği anlamına gelmez. Firewall’ı tümden kapatmayın veya hostu DMZ’ye koymayın.
- DHCP rezervasyonu ile proxy ve backend adreslerini sabitleyin; yeniden başlatmada NAT kuralı eski cihaza gitmesin.
- Windows’ta kuralın TCP portunu, kaynak IP kapsamını ve aktif ağ profilini kontrol edin. Linux’ta kullanılan firewall’a göre nftables/UFW/firewalld kurallarını inceleyin. Uzaktan yönetimde SSH erişimini kesen bir kuralı denemeyin.
- Modemin WAN adresi özel adres veya `100.64.0.0/10` ise upstream NAT/CGNAT olabilir. Çift modem varsa iki NAT katmanını da kontrol edin. CGNAT’ta kendi modeminizin port yönlendirmesi yeterli değildir; sağlayıcıdan erişilebilir IP veya ayrıca yapılandırılmış VPN/tünel gerekir.
- IPv6’da IPv4 NAT kuralı koruma sağlamaz; host/router IPv6 firewall’ını ayrıca kontrol edin. Çalışmayan AAAA kaydı bazı istemcilerin erişimini bozabilir.
- İçeriden dış hostname başarısız, mobil veriyle başarılıysa NAT loopback/hairpin eksik olabilir. LAN DNS override/split DNS ile **aynı hostname’i proxy’nin LAN IP’sine** çözebilirsiniz; bu sertifika eşleşmesini korur. HTTPS’i LAN IP’siyle açmak hostname sertifikasına uymaz.
- LAN’daki `localhost` testi dış erişim testi değildir. Telefonda Wi-Fi’yi kapatıp mobil veriyle dış hostname’i deneyin.
- HSTS tarayıcıda kalır; HTTP’ye dönmek veya sertifika uyarısını geçmek zorlaşabilir. Kalıcı HTTPS planı yoksa etkinleştirmeyin. Aynı hostname’in HTTP/HTTPS oturumlarını karıştırmayın; yerel HTTP için LAN adresi, WAN için HTTPS hostname kullanın.

## 8. Sorunu katman katman bulma

Sırayla ilerleyin; bütün ayarları aynı anda değiştirmeyin.

1. Hostta `http://127.0.0.1:8080/actuator/health` yanıt veriyor mu? Yalnız belirli LAN IP’sine bind ettiyseniz o IP’yi deneyin. Konsol/logda port, bind veya sertifika hatası var mı?
2. LAN istemcisinden `http://192.168.0.5:8080/actuator/health` erişiliyor mu? Proxy’nin kendisinden backend’e de deneyin. Windows PowerShell’de `Test-NetConnection 192.168.0.5 -Port 8080`; Linux’ta `ss -ltn` dinleme portlarını gösterir. Sağlık yanıtı tüm kullanıcı işlemlerinin doğru olduğunu kanıtlamaz.
3. `nslookup notes.example.org` beklenen WAN adresini veriyor mu? LAN split DNS varsa içeride farklı sonuç normal olabilir.
4. Mobil veriyle HTTPS çalışıyor mu? Çalışmıyorsa router NAT, CGNAT, host/container firewall ve sertifika doğrulama yolunu inceleyin.
5. Oturumsuz `curl -I https://notes.example.org/settings` HTTPS `/login` yönlendirmesi göstermeli. HTTPS giriş oturumunda çerez özellikleri `Secure`, `HttpOnly`, `SameSite=Lax`; HSTS açıkken HTTPS yanıtta HSTS bulunmalı. `curl -k` kullanarak sertifika sorununu gizlemeyin; Set-Cookie değerini paylaşmayın.
6. Yeni özel tarayıcı penceresinde giriş → CTB yükleme → düğüm açma → çıkış deneyin. Eski oturum, eski CSRF tokenı veya eski tenant sekmesi yanlış sonuca götürebilir. CSRF veya tenant kontrolünü kapatarak çözmeyin.

| Belirti | Önce bakılacak yer |
|---|---|
| Connection refused | Süreç çalışıyor mu, doğru port/IP’de dinliyor mu? |
| Timeout | Rota, firewall, NAT, DNS/AAAA, CGNAT |
| Proxy 502 | Proxy’den backend bağlantısı, yanlış IP/port veya HTTP/HTTPS upstream seçimi |
| Sertifika uyarısı | SAN/hostname, süre, saat, güven zinciri; self-signed güven kurulumu |
| HTTP login yönlendirmesi / Secure eksik | Dış YAML gerçekten yüklü mü, peer doğru mu, özel proto başlığı üzerine yazılıyor mu? |
| Bütün kullanıcılar aynı IP | Proxy/Docker yolu ve istemci başlığı; güven listesi genişletilmemeli |
| 403 / Forbidden | Eski/missing CSRF, rol, tenant görünüm belirteci, oturum; log takip numarası |
| Login tekrar açılıyor | Süre dolması, engelleme, cookie Secure/hostname/protokol karışması |
| Port already in use | Aynı portlu connector’lar veya başka süreç; firewall sorunu değildir |

## 9. Git dışında kalan kurulum kaydı ve destek sorusu

Özel notunuzda şu bilgileri tutun: kurulum klasörü/çalışma dizini, Java sürümü, JAR sürümü/commit, dış YAML’nin **gizli değerleri çıkarılmış** kopyası, çalıştırma komutu veya systemd birimi, servis kullanıcısı, sertifika dosya yolları/yenileme hook’u, DNS güncelleme yöntemi, Caddyfile/container volume ve port eşlemeleri, DHCP rezervasyonları, modem NAT ve host firewall kuralları, CTB ve ayar yedeği/geri yükleme yöntemi. Şifre/token/özel anahtarları bu notun yayımlanan kopyasına koymayın.

Bir yapay zekâya veya destek kanalına şu şablonla danışabilirsiniz:

> SweetCherry [commit/sürüm], Java [sürüm], işletim sistemi [...] kullanıyorum. Erişim biçimi [LAN HTTP / native TLS / HTTPS proxy]. Trafik yolu [istemci → router → proxy → backend]; gerçek adresleri tutarlı örneklerle değiştirdim. Hostta sağlık testi [...], proxy’den backend testi [...], mobil veri testi [...] sonucu verdi. DNS A/AAAA sonucu [...]. Beklenen [...] ama görülen [...]. Dış YAML, port/firewall eşlemeleri ve hatanın takip numarası aşağıda; şifre, token, cookie ve private key çıkarıldı. Sorun hangi katmanda olabilir? Mevcut yerel HTTP ve WAN HTTPS kullanımını bozmadan nasıl doğrularım?

Giriş şifresini test şifresi olarak bırakmayın. Şifre değişikliği, güncellemeler ve CTB yedeği internet yayınının parçasıdır. HTTPS kurulması uygulamanın bütün güvenlik kontrollerinin doğrulandığı anlamına gelmez.

## Resmi kaynaklar

- Spring Boot dış yapılandırma ve öncelik: https://docs.spring.io/spring-boot/3.5/reference/features/external-config.html
- Spring Boot TLS / ek connector: https://docs.spring.io/spring-boot/3.5/how-to/webserver.html
- JDK keytool: https://docs.oracle.com/en/java/javase/17/docs/specs/man/keytool.html
- Let’s Encrypt doğrulama yöntemleri: https://letsencrypt.org/docs/challenge-types/
- Certbot kurulum ve yenileme: https://certbot.eff.org/instructions ve https://eff-certbot.readthedocs.io/en/stable/using.html
- DuckDNS DNS ve TXT API: https://www.duckdns.org/spec.jsp
- Caddy otomatik HTTPS: https://caddyserver.com/docs/automatic-https
- Caddy proxy: https://caddyserver.com/docs/caddyfile/directives/reverse_proxy
