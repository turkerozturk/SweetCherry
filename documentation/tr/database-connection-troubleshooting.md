# MariaDB / PostgreSQL bağlantı ve SQL sorunları

## MariaDB düğüm oluşturma

Kardeş ve alt düğüm oluşturma aynı servisi kullanır. ID sorgusundaki UNION alt sorgusu `AS occurrence_ids` ile adlandırılmalıdır; MariaDB bu alias olmadan sorguyu reddeder. ID hâlâ node ve children tablolarındaki en büyük ID'nin bir üstüdür; shared-only ID'ler de dikkate alınır.

## PostgreSQL: yerel ve laptop bağlantısı

Pi'de `ss` yalnız `127.0.0.1:5432` ve `[::1]:5432` gösteriyorsa LAN bağlantısı çalışmaz. HBA satırı eklemek dinleme adresini açmaz.

Gerçek dosyaları bulun:

```bash
sudo -u postgres psql -c 'SHOW config_file;'
sudo -u postgres psql -c 'SHOW hba_file;'
sudo -u postgres psql -c 'SHOW listen_addresses;'
```

Gösterilen postgresql.conf içinde mevcut listen_addresses satırını düzenleyin:

```conf
listen_addresses = 'localhost,192.168.1.114'
```

PostgreSQL 17/main kümesi örneğinde yeniden başlatın (aktif bağlantılar kesilir):

```bash
sudo pg_ctlcluster 17 main restart
sudo ss -ltnp | grep ':5432'
pg_isready -h 192.168.1.114 -p 5432
```

HBA'da mevcut yerel yönetim kurallarını koruyun. Laptopun gerçek IP'si 192.168.1.21 ise şu kural yeterlidir:

```conf
host sweetcherry ct_sync 192.168.1.21/32 scram-sha-256
```

Mevcut `host all all 127.0.0.1/32 scram-sha-256` satırı ct_sync için loopback erişimini de kapsar. HBA ilk eşleşen kuralla çalışır. Hostname alanı sunucu adını değil istemciyi eşleştirir. Tarayıcının SweetCherry'ye internetten gelmesi PostgreSQL'e doğrudan bağlantı anlamına gelmez; PostgreSQL SweetCherry sunucusunun JDBC bağlantısını görür. Sırf web erişimi için 5432 modem yönlendirmesi veya geniş HBA kuralı eklemeyin.

HBA düzenlendiyse:

```bash
sudo -u postgres psql -c 'SELECT line_number,error FROM pg_hba_file_rules WHERE error IS NOT NULL;'
sudo -u postgres psql -c 'SELECT pg_reload_conf();'
```

CONNECT yetkisi ayrı bir konudur. Var olan ct_sync rolüne, sweetcherry veritabanının public şemasındaki mevcut tabloları düzenleme yetkisi vermek için:

```bash
sudo -u postgres psql -d sweetcherry
```

```sql
GRANT CONNECT ON DATABASE sweetcherry TO ct_sync;
GRANT USAGE ON SCHEMA public TO ct_sync;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO ct_sync;
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA public TO ct_sync;
\q
```

ct_local kullanmaya devam edilecekse gereken aynı yetkiler bu role ayrıca verilir. Rol zaten bulunmalıdır. Bu izinler şema oluşturma/superuser yetkisi veya gelecekte oluşturulacak tüm tablolara otomatik izin vermez.

Pi üzerinde JDBC host `127.0.0.1`, laptopta `192.168.1.114` olmalıdır. LAN testinde Windows PowerShell:

```powershell
Test-NetConnection 192.168.1.114 -Port 5432
```

## Properties kaçışları

`Properties.store` dosyada `jdbc\:postgresql\://...` ve `sslmode\=prefer` yazabilir. `Properties.load` bunları `jdbc:postgresql://...` ve `sslmode=prefer` olarak okur. Bu normaldir; sihirbaz bu yüzden URL'yi bozmuş sayılmaz.

