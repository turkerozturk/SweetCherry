# Mobil okuma görünümü

Veritabanını seçtikten sonra görünüm menüsünden **Mobil okuma görünümü** seçilir.
Eski Mobil Görünüm ve masaüstü görünümleri kullanılmaya devam edilebilir.

- İçerik mevcut mobil içerik fragment'i ve CSS'iyle gösterilir. Ortak footer gösterilmez.
- Üstte tek satır bulunur: tam ağaç, önceki kardeş, parent, ilk alt düğüm,
  sonraki kardeş ve bilgiler/işlemler. Bulunmayan yönler gösterilmez.
- Üst düzey düğümlerde parent düğmesi yoktur. Veritabanı köküne ağaç panelindeki
  veritabanı adından gidilir. Shared node'lerde alt düğüm düğmesi gösterilmez.
- Sağa hızlı yatay swipe tam ağacı; sola swipe bilgiler, ana menü, export ve düzenleme
  işlemlerini açar. Paneller kapat düğmesiyle veya Escape ile kapanır.
- Zoom yapılmışken, kod/tablo alanlarında ve metin seçilmişken swipe panel açmaz.
  Dikey kaydırma ve pinch zoom engellenmez. Panel düğmeleri her zaman kullanılabilir;
  fareyle kullanımda bu düğmeler tercih edilir.
- Ağaç gerektikçe yüklenir. Dal durumu ve kaydırma konumu CTB oturum belirtecine göre
  sekmede saklanır. Seçili düğümün üst dalları açılır. Yeni sekmeler ayrı duruma sahiptir.
- Düğüm seçmek mevcut `/nodes/{id}` sayfasını açar; bu mobil görünümde içerik geçişleri
  henüz AJAX değildir. Kaydedilen ağaç durumu sayfa geçişinden sonra geri yüklenir.
- İşlemler mevcut admin, writable, shared node, readonly ve CSRF/tenant kontrollerini
  kullanır. Ağaç istekleri de CTB oturum belirtecini taşır.

## Manuel kontrol

1. Gerçek düğüm, shared node ve veritabanı kökünde yön düğmelerini kontrol edin.
2. Ağaçta birkaç dal açın, başka düğüme geçin; açık dalları ve konumu kontrol edin.
3. Uzun düz metin, rich text, kod, resim ve tablo içeriğini eski mobil görünümle karşılaştırın.
4. Telefonunuzda dikey kaydırma, sağ/sol swipe, pinch zoom ve zoom sonrası yatay pan deneyin.
5. Admin ve user hesaplarında işlemleri; CTB değişimi ve oturum bitişinde eski sekmeyi deneyin.

Tarayıcıya özgü zoom/gesture davranışları gerçek mobil cihazda kontrol edilmelidir.

## Yakındaki düğümler çekmecesi

Üst bardaki düğüm adına basınca barın altında parent, sibling ve seçili düğümün
child listesini gösteren çekmece açılır. Parent'in siblingleri gösterilmez.
En altta breadcrumbs bulunur; aynı yol metadata panelinde de gösterilir.
Tekrar düğüm adına basınca çekmece kapanır. Otomatik açılma/kapanma yoktur.
Açık/kapalı durumu CTB oturumuna göre aynı sekmede sayfa geçişleri boyunca korunur.
Açıldığında seçili düğüm çekmece içinde yukarı kaydırılır; içerik sayfası kaydırılmaz.
Tam ağaç paneli bağımsız çalışmaya devam eder.

Yeni masaüstü ağaç görünümünden Mobil okuma görünümüne geçildiğinde URL'deki
seçili düğüm ID'si ve CTB oturum belirteci korunur. Shared node için alias ID korunur.
Yeni mobil ve masaüstü görünümlerinde üst düzey veya alt düğüm eklemeden önce
onay penceresi gösterilir. Vazgeçmek sunucuya oluşturma isteği göndermez.
Bu onay bir arayüz kolaylığıdır; mevcut sunucu yetki ve CTB kontrolleri geçerlidir.

## Görseller, tablolar ve kod blokları

İçerikteki görseller ekrana sığdırılır. Fareyle üzerlerine gelince büyüteç imleci
görünür; tıklama veya klavyede Enter/Space gerçek boyutlu kaynağı yeni sekmede
açar. Telefonda görsele dokunarak açıp tarayıcının pinch zoom özelliği kullanılabilir.
Görsel, tablo ve kod alanındaki kaydırma panel açma hareketi olarak yorumlanmaz.
Tablolar, pre ve codebox blokları kendi alanlarında yatay kaydırılır.
Bu kurallar yalnızca yeni mobil içerik alanına uygulanır; navigasyon ikonları,
metadata ve eski görünümler değiştirilmez.
