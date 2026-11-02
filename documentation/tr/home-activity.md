# Veritabanı açılışı ve ana sayfadaki son düğümler

Dağıtımdaki YAML artık veritabanı seçildikten sonra varsayılan olarak son güncellenen düğümü açar. Sanal kökün açılmasını isterseniz `open-latest-modified: false` yapın. Sanal kök gerçek bir düğüm değildir; iki seçenek de CTB şemasını ve düğüm tarihlerini değiştirmez.

`SweetCherry.jar` yanındaki harici `application.yml` dosyasında mevcut `myapp` bölümüne aşağıdaki ayarları ekleyin veya düzenleyin, ardından uygulamayı yeniden başlatın:

```yaml
myapp:
  home:
    open-latest-modified: true
    modified-minimum: 5
    created-minimum: 5
```

* `open-latest-modified: true`, başarılı veritabanı seçiminden sonra en güncel güncelleme tarihli ağaç konumunu açar. Sihirbazdan seçim de buna dahildir. Mevcut masaüstü/mobil görünüm tercihi korunur. Veritabanı boşsa veya son düğüm okunamazsa ana sayfa açılır. Sanal köke doğrudan gitmek her zaman ana sayfayı açar. Tarihler eşitse gerçek konum, ardından küçük konum ID'si tercih edilir.
* Etkin listeler bugünün **tüm** düğüm konumlarını en yeniden eskiye gösterir. Bugünkü kayıtlar ayarlanan asgari sayıdan azsa en yakın eski kayıtlarla tamamlanır. Sayılar üst sınır değildir. İlgili sayıya `0` vermek o listeyi gizler; negatif değer geçersizdir. Toplam kayıt sayısı azsa mevcut olanlar gösterilir.
* Bugün sınırı `myapp.calendar.time-zone` ayarına göre hesaplanır; `system` ise işletim sisteminin saat dilimi kullanılır. Yarın ve daha ilerideki tarihli kayıtlar bu listelerde gösterilmez; takvim özelliğinde gelecek oluşturulma tarihleri kullanılabilir. Gösterilen tarihler de aynı saat dilimindedir. Tarihler eşitse konum ID'sine göre sıralanır.
* Paylaşımlı konumlar kendi ID ve hiyerarşik yollarıyla listelenir; ad, ikon, başlık biçimi ve tarihler master'dan gelir. Bağlantılar geçerli veritabanı görünüm anahtarını taşır. Liste hazırlanırken düğüm metinleri ve gömülü ikili nesneler yüklenmez.
* Son Güncellenenler, Son Eklenenler ve Yer İşaretleri bağlantıları düğme görünümündedir. Masaüstünde uzun listeler içerik alanında kayar; mobilde normal sayfa kaydırması kullanılır.

Mevcut harici YAML dosyalarının üzerine yazılmaz. Harici `application.yml` dosyanızda açıkça `false` yazıyorsa bu değer önceliklidir; yeni varsayılanı isterseniz `true` yapıp yeniden başlatın. Eksik anahtarlar yukarıdaki dağıtım YAML değerlerini devralır.

## Manuel kontrol

1. Dağıtım varsayılanıyla son güncellenen düğümün açıldığını doğrulayın. Seçeneği false yapıp yeniden başlatınca ana sayfa açılmalı. Farklı veritabanlarını seçerken önceki veritabanının değil yeni seçilenin son düğümünün açıldığını kontrol edin. Salt okunur veritabanını da deneyin.
2. Sanal kökü doğrudan açın. İki listede tam tıklanabilir yolları, ikonları, kalın/renkli başlıkları ve tarihleri Türkçe/İngilizce kontrol edin; farklı parent altındaki paylaşımlı konumları da deneyin.
3. Bugün beşten az kayıt varsa eski kayıtlarla beşe tamamlanmasını, beşten fazla varsa tümünün gösterilmesini doğrulayın. Sıfır ve farklı sayı ayarlarını, boş veritabanını ve gelecekteki oluşturulma tarihlerini kontrol edin.
4. Masaüstü kaydırmasını ve dar mobil görünümü deneyin. Görüntülemenin düğümlerin oluşturulma/güncellenme tarihlerini değiştirmediğini doğrulayın.
