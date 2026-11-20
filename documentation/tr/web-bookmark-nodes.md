# Canlı web yer imi düğümleri

Aşağıdaki koşullar birlikte sağlandığında düğüm canlı web yer imi gibi davranır:

- Düğümün **Read Only / Salt okunur** işareti açıktır. Yalnız veritabanı bağlantısının salt okunur olması yeterli değildir.
- Düğümün adı bütünüyle geçerli bir `http://` veya `https://` adresidir.
- İçerik boştur; gömülü resim/ek, tablo veya codebox yoktur. Boş rich text işaretlemesi boş sayılır.

Düğümü `/tree`, split view veya mobil okuyucuda açın. Uygulama genel erişime açık sayfayı sunucudan indirip statik önizleme gösterir. Düğüm yeniden açıldığında yeni istek yapılır; **Web sayfasını yeniden yükle** düğmesi seçimi değiştirmeden tekrar indirir. Başlığın en sağındaki **CT_LINK_WEBSITE** ikonu özgün adresi yeni tarayıcı sekmesinde açar. Önizleme içindeki bağlantılar yalnız metindir, tıklanamaz. İçerik düğüme kaydedilmez; bu bir çevrimdışı arşivleme veya içe aktarma işlemi değildir. PDF dışa aktarma ve bağlı içerik fragment'leri web sayfasını indirmez.

Mevcut genel web içe aktarıcısının doğrulanmış/sabitlenmiş DNS çözümlemesi, yönlendirme kontrolü, cookie/kimlik bilgisi izolasyonu, süre/boyut sınırları ve aynı anda iki iş sınırı kullanılır. Özel ağ, loopback, link-local ve ayrılmış adresler; resim ve yönlendirmeler dahil her bağlantıda engellenir. Yalnız standart HTTP/HTTPS portları kabul edilir. İstek sunucudan yapılır; tarayıcının web sitesi cookie'leri, oturumu ve betikleri kullanılmaz.

İçe alınan sayfa yalnız biçimlendirme etiketlerine izin veren ikinci bir temizlemeden geçer. Betik, event handler, uzak stil, bağlantı, form, frame ve aktif medya kaldırılır. Sınırlı sayıdaki doğrulanmış, sunucudan indirilmiş resim gömülü PNG verisi olarak gösterilebilir. Sonuç JavaScript izni olmayan, opaque sandbox iframe içinde açılır; Content Security Policy dış kaynak yüklemeyi de engeller. Özgün siteyi açan ikon bu sandbox dışındadır.

Bu, genel erişime açık statik içerik okuyucusudur; tam web tarayıcısı değildir. Oturum, JavaScript veya bot doğrulaması gerektiren; yalnız sıkıştırılmış cevap veren, büyük ya da erişilemeyen sayfalar eksik görünebilir veya açılamayabilir. Yerelleştirilmiş hata mesajı gösterilir; özgün site ikonu kullanılabilir kalır. Sayfanın güncellenmesi, yönlendirmeleri ve erişilebilirliği web sitesine bağlıdır.

Önizleme oturum açılmasını ve güncel CTB görünüm belirtecini gerektirir. API istemciden serbest URL almaz; düğüm kimliğini alıp güncel koşulları tekrar kontrol eder. Shared occurrence asıl içerik düğümü üzerinden çözülür. İçeriğe metin eklemek veya Read Only işaretini kaldırmak normal içerik gösterimine döndürür.

Varsayılan açıktır. Önizlemeyi ve web sitesi ikonunu kapatmak için `/system/settings` veya harici `application.yml` üzerinden aşağıdaki ayarı değiştirip yeniden başlatın:

```yaml
myapp:
  web-bookmarks-enabled: false
```

Ayrı `myapp.web-import-enabled` ayarı yalnız editör içe aktarma işlemlerini kontrol etmeye devam eder.