## MariaDB 1020: kayıt okunduktan sonra değişmiş

Bu hata yeni düğüm SQL alias probleminden ayrıdır. Tek log, kesin nedenini belirlemez. Senkronizasyon aynı hedefi değiştiriyorsa veya başka istemci yazıyorsa çakışma olabilir; sunucu sürümü/izolasyon davranışı da incelenmelidir. Koşullu güncelleme ve revision denetimini kaldırarak ya da otomatik yeniden yazarak hatayı gizlemeyin.

Sorunu ayırmak için Sync scheduler'ı durdurun, tek editör açın, düğümü yeniden yükleyip kaydetmeyi deneyin. Tekrarlanırsa MariaDB'de şu salt okunur çıktıları kaydedin:

```sql
SELECT VERSION();
SHOW VARIABLES LIKE 'innodb_snapshot_isolation';
SHOW TABLE STATUS LIKE 'node';
```

Diğer notlar/parolalar içeren geniş log yerine ilgili hata, işlem zamanı ve eşzamanlı Sync durumu paylaşılmalıdır. Bu aşamada 1020 için izolasyon ayarı değiştirilmiyor.

## children sayfasındaki 404

HTML `background="black"` renk değil resim adresidir; tarayıcı black adlı kaynağı isteyebilir. Gereksiz özellik kaldırılmıştır. 404 devam ederse tarayıcı Developer Tools → Network'te 404 dönen isteğin tam yolunu belirleyin. Ana sayfa başarılıyken alt kaynak 404 verebilir; bu veritabanı sorgu hatası değildir.

## Resmî kaynaklar

- https://www.postgresql.org/docs/17/runtime-config-connection.html
- https://www.postgresql.org/docs/17/auth-pg-hba-conf.html
- https://www.postgresql.org/docs/17/sql-grant.html
- https://mariadb.com/docs/server/reference/error-codes/mariadb-error-codes-1000-to-1099/e1020

## Aktif tenant health kontrolü

Actuator'ın genel routing DataSource kontrolü kapalı tenant kayıtlarına da bağlanmaya çalışabilir. SweetCherry'nin `dbHealthIndicator` kontrolü yalnız başlatılmış ve kapatılmamış tenant havuzlarını kontrol eder. Kapalı/henuz açılmamış kayıtlar bağlantı açılmadan atlanır; tenant seçimi değiştirilmez. Çalışan bir havuzda bağlantı doğrulaması başarısızsa DOWN korunur.

Hiç açık havuz olmaması uygulamanın hatalı çalıştığı anlamına gelmez; bu durumda DB bileşeni UP döner. Bu sonuç tüm tenant config dosyalarının bağlantılarının başarılı olduğu anlamına gelmez. Health detayları mevcut Actuator erişim ayarlarına bağlıdır; bu değişiklik ayrıntıları herkese açmaz.

## Kısa bağlantı hataları ve güvenli ekran açıklaması

Tenant açılışındaki bağlantı hataları loga tek satır kategorisiyle yazılır; JDBC stack trace yerine network/credentials/connect/database/unknown ayrımı kullanılır. Önceki seçili tenant korunur. Seçim ekranında Türkçe/İngilizce açıklama gösterilir. Veritabanından gelen ham mesaj SQL, parola veya bağlantı URL'si içerebileceği için doğrudan HTML/loga taşınmaz. Sınıflandırılamayan hata genel açıklamayla bildirilir. Bu düzenleme tüm uygulama hatalarını susturmaz; veri kaynağı seçimiyle sınırlıdır.

## PostgreSQL nesne silme bayrakları

`has_image`, `has_table` ve `has_codebox` integer sütunlarıdır. Nesne silme sonrasında `CASE WHEN EXISTS(...) THEN 1 ELSE 0 END` yazılır; PostgreSQL boolean sonucu integer sütuna doğrudan yazılmaz. Bu sorgu SQLite ve MariaDB ile de uyumludur. Şema ve nesne silme onay davranışı değiştirilmez.
