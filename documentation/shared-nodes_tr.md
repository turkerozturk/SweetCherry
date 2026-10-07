# CherryTree paylaşılan düğümleri ve silme davranışı

SweetCherry'de ağaç yapısı `children` tablosunda tutulur. Gerçek bir düğümün hem
`node` hem de `children` tablolarında aynı `node_id` ile kaydı vardır. `father_id=0`
olan kayıtlar ağacın en üst düzeyindeki birbirinden ayrı düğümlerdir; bunların
üstünde gerçek bir düğüm bulunmaz.

Paylaşılan düğüm (alias), başka bir dalda aynı içeriği göstermek için kullanılan
bir başvurudur. Yalnızca `children` tablosunda kendi `node_id` değeriyle bulunur;
`node` tablosunda aynı kimlikle bir satırı yoktur. `children.master_id` alanı
başvurunun işaret ettiği gerçek düğümün `node_id` değeridir. Gerçek düğümlerde
`master_id` genellikle `0`, eski CTB sürümlerinde `NULL` olabilir. Bir gerçek
düğüme sıfır veya daha çok paylaşılan düğüm bağlanabilir.

Yazma izni `allTenants` altındaki ilgili tanımda `custom.isWritable=true` olarak
açıldığında silme işleminin hedefi şu şekilde belirlenir:

- Gerçek veya paylaşımlı düğüm seçilirse yalnız seçilen konum ve kendi alt ağacı silinir. Master bağlantısı bir ağaç kenarı değildir.
- Silinen alt ağaç dışında kalan referanslar ve onların alt düğümleri korunur.
- Silinen bir gerçek düğümün dışarıda referansları varsa biri yeni gerçek düğüm/master olur. İçerik ve tüm nesne satırları onun kimliğine taşınır; diğer referanslar ona bağlanır.
- Silinen konumların bookmark’ları kaldırılır; kalan konumların bookmark’ları korunur.
- Döngü, eksik parent veya bozuk içerik referansı varsa herhangi bir yazmadan önce işlem reddedilir.

Örnekler ve test listesi: [Paylaşımlı düğüm işlemleri](tr/shared-node-operations.md).

İşlem tek veritabanı transaction'ında yürütülür. Denemeler için gerçek not
arşivi yerine CTB dosyasının bir kopyasını kullanın. Bu açıklama CherryTree
veritabanı düzenini ve SweetCherry'nin bu düzen üzerindeki silme kurallarını
anlatır; dosyanın `node`/`children` tablolarında sıra dışı kayıtlar varsa
uygulama silmeyi reddeder.

## Release öncesi kullanım notu

Silme özelliği henüz gerçek not arşivleri için önerilmiyor. Şimdilik yalnızca
ayrı bir demo/kopya CTB üzerinde deneyin; diğer `allTenants/*.txt` tanımlarında
`custom.isWritable=false` kullanın veya ayarı hiç eklemeyin. Paylaşılan düğüme
ait ağaç bağlantısının URL'si kendi `children.node_id` değerini korumalıdır;
silme onayında bu kimliği ve gerçek düğümün kimliğini ayrı ayrı kontrol edin.
Eski sürümlerde silinmiş düğümlerin geride bıraktığı bookmark kayıtları okuma
sayfasında ayrıca bildirilir, otomatik olarak veritabanından temizlenmez.


## DİKKAT: Shared node altında gerçek düğüm

Manuel bir denemede CherryTree arayüzü gerçek bir düğümün shared node altına taşınmasına izin verdi. Bu gözlem tek başına CherryTree'de bir hata bulunduğu anlamına gelmez; bu düzenin silme ve gösterim kuralları ayrıca araştırılmalıdır.

SweetCherry şu anda shared node'yi yaprak olarak gösterir. `children.father_id` bir shared node'yi gösteriyorsa onun altındaki kayıtlar bu görünümde beklenen yerde görünmeyebilir. Böyle bir CTB'de taşıma seçenekleri yüklenmeyebilir; silme işlemi çocuk kayıtları olan shared node için reddedilir. Çoğaltma da shared node altındaki düğümü veya böyle bir yapı içeren alt ağacı reddeder. Kayıtlar otomatik düzeltilmez ve silinmez.

**TODO:** Bu hiyerarşinin CherryTree'deki gösterim, taşıma ve silme davranışını bir CTB kopyasında incelemek; SweetCherry'de uyumlu gösterim ve açık bir işlem politikası belirlemek. Şimdilik bu düzeni kullanmadan önce yedek alın; normal SweetCherry işlemlerine dönmek için gerçek düğümü CherryTree'de gerçek bir parent altına veya top-level'e taşıyın.
