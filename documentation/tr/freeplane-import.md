# Freeplane / FreeMind içe aktarma

ADMIN olarak yazılabilir bir veri kaynağı açın. **Uygulama → Freeplane / FreeMind içe aktar** menüsünden (veya Mindmap Export sayfasındaki bağlantıdan) `.mm` dosyasını yükleyin. Başlığı, düğüm sayısını ve dönüştürme uyarılarını inceleyip onaylayın. Haritanın kökü, mevcut kök düğümlerinin **en sonuna yeni bir dal** olarak eklenir; alt düğümlerin sırası ve hiyerarşisi korunur. Önizleme veritabanına yazmaz. İptal edilebilir; önce yedek alın.

Önizleme on dakika boyunca, aynı oturum ve aynı veri kaynağı görünümü için geçerlidir ve bir kez onaylanabilir. Başka veri kaynağına geçip geri dönmek de eski önizlemeyi geçersiz kılar. Onayda yazılabilirlik tekrar kontrol edilir. Hata olursa işlemin tamamı geri alınır. Mevcut notlar, yer imleri ve kaynak dosya değişmez; yeni kimlikler atanır. Aynı dosyayı tekrar aktarmak ayrı bir kopya oluşturur.

## Aktarılan bilgiler

- Ağaç, düğüm sırası ve `TEXT` veya HTML `NODE` başlıkları korunur. Boş başlık `(Untitled)` olur. 255 Unicode karakterden uzun başlık kısaltılır; tamamı içerikte tutulur.
- `NOTE` ve `DETAILS`, okunabilir Düz Metin içeriğine dönüşür. Paragraflar, liste maddeleri, satır sonları ve bağlantı adresleri korunur; ikisi birlikte varsa bölüm adları eklenir. SweetCherry export içeriğindeki `<pre>` metninin boşlukları aynen korunur.
- Doğrudan verilmiş başlık rengi, kalınlık ve geçerli oluşturulma/değiştirilme tarihleri aktarılır. Freeplane milisaniyeleri CTB saniyelerine çevrilir; eksik/geçersiz tarihler için aktarma zamanı kullanılır. Freeplane'den miras alınmış stiller çözülmez.
- `ctbicons/<standart-ad>.png` referansı, resmi yüklemeye gerek olmadan ilgili **Node Properties ikonuna** çevrilir. Yeni ikonlu SweetCherry export'ları ayrıca `sweetcherry.node-icon` bilgisi taşır. Eski export'larda kökteki veritabanı simgesini gerçek düğüm ikonundan ayırmak için SweetCherry export açıklaması ve hook sırası kullanılır; eski dosyanın açıklamasını silmeyin.
- Freeplane'in kendi ikonları, yerleşimleri, katlanması, kenar/bulut çizimleri, hatırlatıcıları ve betikleri etkin özellik olarak aktarılmaz. Formüller yalnızca metindir. Şifreli içerik çözülemez; görünen bilgiler ve uyarı korunur.
- Bağlantılar, normal öznitelikler, okların hedef kimlikleri ve dış nesne/resim referansları mümkün olduğunca düz metin olarak tutulur. Freeplane kimlikleri canlı SweetCherry iç bağlantılarına çevrilmez. Hiçbir dış dosya, URL veya resim okunmaz. Stil/hook öğeleri düğüme dönüşmez.

## SweetCherry export dosyasını geri alma

İçeriği de geri almak için export sırasında **not içeriğini dahil etme** seçeneğini açın. İkonlar için referans veya ikon paketi modunu; renkler için renkleri dahil etmeyi seçin. ZIP indirilmişse içinden yalnızca `.mm` dosyasını çıkarıp yükleyin. Import için ikon klasörüne gerek yoktur.

MM'de taşınan bilgiler geri alınır; MM tam CTB yedeği değildir. Özgün zengin metin/Markdown/kod türü, ikili nesneler, etiketler, yer imleri, salt okunur bayrağı ve ortak düğüm ilişkileri mevcut export'ta taşınmaz. Ortak düğüm görünümleri bağımsız kopyalara dönüşür. Veritabanının tamamının export'undaki yapay kök, yeni kapsayıcı dal olur. Sınırlı derinlikle export edilen dosyada olmayan seviyeler geri alınamaz. Bu ek ayrıntıların da korunması gerekiyorsa CTB export/import veya CTB yedeği kullanın.

Sınırlar: 1 MiB, 10.000 düğüm, 128 alt seviye; özel servlet yükleme ayarları daha düşük bir sınır koyabilir. DTD, harici XML varlıkları ve bozuk/çok köklü XML reddedilir. Yazma için işlemsel tablolar gerekir (MariaDB/MySQL'de node/children için InnoDB). Testlerde, verilen `freeplaneFunctions.mm` dosyasının 69 gerçek düğümünün korunması da kontrol edilir.
