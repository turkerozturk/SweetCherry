# Veri kaynağı seçimi ve bağlantı yaşam döngüsü

Menü ve veri kaynağı seçim sayfası aynı POST endpointlerini kullanır: `/setTenant`, `/close-datasources`, `/reload-datasources`. Kontroller ve bağlantı işlemleri ortaktır. Görsel kontroller dışında aynı backend işlemini iki kez test etmek gerekmez.

Eskiden **CTB’yi kapat** yalnız oturum seçimini ve görünüm token'ını temizliyordu. Havuz açık kaldığı için Windows CTB dosyasını kullanımda görüyordu. Artık seçili kaynağın JDBC havuzu da kapatılır. Sonra yeniden seçildiğinde havuz yeniden oluşturulur. Devam eden, bağlantı ödünç almış bir sorgu/transaction varsa kapatma reddedilir; işlemin tamamlanmasından sonra tekrar denenir. Bu işlem zorla connection kapatıp transaction kesmez.

Havuzlar uygulama genelinde paylaşılır. Kapatılan/reload edilen kaynağı kullanan diğer oturumların seçimi sonraki istekte temizlenir; kullanıcıların login oturumu kapatılmaz. Aynı CTB farklı tenant dosyalarıyla tanıtılmışsa diğer havuzlar dosyayı açık tutabilir: dosyayı değiştirmeden önce ilgili tüm kaynakları kapatın ve başka uygulamaların da dosyayı kullanmadığından emin olun. Havuz kapatma veritabanı sunucusunu durdurmaz, JDBC bağlantılarını bırakır.

`/tenants` tablosundaki Active/Passive o oturumdaki seçimi gösterir; diğer oturumların veya diğer alias havuzlarının bağlantısız olduğunun kanıtı değildir. Veri kaynağı değiştirmek önceki havuzu otomatik kapatmaz. Veri kaynaklarını yeniden yüklemek önceki havuzları kapatır ve kayıtları yeniden okur. Devam eden işlem yüzünden reddedilirse, bazı önceki havuzlar kapanmış olabilir; kaynak tekrar seçilerek açılabilir.

SQLite JDBC normalde eksik dosyayı oluşturabilir. Mevcut CTB açılırken CREATE bayrağı kaldırılır; dosya eksikse sıfır bayt dosya oluşturmak yerine seçim reddedilir. SQLite kaynağı seçilirken `node`, `children`, `bookmark`, `image`, `grid`, `codebox` tablolarının varlığı kontrol edilir. Bu temel kontrol tüm veritabanı bütünlüğünü doğrulamaz. Boş/yanlış formatlı dosya reddedilir. Bu davranış yeni veritabanı oluşturma özelliği değildir. MySQL/MariaDB/PostgreSQL ve diğer JDBC URL'lerine SQLite dosya/şema kontrolü uygulanmaz; sürücü bağlantısı denenir. JDBC havuzunun kapatma/yeniden açma akışı ortaktır; uzak sürücülerle uyumluluk ayrıca gerçek ortamda test edilmelidir.

Yükleme başarısızsa önceki oturum seçimi korunur; kullanıcıya genel açıklama, loga teknik neden verilir. İndirme ikonu `/tenants` sayfasında isim yanında durur; hover dosya adını gösterir. Tenant config dosyası bağlantı şifresi içerebildiğinden indirme yalnız ADMIN içindir ve HTTP cache kapalıdır. Tablo DB şifresini açık metin göstermez. Tenant dosyaları değiştirilmez ve SweetCherry yedek oluşturmaz.

Manuel kontrol: CTB seç → sorgu aç → CTB kapat → Windows'ta dosyayı yeniden adlandır/overwrite et → kaynağı yeniden seç. Eksik dosyayla seçimde dosyanın kendiliğinden oluşmadığını kontrol et. Boş dosya ile seçimde okunur uyarı bekle. Menü ve seçim sayfasındaki yükleme/kapatma aynı backend'i kullanır. İki oturumla aynı kaynağı açıp kapatıldığında diğer oturumun yeniden kaynak seçmek zorunda kaldığını kontrol et.

Tenant config silme ve aktarım güvenliği: [Ayarlar ve bağlantı portları](settings.md). Silme yalnız config dosyasını kaldırır; CTB veya uzak veritabanını silmez.
