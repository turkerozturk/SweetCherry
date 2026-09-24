# CTB koleksiyonuna dışa aktarma

Bu belge SweetCherry'de görüntülenen CherryTree notlarının ortak bir CTB dosyasına aktarılmasını, mevcut davranışı ve ileride ele alınacak konuları açıklar.

## Terimler

CherryTree'deki her not bir **node** olarak adlandırılır. SweetCherry belgelerinde geçen **not**, **not düğümü**, **düğüm** ve **node** ifadeleri aynı kavramı anlatır.

## `exportednodes.ctb` ne işe yarar?

Bir not sayfasındaki **CTB Koleksiyona Aktar** düğmesi, seçilen düğümü `exportedFiles/exportednodes.ctb` dosyasına kopyalar. Bu dosya farklı CTB veritabanlarından seçilen notların bir araya getirildiği bir sepet veya koleksiyon olarak düşünülebilir.

- `exportedFiles` klasörü paketleme sırasında hazırlanır; çalışma anında eksikse export işlemi de otomatik oluşturur.
- `exportednodes.ctb` ilk başarılı koleksiyon export işleminde oluşturulur.
- Sonraki export işlemleri aynı dosyaya yeni düğümler ekler.
- Aynı düğüm tekrar aktarılırsa halen yeni bir kopya eklenir; yinelenen içerik kontrolü yapılmaz.
- Seçilen düğümün alt düğümleri varsa mevcut controller bunları da bulup aynı export işlemine dahil eder ve ilişkilerini yeni kimliklere göre kurar.
- Export işlemi artık `POST` isteğidir. Başarılı işlemden sonra sonuç sayfasına yönlendirme yapıldığı için tarayıcının geri/ileri veya yenile hareketi export'u kendiliğinden tekrarlamaz.

## Düğüm kimlikleri neden değişir?

Kaynak CTB'deki `node_id` değerleri hedefte aynen korunmaz. Güncel algoritma:

1. `exportednodes.ctb` içindeki en büyük `children.node_id` değerini bulur.
2. Bu değerin bir fazlasını işlem için başlangıç/ofset değeri yapar. Boş dosyada başlangıç değeri `1` olur.
3. Aktarılan her düğüm için `yeni node_id = başlangıç değeri + kaynak node_id` hesabını kullanır.

Bu nedenle kimlikler arasındaki boşluklar rastgele değildir; kaynak düğümlerin kimlikleri ve önceki en büyük hedef kimliği tarafından oluşur. Aynı ofset bir dalın bütün düğümlerine uygulandığı için dal içindeki baba/çocuk ve shared-node referansları yeni kimliklere taşınabilir. Bununla birlikte, yalnız tek düğüm aktarımlarında gereğinden büyük ve seyrek kimlikler üretilebilir.

## Dosyayı indirme ve silme

`/exportedFiles` sayfası oluşturulan CTB dosyalarını listeler.

- **Download**, `exportednodes.ctb` dosyasını bilgisayarda seçilen konuma indirir. Dosya CherryTree Desktop ile açılabilir.
- **Delete**, kullanıcı onayından sonra dosyayı siler. Silme işlemi veri değiştirdiği için `POST` isteği kullanır.
- Dosya silindikten sonra yeni bir düğüm export edilirse boş bir `exportednodes.ctb` yeniden oluşturulur.

Silmeden veya CherryTree ile düzenlemeden önce gerekli dosyaların yedeğini alın.

## SweetCherry içinde veri kaynağı olarak açma

`exportednodes.ctb`, başka CTB dosyaları gibi `allTenants` altında bir bağlantı tanımı oluşturularak SweetCherry'ye tanıtılabilir. Ancak hem export hedefi hem de aktif veri kaynağı olarak aynı dosyayı kullanmak kafa karıştırabilir ve eşzamanlı yazma riski doğurabilir. Şimdilik dosyayı önce indirmek/kopyalamak ve ayrı bir adla tanıtmak daha güvenlidir.

Bağlantı tanım dosyalarını `allTenants` klasörüne yüklemek için SweetCherry'de bir yükleme arayüzü de bulunmaktadır.

## TODO ve sağlamlaştırma notları

- Aynı kaynak düğümün daha önce export edilip edilmediğini belirleyen isteğe bağlı yinelenen kayıt kontrolü tasarla.
- Tek düğüm ve dal export seçeneklerini arayüzde açıkça ayır; mevcut alt düğüm davranışını otomatik testle sabitle.
- Kimlik üretimini, ilişkileri koruyan açık bir eski-yeni ID eşleme tablosuyla yeniden tasarla.
- Bir dalın tamamını tek transaction içinde yaz; yarım kalan export sonucunda eksik CTB oluşmasını engelle.
- Controller içindeki export durumunu istek başına yerel hale getir; eşzamanlı iki export isteğinin birbirini etkilemesini engelle.
- Export sırasında hedef CTB'nin SweetCherry veya CherryTree tarafından açık olması durumunu algıla ya da kullanıcıyı daha belirgin uyar.
- Export sonucu, dosya yolu ve eklenen düğüm sayısını kullanıcıya daha ayrıntılı göster.
- Download ve delete işlemlerinde izin verilen dosya adlarını sunucu tarafında daha sıkı doğrula.
- `exportednodes.ctb` dosyasını aktif tenant olarak kullanma senaryosunu ayrıca test et ve belgele.
