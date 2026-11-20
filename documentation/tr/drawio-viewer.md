# Draw.io ek görüntüleyici

Ekler sayfasında veya rich-text içerikte `.drawio` eki için **Diyagramı görüntüle** bağlantısını açın. Sayfa seçimi (en fazla 128 sayfa), %2–400 yakınlaştırma, %100, ekrana sığdırma, kaydırma çubukları ve tam ekran vardır. Diyagram üzerinde Ctrl/Command + tekerlek yakınlaştırır; normal tekerlek kaydırır. Tarayıcı tam ekranı kullanılamazsa büyütülmüş panel açılır; Escape kapatır. Özgün dosya indirilebilir.

Normal XML ve raw-DEFLATE/base64 sıkıştırılmış mxfile sayfaları ile tek mxGraphModel belgeleri desteklenir. Draw.io’nun sabit commit’e bağlı statik görüntüleyicisi yerel paketlenir; dosya veya resimler çevrim içi servise gönderilmez. Yeni Maven bağımlılığı yoktur.

Salt okunur önizlemede HTML etiketler satır sonlarıyla düz metne çevrilir. Bağlantılar, çalıştırılabilir içerik, özel XML stencil’leri, dış yazı tipleri/arkaplanlar ve kaynak URL’leri kaldırılır. Resimlerin yerine dosya adını gösteren dikdörtgen konur (gömülü resimler dahil). Normal vektör şekiller, çizgiler, gruplar ve güvenli renk/çizgi stilleri korunur. Özel şekiller, ikon fontları, matematik ve biçimli metin özgün Draw.io görünümünden farklı olabilir.

Görüntüleme allow-same-origin içermeyen sandbox iframe’de yapılır; CSP dış bağlantıları, resim/fontları ve aktif kaynakları engeller. İletişim özel MessageChannel ile kurulur; çizim koduna oturum belirteci veya veritabanı erişimi verilmez. Frame’in açılması, veri ve indirme istekleri oturum ve güncel CTB belirteci gerektirir. USER ve salt okunur kaynaklar da görüntüleyebilir; yazma endpoint’i yoktur.

Sınırlar: 8 MiB dosya, sayfa başına 2 MiB açılmış/temizlenmiş içerik, toplam 16 MiB açılım/çıktı, 128 sayfa, sayfa başına 4.000 ve belge başına 20.000 hücre. Metin, XML derinliği, koordinatlar ve eşzamanlı okuma da sınırlıdır. Bozuk veya büyük belgeler sayfa atlanarak gösterilmez; açık hata verilir. CTB içeriği ve ekin özgün baytları değişmez.

Manuel örnek: `documentation/examples/drawio-viewer-demo.drawio` dosyasını bir düğüme ekleyin; üç sayfa normal şekilleri, sıkıştırılmış XML’i ve dış resim yer tutucusunu gösterir.
