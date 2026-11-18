# PDF içerik ve yerleşim ayarları

Düğüm PDF ayarları sayfasında, alt düğümlerle birlikte dışa aktarma dahil, iki bağımsız seçim bulunur:

- **İç bağlantılar:** bağlantı metinlerini göster (varsayılan) veya bağlantı konumuna hedef düğümün içeriğini ekle.
- **Düğüm yerleşimi:** sonraki satırdan devam et (varsayılan) veya her düğümü yeni sayfadan başlat. Bu seçim bağlantılardan eklenen içeriklere de uygulanır. Sayfa dolduğunda doğal olarak yeni sayfaya geçilir.

Seçimler başarılı indirme sonrasında mevcut oturumda hatırlanır; tek düğüm ve alt ağaç PDF ayarları arasında paylaşılır. Tarayıcıdaki bağlı içerik görünümü düğmesinden bağımsızdır. İçindekiler seçilirse içerikten ayrı kalır; içindekiler bağlantıları ve PDF yer imleri her iki yerleşimde de çalışır.

Bağlı rich text içeriklerde seçilen resim, codebox, tablo ve LaTeX nesneleri korunur. Düz metin, kod, Markdown ve temizlenmiş HTML hedefleri eklenebilir; yalnız rich text hedeflerin bağlantıları tekrar takip edilir. Anchor içeren başka düğüm bağlantısı hedefin tamamını ekler; aynı düğümün anchor bağlantıları yerel kalır. Her kopyanın anchor kimlikleri ayrıdır.

PDF eklemelerinde mevcut `myapp.node-fragments` derinlik, ekleme, kaynak, HTML ve nesne sınırları ile görünürlük kontrolleri geçerlidir. Ekleme denemesi sayısı ve 4.194.304 karakterlik ekleme bütçesi, alt ağaçtaki tüm düğümler dahil, bütün PDF işi için paylaşılır. Mevcut `myapp.pdf` sınırları da geçerlidir. Alias dahil döngülerde, eksik veya korumalı yardımcı hedeflerde ve ekleme sınırı aşıldığında özgün bağlantı metni PDF içinde kalır. Dışa aktarılan belge dışındaki iç bağlantılar mevcut PDF davranışındaki gibi yalnız metin olarak gösterilir. Harici kaynak indirilmez, düğüm içeriği değiştirilmez.
