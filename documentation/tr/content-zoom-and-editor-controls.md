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


## Editör yerleşimi ve kod dili seçimi

- Masaüstünde zengin metin yazma alanı kalan ekran yüksekliğini ve içerik bölümünün genişliğini doldurur. “Text” başlığı görsel yer kaplamaz; erişilebilir etiket korunur. Yazma bölümünün dışında ikinci bir dikey kaydırma yoktur. Uzun metinde yazarken imleç görünür tutulur; sol araçlar bağımsız kaydırılabilir.
- Düz metin ve Markdown düzenleyicide, en az 900 px genişlik ve 500 px yükseklikte, üst menü, kurtarma paneli ve altbilgiden kalan alan yazmaya ayrılır. Alanı aşağı taşırmak yerine metin kendi içinde kayar. Küçük ekranlarda mevcut sayfa kaydırma ve yeniden boyutlandırma davranışı korunur.
- Markdown kaynak alanı ve önizlemesi üstten aynı hizadadır. Önizleme seçenekleri üst araç çubuğundadır.
- Özelliklerde **Kod Dili** araması, ikonlu alfabetik liste ve **Markdown** kısayolu bulunur. Bunlar içerik tipinin mevcut kilitlerini kaldırmaz; seçim Kaydet ile kalıcı olur. JavaScript yoksa standart dil listesi kullanılabilir.

Manuel kontrol: uzun içerik yazın; Enter, yapıştırma, geri alma/yineleme ve seçili metin biçimlendirmesini deneyin. Kurtarma panelini açıp kapatın ve pencereyi yeniden boyutlandırın. Markdown önizlemesini gizleyip açın. Kod dili arayın, Markdown seçin; salt okunur ve zengin metin düğümlerinin kilitli kaldığını doğrulayın. Mobilde araç çekmecesini ve metin seçimini kontrol edin.

## Editörde başlangıç, metin yakınlaştırma ve yazım alanı

Düz metin/kod, Markdown, HTML kaynak ve rich text görsel/kaynak editörleri açıldığında imleç son satırın sonuna gelir. Yerleşim tamamlanınca yazım yüzeyi ve sayfa sona kayar. Bu sırada seçim değiştirir, diyalog açar veya kurtarma taslağı yüklerseniz başlangıç kaydırması yeni konumunuzu bozmaz. Save and continue sonrası da kayıtlı içeriğin sonundan devam edilir. Gömülü codebox metni açıldığında da imleç sona gelir.

Save / Save and continue / Cancel sağındaki eksi, yüzde ve artı düğmeleri editör metnini %70–%200 aralığında, 10 puanlık adımlarla büyütür/küçültür. Yüzde düğmesi %100'e döndürür. Tercih aynı tarayıcı/site üzerindeki editörler arasında hatırlanır (`sweetcherry.editorTextZoom`); okuyucu yakınlaştırmasından bağımsızdır. Seçimi korur, undo adımı oluşturmaz ve kayıtlı metni/yazı boyutunu değiştirmez. Depolama engelliyse o sayfada çalışır.

Son satırın altında yazım yüzeyinin yaklaşık %35'i kadar (32–240 piksel aralığında) rahatlık alanı bulunur. Bu CSS boşluğudur; içerikte boş paragraf/satır oluşturmaz. Sonda yazarken veya Enter kullanırken boşluk görünür kalır. Önceki satırlarda çalışmak sizi tekrar sona atmaz.

Rich text araçlarında Undo/Redo WYSIWYG anahtarının sağındadır; stil listesinin yanında dört hizalama düğmesi bulunur. Kontroller metinleri kadar yer kaplar, dar alanda gerektiğinde sarılır. Nesne silme izni ve Manage objects, Dismiss link üzerinde ayrı bir satırdadır. Yedekleme hatırlatması araç panelinin en altındadır.

## 1.15.0: ağaç içinde düzenleme ve resimli içerik

`/tree` içindeki Edit, editörü içerik bölmesinde açar. Save okuma görünümünü ve
kaydırma konumunu geri getirir; Save and continue aynı bölmede düzenlemeye devam
eder. Kaydedilmemiş değişikliklerle gezinme veya split bölmesini kapatma onay ister.
Okuma düzeni tercihleri korunur; dar bölmede rich text araçları Controls çekmecesindedir.

