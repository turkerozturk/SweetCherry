# Son düğümler ve yer işareti sıralaması

## Görünüm menüsü ve ana sayfa

Veri kaynağı seçiliyken **Görünüm / View Mode** menüsünün sonunda ayırıcıdan sonra **Son Güncellenenler / Recently Modified** ve **Son Eklenenler / Recently Added** bulunur.

Ana sayfada düğüm seçilmemişken veritabanı adı, ağaçtan seçim açıklaması ve yetkili kullanıcı için Düğüm ekle düğmesinin altında aynı iki bağlantı ve Yer İşaretleri bulunur. Masaüstü ve mobil aynı karşılama şablonunu kullanır. Veri kaynağı yokken bu sayfalar veri kaynağı seçimine yönlenir.

## Son düğümler tablosu

- `/nodes/timeline/modified`: son güncelleme tarihine göre azalan sıra.
- `/nodes/timeline/created`: oluşturulma tarihine göre azalan sıra.
- Tarihler eşitse düğüm ID'si azalan sırayla kullanılır.
- Varsayılan 50 satırdır. Satır sayısı alanına **1–5000** arası değer yazıp Göster düğmesine basılabilir. Geçersiz URL parametresi sunucuda da kontrol edilir; açıklamayla 50 satıra dönülür.
- Düğüm adının solunda yerel `img/ctbicons` PNG ikonu bulunur; görüntüler lazy-load edilir.
- Ad düğüm içeriğini açar. **ID düğüm bilgisini offcanvas yan panelde açar**; `/tree` sayfasındaki metadata şablonu kullanılır, not içeriği render edilmez. JavaScript yoksa bağlantı bilgi fragment'ini normal sayfada açabilir.
- Bağlantılar altı çizgisiz, tablo ve hücreler 1 px siyah kenarlıklıdır. Mobilde tablo gerekirse yatay kayar.

## Yer işaretlerini sıralama

Yönetici ve writable veri kaynağı gerekir. `/bookmarks` sayfasında mevcut sıra hücresinin içindeki **↕** tutamacını mouse veya dokunmayla sürükleyin. Tutamaç odaktayken klavyenin yukarı/aşağı okları da kullanılabilir. Bırakınca sıralama kaydedilir ve sayfa yeniden açılır; yeni sütun yoktur. Normal sayfa kaydırması tutamaç dışından yapılabilir.

Yeni liste tek transaction ile `POST /bookmarks/order` üzerinden kaydedilir. Mevcut `POST /bookmarks/{id}/sequence` tek sıra anahtarı endpoint'i de korunur. Oturum, ADMIN, writable kaynak, tenant-view ve CSRF denetimleri geçerlidir.

Sunucu listeyi sayfada görülen snapshot ile karşılaştırır. Başka sekmede bookmark eklenmiş/silinmiş/sıralanmışsa eski sayfanın isteği reddedilir; güncel liste ve açıklama gösterilir. Eksik, tekrarlı veya yabancı ID içeren sıralamalar reddedilir. Görüntülenemeyen/sahipsiz bookmark'lar silinmez, kendi slotlarında korunur.

Gerçek değişiklikte sıra anahtarları 1'den başlayarak düzenlenir; anahtarı değişen satırların bağlı olduğu gerçek düğüm/master modified tarihi güncellenir. Aynı sırayla bırakmak yazma yapmaz. Sürükleme pointer-cancel ile iptal edilirse önceki görsel sıra geri gelir. [Düğüm tarih kuralları](node-timestamps.md) geçerlidir.

## `/actuator/info` sürümü

Maven build-info dosyası üretir. `app.version` çalışan artifact'in bu bilgisinden alınır. Harici `application.yml` dosyasında eski `@project.version@` satırı kalsa da endpoint'te onun yerine gerçek artifact sürümü gösterilir. Kullanıcının harici YAML'ını değiştirmek gerekmez. Build bilgisi olmayan kaynak/IDE çalıştırmasında `development` gösterilebilir. Yeni JAR'ın üretilip uygulamanın yeniden başlatılması gerekir.

## Manuel kontrol

1. Bir kaynak açın. Türkçe ve İngilizce menüde son iki bağlantıyı ve ayırıcıyı kontrol edin.
2. Masaüstü ve mobil ana sayfada düğüm seçimini kaldırın: aynı veritabanı başlığı, izin varsa Düğüm ekle ve üç bağlantı görünmeli.
3. Her iki timeline sayfasında ikon, sütun isimleri, altı çizgisiz link ve siyah kenarlıkları kontrol edin. Ad düğümü, ID yan paneli açmalı. ID'ler arasında hızlı geçişte son seçimin bilgisi kalmalı.
4. 10, 50 ve 125 satırı deneyin. URL'den `limit=-1`, `limit=abc` veya `limit=99999999999999` gönderince açıklamayla 50 satır gösterilmeli; SQL hatası olmamalı.
5. Yer işaretini aşağı ve yukarı sürükleyin; sayfayı yenileyince sıra korunmalı. Mobilde sağa taşma olmamalı. Tutamacı Tab ile seçip oklarla da sıralayın.
6. İki sekmede bookmark sayfasını açın. İlkinde sıralayın, ikincide eski listeyi gönderin: açıklama gösterilmeli ve ilk sıralama korunmalı.
7. Sıra değişikliğinde ilgili düğümlerin modified tarihini kontrol edin. Değişikliksiz bırakmada tarih aynı kalmalı. Salt okunur kaynakta veya USER hesabında tutamaç görünmemeli.
8. Yeni paketi başlatıp `/actuator/info` verisindeki `app.version` değerini `pom.xml` ile karşılaştırın. Eski harici YAML'da `@project.version@` kalsa da sürüm doğru olmalı.
