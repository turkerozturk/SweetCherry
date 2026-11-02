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

Otomatik zamanlama ve saklama kuralı isteğe bağlıdır (aşağıya bakın). Elle yedek yönetimi aşağıda açıklanır. Düğüm sürüm geçmişi dahil değildir. Uygulama YAML dosyaları, giriş bilgileri, tenant config dosyaları ve notlardan dışarıya linklenmiş dosyalar bu CTB yedeğine dahil değildir. Yedekler notları ve ekleri içerdiğinden klasörü özel tutun. Asıl dosyayla aynı diskteki yedek, o diskin bozulmasına karşı koruma sağlamaz; indirilen kopyaları farklı cihaz/diskte de saklayın.

## Manuel kontrol

* Yazılabilir ve salt okunur CTB’leri yedekleyin. İndirilen dosyayı CherryTree’de açıp metinleri, resim/ekleri, tabloları, kod kutularını, yer işaretlerini ve paylaşımlı hiyerarşiyi kontrol edin. Kaynak tarihleri değişmemeli.
* WAL kullanan açık kaynakta değişiklik kaydedip yedek alın. Son kaydedilmiş içerik bulunmalı; editördeki kaydedilmemiş metin bulunmamalı.
* Yedekleme sürerken tenant değiştirin. Yedek, ilk seçilen kaynağın adı ve içeriğiyle tamamlanmalı. Aynı dosyanın başka alias’ı dahil ikinci iş reddedilmeli.
* Çalışma kopyası açıp sihirbazı onaylayın. Yazılabilir yaparak düzenlerseniz asıl dosya ve saklanan yedek değişmemeli. Aktif CTB’yi kapattıktan ve uygulamayı yeniden başlattıktan sonra katalog/indirmeyi deneyin.
* Geçersiz ID, sonradan değiştirilmiş yedek, kapalı oluşturma ayarı, az disk alanı/yazılamayan klasör, yarım iş ve ADMIN olmayan hesap senaryolarını deneyin. Geçersiz/eksik dosya geçerli kurtarma kaynağı olarak sunulmamalı.

