# SweetCherry ayarları ve bağlantı portları

Çalışan uygulamanın yöneticiye açık `/settings` sayfası seçili ayarları ve kısa açıklamalarını gösterir. Şifre, private key ve keystore şifreleri bu listeye alınmaz. Ayarlar sayfası ayar değiştirme formu değildir.

## Ayarlar nereden okunur?

Dağıtım klasöründe, `SweetCherry.jar` yanında bulunan `application.yml` dosyasını düzenleyin ve uygulamayı yeniden başlatın. Çalışma klasörünün doğru olduğundan emin olun. Komut satırı, ortam değişkenleri ve etkin Spring profilleri dosyadaki değerleri geçersiz kılabilir; sayfa çalışan uygulamanın okuyabildiği değerleri gösterir. YAML içindeki aynı `server` veya `myapp` bölümünü ikinci kez açmak yerine mevcut bölümle birleştirin.

Tenant bağlantı ayarları `allTenants/<dosya>.txt`, login şifreleri `login-credentials.properties` içindedir. Tenant ayarları ile uygulama ayarları ayrıdır.

## Ayarlar 443 gösterirken adres çubuğu neden 8080 gösteriyor?

`server.port` ana Tomcat connector portudur. SweetCherry ayrıca `server.http.port` üzerinde bir HTTP connector açar. İkisi ayrı dinleyicidir ve farklı port numaraları kullanmalıdır. Mevcut varsayılan örnekte ana port 443, ek HTTP portu 8080'dir. `server.ssl.enabled` etkin değilse **ana port 443 de HTTP kullanır**. Port numarası HTTPS'i kendiliğinden açmaz. `http://localhost:8080` ile erişmek, ek connector'ı kullanmak anlamına gelir; ayarlar sayfasındaki `server.port=443` ile çelişmez.

Proxy varsa tarayıcı proxy'nin portunu görür. Örneğin `https://notes.example.org` dışarıdan 443 üzerinden Caddy'ye, Caddy ise iç ağdaki SweetCherry HTTP portuna bağlanabilir. `myapp.security.proxy-https-port` bu dış portun uygulamaya bildirilmesidir; yeni bir dinleyici açmaz. Ayarlar sayfasındaki “Uygulamanın bu istek için gördüğü adres” güvenilir proxy düzeninde dış HTTPS adresini gösterebilir; fiziksel iç bağlantının şifreli olduğunun kanıtı değildir.

## Port ve güvenlik ayarları

| Ayar | Açıklama |
|---|---|
| `server.address` | `127.0.0.1` yalnız yerel bilgisayardan erişim sağlar. `0.0.0.0` tüm IPv4 arayüzlerinde dinler; firewall ve HTTPS ayrıca yapılandırılır. |
| `server.port` | Ana HTTP/HTTPS dinleyici portu. Native TLS açıkken HTTPS kullanır. |
| `server.http.port` | Ek HTTP dinleyici portu. Yerel erişim veya proxy upstream bağlantısı için kullanılır; şifrelenmemiştir. |
| `server.ssl.enabled` | TLS işleminin SweetCherry içinde yapılıp yapılmayacağını belirler. Proxy TLS'i sonlandırıyorsa false kalabilir. |
| `server.ssl.*` | Sertifika/keystore ayarları. Private key ve şifreler paylaşılmaz. Ayrıntı için network-access.md belgesine bakın. |
| `server.forward-headers-strategy` | SweetCherry'nin açıkça tanımlanmış güvenilir proxy yöntemiyle `none` bırakılır. |
| `myapp.login.trusted-proxy-address` | SweetCherry'ye bağlanan proxy'nin tam TCP peer IP adresi. Boşsa doğrudan istemci IP'si kullanılır. |
| `myapp.security.proxy-https-enabled` | HTTPS bilgisini yalnız güvenilir proxy'nin yazdığı özel başlıktan kabul eder. HTTP trafiğini şifrelemez. |
| `myapp.security.proxy-https-port` | Dış HTTPS portu, genellikle 443. İç dinleyici portundan ayrıdır. |
| `myapp.security.hsts-enabled` | Tarayıcıya sonraki erişimleri HTTPS üzerinden yapmasını söyler. Güvenilir sertifikalı hostname için açılır; self-signed/yerel HTTP denemelerinde kapalı tutulur. |
| `server.servlet.session.timeout` | Boşta kalan login oturumu süresi. Yeni isteklerle sayaç yenilenir; toplam oturum ömrü değildir. |
| `server.servlet.session.cookie.*` | Cookie erişim ve aktarım ayarları. Mevcut güvenlik varsayılanlarını gelişigüzel gevşetmeyin. |
| `server.tomcat.max-http-form-post-size` | Form/XML içine resim dahil edilen isteklerin üst boyut sınırı. Tenant'ın tek dosya sınırından ayrı bir toplam istek sınırıdır. |

## Diğer uygulama ayarları

