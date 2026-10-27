# ID ile düğüme gitme ve CTB dosya işlemleri

## Application / Uygulama → Düğüm ID’sine git

Menüdeki iki ayraç arasındadır. Önce veri kaynağı seçilmelidir; seçili değilken düğme devre dışıdır.

- Pozitif düğüm ID’sini yazın veya **Panodan yapıştır** ikonunu kullanın. Pano izni yoksa `Ctrl+V` / mobil yapıştır menüsü kullanılabilir; tarayıcının pano düğmesi HTTPS veya localhost gerektirebilir.
- Yer işareti ikonuyla ad, ikon, renk, kalınlık ve `[ID]` gösteren kısayolları açabilirsiniz. Ad filtresi Move to ile aynıdır. Seçim yalnız ID kutusunu doldurur; **Git** düğmesi sayfayı açar.
- Gerçek veya paylaşımlı konumun **kendi ağaç ID’si** kullanılır; shared ID master ID’ye dönüştürülmez.
- Seçili görünüm moduna göre masaüstü `/tree?nodeId=...` veya mobil okuyucu `/nodes/...` açılır.
- Hatalı/negatif/sınır dışı ID veya bulunamayan düğüm, sayfadan çıkmadan diyalogda bildirilir. Veri kaynağı başka sekmede değişirse eski pencereyle işlem yapılmaz; sayfayı yenileyin.
- Normal kullanıcı ve salt okunur veritabanıyla da kullanılabilir. Bu işlem veri yazmaz, tarihleri değiştirmez. Yer işareti listesi not metinlerini veya resim/BLOB içeriklerini yüklemez.

## Dışa aktarılan dosyalar

`/exportedFiles` başlıkları, düğmeleri ve silme onayı Türkçe/İngilizcedir. Tarih sütunu dosyanın **son değiştirilme** zamanıdır. Silme yalnız admin yetkisiyle ve onaylı POST isteğiyle yapılır. Başarılı silme, bulunamayan dosya veya silme hatası için sonuç sayfasında dosya listesine geri dönüş düğmesi vardır; otomatik süreli yönlendirme yoktur.

Düğüm sayfasındaki dört CTB export seçeneği aktarım başlamadan önce düğüm adı/ID, yalnız düğüm veya alt ağaç kapsamı ve ortak/yeni CTB hedefiyle onay ister. İptal hiçbir dosya oluşturmaz veya değiştirmez. Onay sonrasında mevcut sunucu yetki, veri kaynağı ve transaction kontrolleri uygulanır.

## Manuel kontrol

1. Türkçe ve İngilizce dosya listesini açın; bir test dosyasını silip sonuçtan listeye dönün. Eksik dosya sonucunda da geri dönüş düğmesi bulunmalı.
2. Gerçek/shared düğümde dört CTB seçeneğini masaüstü ve mobil okuyucuda deneyin. Önce onayı iptal edin; yeni CTB, `.old` veya koleksiyon satırı oluşmamalı. Onay verince tek aktarım yapılmalı.
3. ID ile gitme diyaloğunda yazma, pano, filtreli yer işareti seçimi, Git, İptal ve Escape’i deneyin. Sayısal olmayan/negatif/çok büyük/bulunamayan ID sayfayı değiştirmemeli.
4. Paylaşımlı bir konuma gidin; onun kendi alt ağacı ve içeriği açılmalı. Salt okunur kaynak ve normal kullanıcıyla tekrar deneyin.
5. Bir sekmede veri kaynağını değiştirip diğerinde açık diyaloğu gönderin: eski kaynak ID’si yeni veritabanında yanlışlıkla açılmamalı.

## English

**Application → Go to Node ID** accepts a positive occurrence ID, clipboard text or a filtered bookmark shortcut. Selecting a shortcut only fills the field; **Go** opens the real/shared occurrence in the selected tree/reader mode. It also works for authenticated readers and read-only databases, performs no writes, and preserves the tenant-view guard. Invalid or missing IDs leave the current page in place with a dialog message.

Exported-file labels, delete confirmations and result pages are translated. Every delete result has a **Back to exported files** link. The four CTB export options require confirmation before submitting; cancelling creates no output or archive copy.
