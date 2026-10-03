# Konum, tarih/saat ve arayüz dili

Bu belge mevcut davranışı ve release öncesi kalan düzenlemeleri ayırır. Ayarlar CTB dosyasına değil, JAR'ın yanındaki `application.yml` dosyasına aittir. Aynı sunucuyu kullanan istemciler astronomi için aynı yapılandırılmış konumu kullanır.

## Mevcut konum ayarları

```yaml
astronomy:
  latitude: "40°59'21.5\"N"
  longitude: "29°02'14.4\"E"
  timezone: Europe/Istanbul
  zonalOffset: 10800
```

Bu değerler uygulamanın mevcut örnek konumudur; kullanıcının konumu otomatik bulunmaz. Tarayıcıdan GPS izni istenmez. Güneşin doğuş/batış saatleri için enlem ve boylam, yerel gösterim için zaman dilimi gerekir. Şehir düzeyinde konum seçmek bu yardımcı widget için genellikle yeterlidir; evin tam koordinatını yayımlamak gerekmez. Ay fazının anı konumdan bağımsızdır; onu yerel saat olarak göstermek zaman dilimine bağlıdır.

**DİKKAT:** `CommonsSunCalc` içindeki mevcut koordinat ayrıştırması N/S/E/W yönlerini ve kesirli saniyeleri doğru değerlendirmiyor. Örnekteki saniyelerin kesir kısmı kaybolabilir; S/W değerlerine güvenilmemeli. Yukarıdaki örnek mevcut yapılandırmayı belgeler; farklı konumlar için güvenilir yapılandırma desteği tamamlanmış sayılmaz.

## Tarih ve zaman kaynakları

| Alan | Mevcut kaynak |
| --- | --- |
| Header saati | Tarayıcıdaki `Date`; cihazın saati ve zaman dilimi. |
| CTB `ts_creation` / `ts_lastsave` | Unix saniyesi; yeni yazmalarda `Instant.now()` ile alınır. |
| `#dates` ile CTB tarih gösterimi | Sunucunun/JVM'nin varsayılan zaman dilimine bağlıdır. |
| Güneş hesabı | Yapılandırılmış `astronomy.timezone`; hesap gününü seçen `LocalDate.now()` ise sunucunun varsayılan bölgesini kullanır. |
| Ay fazı tarih gösterimi | `astronomy.zonalOffset` sabit saniye offset'i; duration hesabında sistem zaman dilimi de kullanılır. |

`10800`, UTC+03:00 için saniye değeridir. Sabit offset yaz/kış saati değişen bölgelerde yeterli değildir. Dil seçmek zaman dilimi veya GPS konumu seçmek değildir. Sunucu ve istemci saatlerinin doğru olması gerekir.

Ay fazı widget'ı `MoonTime4j` içinde başlangıçta hazırlanır; uzun süre çalışan uygulamada güncel anla yeniden hesaplama ihtiyacı vardır. Güneş hesabı ise controller advice üzerinden istekler sırasında çalışır. Bu yardımcı bilgiler henüz bütün bölgeler için doğrulanmış değildir.

## Dil

`LocaleConfiguration`, `locale` çereziyle dili saklar; varsayılan Türkçe'dir ve mevcut çerez süresi bir saattir. `lang` parametresi dili değiştirir. Mesaj dosyaları `messages.properties`, `messages_en.properties`, `messages_tr.properties` olup UTF-8 olarak düzenlenmelidir.

Dil değiştirme seçili düğümü, `_tenantView`, diğer URL seçeneklerini ve çapa bağlantısını korur. Bazı eski ekranlarda ve astronomi widget'ında sabit/teknik İngilizce veya Türkçe ifadeler vardır; mesaj dosyalarına taşınmaları gerekir. Tarih biçimi ile dil tercihi ayrıca ele alınmalıdır. `spring.mvc.locale` altındaki mevcut dil listesi, özel locale resolver'ın varsayılanını değiştiren bir kullanıcı ayarı olarak düşünülmemelidir.

## Release öncesi çalışma

- [ ] Koordinatları signed decimal derece veya açık DMS biçimiyle ayrıştır; yön/kesir ve geçerli aralıkları test et. Geçersiz konum sessizce başka konuma dönüşmemeli.
- [ ] Güneş hesabının gününü yapılandırılmış IANA zaman diliminde seç.
- [ ] Ay tarihlerini de aynı IANA zaman dilimiyle göster; sabit offset'e bağımlılığı kaldır.
- [ ] Ay fazını güncel anla yenile; paylaşılan değişkenlerde eşzamanlı istek davranışını gözden geçir.
- [ ] Widget'ı isteğe bağlı yap; konum belirtilmemişse örnek bir konumu gerçek kullanıcı konumu gibi sunma. Kapalıyken astronomi hesabı da çalışmamalı.
- [ ] Ana ekran tarihlerini, locale çerezini ve astronomi mesajlarını TR/EN için kontrol et.
- [ ] Farklı zaman dilimleri, S/W koordinatları, gün sınırı ve yaz/kış saati senaryolarını test et.

Otomatik tarayıcı konumu, kullanıcıya özel konum saklama ve ayar arayüzü sonraki aşamadır; ilk sürüm için elle belirlenen konum ve açık bir etkinleştirme seçeneği yeterlidir.