| Ayar | Açıklama |
|---|---|
| `myapp.openWebBrowserOnStartup` | Başlangıçta tarayıcı açma. Windows kontrol penceresinde ayrıca tarayıcı açma düğmesi vardır. |
| `myapp.debug` | Uygulamanın debug özellikleri; Spring log seviyesi ayarından ayrıdır. Normal kullanımda false. |
| `myapp.syntax-highlighting.enabled` | Tarayıcıda yerel highlight.js ile kod renklendirme; Pygments/Jython kullanılmaz. |
| `myapp.exportingFolderName` | Üretilen dışa aktarma dosyalarının hedef klasörü. |
| `myapp.command-line-runner.enabled` | Kaynak kodda korunan başlangıç denemeleri. Normal dağıtımda kapalıdır. |
| `myapp.operations.enabled` | Operations menü seçenekleri; tek başına bütün endpointleri etkinleştirmez. Profil ve endpoint ayarları için optional-features.md belgesine bakın. |
| `myapp.scheduling.enabled` | Deneysel zamanlanmış işler; normal dağıtımda kapalıdır. |
| `myapp.login.admin.name`, `myapp.login.user.name` | Hesap adları. Şifre değerleri ayrı login-credentials.properties dosyasındadır. |
| `myapp.desktop.enabled` | Masaüstü kontrol penceresi, başlangıçta JVM `-Dmyapp.desktop.enabled=true` veya `--myapp.desktop.enabled=true` ile açılır. Yalnız YAML'e yazmak erken başlangıç penceresini etkinleştirmez. Windows launcher bunu sağlar. |
| `astronomy.enabled` | İsteğe bağlı çevrimdışı güneş/ay widget'i. |
| `astronomy.latitude`, `astronomy.longitude` | Hesaplama konumu; otomatik GPS değildir. |
| `astronomy.timezone` | Europe/Istanbul gibi IANA zaman dilimi; yaz saati kurallarını da içerir. |

## Tenant config dosyalarının aktarımı ve saklanması

Tenant dosyası uzak veritabanının kullanıcı adı ve şifresini içerebilir. Yöneticiye açık indirme özelliği bu dosyanın bilinçli olarak yöneticiye verilmesidir. Yetkilendirme şifrelemenin yerine geçmez.

HTTPS, tarayıcı ile TLS'i sonlandıran sunucu/proxy arasındaki upload ve download içeriğini şifreler. HTTP şifrelemez. `http://localhost` trafiği normalde o bilgisayarın loopback arayüzündedir; LAN/WAN erişimiyle aynı değildir, ancak HTTP yine TLS kullanmaz. Caddy `reverse_proxy 192.168.0.5:8080` gibi kullanılıyorsa Caddy–uygulama bağlantısı varsayılan olarak plaintext HTTP'dir. Bu iç ağ bağlantısını da korumak gerekiyorsa doğrulanan upstream HTTPS veya güvenli ağ/tünel gerekir. Proxy adresini güvenilir tanımlamak yalnız başlık kabulünü kontrol eder; taşıma şifrelemesi sağlamaz.

İndirme yanıtı `no-store` kullanır; fakat indirilen dosya istemcinin indirme klasöründe düz metin kalır. Upload edilen tenant config de sunucuda düz metin saklanır. Bunları herkese açık klasörlere veya Git'e koymayın. Bilgisayar/proxy ele geçirilmesi veya yönetici hesabı ele geçirilmesi TLS'in çözebileceği bir durum değildir. SweetCherry tenant dosyalarını ayrıca şifreleyen bir kasa değildir.

`/tenants` üzerindeki çöp kutusu onaydan sonra yalnız seçilen config dosyasını kaldırır, bağlantı havuzunu kapatır ve kaydı listeden çıkarır. CTB dosyasını, uzak veritabanını veya notları silmez. Aktif sorgu varsa silme reddedilir. Başka oturumların silinen kaynak seçimi sonraki istekte temizlenir; login oturumu korunur. Sembolik bağlantı ile tanıtılmış config dosyaları web üzerinden silinmez. Dosya silme işlemi başarısız olursa kayıt korunur; havuz kapanmış olabilir ve yeniden seçimle açılabilir.

Ayrıntılı kurulum: [Ağ erişimi](network-access.md), [HTTPS ve oturum güvenliği](https-and-session-security.md), [Veri kaynağı yaşam döngüsü](tenant-lifecycle.md), [İsteğe bağlı özellikler](optional-features.md).

Kaynaklar: https://developer.mozilla.org/en-US/docs/Web/Security/Defenses/Transport_Layer_Security ve https://caddyserver.com/docs/caddyfile/directives/reverse_proxy

## Rutin loglar

`myapp.debug=false` iken veri kaynağı klasör yolu, kayıt/custom property listesi, isWritable bilgileri, mevcut seçim, kapatma/yeniden yükleme ve exportedFiles klasör yolu gibi rutin INFO mesajları yazılmaz. `true` iken bu bilgiler yeniden görünür. Hikari havuzunun start/stop INFO mesajları da bu ayara uyar; `logging.level.com.zaxxer.hikari` açıkça tanımlanmışsa bu log seviyesi tercih edilir. Ayar uygulama yeniden başlatıldığında uygulanır.

Gerçek WARN/ERROR mesajları ve login güvenlik kayıtları gizlenmez. Bütün Spring/Hibernate/Tomcat logları bu ayarın kapsamına alınmamıştır. Tenant klasörünün alt klasörleri taranmaz; SQLite dosya başlığı taşıyan dosyalar config olarak parse edilmeye çalışılmaz. Metin config dosyalarının adları ve uzantıları kayıt birleştirmede kullanılmaz. Aynı veritabanına ayrı name ve ayarlarla bağlanan tenantlar ayrı kalır.

## PDF aktarımı

`myapp.pdf.*` sınırları ve nesne seçimleri: [PDF sınırları ve nesne seçimleri](pdf-limits-and-objects.md).

## Ayarları arayüzden düzenleme

Seçilmiş güvenlik dışı alanlar için [Uygulama Ayarları ve yeniden başlatma](application-settings-ui.md) sayfasına bakın.
