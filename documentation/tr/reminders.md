# Düğüme bağlı hatırlatıcılar

Düğümün araç çubuğundaki **Hatırlatıcı ekle** bağlantısını veya TODO satırındaki
aynı bağlantıyı kullanın. Tarih-saat, saat dilimi ve isteğe bağlı bir not girin.
Yeni formun saat dilimi, takvim ayarındaki `myapp.calendar.time-zone` değerinden
alınır; `system` ise sunucunun saat dilimi kullanılır. Kayıt saati UTC anına
çevrilir ve seçilen saat dilimi de ayrıca saklanır. Tarih/saat dakika hassasiyetindedir.

**View Mode → Hatırlatıcılar** (`/nodes/reminders`) bekleyen, yaklaşan, geciken,
tamamlanmış, iptal edilmiş ve tüm kayıtlar filtrelerini sunar. Yaklaşan, kontrol
anından sonraki bekleyen kayıttır; kontrol anına eşit veya geçmiş tarih gecikendir.
Durum sayfa isteği sırasında hesaplanır. **Yenile / filtrele** ile tekrar kontrol
edilir. Bu sürüm otomatik alarm çalmaz, tarayıcı bildirimi göndermez; yıllık
tekrar ve sesli/görsel uyarı sonraki aşamalardır.

- Geçmiş tarih girilebilir. Yaz saati geçişinde mevcut olmayan veya iki anlama
  gelen yerel saat sessizce kaydırılmaz. Başka saat veya açık UTC farkı
  (`+02:00` gibi) girilmesi istenir.
- Bir düğüme birden çok hatırlatıcı eklenebilir. Aynı içeriği kullanan paylaşılan
  yerleşimler aynı hatırlatıcılara bağlanır. Form, hedefin içerik ID bilgisini
  kontrol ederek sonradan başka içeriğe bağlanan paylaşılan yerleşime yanlış
  kayıt yapılmasını reddeder.
- Tercih edilen yerleşim kaybolur veya çöp kutusuna giderse etkin gerçek/paylaşılan
  yerleşim aranır. Hedef tümüyle kullanılamıyorsa kayıt saklanır, bağlantı kapanır;
  yine de tamamlanabilir veya iptal edilebilir. Düzenlemek/yeniden açmak etkin
  hedef gerektirir.
- Düzenleme, mevcut tamamlanmış/iptal edilmiş durumunu korur. Etkinleştirmek
  için listeden **Yeniden aç** kullanılır. Hatırlatıcıyı tamamlamak `todo`/`done`
  etiketlerini değiştirmez; görev ve hatırlatıcı durumları bağımsızdır.
- Düğümün içeriği, etiketleri, oluşturulma ve değiştirilme tarihleri korunur.
- Liste, USER ve salt okunur kaynaklar için de açıktır. Formlar ve işlemler ADMIN
  ve yazılabilir kaynak gerektirir. POST işlemleri CSRF ve tenant belirteciyle
  korunur. Başka sekmede herhangi bir hatırlatıcı değiştiyse eski form reddedilir.
  Geçersiz tarih/saat/not, formdaki değerler korunarak bildirilir.

## Saklama ve koruma

İlk başarılı kayıtta CTB içinde `REMINDERS` adlı, salt okunur ve aramadan hariç
bir kök düğüm oluşturulur. Okuma veya form açma bu düğümü oluşturmaz. Mevcut
normal bir `REMINDERS` düğümünün üzerine yazılmaz: tanımlayıcı ad değil,
`__sweetcherry_reminders_v1__` etiketi ve sürümlü içerik başlığıdır.

Düğüm içeriği sürümlü JSON'dur. Her hatırlatıcı UUID, içerik ID, tercih edilen
gezinti ID, hedef adının kopyası, UTC zamanı, saat dilimi, not, durum ve kayıt/güncelleme
zamanlarını taşır. En çok 5000 kayıt ve 2.097.152 Java karakteri saklanır.
CTB şeması değişmez. Bütün CTB'nin taşınması/yedeklenmesi kayıtları da taşır;
tek düğüm veya alt ağaç dışa aktarma/import sırasında bu bağlantılar otomatik
aktarılmaz ya da yeniden eşlenmez.

Yardımcı düğüm normal düzenleme, etiket değiştirme, alt düğüm ekleme, taşıma,
kopyalama, silme ve çöp kutusuna alma işlemlerine kapalıdır. Etiket katalogları,
genel arama, takvim ve son etkinlik listeleri bu düğümü kullanıcı kaydı saymaz.
Node tree'de açıldığında ham JSON yerine yönetim sayfasına yönlendiren açıklama
vardır. Bozuk, çoğaltılmış, taşınmış, paylaşılan veya sınırı aşan depo sessizce
sıfırlanmaz; işlem durur ve yedekten düzeltme gerektiği bildirilir.

İşlemler mevcut veritabanı transaction'ında çalışır. Kaydetme sırasında kayıtlar
kilitlenir ve deponun tam içerik revizyonu kontrol edilir. İlk düğüm ve ağaç
kaydı birlikte kaydedilir/geri alınır. Aynı formun tekrar gönderilmesi ikinci
kayıt oluşturmaz. Bu aşamada hatırlatıcı işlemleri ayrı bir CTBLOGS olayı üretmez;
kayıtların kendi oluşturulma/güncellenme zamanları saklanır. Yardımcı düğüm
normal içerik geçmişine karışmaz.

## Doğrulama

```bat
mvnw.cmd -Dtest=ReminderTimeTest,ReminderServiceTest,ReminderControllerTest,ReminderSecurityTest,ReminderTemplateTest,AuditIntegrationTest test
mvnw.cmd clean package
```

Manuel: bir normal ve bir paylaşılan düğümden gelecek/geçmiş tarihli kayıtlar
ekleyin; liste filtrelerini, düzenleme, tamamlama, iptal ve yeniden açmayı deneyin.
Saat dilimini değiştirip aynı anın doğru gösterildiğini kontrol edin. Başka sekmede
kayıt değiştirdikten sonra eski formu gönderin. Hedef yerleşimi çöp kutusuna alıp
etkin paylaşılan yerleşime geçişi, sonra tüm hedefler kaybolduğunda kaydın
korunmasını kontrol edin. USER/salt okunur erişimi ve CTB yeniden açıldıktan sonra
kayıtların kalıcılığını doğrulayın.
