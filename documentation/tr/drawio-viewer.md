# Draw.io ek görüntüleyici

Ekler sayfasında veya rich-text içerikte `.drawio` eki için **Diyagramı görüntüle** bağlantısını açın. Sayfa seçimi (varsayılan en fazla 512 sayfa), %2–400 yakınlaştırma, %100, ekrana sığdırma, kaydırma çubukları ve tam ekran vardır. Diyagram üzerinde normal tekerlek yakınlaştırır; Shift basılıyken kaydırır. **Genişliğe sığdır** yatay alanı doldurur; yüksek diyagram dikey kaydırılabilir. Tarayıcı tam ekranı kullanılamazsa büyütülmüş panel açılır; Escape kapatır. Özgün dosya indirilebilir.

Normal XML ve raw-DEFLATE/base64 sıkıştırılmış mxfile sayfaları ile tek mxGraphModel belgeleri desteklenir. Draw.io’nun sabit commit’e bağlı statik görüntüleyicisi yerel paketlenir; dosya veya resimler çevrim içi servise gönderilmez. Yeni Maven bağımlılığı yoktur.

Salt okunur önizlemede HTML etiketlerin güvenli biçimlendirmesi, blok/satır sonları, yatay/dikey hizalaması ve kaydırması korunur. Bağlantılar, çalıştırılabilir içerik, özel XML stencil’leri, dış yazı tipleri/arkaplanlar ve kaynak URL’leri kaldırılır. Resimlerin yerine dosya adını gösteren dikdörtgen konur (gömülü resimler dahil). Normal vektör şekiller, çizgiler, gruplar ve güvenli renk/çizgi stilleri korunur. Özel şekiller, ikon fontları, matematik ve biçimli metin özgün Draw.io görünümünden farklı olabilir.

Görüntüleme allow-same-origin içermeyen sandbox iframe’de yapılır; CSP dış bağlantıları, resim/fontları ve aktif kaynakları engeller. İletişim özel MessageChannel ile kurulur; çizim koduna oturum belirteci veya veritabanı erişimi verilmez. Frame’in açılması, veri ve indirme istekleri oturum ve güncel CTB belirteci gerektirir. USER ve salt okunur kaynaklar da görüntüleyebilir; yazma endpoint’i yoktur.

Kaynak bütçeleri YAML ve **System settings** içinde `myapp.drawio-preview` altında ayarlanır; değişiklikten sonra yeniden başlatın. Aşağıdaki varsayılanlar eski sabit bütçelerin (8 MiB kaynak, 2 MiB/sayfa, 128 sayfa, 4.000 hücre/sayfa) yerini alır. Mevcut dış YAML paket varsayılanlarını geçersiz kılar. Sınır hatası ilgili ayarın tam adını gösterir. URI kodlu açılım için çözülmüş sayfa bütçesinin üç katına kadar okuma yapılır, ardından gerçek UTF-8 bayt boyutu kontrol edilir; toplam bütçe giriş ve çıktıyı iki kere saymaz. Yapısal doğrulama ve aktif içerik korumaları devam eder.

Manuel örnek: `documentation/examples/drawio-viewer-demo.drawio` dosyasını bir düğüme ekleyin; üç sayfa normal şekilleri, sıkıştırılmış XML’i ve dış resim yer tutucusunu gösterir.

Bu anahtarları mevcut `myapp` eşlemesine ekleyin; ikinci bir `myapp` bölümü oluşturmayın.

```yaml
myapp:
  drawio-preview:
    max-file-bytes: 16777216
    max-page-bytes: 16777216
    max-total-bytes: 134217728
    max-pages: 512
    max-cells-per-page: 20000
    max-total-cells: 200000
    max-label-characters: 100000
    max-xml-elements: 500000
    max-xml-attributes: 128
    max-xml-depth: 128
    max-parent-depth: 256
    max-geometry-depth: 16
    max-geometry-points: 10000
    max-coordinate: 1000000
```

Yeni varsayılanlar 16 MiB kaynak, sayfa başına 16 MiB açılım, 128 MiB toplam, 512 sayfa, sayfa başına 20.000 ve belge başına 200.000 hücreye izin verir. Tarayıcı sunucunun sayfa bütçesini kullanır; eski 128 sayfa/2 MiB kontrolleri geçerli çıktıyı engellemez. System settings üst sınırları sınırsız kaynak kullanımını önler. Desteklenmeyen belge yapıları, dosya boyutundan bağımsız olarak hâlâ reddedilebilir.
