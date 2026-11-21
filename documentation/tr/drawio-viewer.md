# Draw.io ek görüntüleyici

Ekler sayfasında veya rich-text içerikte `.drawio` eki için **Diyagramı görüntüle** bağlantısını açın. Sayfa seçimi (varsayılan en fazla 512 sayfa), %2–400 yakınlaştırma, %100, ekrana sığdırma, kaydırma çubukları ve tam ekran vardır. Diyagram üzerinde normal tekerlek işaretçinin konumunu sabit tutarak yakınlaştırır; Shift basılıyken kaydırır. **Genişliğe sığdır** yatay alanı doldurur; yüksek diyagram dikey kaydırılabilir. Tarayıcı tam ekranı kullanılamazsa büyütülmüş panel açılır; Escape kapatır. Özgün dosya indirilebilir.

Normal XML ve raw-DEFLATE/base64 sıkıştırılmış mxfile sayfaları ile tek mxGraphModel belgeleri desteklenir. Draw.io’nun sabit commit’e bağlı statik görüntüleyicisi yerel paketlenir; dosya veya resimler çevrim içi servise gönderilmez. Yeni Maven bağımlılığı yoktur.

Salt okunur önizlemede HTML etiketlerin güvenli biçimlendirmesi, blok/satır sonları, yatay/dikey hizalaması ve kaydırması korunur. Güvensiz bağlantılar, çalıştırılabilir içerik, özel XML stencil’leri, dış yazı tipleri/arkaplanlar ve kaynak URL’leri kaldırılır. Paketlenmiş Draw.io kütüphane ikonları yerel olarak gösterilir; diğer resimlerin yerine dosya adını gösteren dikdörtgen konur (eke gömülü resimler dahil). Normal vektör şekiller, çizgiler, gruplar ve güvenli renk/çizgi stilleri korunur. Özel şekiller, ikon fontları, matematik ve biçimli metin özgün Draw.io görünümünden farklı olabilir.

Görüntüleme allow-same-origin içermeyen sandbox iframe’de yapılır; CSP dış bağlantıları, resim/fontları ve aktif kaynakları engeller. İletişim özel MessageChannel ile kurulur; kanal tenant/oturum belirteci taşımaz ve veritabanı erişimi vermez. Frame’in açılması, veri ve indirme istekleri oturum ve güncel CTB belirteci gerektirir. USER ve salt okunur kaynaklar da görüntüleyebilir; yazma endpoint’i yoktur.

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

## Renkler, yer tutucular ve yerel görsel tamamlama

HTML metinlerinde güvenli foreground/highlight renkleri (isim, hex, RGB/RGBA/HSL) ve tek renk içeren `background` korunur. Hex renkler sayısal boyut sınırına takılmaz. Hücre font/etiket arkaplan renkleri de korunur; aktif CSS, URL ve scriptler çalışmaz.

**Eksik şekil ve resim adlarını göster** kutusu, diyagramdaki yer tutucu metinlerini ve üstteki uyarı özetini açar/kapatır. Varsayılan açıktır; tercih tarayıcıda hatırlanır. Özgün açıklama metni ve hücre geometrisi korunur. Konsol logları devam eder. Bilinmeyen vektör şekillerin tanım adı, bulunamayan ikonların kütüphane yolu; diğer resimlerin dosya adı, varsa shape adı/açıklaması veya `missing source` gösterilir. Dış resim URL'si ziyaret edilmez.

Yönetici, SweetCherry’nin çalıştığı bilgisayarda bir klasöre **PNG/JPEG karşılıkları** yerleştirebilir. Bu özellik shape/resim hücreleri içindir; JavaScript, XML stencil, SVG veya HTML etiketinin içindeki img öğeleri için bir eklenti yükleyicisi değildir.

1. Örneğin `C:/SweetCherry/drawio-images` klasörüne `appliance.png` dosyanızı koyun.
2. Aynı klasörde UTF-8 `images.properties` oluşturun:

```properties
shape.mxgraph.example.missing=appliance.png
# Bir resim için yer tutucu hover bilgisinden veya tarayıcı konsolundan anahtarı kopyalayın:
# image.<64-karakterlik-SHA-256-anahtarı>=screenshot.jpg
```

3. System settings içindeki **Draw.io yerel PNG/JPEG eşleme klasörü** alanını ayarlayın veya mevcut YAML eşlemesine ekleyin:

```yaml
myapp:
  drawio-preview:
    local-images-directory: 'C:/SweetCherry/drawio-images'
```

4. Yeniden başlatıp diyagramı açın. Dosya/eşleme değişikliklerinde de restart gerekir; ayarı boş bırakmak özelliği kapatır. Dosya adları bu klasöre göre göreli olmalıdır; mutlak yollar, klasör dışına çıkış ve dışarı yönelen symlink'ler reddedilir. URL yazılmaz. Yerel karşılıklar kaynak hücrenin geometrisinde gösterilir; kaynak CTB/ek dosya değiştirilmez. Açıklama metni kalır, çözülen resmin yer tutucusu kaldırılır. Eşlemeler uygulama genelindedir ve farklı CTB'lerden erişen okuyuculara da sunulur; yalnızca sunmak istediğiniz görselleri içeren bir uygulama klasörü kullanın. Açıkça tanımlanan yerel karşılıklar eşleşen hazır/fallback şekilden önce gelir.

Resim hücresinin eşleme anahtarı özgün kaynak metninin (sorgu parametreleri dahil) SHA-256 değeridir. Ham dış URL temizlenmiş diyagrama gönderilmez. Sunucunun oluşturduğu resim yer tutucusunun üzerine gelince `image.<anahtar>` görülür; eksik resim hücresi anahtarları tarayıcı konsoluna da yazılır. Dış görüntüyü kendiniz temin edip bu anahtara bağlayabilirsiniz.

