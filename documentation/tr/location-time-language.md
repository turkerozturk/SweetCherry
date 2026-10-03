# Konum, tarih/saat ve arayüz dili

Astronomi widget’ı çevrimdışı hesaplama yapan yardımcı bir özelliktir. Harici servis veya internet bağlantısı kullanmaz; hesaplar Commons Suncalc ve Time4J ile sunucuda yapılır. Ayarlar CTB dosyasına değil, JAR’ın yanındaki `application.yml` dosyasına aittir. Aynı sunucuyu kullanan istemciler aynı ayarlanmış konumu kullanır.

## Konum ve etkinleştirme

```yaml
astronomy:
  enabled: true
  latitude: "40°59'21.5\"N"
  longitude: "29°02'14.4\"E"
  timezone: Europe/Istanbul
```

Widget varsayılan olarak açıktır. Bu koordinatlar İstanbul’da örnek bir konumdur; kullanıcının gerçek konumu otomatik bulunmaz. Tarayıcıdan GPS izni istenmez. Kendi şehrinizin koordinatlarını ve IANA zaman dilimini girip uygulamayı yeniden başlatın. Şehir düzeyinde konum bu yardımcı bilgi için genellikle yeterlidir; evinizin tam koordinatını yayımlamanız gerekmez. Açılan bilgi kutusu, hesapların ayarlanmış konuma ait olduğunu, tarihi ve zaman dilimini belirtir.

`astronomy.enabled: false` güneş/ay göstergesini ve hesaplamaları kapatır. Header’daki tarayıcı saati kalır. Widget kapalıyken konum alanlarının geçerli olması gerekmez.

Koordinatlar signed decimal derece veya derece/dakika/saniye (DMS) olarak verilebilir:

| Biçim | Enlem | Boylam |
| --- | --- | --- |
| Ondalık derece | `-33.5` | `151.2` |
| DMS | `33°30'0"S` | `151°12'0"E` |

N/E pozitif, S/W negatiftir; kesirli saniyeler korunur. İşaret ve yönü aynı değerde birlikte kullanmayın. Enlem −90…90, boylam −180…180 aralığındadır; dakika/saniye 60’tan küçük olmalıdır. Ondalık ayırıcı noktadır. Hatalı koordinat veya zaman diliminde başka bir konum kullanılmaz: bilgi kutusu uyarı verir, loga mesaj yazılır; düğüm sayfaları çalışmaya devam eder.

Doğuş/batış bilgileri seçilen yerel gün içindir. Bazı enlem ve günlerde olay gerçekleşmez; örneğin kutup yazında güneş batmayabilir. Böyle durumlarda “—” gösterilir. Hesaplar yaklaşık astronomi bilgisidir; gözlem koşulları gerçek ufuk ve hava durumuna göre değişebilir.

## Zaman kaynakları ve güncelleme

| Alan | Kaynak |
| --- | --- |
| Header saati | Tarayıcıdaki `Date`; cihazın saati ve zaman dilimi. |
| CTB `ts_creation` / `ts_lastsave` | Unix saniyesi; yeni yazmalarda `Instant.now()`. |
| `#dates` ile CTB tarih gösterimi | Sunucunun/JVM’nin varsayılan zaman dilimi. |
| Güneş/ay doğuş ve batış günü | Güncel anın `astronomy.timezone` içindeki yerel tarihi. |
| Ay fazı tarihleri | Aynı IANA zaman dilimi; yaz/kış saati kuralları dahil. |

`astronomy.zonalOffset` eski yapılandırmalarla uyumluluk için okunabilir, ancak widget tarafından kullanılmaz; kaldırabilirsiniz. Dil seçmek zaman dilimi seçmek değildir. Ay fazının anı konumdan bağımsızdır; yerel saat gösterimi zaman dilimine bağlıdır.

Hesap sonuçları değiştirilemez bir snapshot olarak en fazla bir dakika önbellekte tutulur. Sonraki sayfa isteği veya bilgi kutusunu açma işlemi gerektiğinde yeniden hesaplar. Ay fazı artık yalnızca uygulama açılışında hesaplanmaz. Her istemci için ayrı astronomi hesabı veya sürekli arka plan isteği yoktur. Açık bırakılan sayfadaki küçük faz göstergesi bir sonraki sayfa yüklemesinde güncellenir. Sunucu ve istemci saatleri doğru olmalıdır.

## Dil

`LocaleConfiguration`, `locale` çereziyle dili saklar; varsayılan Türkçe, mevcut çerez süresi bir saattir. `lang` parametresi dili değiştirir. Mesaj dosyaları `messages.properties`, `messages_en.properties`, `messages_tr.properties` olup UTF-8 olarak düzenlenmelidir.

Dil değiştirme seçili düğümü, `_tenantView`, diğer URL seçeneklerini ve çapa bağlantısını korur. Astronomi başlıkları, faz adları, durum mesajları ve tablo etiketleri TR/EN mesaj dosyalarından gelir. Bazı eski ekranlarda sabit ifadeler bulunabilir. CTB tarih biçimi ve locale çerezi süresi ayrıca değerlendirilecektir. `spring.mvc.locale` altındaki mevcut dil listesi, özel locale resolver’ın varsayılanını değiştiren kullanıcı ayarı değildir.

## Kontroller ve sonraki işler

Otomatik testler koordinat yönleri/kesirleri, geçersiz ayarlar, UTC gün sınırı, kutup yazı, IANA yaz saati, kapalı widget ve önbellek yenilemesini kapsar. Manuel kontrolde kendi konumunuzla saatleri, TR/EN etiketlerini, kapalı widget’ı ve hatalı ayarlarda düğüm sayfasının çalışmasını doğrulayın.

Kullanıcıya özel konum, isteğe bağlı tarayıcı konumu, ayar arayüzü ve CTB tarih gösteriminin zaman dilimi tercihi sonraki aşamadır. Mevcut widget sunucu düzeyinde elle belirlenmiş konum kullanır.

Kullanılan kütüphaneler:
- https://shredzone.org/maven/commons-suncalc/
- https://github.com/MenoData/Time4J
