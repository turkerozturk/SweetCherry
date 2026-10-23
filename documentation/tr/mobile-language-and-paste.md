# Arayüz dili, boş içerik ve mobil yapıştırma

## Dil seçimi

Öncelik: kullanıcının `locale` çerezi → YAML başlangıç dili → tarayıcının Accept-Language tercihleri. Türkçe/İngilizce dışındaki tercihlerde ilk desteklenen dil seçilir; hiçbiri yoksa İngilizce kullanılır. Sunucunun işletim sistemi dili arayüz fallback’ini değiştirmez.

```yaml
myapp:
  default-language: "" # boş: tarayıcı; isteğe bağlı en veya tr
```

`spring.web.locale` veya eski tek değerli `spring.mvc.locale` de özel ayar yokken başlangıç değeri olarak okunabilir. `myapp.default-language` tanımlıysa önceliklidir; boş tanımlamak tarayıcıyı seçer. Eski `spring.mvc.locale: en_US, tr_TR` listesi varsayılan dil sayılmaz. Menüden dil sonradan değiştirilebilir; mevcut çerez süresi bir saat olarak korunmuştur. Yeni ilk-açılış davranışını denerken yalnız `locale` çerezini silin veya gizli sekme kullanın.

Ana sayfa gezinme açıklaması ve düğüm özellikleri metinleri TR/EN mesaj dosyalarından gelir. Veritabanının kullanıcı tarafından verilmiş adı çevrilmez. Boş düğüm ayrı, soluk/italik/çerçeveli bir bilgi satırı gösterir; bu satır CTB içeriğine yazılmaz. Zengin metinde yalnız boş XML/HTML veya satır sonu boş sayılır; resim/tablo/kod kutusu gibi nesneler içerik sayılır.

## HTTP üzerinden resim/dosya/tablo ekleme

Önceki kod yeni nesne ID’si için `crypto.randomUUID()` kullanıyordu. Bu API HTTPS/localhost dışında bulunmayabilir. Bu yüzden telefondan laptopun HTTP adresine erişirken dosya/resim/tablo veya URL aktarımı başarısız olabiliyordu. Artık gerektiğinde `crypto.getRandomValues()` ile UUID v4 üretilir. `Math.random()` kullanılmaz. Bu düzeltme sunucuya gönderilen nesne formatını veya CTB şemasını değiştirmez.

Google Drive/Android dosya seçicisi MIME tipini boş/genel bildirebilir. Böyle resimlerde dosya adına güvenilmez; PNG/JPEG başlangıç baytları incelenir ve data-URI MIME tipi düzeltilir. Asıl resim biçimi, piksel ve boyut kontrolleri sunucuda tekrar yapılır. WebP/HEIC gibi başka biçimler bu düzeltmeyle desteklenmiş olmaz. Bulut dosyası okunamıyorsa önce cihaza indirilerek seçilebilir. Hatalar artık tür/boyut/okuma/içerik değişimi/nesne sınırı olarak ayrılır.

## Pano düğmesi ve yerel Yapıştır

📋 düğmesi artık yalnız resim aramaz; güvenli bağlamda varsa zengin HTML’yi, ardından uygun resim/metin temsilini mevcut yapıştırma dönüştürücüsüne verir. Bu doğrudan Clipboard API’si HTTPS veya localhost gerektirir. Telefonda `http://192.168...` bu koşulu karşılamaz; uygulama açıklama gösterir. API kısıtını kaldırmak için tarayıcının güvenlik ayarları değiştirilmez.

Tarayıcının yerel **Yapıştır** seçeneği veya Ctrl+V ayrı yoldur. `paste` yanında `beforeinput` olayında HTML içeren `dataTransfer` sunulursa aynı rich-text dönüştürücüsü kullanılır. Tarayıcı/Android/scrcpy yalnız `text/plain` sağlıyorsa biçim bilgisi editöre ulaşmaz; SweetCherry bunu geri üretemez. Uygulama panoya kendiliğinden yazmaz; pano yazımı yalnız kullanıcının copy/cut işleminde yapılır. Önceki yapıştırmadan sonra davranış değişiyorsa kaynaktan yeniden kopyalayıp yolları ayrı ayrı karşılaştırın. Alternatif olarak 🌐 URL’den içerik ekleme akışı HTTP’de de kullanılabilir.

## Manuel kontrol

1. Yeni/gizli oturumda tarayıcı dili tr/en iken ana sayfa, gezinme açıklaması, Son Güncellenenler/Son Eklenenler/Yer İşaretleri ve özellikler sayfasını açın. Sonra menüden dili değiştirin; seçim sonraki sayfada korunsun.
2. YAML’de `myapp.default-language: en` ayarlayıp yeniden başlatın. Çerezsiz Türkçe tarayıcı ilk kez İngilizce açılmalı; kullanıcı Türkçe seçebilmeli.
3. Boş plain/rich/code düğümlerini, yalnız satır sonu içeren rich düğümü ve yalnız resim/tablo içeren düğümü açın. Bilgi satırı yalnız boş olanlarda görünmeli.
4. Telefondan HTTP ile PNG/JPEG, dosya ve tablo ekleyin; ardından 🌐 ile küçük resimli sayfa alın. Undo/redo ve Kaydet sonrası içeriği kontrol edin.
5. Google Drive PNG/JPEG dosyasını seçin. Başarısızsa yeni hata mesajının boyut, biçim veya okuma hatasını ayırdığını kontrol edin; yerel kopyayla da deneyin.
6. HTTPS’de 📋 ile biçimli metin deneyin. HTTP’de 📋 açık HTTPS/localhost açıklaması göstermeli; yerel Yapıştır/Ctrl+V ayrıca denenmeli.
7. Android Firefox’ta Wikipedia’dan her test öncesi yeniden kopyalayın. Yerel Yapıştır ve fiziksel Ctrl+V sonucunu karşılaştırın. Yalnız düz metin gelirse bilgi mesajını, geri alma ve mevcut nesnelerin korunmasını doğrulayın.

Teknik kaynaklar:
- https://developer.mozilla.org/en-US/docs/Web/API/Crypto/randomUUID
- https://developer.mozilla.org/en-US/docs/Web/API/Crypto/getRandomValues
- https://developer.mozilla.org/en-US/docs/Web/API/Clipboard/read

### Değiştirilen eski YAML satırı

`src/main/resources/application.yml` içindeki aşağıdaki eski blok kaldırıldı; yorumu hatırlatma amacıyla burada korunmuştur:

```yaml
  mvc:
    locale: en_US, tr_TR # MULTILANGUAGE Supported languages
```

Yerine `myapp.default-language` açıklamalı olarak eklendi; desteklenen dil listesi ile başlangıç dili böylece ayrıldı.