Yüklü resimler, tablolar ve codebox'lar metin içindeki konumlarıyla kopyalanabilir;
yapıştırıldıklarında yeni nesneler oluşturulur. **Allow object deletion / Nesne
silmeye izin ver** açıkken Delete, Backspace, Kes ve seçimin üzerine yapıştırma
nesnelerle de çalışır. Save öncesinde undo/redo ile geri alınabilir. Mevcut kayıtlı
ekli dosya ve anchor'lar nesne olarak kopyalanmaz; metin yer tutucusu ve uyarı
kullanılır, tam kopyalanamayan nesne varsa Kes seçimi silmez.

PNG, JPG/JPEG, WebP, GIF, BMP, TIFF ve WBMP içe aktarılabilir. Yeni CTB resimleri
PNG olarak kaydedilir; animasyonlu GIF/WebP ve çok sayfalı TIFF'in ilk karesi/sayfası
alınır. SVG ve AVIF bu yolda desteklenmez. Yerel dosyalarda PNG dışındaki resimler,
editöre eklenmeden önce yerel Java decoder ile dönüştürülür; bu hazırlık işlemi CTB'ye yazmaz.

### Resim adedi ve güvenlik bütçeleri

Eski 8 web resmi ve 10 yeni nesne sınırları resimler için kaldırıldı. Ancak resim
adedi sınırının olmaması, toplam veri veya çalışma süresinin sınırsız olması değildir:

| Kontrol | Sınır |
| --- | --- |
| Tek dosya/resim | Varsayılan 9 MB; tenant ayarı `custom.maxEmbeddedFileSizeMB` ile 1–20 MB; kaynak ve dönüştürülmüş PNG ayrı ayrı sığmalı. |
| Bir kayıttaki toplam yeni ikili yükleme | 20 MB; resim ve ekli dosyalar birlikte. |
| Yükleme JSON'u | Toplam 30 milyon karakter. |
| Tek resmin genişliği/yüksekliği | Her kenar en çok 8192 piksel. |
| Tek resmin piksel sayısı | En çok 16 milyon. |
| Bir kayıttaki resimlerin toplam çözülmüş pikseli | En çok 64 milyon. |
| Bir kayıt için resim çözme bütçesi | 30 saniye. |
| Web içe aktarmadaki dönüştürülmüş resimler | Toplam 10 MB. |
| Web sayfası ve resimlerin ağ bütçesi | Toplam 20 MB ve 30 saniye. |

Tek dosya ayarı dışında bunlar mevcut kodun güvenlik bütçeleridir; hepsi için
`application.yml` veya Settings alanı bulunmaz. Resim dışındaki nesnelerin ayrı
sınırları da devam eder. HTML panosunda ayrıca boyut ve DOM karmaşıklığı kontrolleri vardır.

### Gazete veya başka bir siteden bazı resimler neden gelmeyebilir?

Limitler tek olası neden değildir. Resim sunucusu cookie/giriş veya uygun Referer
isteyebilir, doğrudan indirmeyi engelleyebilir, URL erişilemez olabilir ya da resim
yalnız JavaScript çalıştıktan sonra yükleniyor olabilir. `srcset` veya siteye özel
lazy-loading kodu her zaman gerçek resim URL'sine çevrilemez. Panodaki göreli URL'ler
kaynak sayfa bilinmediğinde çözülemez; desteklenmeyen veya bozuk resimler de atlanır.

SweetCherry web sayfasının scriptlerini çalıştırmaz ve tarayıcınızın oturum
cookie'lerini kullanmaz. Yalnız doğrulanmış herkese açık HTTP/HTTPS adreslerini
alır; özel/yerel ağ hedefleri engellenir. Yapıştırmada uzak resimler indirilmeden
önce onay sorulur. Atlanan resimlerin sayısı sonuçta gösterilir; resim yerine varsa
alternatif metin kullanılır. Gerekirse erişebildiğiniz resmi ayrıca indirip Resim
ekle ile deneyebilirsiniz. İçe aktarma başarısızsa veya içerik yükleme sırasında
değişmişse mevcut seçimin üzerine yazılmaz.
