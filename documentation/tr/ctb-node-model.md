# CTB düğüm kimlikleri, sanal kök ve etiketler

## Sanal kök: 0

`0` SweetCherry’de ağacın sanal ana düğümüdür; `node` tablosunda gerçek bir içerik satırı değildir. Üst düzey düğümlerin her biri `children` tablosunda `father_id = 0` taşır. İlk üst düzey düğüm oluşturulduğunda bu ilişki oluşur; boş veritabanında böyle bir ilişki bulunmayabilir. SweetCherry boş ağacı da `0` sanal köküyle gösterebilir. Bu, veritabanına gerçek bir `node_id = 0` düğümü kaydettiği anlamına gelmez.

## Gerçek ve paylaşımlı düğümler

Gerçek düğümün içeriği `node` tablosundadır. Paylaşımlı düğüm (shared node / alias), gerçek düğüm içeriğini başka bir ağaç konumundan gösteren referanstır. Kendi `children.node_id` değeri ile gerçek düğümü işaret eden `children.master_id` değeri bulunur; kendi adına `node` içerik satırı yoktur. Konumu `father_id` ve `sequence` alanlarıyla belirlenir. Aynı gerçek düğümün birden fazla referansı olabilir.

Eski yardım notlarında CherryTree 1.1.0 ile paylaşımlı düğüm desteği ve düğüm özelliklerinde gerçek/paylaşımlı kimliklerin görüntülenmesi anlatılıyordu. Bu tarihsel not burada korundu. SweetCherry açısından asıl ayrım, içerik kimliği ile ağaçta seçilen referans kimliğidir: bağlantılar ve bookmark’lar referansın kendi ID’sini korumalıdır.

Gerçek düğümün silinmesi ile tek bir paylaşımlı referansın silinmesi farklı işlemlerdir. Paylaşımlı düğümlerin altında kayıt bulunmasıyla ilgili uyumluluk uyarısı için `node-movement.md` belgesine bakın.

## Etiketler ve sonraki işler

- TODO: Etiketlerin arama/kategori kullanımını ve SweetCherry şablon görünümlerindeki özel anlamını örneklerle belgeleyin. Mevcut şablon kodunda tags eşleşmesi kullanılır; bu not yeni bir etiket sistemi eklendiği anlamına gelmez.
- TODO: CherryTree’nin “aramaların dışında tut: bu düğüm / alt düğümleri de” seçeneklerinin CTB’de nasıl saklandığını ve SweetCherry aramalarında uygulanıp uygulanmadığını inceleyin; davranış testlerini ekleyin.
- TODO: Şablon PDF davranışını doğrulayın. `/cherrytemplatenode/pdf/{templateNodeId}` endpoint’i mevcut, `TemplateService.getTemplateNode(...)` sonucunu `cherrytemplatenode-pdf` üzerinden PDF’e dönüştürüyor. TemplateNode tags değeriyle eşleşen düğümlerin alt içeriklerini toplama iddiası, şablon/veri hazırlığı ve çıktı birlikte test edilmeden tamamlanmış özellik sayılmamalı.

`/help/index` artık proje README’sine yönlenir. Eski genel yardım ve şablon PDF yardım şablonları kaldırıldı; Git geçmişinden incelenebilir. Şablon görev/düğüm yardım sayfaları korunur ve `/cherrytemplatetasks` üzerinde bağlantıları bulunur.
