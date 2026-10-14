# İkon seçimi ve üst düğüm ID doğrulaması

Gelişmiş aramada masaüstü/mobil ikon listesinin üzerindeki arama kutusu yalnız ikon seçeneklerini daraltır. Not içerik araması hâlâ raw node.txt üzerinde çalışır. İkon seçimleri saklanır; seçili ikonlar listenin dışında yan yana gösterilir. İkon filtresini daraltmak gizlenen ikonun seçimini kaldırmaz. Asıl arama mevcut Filtrele düğmesiyle yapılır; arama formunun POST/binding yöntemi korunmuştur.

Listenin ilk seçeneği İkonsuz / No icon'dur. Bu seçenek veritabanındaki açık ikon ID'sinin 0 olması anlamındadır; kod düğümünde arayüz otomatik dil ikonu gösterebilir. Hiç seçenek işaretlenmezse tüm ikonlar kabul edilir; İkonsuz ve başka ikonlar birlikte seçilebilir. İkon seçimleri content read-only bitinden bağımsız eşleşir.

Düğüm özelliklerinde ikonlar alfabetik, resimli ve tek seçimlidir. İkonsuz seçeneği ilk sırada tutulur. İkonlar yerel ctbicons klasöründen gelir. Tam liste resimleri ancak liste açıldığında kaynak alır, img loading=lazy kullanır; seçili ikon özeti başlangıçta yalnız seçili resmi yükler. Arama kutusuna yazarken ağ isteği gönderilmez. Sonraki sayfa açılışlarındaki HTTP cache/revalidation tarayıcı ve sunucu cache ayarlarına bağlıdır.

Başlık renkleri Türkçe/İngilizce adlarla gösterilir. Hex değer etiket tooltip'inde görünür. Mevcut özel renk korunabilir.

/father sayfasına sayısal olmayan, negatif veya Long sınırını aşan fatherId verilirse boş sonuç ve açıklama gösterilir; sorgu çalıştırılmaz. 0 kök düğümler için geçerlidir. Parametre verilmezse veya boşsa 0 kabul edilir. Geçerli fakat mevcut olmayan ID normal boş sonuç verebilir.

Arama sırasında tablo dönüşümünden gelen HTML çıktısı ve NodeService rutin arama kayıtları yalnız myapp.debug=true ise konsola yazılır. Gerçek hata kayıtları kapatılmamıştır.
