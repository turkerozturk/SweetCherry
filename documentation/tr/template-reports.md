# Şablon raporları — sıralı alanlar ve açıklamalar

Mevcut dört adres korunur: `/cherrytemplatenode/<id>` ayrıntılı HTML liste; `/cherrytemplate-type-1/<id>` karşılaştırmalı HTML tablo; `/cherrytemplatenode/pdf/<id>` A4 yatay PDF; `/cherrytemplatenode/xlsx/<id>` XLSX tablo. Bu görünümler veritabanı şemasını, notları, tagları ve tarihleri değiştirmez. Tek kayıt için özelleştirilebilir tasarımlar/placeholder'lar sonraki adımdır.

## Kayıt ve alan kuralları

Proje kayıtları eskisi gibi **`node.tags` alanının tamamının** şablon düğümü adına eşitliğiyle bulunur. Tag parçaları üzerinden aramaya geçilmez; paylaşımlı kayıt konumları bu adımda fazladan proje satırlarına dönüştürülmez. Sıralama ad, sonra ID şeklindedir; `yyyy-mm-dd` adları kronolojik sıralanır.

Şablonda her satır `anahtar<TAB>görünen başlık` biçimindedir. Başlık yok/boş ise anahtar kullanılır. Dört çıktıda alanlar tanım sırasını korur. Boş ve `#` yorum satırları atlanır. Eski davranışı korumak için **ilk karakteri ASCII boşluk olan** satırlar da devre dışıdır. Bu eski kural alan tanımı içindir; ana kurulum düğümünün ayrıştırma kuralları kendi dokümantasyonundaki gibi kalır.

Anahtarı boş tanım geçersizdir. Tekrarlanan etkin anahtarların ilk satırı dahil tüm eşleşmeleri dışlanır; sessizce bir başlık seçilmez. HTML/PDF satır numarası ve anahtarla açıklama gösterir; XLSX'e Definitions sayfası eklenir. Kaynak içerik değiştirilmez.

Değerler, kayıt düğümünün yalnız doğrudan altındaki ve adı değişken anahtarına tam eşit düğümlerden alınır. Diğer alt düğümler gösterilmez. Paylaşımlı alt düğümde konum/bağlantı `children` ID'siyle, içerik master'dan alınır. Aynı master'a bağlı iki konum da iki eşleşmedir.

| Durum | Anlamı |
| --- | --- |
| Değer | Tek eşleşme ve dolu içerik |
| Alt düğüm yok | Eşleşen çocuk yok |
| İçerik boş | Tek eşleşme var, içerik boş/yalnız boşluk |
| Birden fazla eşleşen alt düğüm | Birden fazla konum; ID'ler gösterilir, rastgele içerik seçilmez |

PDF'nin mevcut sütun oranları (%5 sıra, %20 ad, %75 alanlar; sayfanın yarısı değerler), ad/başlıkta iki kelimede satır kırma, başlık ve sayfa toplamları korunur. Eksik/boş alanlarda küçük durum yazısı ve elle doldurulacak yer vardır. XLSX görünen başlıkları, çok satırlı metni, düz metin hücrelerini, sabit başlık/kimlik sütunlarını, filtreleri ve eksiklerde renkli vurguyu kullanır. Excel'in normal satır/sütun/hücre sınırları geçerlidir; uzun notlar sessizce kesilmez.

HTML'de rich text mevcut okuyucu ile, Markdown render edilerek; düz metin/kod güvenli kaçışla ve satır sonları korunarak gösterilir. XLSX, HTML'yi XML sanarak ayrıştırmak yerine ortak düz metin dönüşümünü ve nesne açıklamalarını kullanır. HTML ek dosya bağlantılarında geçerli bağlantı belirteci vardır.

## Ekran kontrolleri

İki HTML görünümünde çok kelimeli arama (tüm kelimeler), yalnız eksik/belirsiz kayıtlar ve Bugüne git bulunur. Arama görünen satır metni, breadcrumb, ID ve değerleri kapsar. Bunlar ekran filtresidir; PDF/XLSX **tüm kayıtları** içerir. Açıklama kontrol alanında gösterilir.

Tarayıcının yerel bugünkü tarihiyle tam eşleşen `yyyy-mm-dd` adlı kayıt varsa sekme/bağlantı/görünüm için ilk ziyarette ona kayılır. Yenileme/geri dönüşte tekrar tekrar sıçramaz. Bugüne git, filtreyle gizlenmiş kaydı da açabilir. TODO/FOCUS vurguları yanıp sönmeden korunur. Önümüzdeki 90 gündeki tarihli kayıtların `dailyalarm` değerleri otomatik popup yerine açılır bir listede gösterilir.

## Manuel kontrol

1. `z<TAB>Zed`, sonra `a<TAB>Alpha` tanımlayın; dört çıktıda sırayı kontrol edin.
2. `#disabled`, başında ASCII boşluk olan eski yorum, boş satır ve tekrarlı `z` ekleyin. Yorumların atlandığını, tekrarlı anahtarın tamamen dışlandığını ve açıklamaların dört çıktıda bulunduğunu görün.
3. `a` olmayan, boş `a`, dolu `a`, iki `a` alt düğümlü kayıtlar oluşturun. Dört durumu ve çoklu ID bağlantılarını kontrol edin. Aynı master'a bağlı paylaşımlı çocuklarla da deneyin.
4. Resimli rich text, Markdown, çok satırlı düz metin/kod, düz metin içinde `<script>` ve `=1+1` deneyin. Son örneğin XLSX'te formül olmadığını görün.
5. Çok kelimeli arama ve eksik filtresini iki görünümde deneyin; indirmelerin tüm kayıtları kapsadığını görün. Bugünkü tarihli kayıtta otomatik/elle kaydırmayı kontrol edin.
6. TR/EN, PDF elle doldurma yeri/başlık/sayfa toplamı ve XLSX sabit başlıkları kontrol edin. USER/salt okunur erişimi, bağlantı kapatmayı ve başka sekmede bağlantı değiştikten sonra eski belirteçli linklerin sorgu yapmamasını deneyin.

PDF’de normal resimler değer sütununa sığdırılıp ayrı satırlarda gösterilir; çizicinin metinle çakışması önlenir. Satır içi LaTeX ve ataş ikonları mevcut akışını korur.
