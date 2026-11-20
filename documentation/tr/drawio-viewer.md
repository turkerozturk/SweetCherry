# Draw.io ek görüntüleyici

Ekler sayfasında veya rich-text içerikte `.drawio` eki için **Diyagramı görüntüle** bağlantısını açın. Sayfa seçimi (varsayılan en fazla 512 sayfa), %2–400 yakınlaştırma, %100, ekrana sığdırma, kaydırma çubukları ve tam ekran vardır. Diyagram üzerinde normal tekerlek yakınlaştırır; Shift basılıyken kaydırır. **Genişliğe sığdır** yatay alanı doldurur; yüksek diyagram dikey kaydırılabilir. Tarayıcı tam ekranı kullanılamazsa büyütülmüş panel açılır; Escape kapatır. Özgün dosya indirilebilir.

Normal XML ve raw-DEFLATE/base64 sıkıştırılmış mxfile sayfaları ile tek mxGraphModel belgeleri desteklenir. Draw.io’nun sabit commit’e bağlı statik görüntüleyicisi yerel paketlenir; dosya veya resimler çevrim içi servise gönderilmez. Yeni Maven bağımlılığı yoktur.

Salt okunur önizlemede HTML etiketlerin güvenli biçimlendirmesi, blok/satır sonları, yatay/dikey hizalaması ve kaydırması korunur. Bağlantılar, çalıştırılabilir içerik, özel XML stencil’leri, dış yazı tipleri/arkaplanlar ve kaynak URL’leri kaldırılır. Paketlenmiş Draw.io kütüphane ikonları yerel olarak gösterilir; diğer resimlerin yerine dosya adını gösteren dikdörtgen konur (eke gömülü resimler dahil). Normal vektör şekiller, çizgiler, gruplar ve güvenli renk/çizgi stilleri korunur. Özel şekiller, ikon fontları, matematik ve biçimli metin özgün Draw.io görünümünden farklı olabilir.

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

## Uyumluluk ve hata ayrıntıları

Draw.io kök ve katman kimlikleri `0`/`1` olmak zorunda değildir. Görüntüleyici hiyerarşiyi çıkarır; hücre/üst/çizgi/port referanslarını güvenli görüntüleme kimliklerine eşler. UserObject sarmalayıcı kimliği, iç mxCell kimliğinden önceliklidir. Ekin özgün baytları değiştirilmez. Boş diyagram sayfaları ve boş grafik modelleri boş sayfa olarak seçilebilir.

Ayrıştırma hatasında yerelleştirilmiş açıklama, `compression`, `graph_root`, `missing_parent`, `geometry_number` gibi sabit neden kodu, 1’den başlayan sayfa numarası ve varsa XML satır/sütunu gösterilir. Konsol uyarısında da kod/sayfa/konum bulunur; dosya adı, hücre metni veya ham XML loglanmaz. `invalid`, bu görüntüleyicinin bir yapıyı ayrıştıramadığını belirtir; dosyanın bozuk olduğunu kanıtlamaz. Limit hataları ayarlanabilir anahtarı göstermeye devam eder.

Regresyon testlerinde farklı kök kimliği kullanan iki değiştirilmemiş upstream şablon vardır; kaynak/lisans bilgisi `src/test/resources/drawio/UPSTREAM.md` dosyasındadır. Geliştirme sırasında 155 upstream diyagram şablonu başarıyla ayrıştırılmıştır; ayrı katalog indeksi diyagram değildir. Bu kontrol ayrıştırma uyumluluğunu doğrular; tam tarayıcı görünümünü veya özel bir dosyanın içeriğini doğrulamaz.

## Yerel şekil kütüphaneleri

Sabit Draw.io sürümündeki 204 stencil seti (9.089 benzersiz şekil adı) ve 1.910 PNG/SVG kütüphane ikonu paketlenir. Infographic gibi JavaScript şekilleri mevcut statik görüntüleyicide bulunur. Standard/Basic/Flowchart, Cabinets, EIP, ağ ve diğer paketli aileler bu yapıya dahildir. Yeni Java bağımlılığı veya çevrim içi servis yoktur. General temel geometrik şekilleri ayrı anahtar gerektirmeden kullanılabilir.

75 aile anahtarının hepsi varsayılan olarak açıktır. **System settings** filtresinde `drawio` veya `cabinets` gibi aile adıyla arayın, etkinliğini değiştirin, kaydedip uygulamayı yeniden başlatın. Tam liste `application.yml` içindedir. Mevcut eşlemeye eklenecek örnek:

```yaml
myapp:
  drawio-preview:
    shape-libraries:
      cabinets: true
      infographic: true
      eip: true
      clip-art: true
      cisco: true
      azure2: false
```

Tanımlar `static/js/drawio/stencils.min.js` ve `catalog.js` içinde hazırdır. Stencil seti ilk kullanıldığında açılır/kaydedilir; her sayfada bütün şekiller oluşturulmaz. İkonlar `static/js/drawio/icons/` altında ailelere ayrılmıştır; kapalı ikon paketleri iframe'e yüklenmez. Dosyalar uygulamayla gelir ve görüntüleme sırasında internetten kaynak indirilmez. Dış bir resim URL'si ikon desteği sayesinde izinli hale gelmez.

Bulunamayan veya kapalı şekiller diyagramın üzerinde uyarı olarak ve **tarayıcı geliştirici konsolunda** `[SweetCherry Draw.io]` önekiyle görünür. Yalnızca tanım adı loglanır; içerik ve ham XML loglanmaz. Uyarılar tekrarsızdır ve her sayfa çiziminde en fazla 100 ad tutulur. Desteklenmeyen şekil normal dikdörtgene düşerken diğer hücreler çizilmeye devam eder. Dosyaya gömülmüş özel XML stencil'leri, kullanıcı kütüphaneleri, dış resimler, daha yeni sürümlerin şekilleri ve desteklenmeyen ikon fontları kapsam dışıdır; bütün diyagramlar için birebir görünüm garantisi değildir.

Manuel örnek: `documentation/examples/drawio-library-shapes.drawio` dosyasını ekleyin. Cabinets/EIP/Flowchart/Infographic ve yerel ikon hücrelerini görüntüleyin; bir aileyi kapatıp yeniden başlatarak tekrar açın. Örnekte bilerek bulunamayan bir tanım ve dış bitmap yer tutucusu da vardır.

Kod ve görsel varlık lisans koşulları aynı değildir. Kaynak sürümü, özgün ikon hash'leri ve tam kullanım bildirimleri `distribution-notices` ile `static/js/drawio/SOURCE.txt` içindedir. Atlassian ürünleri/marketplace için ek varlık kısıtları korunmuştur; bütün çizimler sınırsız Apache/GPL olarak etiketlenmez. Paket üretimi için isteğe bağlı geliştirici komutu ve ayrıntılar İngilizce kılavuzdadır; normal derleme/çalıştırma sırasında kaynak indirme veya Python/Node.js gerekmez.
