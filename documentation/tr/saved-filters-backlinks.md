# Kaydedilmiş filtreler ve geri bağlantılar

## Kaydedilmiş arama filtreleri

Gelişmiş arama sayfasında arama alanlarını doldurun, bir filtre adı yazıp **Kaydet / güncelle** seçin. Kayıtlı bir filtreyi seçip **Uygula ve ara** ile mevcut POST aramasını çalıştırın. Seçili kayıt güncellenir; yeni kayıt için listeden **Yeni filtre** seçin. Silme onay ister. **Yenile**, başka sekmede yapılan değişiklikleri yükler.

Metin, ad, etiket, tür, tarih/ID aralıkları, ikonlar ve sayfa boyutu saklanır; sonuç sayfası sıfırlanır. İçerik araması hâlâ ham node.txt üzerinde çalışır. Tarihler kaydedildiği tarihlerdir; göreli tarih aralığı değildir.

Tercihler CTB şemasını değiştirmeden, uygulama çalışma klasöründeki `saved-filters/` altında saklanır. Aynı kullanıcı ve aynı bağlantı için farklı cihazlarda kullanılabilir. Başka kullanıcı veya bağlantı adıyla paylaşılmaz. Salt okunur veritabanlarında da kişisel filtre kaydedilebilir. Bağlantı adı, JDBC adresi veya uygulama klasörü değişirse ayrı kapsam oluşur. Kayıt başına ad 80 karakter, kapsam başına 50 kayıt ve dosya başına 1 MiB sınırı vardır. Arama metinleri bu dosyalarda bulunduğundan klasörü özel tutun; başka kişilere dağıtmayın. Dağıtım paketine dahil edilmez. Farklı sekmelerde eşzamanlı değişiklik olduğunda son değişiklik ezilmez; yeniden yükleyin.

## Geri bağlantılar

Düğüm içeriğindeki **Geri bağlantılar** bölümünü açın. Geniş masaüstü içerik alanında panel sağda, dar ekranlarda içerik üstünde görünür. Normal düğüm açılışında tarama yapılmaz. Panel kaynak konumu, ikon/kalınlık/renk, ad/ID, bağlantı metni ve hedef ID gösterir; kaynak üzerinde beklemek ağaç yolunu gösterir. Sayılar bağlantı adedi / kaynak konumu adedidir.

Varsayılan olarak tam olarak seçili ağaç konumuna gelen bağlantılar listelenir. **Tüm paylaşımlı konumları dahil et** aynı master içeriği kullanan konumları da kapsar; gerçek hedef ID korunur. Kaynak paylaşımlıysa bağlantı kendi ağaç ID’sini açar. Rich-text içindeki CherryTree `node ID [anchor]` bağlantıları ve nesne bağlantıları taranır. Markdown URL’leri bu taramaya dahil değildir. Kayıt ve değiştirilme tarihleri değişmez.

Sayfa başına 20 bağlantı gösterilir. Her açma/yenilemede en fazla 10.000 rich-text içerik ve toplam 32 Mi karakter taranır; tek içerik 2 milyon karakteri aşarsa atlanır. Görsel BLOB’ları okunmaz. Sınır veya bozuk XML varsa sonuçların eksik olabileceği gösterilir. Paylaşımlı kaynak konumları ayrı sayılır. Panel oturumu yenileme veya veritabanı değişiminde eski bağlantıyı kullanmayı reddeder.

## Manuel kontrol

- Nokta, Türkçe karakter ve birden çok ikon içeren filtre kaydedin; alanları değiştirip yeniden uygulayın. Değerler çoğalmamalı, POST araması çalışmalıdır.
- Yeniden girişte ve başka cihazda aynı kullanıcı/bağlantı kaydı görmeli; farklı kullanıcı/bağlantı görmemelidir.
- İki sekmede aynı kaydı değiştirin: eski revizyon reddedilmeli. Kaydı silin; sayfayı yenilediğinizde geri gelmemelidir.
- Gerçek ve paylaşımlı hedeflere bağlantılar oluşturun; panelin tam konum / tüm konumlar ayrımını ve kaynak ağaç ID’lerini kontrol edin.
- Masaüstü ve mobilde paneli açın, sayfalayın ve yenileyin; veritabanı değişmiş eski sekmeden istek kabul edilmemelidir.
