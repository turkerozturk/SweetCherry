# Uygulama ayarları ve yeniden başlatma

ADMIN hesabıyla **Sistem → Uygulama Ayarları** açılır. Astronomi ayarları, başlangıç tarayıcısı, syntax highlighting, debug ve PDF limitleri düzenlenir. Login, oturum, HTTPS, proxy, portlar, dosya yolları ve diğer güvenlik ayarları bu aşamada formda yoktur.

Spring tarafından gerçekten yüklenmiş tek bir harici `application.yml` veya `application.yaml` gerekir. Release klasöründeki dosya normalde bu koşulu sağlar. JAR içindeki dosya veya kaynak kod dosyası değiştirilmez. Birden fazla harici YAML, çok belgeli YAML, yinelenen anahtarlar ve alias/merge içeren yapı için elle düzenleme kullanılır. Form gizli değerleri göstermez veya yeniden göndermez.

**Kaydet** yalnız dosyayı değiştirir. Yorumlar ve form dışındaki ayarlar korunur; eksik alanlar YAML sonunda noktalı property adıyla eklenebilir. Dosya form açıldıktan sonra değişmişse kayıt reddedilir. Geçici dosya ve atomik değiştirme kullanılır. Kaydetme otomatik yeniden başlatmaz.

**Sistem → SweetCherry’yi Yeniden Başlat** onay ister. Aynı JVM içinde eski Spring context kapanır, CTB havuzları kapanır, Swing pencere/tepsi kapanır ve masaüstü modunda tekrar oluşturulur. Başlatma argümanları korunarak yeni context açılır; YAML tekrar okunur. Tüm kullanıcılar yeniden giriş yapar ve CTB seçer. Yeni Java süreci oluşturulmaz. run.sh/run.bat ve masaüstü EXE aynı mekanizmayı kullanır. IDE yerine doğrudan Maven çalıştırması da main metodunu kullandığı sürece desteklenir.

Yanıt geldikten sonra birkaç saniye bekleyip bağlantıdan ana sayfayı açın. Başlatma hatası olursa `myapp.log` kontrol edilip elle yeniden açılır. Başarılı yeniden başlatma bir sağlık garantisi değildir. Port ayarları bu formda değişmediğinden adres aynı kalır.

Komut satırı ve ortam değişkenleri YAML değerlerinden önceliklidir. Örneğin run.sh tarayıcı açma ayarını argümanla true yapar. Formda dosya değeri ve şu anda etkin değer ayrı gösterilir. PDF limitini yükseltmek özellikle Raspberry Pi’de bellek tükenmesine yol açabilir; derinlik formda 256 ile sınırlandırılır. SweetCherry CTB yedeği almaz.

Masaüstünde tüm YAML alanları için ayrı [YAML editörü](desktop-yaml-editor.md) vardır; bu web formunun güvenlik dışı kapsamını değiştirmez.
