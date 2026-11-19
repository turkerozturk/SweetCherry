# İki düğümlü bölünmüş görünüm

`/tree` sayfasında **View Mode → Bölünmüş görünüm** veya içeriğin üstündeki **Bölünmüş görünüm** düğmesi ikinci okuma panelini açar. İlk kullanımda soldaki düğüm sağda da açılır. Sonraki kullanımlarda sağda en son açılan düğüm, veritabanı ana sayfası dahil, hatırlanır.

- **Sol panel** / **Sağ panel** düğmesiyle veya panelin içine tıklayıp odaklanarak etkin tarafı seçin. Etkin panelin üstünde mavi çizgi görünür.
- Ortak ağaç ve ağaç üzerindeki yön tuşları etkin panele yönlenir. Diğer panelin içeriği ve kaydırma konumu yerinde kalır.
- İç bağlantılar ve üst düğüm bağlantıları kendi panellerinde açılır. Ağaç etkin paneldeki düğümü işaretler ve üst dallarını açar.
- Footer etkin panelin bilgilerini gösterir. Sağdaki düğüm ayrıca **Yeni sekmede aç** düğmesiyle bağımsız açılabilir.
- İki içerik arasındaki ayırıcı sürüklenerek boyutlandırılabilir. Ayırıcıya klavyeyle odaklanıp yön tuşları da kullanılabilir. Dar pencerelerde paneller alt alta yerleşir.
- Sağ paneldeki kapatma düğmesi veya görünüm düğmesine tekrar basmak tek panele döndürür. Sağda bir form sayfası açıkken kapatma onayı istenir; editör panel kaldırılarak sessizce kaybedilmez.

Açık/kapalı durum, sağdaki düğüm, etkin taraf ve panel oranı bu tarayıcı sekmesinin oturumunda, seçili veritabanı görünüm belirtecine göre ayrı hatırlanır. Ana adres soldaki düğümü göstermeye devam eder; adresi başka sekmede açmak iki düğümü taşıyan paylaşılabilir bağlantı değildir. Yerleşim değişikliği düğüm içeriğine veya veritabanı şemasına yazmaz.

Her panelin ayrı belgesi vardır; aynı düğüm iki tarafta açıkken bile editör, menü, modal, anchor ve form kimlikleri çakışmaz. Sağda açılan form ve editörler sağ panelde kalır. Soldaki işlemler uygulamanın normal tam sayfa düzenleme akışını kullanır; `/tree` sayfasına dönünce bölünmüş görünüm geri yüklenir. Yazma yetkileri, CSRF, veritabanı belirteçleri ve revision kontrolleri geçerlidir. Bir düğümü bir tarafta düzenlemek diğer tarafta açık bir editörü ezmez; güncel içeriği yüklemek için düğümü tekrar açın.

Mevcut Alt+Shift+Yön tuşu taşıma kısayolu etkin paneli hedefler. Ağaçtan başlatılmış başarılı taşımanın ardından taşınan düğüme klavye odağı geri dönebilir. Normal yön tuşları yalnız odaklanılmış ağaç bağlantılarında gezinme yapar; içerik kaydırma, seçim ve editör gezinmesini değiştirmez.

Sağ panel aynı uygulama tarafından sunulur. `X-Frame-Options: SAMEORIGIN` uygulamanın kendi içinde panel açmasına izin verirken başka origin üzerinden çerçevelenmeyi engeller. Gömülü okuma paneli ikinci alarm sorgulayıcısı veya sesi başlatmaz; alarmı ana çalışma alanı yönetir. Geçersiz oturum ve değişen veritabanı seçimi için mevcut sayfa/oturum kontrolleri korunur.
