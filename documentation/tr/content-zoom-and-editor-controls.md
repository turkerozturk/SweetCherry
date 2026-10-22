# İçerik büyütme ve editör kontrolleri

## Görünüm menüsü

Menünün sonundaki büyüteç düğmeleri düğüm içeriğini her tıklamada %10 büyütür/küçültür. Aralık %70–%200, ortadaki yüzde düğmesi %100'e dönüş içindir. Bu bir görüntüleme ayarıdır; CTB içeriğini, kaydedilen biçimleri veya PDF çıktısını değiştirmez. Metinle birlikte içerikteki nesneler de ölçeklenir; ana menü ve düğüm ağacı ölçeklenmez.

Ayar aynı tarayıcının aynı site adresindeki yerel depolamasında saklanır; sayfa ve düğüm geçişlerinde, masaüstü/mobil görünüm değişiminde korunur. HTTP/HTTPS, farklı sunucu adı veya port ayrı site sayılır. Tarayıcı depolaması engelliyse o sayfada çalışır, sonraki sayfada varsayılana döner.

## Kaydet, iptal ve çıkış koruması

Her iki editörde Kaydet ve İptal, düzenleme araçlarının ve yazım alanının üzerindedir. İçerik değiştiyse İptal veya aynı sekmedeki bir bağlantı onay ister. Yenileme, geri/ileri veya sekmeyi kapatmada tarayıcının kendi kaydedilmemiş değişiklik uyarısı kullanılır; metnini tarayıcı belirler. Sekmenin/uygulamanın işletim sistemi tarafından zorla kapatılması için bu uyarı garanti edilemez. Otomatik kayıt veya taslak kaydı yapılmaz.

Yalnız seçim/cursor/görünüm değişimi uyarı doğurmaz. Geri alarak başlangıç içeriğine dönmek uyarıyı kaldırır. Kaydet tıklanınca normal gönderim uyarı vermez; yükleme sürmesi veya kilitli nesne silme gibi sebeplerle editör kaydı engellerse çıkış koruması devam eder.

## Düz metin

Geri al/yinele düğmeleri, Ctrl+Z, Ctrl+Y ve Ctrl+Shift+Z aynı geçmişi kullanır (macOS için Command da desteklenir). Yazma, yapıştırma, silme ve 33~ ekleme geri alınabilir. En çok 200 durum bellekte tutulur. Eski duruma dönüp yeni değişiklik yapmak sonraki yineleme dalını kaldırır. CTB'ye yalnız Kaydet ile yazılır.

## Zengin metin

- Biçim ve renkler bir gruptadır; eş aralıklı yazı düğmesi `ms` ile gösterilir.
- Resim, dosya, tablo, 33~ ve nesne yönetimi bir gruptadır.
- Geri al/yinele ok simgeleridir; üzerlerinde açıklamalar bulunur.
- ☐, ☑, ☒ ve • karakterleri ayrı bir gruptadır; seçili metnin yerine karakter ve bir boşluk ekler. İşlem geri alınabilir. Satır başındaki kutuların önceki tıklamayla döndürme özelliği korunur.
- Dış bağlantı düğmesi, seçili herhangi bir metne adres atamak veya mevcut bağlantıyı değiştirmek/silmek içindir.
- **Seçili URL’yi bağlantı yap**, seçili http/https adresini doğrudan tıklanabilir yapar. `www.` ile başlayan adreslerde https eklenir. Gösterilen metin ve diğer biçimler korunur. Başka protokoller veya geçersiz adresler reddedilir; mevcut Dış bağlantı diyaloğu değişmez.
- Görsel yazım alanı ve metin modu alanı, masaüstünde sağ alt köşesinden sürüklenerek dikey boyutlandırılabilir; dokunmatik cihazlarda tarayıcının desteklediği ölçüde çalışır.

## Manuel kontrol

1. Masaüstünde düğümü büyütün, başka düğüme ve mobil görünüme geçin; yüzde korunsun. Sayfayı yenileyin, %100'e sıfırlayın. Sınırlarda ilgili düğme devre dışı olsun.
2. Düz metinde yazın, 33~ ekleyin, düğmeler ve kısayollarla geri alın/yineleyin. Unicode karakterler ve seçim konumu korunsun.
3. İki editörde de değişiklik yapmadan İptal uyarısız olsun. Değişiklik yapınca İptal uyarı versin; vazgeçince içerik korunsun. Yenileme/geri/sekme kapatma uyarısını da deneyin.
4. Geri al ile ilk içeriğe dönün: uyarı kalmasın. Kaydet, fazladan çıkış onayı istemesin. Kilitli nesne silme nedeniyle engellenen kayıtta uyarı korunsun.
5. Zengin metinde biçim, renk, dört liste karakteri, resim/dosya/tablo ekleme ve nesne silme işlemlerini deneyin; geri al/yinele kontrol edin.
6. Metin seçip mevcut Dış bağlantı diyaloğunu kullanın. Ayrıca URL seçip yeni düğmeyi deneyin; biçimler ve seçim korunsun, geri alınabilsin. `javascript:` veya düz cümle bağlantıya dönüşmesin.
7. Görsel yazım alanını ve metin modundaki alanı fareyle dikey boyutlandırın.