Kaynaklar: [SQLite Online Backup API](https://www.sqlite.org/backup.html), [SQLite backup eşzamanlılığı](https://www.sqlite.org/c3ref/backup_finish.html), [açık dosyayı kopyalamanın riskleri](https://www.sqlite.org/howtocorrupt.html#_backup_or_restore_while_a_transaction_is_active).


## İsteğe bağlı otomatik yedekleme ve saklama

Otomatik yedekleme **varsayılan olarak kapalıdır**. Mevcut `myapp.ctb-backup` bölümüne ekleyin:

```yaml
myapp:
  ctb-backup:
    enabled: true
    directory: backups/ctb
    automatic:
      enabled: true
      interval-minutes: 1440
      retry-minutes: 5
      keep-last: 7
      tenants:
        - "Notlarım"
        - "Diğer CTB"
```

Dosya yolu değil, kayıtlı tenant bağlantılarının tam görünen adlarını kullanın. Varsayılan liste boştur; yalnız ayarı açmak tüm veritabanlarını otomatik yedeklemeye almaz. `keep-last` varsayılanı **0 (sınırsız)**dır; örnek, fiziksel kaynak başına yedi otomatik yedek saklamayı açıkça seçer. Aralık/tekrar sayıları pozitif, saklama sayısı sıfır veya pozitif olmalıdır. YAML değişikliğinden sonra yeniden başlatın. Deneysel `myapp.scheduling.enabled` ayarını açmak gerekmez.

İlk kontrol SweetCherry hazır olduktan bir dakika sonra, devamında dakikada birdir. Tarayıcıda oturum açılması veya veritabanının seçili olması gerekmez. Kaynaklar sırayla, kontrol başına en fazla bir yeni iş başlayacak şekilde işlenir. Elle başlatılmış/devam eden yedeğin bitmesi beklenir. Uygulama kapalıyken kaçırılan yedekler kuyrukta birikmez; yeniden açılınca zamanı geçmiş kaynak yedeklenir. Başarısız kaynak `retry-minutes` kadar bekler; bu sırada diğer zamanı gelmiş kaynaklar ilerleyebilir.

Aynı CTB’ye bağlı alias’lar, SQLite URI yolları ve sembolik linkler birleştirilir. Kaynak kimliği gerçek dosya konumundan üretilir; katalogda yalnız hash’i tutulur. Tenant adının değişmesi geçmişi bölmez. CTB dosyasını taşımak/adını değiştirmek güvenli tarafta kalmak için yeni kaynak sayılır. Ayrı, salt okunur SQLite bağlantısı kullanılır: kapalı tenant havuzu açılmaz, eksik dosya oluşturulmaz, oturum değiştirilmez, şema güncellenmez. Bulunamayan adlar, desteklenmeyen sunucu veritabanları ve erişilemeyen yollar yedekler sayfasında gösterilir. Plan durumunu güncel görmek için Yenile’ye basın.

Aralık, o kaynağın son başarılı kimlikli yedeğinden başlar; yeni elle alınmış yedek de buna dahildir. Tamamlanma zamanı katalogda saklandığından yeniden başlatmada hatırlanır. Kaynak kimliği olmayan eski katalog kayıtları okunur ve korunur; fiziksel kaynakları güvenle çıkarılamadığı için ilk otomatik işi ertelemezler.

Saklama kuralı **yalnız yeni otomatik snapshot tamamlanıp doğrulandıktan sonra**, aynı kaynağın otomatik yedeklerine uygulanır. Yeni yedek, elle alınmış yedekler, eski katalog kayıtları, başka kaynakların yedekleri ve `working-copies/` her zaman korunur. Tenant olarak tanıtılmış, SQLite WAL/SHM/journal yan dosyası bulunan veya checksum’u değişmiş dosyalar atlanır. Düzenlemek için daima ayrı çalışma kopyası açın; dış programlar SweetCherry’nin koordinasyonuna katılmaz. Temizlik hatası uyarı sayılır; yeni tamamlanmış yedeği başarısız yapmaz. `keep-last: 0` ise otomatik silme yapılmaz. Saklama kuralını açmak/sayısını değiştirmek, başlangıçta hemen silme başlatmaz; sonraki başarılı otomatik yedek beklenir. Yarım temizlemeden kalan emekliye ayrılmış katalog dosyaları listede gösterilmez. Yedek klasörünü dışarıdan değişikliğe karşı koruyun; checksum dijital imza değildir.

### Ek manuel kontroller

* Aynı CTB’ye bakan iki bağlantı adını ve farklı CTB’ye bakan bir bağlantıyı seçin. Alias’ların tek otomatik kaynak olarak görünmesini, işlerin sırayla çalışmasını ve tarayıcının açık kalmasının gerekmemesini doğrulayın.
* Aralık dolmadan SweetCherry’yi yeniden başlatın: son başarılı kimlikli yedek sonraki işi ertelemeli. Eksik kaynak deneyin; tekrar beklemesi sırasında diğer zamanı gelen kaynak ilerlemeli.
* `keep-last: 2` ile üç başarılı otomatik yedek alın. Yalnız o kaynağın son iki otomatik yedeği kalmalı. Elle alınmış yedek ve çalışma kopyası korunmalı. Sıfırda hepsi kalmalı.
* Yeni işi başarısız duruma getirin: eski yedek silinmemeli. Eski otomatik dosyayı değiştirin veya tenant olarak tanıtın: temizlik atlayıp uyarmalı. Türkçe/İngilizce plan durumunu ve dar mobil görünümü de kontrol edin.


## Eski yedekleri gözden geçirme ve silme

ADMIN hesabıyla tek satırdaki **Sil…** düğmesini veya birden çok satırı seçip **Seçili yedekleri gözden geçir** düğmesini kullanın. Onay başına en fazla 200 kayıt seçilebilir. İlk POST yalnız onay sayfasını hazırlar: tam dosyalar, tarihler, kaynak adları, silinebilir sayı/boyut ve koruma nedenleri gösterilir. Önizlemeyi açmak veya iptal etmek yedek silmez.

Onay, yalnız gözden geçirilmiş CTB ve yanındaki JSON katalog kaydını kalıcı siler. Oturuma bağlı, on dakika geçerli, tek kullanımlık bir anahtar kullanılır; aynı oturumdaki başka önizleme eskisinin yerini alır. Önizlemeden sonra oluşturulan dosya seçime eklenmez. Çift gönderim, başka oturum veya süresi dolmuş plan silmeyi tekrarlayamaz. Her dosya tekrar kontrol edilir; bulunamayan dosya, değişmiş katalog veya checksum uyuşmazlığı atlanır. Büyük checksum kontrolleri zaman alabilir. Sonuçta silinen/atlanan sayıları ve dosya bazında nedenler görünür; kısmi başarı açıktır.

Elle silme, elle alınmış, otomatik ve eski yedekleri kapsayabilir. Yine de kayıtlı tenant dosyaları (tenant kapalı olsa bile), çalışan işin dosyası ve SQLite WAL/SHM/journal yan dosyaları bulunanlar korunur. Kayıtlı dosya koruması checksum öncesi ve sonrasında yenilenir. Asıl veritabanları, çalışma kopyaları, yarım dosyalar ve keyfi dosya adları silme hedefi değildir. Zorla silme seçeneği yoktur. Geçersiz/bozuk katalog kayıtları listelenmez; yöneticinin dosya sisteminde bilinçli incelemesini gerektirir. Dış programlar SweetCherry'nin koordinasyonuna katılmaz; düzenleme için ayrı çalışma kopyası açın.

Otomatik yedekleme açıkken son kimlikli yedeği silerseniz kaynak sonraki kontrolde yeniden zamanı gelmiş sayılabilir. Gereken dosyaları kalıcı silmeden önce farklı disk/cihaza indirin. `open-latest-modified` tercihi bu yedek işlemlerini etkilemez.

### Yönetim kontrolleri

* Tek dosyayı önizleyip iptal edin: dosyalar ve checksum değişmemeli. Onaylayın: yalnız o CTB/katalog çifti kalkmalı.
* Birden çok elle alınmış/otomatik/eski yedeği gözden geçirin. Onaydan önce yeni yedek oluşturun: yeni dosya korunmalı. Çift gönderim, on dakikalık süre ve başka tarayıcı oturumunu deneyin.
* Boyutunu değiştirmeden dosyayı değiştirin, kataloğunu değiştirin, tenant olarak tanıtın veya journal yan dosyası oluşturun. Neden belirtilerek atlanmalı. Diğer geçerli seçim silinebilmeli.
* Türkçe/İngilizce onayı, mobil seçimi, aktif tenant olmamasını, ADMIN/USER yetkilerini ve CSRF reddini kontrol edin.
