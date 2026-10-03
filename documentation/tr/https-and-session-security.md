# HTTP, HTTPS ve oturum güvenliği

SweetCherry yerel HTTP, uygulamanın kendi TLS bağlantısı ve HTTPS reverse proxy ile kullanılabilir. Bu seçenekler birbirinin yerine zorunlu tutulmaz. Ağ, sertifika üretimi, dış ayar dosyası, DNS, firewall ve sorun giderme adımları için [erişim kurulum rehberine](network-access.md) bakın.

| Kullanım | Ana bağlantı | Ek HTTP portu | Proxy ayarı | HSTS |
|---|---|---|---|---|
| Localhost/LAN | SSL kapalıysa HTTP | Mevcut `server.http.port` | Kapalı | Kapalı |
| Uygulama TLS | `server.port` ve `server.ssl.*` | Mevcut HTTP connector | Kapalı | Güvenilir sertifikalı hostname için isteğe bağlı |
| HTTPS proxy | Proxy dışarıya HTTPS, backend HTTP | Proxy mevcut HTTP porta bağlanabilir | Açık ve tek güvenilir peer IP | Güvenilir HTTPS hostname için açık |

`server.port` adı tek başına HTTPS anlamına gelmez: ana connector'ın TLS kullanmasını `server.ssl.*` ayarları belirler. Mevcut programatik ek connector `server.http.port` üzerinden HTTP sunar. Bu yama bu portları veya sertifika biçimlerini değiştirmez ve HTTP isteklerini genel olarak HTTPS'e zorlamaz. Mevcut self-signed, PKCS12/PEM ve uygun sertifika dosyalarıyla TLS yapılandırması kullanılmaya devam edebilir. Sertifika üretimine ait özel BAT dosyası bu değişikliğin taban commitinde izlenen dosyalar arasında bulunmadığından test edilmedi.

## Varsayılan oturum politikası

`server.servlet.session.cookie.http-only: true`, `same-site: lax` ve `tracking-modes: cookie` kullanılır. Oturum kimliği URL'ye eklenmez. `server.servlet.session.cookie.secure` özellikle sabitlenmez: Tomcat güvenli istekte Secure çerez üretir; doğrudan HTTP isteğinde yerel kullanım çalışır. Eski dış yapılandırmada `cookie.secure: false` varsa kaldırın. Yalnız HTTPS sunulan ayrı bir kurulumda `secure: true` kullanılabilir; bu durumda HTTP üzerinden oturum açılmasını beklemeyin.

SameSite ve CSRF token farklı korumalardır. CSRF koruması açık kalır. `Referrer-Policy: same-origin` aynı uygulama içindeki dönüş bağlantılarını korurken dış sitelere referer göndermez.

## HTTPS proxy kurulumu

Bu seçenek yalnız proxy TLS'i sonlandırıyorsa gereklidir. JAR dışındaki `application.yml` içinde **mevcut bölümlere birleştirin**; ikinci bir `myapp` veya `server` bölümü eklemeyin, diğer ayarları silmeyin:

