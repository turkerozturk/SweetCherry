# Zengin metin editöründe iç bağlantılar

**İç bağlantı ekle / düzenle** düğmesi aktif veritabanındaki gerçek veya paylaşımlı bir ağaç konumuna bağlantı oluşturur. Bu işlem dış HTTP/HTTPS bağlantısı düğmesinden ayrıdır. Bağlantı CTB XML içinde CherryTree biçiminde `link="node ID"` veya `link="node ID anchor adı"` olarak saklanır; geçici oturum token'ı içermez.

## Kullanım

- Metin seçip düğmeye basın: bağlantı metni seçiminizle doldurulur. Seçilen metinle **tam aynı adlı** hedefler otomatik aranır. Büyük/küçük harf ve boşluklar aynen karşılaştırılır. Aynı isimli gerçek ve paylaşımlı konumlar ayrı ID'lerle listelenir; ikon, kalınlık ve başlık rengi gösterilir. Bir sonuç seçmek yalnız hedefi doldurur; **Bağlantıyı uygula** ile işlem tamamlanır.
- ID yazın veya pano düğmesiyle yapıştırın. **Hedefi bul** adı ve mevcut anchor'ları gösterir. Panoya erişilemiyorsa elle yapıştırma kullanılabilir.
- Seçim olmadan imleç konumunda da bağlantı eklenebilir. Bağlantı metni boş/yalnız boşluksa hedef düğümün adı kullanılır; adsız hedef için `[ID]` kullanılır.
- Mevcut iç bağlantının içine imleci koyup aynı düğmeyi açın. Bitişik, aynı hedefli biçimli metin parçaları birlikte seçilir. Hedef, metin ve mevcut anchor değiştirilebilir. **İç bağlantıyı kaldır**, metni ve diğer biçimlerini korur.
- Bir hedefin mevcut anchor'ı isteğe bağlı seçilebilir. Paylaşımlı hedefte anchor listesi içerik master'ından gelir ama link paylaşımlı konumun ID'sini korur. **Yeni anchor nesnesi oluşturma bu adıma dahil değildir.**

Nesne içeren seçimler reddedilir; resim/tablo/kod kutusu/LaTeX verisi değişmez. Ekleme, düzenleme ve kaldırma tek geri alma adımıdır. Diyalog iptali yazmaz; ana **Kaydet / Kaydet ve devam et** işlemine kadar veritabanı değişmez. Var olan metni değiştirmeden yalnız hedefi düzenlerseniz parça bazındaki kalınlık, renk vb. korunur. Bağlantı metnini değiştirmeniz yeni metnin başlangıç biçimini kullanır.

Hedef ID/anchor uygulamadan hemen önce yeniden okunur. Ana kayıtta yeni veya hedefi değişmiş iç bağlantılar aynı veri kaynağı işlemi içinde tekrar doğrulanır. Hedef bulunmazsa kayıt yapılmaz. Önceden var olan bozuk/legacy bağlantılar başka düzenlemeleri engellememek için değişmeden korunabilir. Daha sonra hedefin silinmesi bağlantıyı yine geçersiz yapabilir; bu özellik düğüm silme davranışını değiştirmez.

Yetki, salt okunur düğüm/veri kaynağı, revizyon ve sekmeler arası veri kaynağı token kontrolleri korunur. Arama metin/BLOB içeriklerini okumaz. En fazla ilk 200 tam eşleşme gösterilir; daha fazlasında uyarı ve ID ile seçim vardır. Dış bağlantı, paste, nesne yönetimi ve kaydetme akışları korunur.

## Manuel kontrol listesi

1. Boş seçimle ID girin, metni boş bırakın: hedef adı imleçte bağlantı olmalı. Olmayan, negatif, sıfır, harf ve Long sınırını aşan ID eklenmemeli.
2. Aynı adlı birkaç gerçek/paylaşımlı konum oluşturun. O adı seçip diyaloğu açın; tüm uygun hedefleri ikon/kalınlık/renk ve kendi ID'leriyle görün. Shared hedef seçildiğinde master ID'ye dönüşmemeli.
3. Mevcut bağlantının ortasına imleci koyup hedefi, metni, mevcut anchor'ı değiştirin; bağlantıyı kaldırın. Biçimleri, geri alma/yeniden yapma ve diyalog iptalini kontrol edin.
4. Aynı işlemleri Türkçe/İngilizce ve mobil görünümde deneyin. Pano izin vermediğinde elle yapıştırmayı kullanın.
5. Nesne içeren seçimin reddini; resim, tablo, kod kutusu ve LaTeX'in değişmediğini kontrol edin. Dış bağlantı ekleme ve düzenlemeyi tekrar deneyin.
6. Diyalogdan sonra hedefi başka oturumdan silin veya veritabanını değiştirin: işlem yeni/geçersiz hedefe sessizce yazmamalı.
7. Kaydet ve devam et ile açılan içeriği ve CherryTree'de CTB'yi kontrol edin. Mevcut anchor hedefi varsa bağlantı onun konumuna gitmeli. PDF/mind-map/CTB export mevcut iç bağlantı işleme kurallarını kullanır.

## English

**Add / edit internal link** links selected text, or inserts text at the caret, targeting an occurrence ID in the current database. Blank text uses the target name. Selected text automatically finds exact-title matches, including real/shared occurrences separately, with icons, boldness, color and IDs. Choose a result and explicitly apply. Existing links can be edited by placing the caret inside them; removal preserves text/formatting. Changing only the target retains inline formatting; replacing the label uses its starting style.

Existing anchors can be selected, including those from a shared target's master content. Creating new anchor objects is deferred. Storage uses CherryTree's `node ID [anchor]` link value, never a session token. Existing broken legacy links can remain unchanged; new/changed target values must resolve when saving. This does not change deletion rules or prevent links becoming broken later.

Editor changes support undo/redo and remain local until the main Save action. Objects, external links and stale/read-only/permission protections are preserved. Exact matching is case/whitespace sensitive; at most 200 results are displayed with a truncation notice and ID lookup fallback. The lookup reads metadata only, not note content or BLOBs. Test ID/name/shared targets, anchors, edit/remove/cancel, invalid or deleted targets, tenant changes, mobile use and mixed-object content before relying on it with important notes.
