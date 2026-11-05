# Markmap üzerinden alt düğüm ekleme

`/markmap-quick-mind-map/<nodeId>` ve tüm üst düğümler haritasında düğüm etiketinin üzerine gelince **+** hemen görünür. Klavyede Tab ile düğmeye gidip Enter/Space kullanabilirsiniz. Dokunmatik ekranda + sürekli görünür.

**+** düğmesine basın, ad yazın ve **Tamam** seçin veya Enter kullanın. **İptal** ve Escape kayıt yapmadan kapatır. Yeni düğüm seçilen üst düğümün son çocuğu olur; içeriği boş Normal Metin (Plain Text) olarak oluşturulur ve bağlantının normal yeni-düğüm etiket varsayılanlarını kullanır. İçeriğini daha sonra mevcut okuyucu/editör üzerinden düzenleyebilirsiniz.

Düğüm adına tıklamak haritada gezinmeye, ikona tıklamak içeriği yeni sekmede açmaya devam eder. + ayrı bir işlemdir. Yalnız ADMIN ve yazılabilir bağlantıda sunulur; sunucudaki yazma yetkisi, CSRF ve seçili veritabanı kontrolleri korunur. Bir düğümün içerik salt-okunur bayrağı bütün alt ağacı kilitlemez; mevcut alt düğüm oluşturma kuralları kullanılır.

Başarılı kayıt onayı gelince aynı harita yenilenir. Kök, ikon/renk/chat seçenekleri ve saklanan zoom ölçeği korunur. Gerekirse seviye, yeni çocuk görünecek kadar artırılır. Kapalı chat üst dalı bu yenilemede açılır. Shared/alias düğümde çocuk, ortak içeriğin ana düğümüne değil seçilen ağaç konumuna eklenir. Sanal veritabanı kökünde + yoktur; üst seviye düğüm için mevcut oluşturma işlemini kullanın.

Hata durumunda yazdığınız ad diyalogda kalır. Doğrulama hatasını düzeltebilirsiniz. Veritabanı değişmişse veya kayıt yanıtı belirsizse, sayfa yeniden açılana/yenilenene kadar yeni kayıt engellenir. Yanıt kaybolsa da kayıt yapılmış olabileceğinden tekrar denemeden önce haritayı kontrol edin; istemci kendiliğinden tekrar kayıt göndermez.

## Manuel kontrol

1. Yazılabilir demo bağlantısında ADMIN ile gerçek ve shared düğüm altında ad girerek çocuk oluşturun. Ağaçta doğru konuma eklendiğini ve içeriğinin boş Normal Metin olduğunu kontrol edin.
2. Seviye sınırındaki bir düğümde ve kapalı `chat` dalında deneyin. Yeni çocuk yenilenen haritada görünmeli. Ad ve ikon bağlantıları önceki gibi çalışmalı.
3. Tab + Enter/Space ile açın, Enter ile kaydedin; başka bir girişi Escape ile iptal edin. Telefonda + üzerine gelmeden görünmeli.
4. USER veya salt-okunur bağlantıda ekleme kontrolleri görünmemeli. Eski diyalog açıkken başka sekmeden veritabanı değiştirip kayıt deneyin; yeni seçilen veritabanında düğüm oluşmamalı.

## Sayfayı kaplayan harita

Köşedeki genişlet simgesi araç çubuğunu gizleyip haritayı web sayfasının tamamına yayar; tarayıcı tam ekranına geçmez. Aynı düğme veya Escape normal görünüme döndürür. Açık diyalog varsa Escape önce o diyaloğu kapatır. Her yüklemede varsayılan normal görünümdür; görünüm değiştirmek haritanın zoom veya verisini sıfırlamaz.

## Yerleşim

Diğer kontrollerin yanındaki **Yerleşim** listesinden **Sağa doğru** (varsayılan) veya **İki yana dengeli** seçip Uygula düğmesine basın. Dengeli görünüm kökü, saklanan okunabilir zoom ölçeğinde ortalar. Ana dallar, kapalı dalların altındaki düğümler de sayılarak alt ağaç büyüklüklerine göre dağıtılır; her tarafta kendi sıraları korunur. Tek ana dal varsa sağda kalır. Shared düğümler dahil bütün alt ağaç seçilen tarafta kalır; dalı kapatmak tarafını değiştirmez.

Adla gezinme, tüm düğümler bağlantısı ve hızlı çocuk ekledikten sonraki yenileme yerleşimi korur. Tercih URL'deki `layout=balanced` görüntüleme ayarıdır; CTB'ye veya Freeplane stiline yazılmaz. Metinler, ikon bağlantıları, +, zoom/kaydırma ve sayfayı kaplayan görünüm kullanılmaya devam eder. Büyük haritalar okunabilir ölçekte ekran dışına taşabilir; kaydırarak veya zoom ile inceleyin.

Uyarlama, paketteki Markmap'in ölçüm/çizim işlevlerini kullanır; üçüncü taraf dosyası değiştirilmez. Kütüphane güncellenirken iki taraftaki çizgiler, dal açma/kapama ve tıklama/klavye kontrolleri tekrar doğrulanmalıdır.
