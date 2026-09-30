# Alternatif desktop: Tam Ağaç Görünümü

Veritabanı seçiliyken görünüm menüsünden **Tam Ağaç Görünümü** seçilir. Doğrudan adresi `/tree` şeklindedir. Desktop ve mobil görünümler menüde kalır; bunlara geçildiğinde önceki sayfa düzeni kullanılır.

Sol bölme veritabanının bütün üst seviye düğümleriyle başlar. Ok düğmesine basılınca bir dal yüklenir ve açılır; shared node kendi kimliğiyle gösterilir ve alt dalı yoktur. Düğüm adına tıklanınca sağ bölmedeki içerik AJAX ile yenilenir. Ağaç ve içerik bağımsız kaydırılır; aradaki ayırıcı fare/dokunmatik ile sürüklenebilir veya odaktayken sol/sağ ok tuşuyla değiştirilebilir.

Ortak footer yerine altta sabit durum çubuğu bulunur: düğüm tipi, oluşturulma/değiştirilme zamanı ve doğrudan alt düğüm sayısı. Başlık ve breadcrumbs içerik bölmesinde dar bir alandadır. Dört CTB export seçeneği tek açılır menüde bulunur; diğer işlemler kısa etiketler ve hover açıklamaları kullanır. Türkçe ve İngilizce metinler mesaj dosyalarındadır.

Açık dallar, seçili düğüm, ağacın kaydırma konumu ve ayırıcı genişliği `sessionStorage` içinde tarayıcı sekmesine ve CTB seçim tokenına bağlı saklanır. Sayfa yenilemede ve işlem sayfasından dönüşte yeniden yüklenir. Veritabanı değiştirilince yeni token ile ayrı durum kullanılır. Tarayıcının geri/ileri düğmeleri seçilen içeriği takip eder.

## İşlem akışı ve sınırlar

- İçerik görünümü normal node controller'ının hazırlama ve yetki kontrollerini kullanır.
- Silme onayı, özellik/içerik düzenleme ve export mevcut tam sayfa/form akışlarında çalışır. Giriş yapan rol, tenant writable ayarı, node readonly ve shared node kontrolleri korunur.
- AJAX istekleri ve form işlemleri CTB seçim tokenını taşır; başka CTB'ye geçilen eski sekme engellenir.
- Bir düğümü değiştirdikten sonra ağaç sayfasına dönüşte dallar tekrar sunucudan alınır. Aynı anda başka uygulama veya başka sekmede yapılan CTB değişiklikleri için sayfayı yenilemek gerekir.
- Klavyeyle düğüm taşıma, kelime sayısı ve mobil tasarım bu görünümün kapsamına henüz dahil değildir.

## Manuel kontrol

1. Aynı CTB'de farklı dalları açın, bir yaprak ve bir shared node seçin; ağaç ve kaydırma konumu sabit kalsın.
2. Ayırıcıyı sürükleyin; uzun ağaç ve içerik kendi bölmelerinde kaydırılsın, durum çubuğu altta kalsın.
3. Sayfayı yenileyin, geri/ileri yapın, özellik düzenleyip geri dönün; seçili düğüm ve açık dallar geri gelsin.
4. Admin/writable, user ve readonly durumlarında düğmeleri ve sunucu izinlerini kontrol edin.
5. İkinci sekmede başka CTB seçin; eski ağaçtan içerik veya değişiklik isteği kabul edilmesin.
6. Desktop ve mobil görünüme dönün; mevcut görünümler kullanılabilsin.
