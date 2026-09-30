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
