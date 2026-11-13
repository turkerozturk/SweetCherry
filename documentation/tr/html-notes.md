# HTML notları

**Düğüm Özellikleri** sayfasında Markdown kısayolunun yanındaki **HTML** düğmesine basıp kaydedin. Bu işlem `html` kod sözdizimini seçer; zengin metin dönüşümü, biçimlendirme ve gömülü nesne kaybı için açık onay gerektirir. Kilitli düğüm önce kilidi kaldırılarak kaydedilmelidir. Yalnızca tam olarak `html` sözdizimi bu görünümü kullanır. Diğer kod türleri ve kod kutularındaki HTML kod olarak gösterilmeye devam eder.

Kaynağı düzenlemek için **Düzenle → HTML** seçeneğini açın. Geniş ekranda kaynak ve canlı önizleme yan yana, dar ekranda alt alta görünür. Araç çubuğu seçili metne kalın, italik, başlık, paragraf, liste, kod, bağlantı ve tablo etiketleri ekler. Düzenlenen belge kaynak metindir; önizleme bir WYSIWYG editörü değildir.

Önizleme yazmaya kısa bir ara verildiğinde otomatik güncellenir. Otomatik önizlemeyi kapatıp **Yenile** düğmesini kullanabilir veya önizlemeyi gizleyerek kaynağa daha fazla alan ayırabilirsiniz. Geri al/yinele, tarayıcıdaki taslakları kurtarma, Kaydet ve Kaydet ve devam et mevcut metin editörüyle aynı şekilde çalışır. Düzenleme için yönetici yetkisi ve yazılabilir veritabanı/düğüm gerekir. Önizleme veritabanına kayıt yapmaz.

Kaynak metin CTB içinde değiştirilmeden saklanır. HTML parçası veya tam HTML belgesi kullanılabilir. Görüntülemede paketle gelen jsoup kütüphanesi ve yalıtılmış tarayıcı çerçevesi kullanılır. Başlıklar, paragraflar, metin vurguları, listeler, tablolar, bağlantılar ve gömülü PNG/JPEG/GIF/WebP görseller desteklenir. Renk, metin hizalama, yazı boyutu, kenar boşlukları ve tablo kenarlıkları gibi temel satır içi stiller kullanılabilir. Normal/hızlı içerik görünümü ve takvim HTML'i işler; PDF ve rapor çıktıları temizlenmiş HTML, not içeriği eklenen zihin haritası çıktıları okunabilir metin kullanır. Dosya ve klasörlerden oluşan metin ZIP çıktısında HTML kaynağı korunur.

Bu özellik tam bir web sitesi barındırıcısı değildir: scriptler, olay işleyicileri, formlar, çerçeveler, SVG, stil dosyaları ve dış kaynaklar görüntülemeye alınmaz. Harici görsel adresleri ve göreli dosya yolları yüklenmez; base64 biçiminde gömülü raster görseller kullanılabilir. Güvenli HTTP/HTTPS ve e-posta bağlantıları yeni sekmede açılır. `/nodes/123` biçimindeki dahili bağlantılara veritabanı görünüm belirteci eklenir. Görüntülenemeyen kısımlar dahil kaynak korunur. HTML kaynağı için önizleme, görüntüleme ve kaydetme sınırı 2.000.000 karakterdir.

Örnek:

```html
<h1>Proje notları</h1>
<p style="color:#3584e4">Bu paragraf mavidir.</p>
<ul><li>Kaynağı düzenleyin.</li><li>Önizlemenin güncellenmesini izleyin.</li></ul>
<table><tr><th>İş</th><th>Durum</th></tr><tr><td>Örnek</td><td>Hazır</td></tr></table>
```