Manifest en fazla 64 KiB/128 kayıt; tek resim 2 MiB/16 milyon piksel; toplam 8 MiB/32 milyon piksel olabilir. Dosyanın gerçek raster türü ve boyutları kontrol edilir. Geçersiz kayıtlar atlanıp sunucuya anahtar bazlı uyarı yazılır. Dosyalar başlangıçta okunur; tarayıcının gönderdiği keyfi bir yoldan dosya okuyan endpoint yoktur.

`documentation/examples/drawio-library-shapes.drawio` içindeki `Colors and placeholders` sayfası renkleri, eksik adları/kaynakları ve `shape.mxgraph.example.missing` yerel karşılığını denemek içindir.

## Gizlilik sınırı ve Windows CI

SweetCherry önizlemesi yerel script/kaynak kullanır. Sandbox içinde `connect-src 'none'`, `img-src data:`, `font-src 'none'` ve nonce'lu script politikası korunur. Diyagram Draw.io’ya gönderilmez, dış resimler indirilmez. Çevrim içi editörün bulut, AI, işbirliği veya sunucu dönüştürme özellikleri bu görüntüleyiciye dahil değildir. Ayrı Draw.io web editörünün güncel gizlilik politikasında bazı PDF/resim export, AI ve işbirliği işlemlerinin sunucuda işlendiği belirtilir: https://www.drawio.com/trust/ .

Önceki kütüphane testindeki CI hatası Windows CRLF satır sonlarından kaynaklanıyordu: test yalnızca LF ile bölünce aile adlarında CR kalıyordu. Fixture düzeltildi ve `core.autocrlf=true` checkout'unda doğrulandı. Özgün vendor paketleri değiştirilmedi.

## İç sayfa bağlantıları ve dış web bağlantıları

Shape/connector üzerindeki `link` alanı (UserObject ve eski style biçimi dahil) ile HTML metninin içindeki bağlantılar desteklenir:

- `data:page/id,<kimlik>` aynı ek dosyanın ilgili sayfasını seçer. Kaynak diagram ID korunur; sayfa adı değişse de bağlantı ID ile çalışır. Hedef yoksa veya ID tekrarlıysa yerel hata gösterilir. URL kodlanmış ID'ler de desteklenir.
- Açık `http://` ve `https://` adresleri yeni sekmede, `noopener` ve `noreferrer` ile açılır. Metin bağlantıları klavye/fare ile, şekil bağlantıları görüntüleyicinin fare/dokunma işleyicisiyle çalışır. Kanal mesajları keyfi dış URL açamaz.
- `javascript:`, `file:`, genel `data:` içerikleri, `data:action/json,...`, göreli URL'ler, kullanıcı/parola içeren URL'ler ve kontrol karakterleri reddedilir. Güvensiz bağlantının açıklama metni kalır. Draw.io action/animasyon komutları açılmaz. İç bağlantı burada başka bir diagram sayfasıdır; CherryTree node linkine otomatik dönüşüm yapılmaz.

Kaynak ek/CTB değiştirilmez. Görüntülerken dış hedef ziyaret edilmez; bağlantıya tıklanınca o adrese bilinçli olarak gidilir. Örnek dosyanın `Links` sayfasında şekil/metin bağlantıları, geri dönüş, eksik hedef ve etkisiz güvensiz bağlantı bulunur.

Dış sekmenin normal çalışabilmesi için sandbox'ta popup ve popup'ın kendi sandbox'ından çıkmasına izin verilir. Çizim iframe'i hâlâ opaque origin'dedir; `allow-same-origin`, form veya üst sayfayı yönlendirme izni yoktur. Script nonce zorunluluğu, `connect-src 'none'`, `img-src data:` ve `font-src 'none'` korunur. Yeni sekmeye opener/referrer verilmez; frame için DNS prefetch kapalı ve referrer politikası `no-referrer` olur.

### Yerel kaynak CSP uyarıları

Genel fallback `default-src 'self'` olarak düzenlendi. Böylece Firefox geliştirici araçlarının script olarak sınıflandırılmayan yerel kaynak yeniden okumalarına izin verilebilir. Script/ağ/resim/font/frame/child/worker/media/manifest/object/form/base türleri açık ayrı kurallarla kısıtlanır. `script-src` içine `'self'`, unsafe-inline veya unsafe-eval eklenmez. Bu değişiklik diyagramın dış kaynağa bağlanma veya iframe'in aynı origin yetkisini kazanması anlamına gelmez.

`NetUtil.sys.mjs` kaynaklı `default-src` mesajları, nonce'lu scriptin çalışamamasından çok kaynak yeniden okumalarıyla uyumludur; ancak Firefox'taki tam tetikleyici bu ortamda yeniden üretilemedi. Yeniden başlatma/sayfa yenileme sonrası konsolu Firefox'ta kontrol edin. Diyagramın çizilmesi tek başına bütün konsol uyarılarının çözüldüğünü doğrulamaz.

## Portable paket kaynakları

Hazır görüntüleyici, shape tanımları ve yerel ikon paketleri `SweetCherry.jar` içindedir; ayrıca çıkarma/indirme gerekmez. Paketlerde bu kılavuz ile `documentation/examples/*.drawio` dosyaları da bulunur. İsteğe bağlı `drawio-images/` klasöründe yorumlu `images.properties` örneği ve README vardır. Kullanmak için mevcut local-images-directory ayarını bu klasörün mutlak yoluna ayarlayın; varsayılan boş/kapalı kalır. Maven hazırlığı ve Windows kurucusu mevcut örnek dosyaları/kullanıcı resimlerini korur. ZIP güncellemelerini yeni klasöre açın; bu klasörü ayrıca taşıyıp yedekleyin.
