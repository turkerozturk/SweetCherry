# Tutarlı CTB yedekleme

**ADMIN** hesabıyla **Sistem → CTB yedekleri** sayfasını açın. Dosya tabanlı SQLite CTB seçip **Seçili CTB’yi yedekle** düğmesine tıklayın ve onaylayın. Salt okunur tenant da yedeklenebilir. Bu özellik MariaDB, MySQL, PostgreSQL ve bellek içi SQLite veritabanları için değildir.

Sunucu, açık dosyayı sıradan dosya kopyasıyla almak yerine SQLite Online Backup API kullanır. WAL içinde bulunan kaydedilmiş veriler de dahil ana veritabanının tamamı alınır; kaynak düğüm ID’leri, tarihleri, hiyerarşi, paylaşımlı referanslar ve şema değiştirilmez. SweetCherry’nin tanımadığı tablolar da dahil SQLite tablolarının ve gömülü ikili verilerin tamamı yedeklenir. Commit edilmemiş işlemler ve editörde kaydedilmemiş metinler dahil değildir. Diğer programlardaki kaydedilmemiş değişiklikleri önce o programdan kaydedin.

Uygulamada aynı anda tek yedekleme çalışır; aynı CTB’ye farklı tenant adlarıyla bağlanmak bunu aşmaz. Onay anındaki bağlantı yakalanır: daha sonra başka veritabanı seçmek devam eden işi başka dosyaya yönlendirmez. İş bitene kadar bağlantı havuzdan ödünç alınmış kalır; mevcut havuz kapatma/yenileme koruması uygulanır. Tamamlanana kadar SweetCherry’yi açık tutun. Diğer bağlantılar kaynağı kullanabilir; yoğun eşzamanlı yazmalar yedeklemeyi geciktirebilir veya kilit hatasına neden olabilir. Kaynağın journal modu değiştirilmez, checkpoint zorlanmaz.

## Tamamlanma ve indirme

1. Arka planda özel bir geçici snapshot oluşturulur; SQLite sayfaları sınırlı büyüklükte gruplarla kopyalanır.
2. Snapshot, `PRAGMA integrity_check` kontrolünü geçmeli ve altı CTB tablosu bulunmalıdır: `node`, `children`, `bookmark`, `image`, `codebox`, `grid`.
3. Kayıt sayıları alınır, dosya akış halinde okunarak SHA-256 hesaplanır ve dosya tamamlanmadan önce diske yazımı zorlanır.
4. Yalnız tamamlanmış, katalog kaydıyla eşleşen snapshot’lar listelenir. Başarısız/yarım iş tamamlanmış yedek sayılmaz. Süreç çökerse `pending-*.partial` dosyaları kalabilir; bunlar indirme/açma için sunulmaz. SweetCherry kapalıyken ve çalışan iş yokken bu yarım dosyalar elle temizlenebilir.
5. `.ctb` dosyasını indirin. Ayrıntılarda doğrulama değeri ve kayıt sayıları görünür. İndirme ve çalışma kopyası oluşturmadan önce SHA-256 tekrar kontrol edilir. Liste, her ziyaretinizde tüm yedekleri baştan hashlemek yerine küçük katalog dosyalarını ve dosya boyutlarını okur.

Tamamlanan dosyalar `backup-<UTC tarih>-<UTC saat>-<UUID>.ctb` adındadır; yanında küçük bir `.json` katalog dosyası bulunur. JSON’da bağlantının görünen adı, tamamlanma zamanı, checksum, boyut ve kayıt sayıları vardır; **JDBC URL’si, kullanıcı adı veya şifre yoktur**. Uygulamadaki katalog için ikisini birlikte saklayın. CTB, CherryTree ile bağımsız olarak açılabilir. Sayfa tamamlanma zamanını sunucunun yerel saat diliminde gösterir. Durum takibi için kaynağın seçili kalması gerekmez; aktif tenant yokken de indirme ve kurtarma yapılabilir.

CTB’nin ve BLOB’ların tamamı Java heap belleğine alınmaz. Yine de bütünlük kontrolü ve hash hesaplama Raspberry Pi’de zaman ve disk okuması gerektirir. Durum otomatik yenilenir. Oturum/ağ bağlantısı kaybolursa tekrar giriş yapıp bu sayfaya dönün; sunucudaki iş bu yüzden iptal olmaz.

## Güvenli inceleme ve geri dönüş

