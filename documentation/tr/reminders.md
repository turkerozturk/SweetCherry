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
edilir. Yıllık tekrar ve sayfa içi görsel/sesli alarm için aşağıdaki ikinci aşama
açıklamasına bakın. İşletim sistemi bildirimi gönderilmez.

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

## Yıllık tekrar ve alarm (ikinci aşama)

Formda **Tekrar → Her yıl** seçilebilir. İlk tarih, tekrarın başlangıç tarihidir;
doğum yılının girilmesi gerekmez. Ay, gün ve yerel saat ayrı bir temel olarak
saklanır. 29 Şubat için artık yıl olmayan yıllarda **28 Şubat** (varsayılan) veya
**1 Mart** seçilir; sonraki artık yılda 29 Şubat korunur. 29 Şubat temeli için
geçerli bir artık yıl tarihi seçin. Not/saat dilimi düzenlemesi bu temeli bozmaz.
Yıllık tekrarda yaz saati boşluğu ileri kaydırılır; saat iki kere oluşuyorsa ilk
UTC farkı seçilir. Elle girilen belirsiz/mevcut olmayan saat hâlâ reddedilir.

**Bu oluşumu tamamla**, tamamlanan tarihi saklayıp şimdi veya mevcut tarihten
(ilerideyse o tarihten) sonraki ilk tekrarı hazırlar. Önceki yıl tamamlanmamış
olsa da yeni yıldönümü geldiğinde yeni oluşum görüntülenir ve yeni alarm anahtarı
oluşur. Geçmiş tüm yıllar ayrı ayrı birikmez; son tamamlanan oluşum saklanır.
Salt okuma bu geçişi hesaplar, CTB'ye yazmaz. Eski yıllık satırın düğmesi yeni
yıldönümünü yanlışlıkla tamamlayamaz; oluşum kimliği kontrol edilir.

Seçili CTB'nin sayfalarında sağ alttaki **Alarmlar** paneli ve ses varsayılan
olarak açıktır. Alarmı durdurma ve sesi kapatma tercihleri kaynak adına göre
bu tarayıcıda saklanır; sayfa geçişlerinde/yeniden yüklemede korunur. Tarayıcı
izin verirse ses otomatik etkinleşir. Otomatik ses engellenirse sayfayla
etkileşin veya **Sesi aç / dene** düğmesini kullanın. Bu düğme iki kısa deneme
sesi üretir; sayfa açılışında otomatik deneme sesi çalınmaz.

Alarm kontrolü sayfa açıkken yaklaşık 20 saniyede bir yapılır. Tarayıcı kapalı,
bilgisayar uykuda veya sayfa arka plandaysa zamanında çalma garantisi yoktur.
İşletim sistemi servisi veya masaüstü bildirim izni kullanılmaz; görsel uyarı
sayfanın içindedir. En fazla 20 güncel geciken kayıt panelde gösterilir; tümü
Hatırlatıcılar sayfasındadır. Etkin hedefi olmayan, tamamlanmış veya iptal edilmiş
kayıt alarm üretmez.

- **Uyarıyı kapat**, sadece o oluşumun alarmını susturur; hatırlatıcıyı tamamlamaz.
- **10 dakika ertele**, sunucu saatine göre erteleme kaydını CTB'ye yazar. Süre
  dolunca yeni sunum anahtarı oluşur ve tekrar uyarır. Eski düğmeler ertelenen veya
  yeniden zamanlanan kaydı sessizce değiştiremez.
- Yıllık yeni oluşum, kapatma/erteleme bilgisini önceki yıldan devralmaz.
- Listedeki **Uyarıyı yeniden etkinleştir** kapatma/erteleme bilgisini sıfırlar.
- Kalıcı işlemler ADMIN, yazılabilir kaynak, CSRF ve güncel tenant gerektirir.
  USER/salt okunur kaynakta kapatma sadece bu tarayıcıda saklanır; erteleme gizlidir.
- Aynı tarayıcıdaki sekmeler Web Locks ve ortak yerel kayıtla ses tekrarını
  önler. Web Locks yoksa yalnız odaktaki sayfa kontrol eder. Son 1000 sunum kaydı
  tutulur; tarayıcı depolaması yoksa bu kayıtlar sayfa belleğiyle sınırlıdır.
  Farklı tarayıcı/cihazlar ayrı uyarı üretebilir. Kaynak/oturum değişirse kontrol
  durur ve ses susturulur.

Depo v1 kayıtları tek seferlik kayıt olarak okunur. İlk değiştirmede (alarm
kapatma/erteleme dahil) sürümlü içerik v2'ye geçer; GET işlemleri sürüm yükseltmez.
Sistem etiketi aynı kalır. Eski SweetCherry sürümleri v2 içeriğini okuyamaz ve
üzerine yazmaz. v2 tekrar temeli, 29 Şubat tercihi, son tamamlanan tarih, oluşum
kimliği ve kapatma/erteleme zamanlarını saklar. CTB şeması değişmez.

Ek kontroller:

```bat
mvnw.cmd -Dtest=AnnualReminderTest,ReminderServiceTest,ReminderControllerTest,ReminderSecurityTest,ReminderTemplateTest test
node --test src/test/js/reminderAlarmState.test.cjs src/test/js/reminderAlarms.test.cjs
```

Manuel: yakın bir geleceğe (1–2 dakika) kayıt ekleyin; paneli ve sesi açın,
deneme sesini dinleyin, uyarıyı bekleyin. Aynı kaynağı iki sekmede açıp ses
tekrarını, kapatma ve ertelemeyi, sayfayı yeniden açınca kalıcılığı deneyin.
29 Şubat temelli yıllık kaydı tamamlayarak sonraki tarihin doğru olduğunu görün.

## Özellik ayarları ve ayar filtresi

Üç ayar da varsayılan olarak `true` değerindedir. `/system/settings` sayfasından
veya YAML dosyasından değiştirip SweetCherry'yi yeniden başlatın:

```yaml
myapp:
  reminders:
    enabled: true
    alarms-enabled: true
    sound-enabled: true
```

- `enabled: false`: hatırlatıcı bağlantıları, formlar ve bağımsız alarm paneli
  gizlenir; hatırlatıcı/alarm okuma ve yazma istekleri reddedilir.
- `alarms-enabled: false`: hatırlatıcı listesi/formları kullanılabilir; bağımsız
  alarm paneli gizlenir, alarm kontrolü ve işlemleri kapanır.
- `sound-enabled: false`: görsel alarmlar çalışır; ses düğmesi ve ses kapanır.

Üst ayar alt ayarları da kapatır. Mevcut kayıtlar ve yardımcı düğüm korumaları
saklanır. Önceden tarayıcıda kapatılmış ses/alarm tercihi dikkate alınır;
tekrar açmak için paneldeki düğmeyi kullanın.

Ayar sayfasındaki metin filtresi açıklamalarda, YAML anahtarlarında ve değerlerde
arar. Bütün sözcüklerin eşleşmesi gerekir; Türkçe harfler sade yazımla da bulunur.
Gizlenen ayarlar da kaydetmeye dahildir. **Temizle** veya Escape filtreyi kaldırır.
