# CTB açma ve veri kaynağı sihirbazı

**Veri kaynağı dosyası yükleme** sayfasında veya **Veri kaynakları** listesinde **CTB aç / Veri kaynağı oluştur** seçeneğini kullanın. Mevcut kaynak yanında kalem ikonu düzenleme formunu açar. Eski dosya upload ve elle config hazırlama seçenekleri korunur.

1. SQLite CTB seçin, **Gözat** ile SweetCherry'nin çalıştığı bilgisayardaki dosyaya gidin. Dosyanın yolunu doğrudan da yazabilirsiniz.
2. Bağlantı adı varsayılan olarak CTB dosya adıdır. Aynı CTB için farklı ad ve ayarlarla birden fazla kaynak oluşturabilirsiniz.
3. Yazma izni varsayılan kapalıdır. Yeni düğüm adı/etiketleri, gömülü yükleme limiti ve eski şema geçiş izni formdan düzenlenebilir.
4. **Kaydet ve aç** config dosyasını `allTenants/tenant-<benzersiz kimlik>.txt` olarak oluşturur. Mevcut kaynak düzenlemesinde kendi config dosyasını günceller. Sonrasında normal veri kaynağı seçim/şema kontrolü çalışır.

CTB upload edilmez, taşınmaz veya kopyalanmaz. Seçili konumla JDBC bağlantısı kurulur. Windows'ta ağ paylaşımı için klasör kutusuna `\\sunucu\notlar` yazın; Linux'ta paylaşım bağlanmış olmalıdır. SweetCherry'yi çalıştıran işletim sistemi kullanıcısının izinleri geçerlidir. Dosyalar sunucudadır: telefondan Raspberry Pi'ye bağlandıysanız telefonun dosyaları listelenmez. Klasör listesi en fazla 500 uygun kayıt gösterir; kalabalık klasörlerde tam alt klasör yolunu yazabilirsiniz.

Sihirbaz ve klasör listeleme uçları yalnız ADMIN içindir. Config kaydetme CSRF korumalı POST kullanır. Klasör gezintisi yalnız ad/yol listeler; dosya içeriklerini indiren genel bir endpoint eklenmez. Ancak ADMIN klasör adlarını görebilir; sunucu yönetici hesabını yalnız güvendiğiniz kişilere verin.

MySQL/MariaDB/PostgreSQL için tür, JDBC adresi ve kullanıcı bilgileri yazılabilir. Şifre mevcut formda gösterilmez; düzenlemede boş bırakmak mevcut şifreyi korur, kaldırma seçeneği siler. Diğer sürücüler için eski manuel config yöntemi devam eder. Config dosyasındaki bağlantı şifresi hâlâ açık metindir; dosyanın işletim sistemi izinlerini koruyun. `security-role` mevcut dosya alanıdır, uygulanmış bir kaynak erişim filtresi veya yeni login hesabı değildir.

Düzenleme bilinmeyen property anahtarlarını korur, fakat Properties biçiminde yeniden yazıldığı için yorumlar/satır düzeni korunmaz. Dosya başka yerde değiştirilmişse revision kontrolü eski formun üzerine yazmasını engeller. Seçili tenant dışında başka havuzlar kapatılmaz. Kullanımdaki havuz düzenlemesi reddedilir. Dosya kaydedilip bağlantı açılamazsa config kayıtlı kalır; normal açılış uyarısını izleyerek düzeltebilirsiniz.

SweetCherry yedek almaz. Şema geçişine veya içerik yazmaya izin vermeden önce CTB dosyanızın kopyasını kendiniz alın. Bu sihirbaz yeni boş CTB oluşturmaz. [Eski CTB şemaları](legacy-ctb-schema.md).