**Ayrı çalışma kopyası aç** düğmesine tıklayıp onaylayın. SweetCherry checksum’u doğrular, kapalı yedeği `working-copies/` altında yeni dosyaya kopyalar, kopyayı doğrular ve mevcut veritabanı sihirbazını açar. Varsayılan **salt okunur**dur; şema güncelleme izni kapalıdır. Adını isterseniz değiştirip sihirbazı onaylayarak kopyayı tanıtın ve yükleyin.

Asıl CTB’nin ve saklanan yedeğin üzerine yazılmaz. Çalışma kopyasını sonradan yazılabilir yaparsanız yalnız o kopya değişir. Sihirbazı iptal ederseniz kopya elle temizlenmek üzere kalır. Kurtarılan içerikle devam etmek için bu ayrı veritabanını seçili tutabilirsiniz. İlk sürüm açık veritabanı dosyasını değiştirmez. Böyle bir değiştirme, aynı dosyaya bağlı tüm tenant havuzlarının kapatılmasını ve CherryTree dahil dış programların da dosyayı bırakmasını gerektirir.

## Ayarlar

JAR yanındaki harici `application.yml` dosyasında mevcut `myapp` bölümüne ekleyin/düzenleyin:

```yaml
myapp:
  ctb-backup:
    enabled: true
    directory: backups/ctb
```

Değişiklikten sonra yeniden başlatın. Bu anahtarlar yoksa da aynı varsayılanlar geçerlidir. Klasör **uygulamanın çalışma klasörüne** göre çözülür; mutlak yol da yazılabilir. `enabled: false` yeni yedek oluşturmayı kapatır; mevcut yedeklerin indirilmesi ve çalışma kopyasıyla kurtarma açık kalır. Klasör yalnız gerektiğinde oluşturulur. SweetCherry’yi çalıştıran hesabın izinleri ve yeterli boş alanı olmalıdır. Çalışma kopyası için yedek boyutu kadar ilave alan gerekir. Desteklenen sistemlerde yeni dosya/klasörler özel POSIX izinleriyle oluşturulur; mevcut klasörleri ve Windows/ağ paylaşımı ACL izinlerini ayrıca koruyun. Git deposu içindeki özel yedek klasörünü de Git’ten hariç tutun; varsayılan `/backups/` zaten hariçtir.

İlk adımda otomatik zamanlama, saklama sayısına göre silme, yedek silme düğmesi veya düğüm sürüm geçmişi yoktur. Uygulama YAML dosyaları, giriş bilgileri, tenant config dosyaları ve notlardan dışarıya linklenmiş dosyalar bu CTB yedeğine dahil değildir. Yedekler notları ve ekleri içerdiğinden klasörü özel tutun. Asıl dosyayla aynı diskteki yedek, o diskin bozulmasına karşı koruma sağlamaz; indirilen kopyaları farklı cihaz/diskte de saklayın.

## Manuel kontrol

* Yazılabilir ve salt okunur CTB’leri yedekleyin. İndirilen dosyayı CherryTree’de açıp metinleri, resim/ekleri, tabloları, kod kutularını, yer işaretlerini ve paylaşımlı hiyerarşiyi kontrol edin. Kaynak tarihleri değişmemeli.
* WAL kullanan açık kaynakta değişiklik kaydedip yedek alın. Son kaydedilmiş içerik bulunmalı; editördeki kaydedilmemiş metin bulunmamalı.
* Yedekleme sürerken tenant değiştirin. Yedek, ilk seçilen kaynağın adı ve içeriğiyle tamamlanmalı. Aynı dosyanın başka alias’ı dahil ikinci iş reddedilmeli.
* Çalışma kopyası açıp sihirbazı onaylayın. Yazılabilir yaparak düzenlerseniz asıl dosya ve saklanan yedek değişmemeli. Aktif CTB’yi kapattıktan ve uygulamayı yeniden başlattıktan sonra katalog/indirmeyi deneyin.
* Geçersiz ID, sonradan değiştirilmiş yedek, kapalı oluşturma ayarı, az disk alanı/yazılamayan klasör, yarım iş ve ADMIN olmayan hesap senaryolarını deneyin. Geçersiz/eksik dosya geçerli kurtarma kaynağı olarak sunulmamalı.

Kaynaklar: [SQLite Online Backup API](https://www.sqlite.org/backup.html), [SQLite backup eşzamanlılığı](https://www.sqlite.org/c3ref/backup_finish.html), [açık dosyayı kopyalamanın riskleri](https://www.sqlite.org/howtocorrupt.html#_backup_or_restore_while_a_transaction_is_active).