```yaml
server:
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

Adres, SweetCherry'nin **TCP peer olarak gördüğü** proxy IP'sidir. Ağdaki tüm IP'lere, tüm LAN'a veya gelen bir client-IP başlığına güvenilmez. Proxy kapalıysa `proxy-https-enabled: false` ve boş trusted-proxy-address yeterlidir. Proxy HTTPS etkin olup peer boşsa uygulama açıklayıcı yapılandırma hatasıyla başlamaz.

Caddy yalnızca örnektir; başka proxy de aynı iki başlığı güvenilir biçimde **üzerine yazarak** gönderebilir:

```caddyfile
notes.example.org {
    reverse_proxy 192.168.0.5:8080 {
        header_up X-SweetCherry-Client-IP {remote_host}
        header_up X-SweetCherry-Forwarded-Proto {scheme}
    }
}
```

İlk başlık giriş sınırının istemci IP'sini, ikinci başlık HTTPS şemasını belirtir. Proxy dışındaki bağlantının protocol başlığı yok sayılır. Yalnız tam olarak tek `https` değeri kabul edilir; eksik, yinelenen veya virgüllü değerler kabul edilmez. Native TLS hiçbir zaman HTTP'ye düşürülmez. Host ve client-IP standart forwarded başlıklardan alınmaz; mevcut client-IP doğrulayıcı aynen çalışır. Backend HTTP kullanan proxy dış hostname'i Host başlığında korumalıdır. Dış HTTPS portu 443 değilse `proxy-https-port` ona göre değiştirilir; port istemci başlığından alınmaz.

`server.forward-headers-strategy: none` korunmalı: genel ForwardedHeaderFilter veya geniş güven listeli RemoteIpValve ile birlikte kullanılmamalıdır. Hem istemci IP'si hem protokol bilgisi uygulamanın izin verdiği peer üzerinden doğrulanır. Backend portunu WAN'a doğrudan açmayın; proxy ile uygulama arasındaki HTTP hattı güvenilir özel ağ üzerinde olmalı. Uzak ağlar arasında backend TLS veya güvenli tünel ayrıca değerlendirilebilir.

## HSTS ve self-signed sertifikalar

`myapp.security.hsts-enabled` varsayılan false'tur. True olduğunda yalnız güvenli isteklerde `Strict-Transport-Security: max-age=31536000` gönderilir; includeSubDomains ve preload kullanılmaz. HTTPS proxy üzerinden yanıt da güvenli kabul edilir. Self-signed denemelerinde, localhost veya HTTP-only kurulumlarda kapalı bırakın. HSTS tarayıcıda kalıcıdır; kapatmak daha önce kaydedilmiş politikayı hemen silmez. Bu yüzden yalnız güvenilir sertifikası devam ettirilecek hostname'de etkinleştirin.

Aynı hostname'in HTTP ve HTTPS adresleri aynı oturum çerezi adını paylaşır. Yerel HTTP için localhost/LAN adresini, WAN için HTTPS hostname'i kullanmak daha anlaşılırdır. Tek hostname üzerinden HTTP/HTTPS karıştırırken oturum geçişlerinin sorunsuz olacağı varsayılmamalıdır.

## Hata sayfaları

Tarayıcıya exception, stack trace, binding error, ham hata mesajı veya istek yolu gönderilmez. HTTP durumunun standart açıklaması ve rastgele takip numarası gösterilir. Aynı takip numarasıyla ayrıntı sunucu loguna yazılır. `myapp.debug` açık olsa bile bu genel hata sayfası teknik ayrıntı yayımlamaz; Session Info debug görünümü ayrı özelliktir.

## Kontrol sırası

1. Önce `mvnw.cmd test`, ardından `mvnw.cmd clean package` çalıştırın. Dağıtıma yeni JAR'ı ve birleştirilmiş dış ayarları alın.
2. Proxy yapılandırmasını doğrulayıp yeniden yükleyin; SweetCherry'yi yeniden başlatın. Eski tarayıcı çerezlerini temizleyerek veya yeni özel pencere açarak deneyin.
3. HTTPS hostname üzerinden oturumsuz `/settings` isteğinin Location değerinde `https://.../login` bulunduğunu doğrulayın; HTTP ara adımı olmamalı.
4. HTTPS giriş yanıtının Set-Cookie satırında Secure, HttpOnly, SameSite=Lax bulunmalı. HSTS etkinse onun başlığı da bulunmalı. Çerez değerlerini paylaşmayın.
5. Aynı uygulamaya localhost/LAN HTTP üzerinden ayrı pencerede giriş çalışmalı; HTTP yanıtında HSTS olmamalı. HTTP bağlantısı şifreli değildir.
6. HTTPS oturumu açma, çıkma, oturum süresi dolunca tekrar giriş ve CTB seçme akışlarını deneyin. WAN ve yerel erişimde giriş sınırlamasının clientIp ayrımı korunmalı.
7. Demo CTB seçiliyken geçersiz düğüm ID'si isteyin. Arayüzde stack trace yerine takip numarası, logda aynı numara olmalı.
8. Kendi SSL kurulumunuzu da kullanıyorsanız proxy ayarı kapalıyken HTTPS portunda Secure çerezi ve ek HTTP portunda yerel giriş denenmeli. Self-signed sertifika uyarısı, sertifikaya güven kararıdır; otomatik olarak atlanmaz.

Otomatik testler güvenilir/güvenilmeyen peer ayrımını, bozuk başlıkları, native TLS'in korunmasını, gerçek Tomcat session cookie üretimini, güvenli giriş yönlendirmesini, HSTS seçeneğini ve hata modelindeki bilgi sınırını kapsar. Tam kapsamlı penetrasyon testi değildir.

## Sonraki sertleştirme

CSP, önce report-only ve bütün editör/mindmap/görünüm akışlarıyla ayrı bir çalışmada ele alınacak. Bu yama mevcut script ve içerik özelliklerini engelleyen genel bir CSP eklemez. Güçlü yönetici parolası, kullanıcı rolü denemeleri ve geçici test CTB'sinde yazma endpointlerinin kontrolleri release öncesi devam eder.

## Teknik kaynaklar

- https://docs.spring.io/spring-boot/3.5/how-to/webserver.html
- https://docs.spring.io/spring-boot/3.3/reference/web/servlet.html
- https://tomcat.apache.org/tomcat-10.1-doc/api/org/apache/catalina/connector/Request.html
- https://docs.spring.io/spring-security/reference/servlet/exploits/headers.html
- https://caddyserver.com/docs/caddyfile/directives/reverse_proxy
