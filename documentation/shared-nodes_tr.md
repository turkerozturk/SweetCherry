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

- Paylaşılan düğüm seçilirse yalnızca o `children` kaydı silinir. Asıl düğüm,
  onun içeriği, diğer paylaşılan başvurular ve varsa bookmark korunur.
- Gerçek düğüm seçilirse düğümün alt ağacı, içerik tablolarındaki kayıtları ve
  silinen gerçek düğümlere başka dallardan bağlı paylaşılan başvurular silinir.
- Altında `father_id` üzerinden çocuk bulunan bir paylaşılan düğüm saptanırsa
  işlem iptal edilir. Bu durumda çocukların nasıl korunacağına ilişkin açık bir
  kural belirlenmeden silmek ağaçta sahipsiz kayıt bırakabilir.

İşlem tek veritabanı transaction'ında yürütülür. Denemeler için gerçek not
arşivi yerine CTB dosyasının bir kopyasını kullanın. Bu açıklama CherryTree
veritabanı düzenini ve SweetCherry'nin bu düzen üzerindeki silme kurallarını
anlatır; dosyanın `node`/`children` tablolarında sıra dışı kayıtlar varsa
uygulama silmeyi reddeder.
