# Tenant'a göre Hibernate dialect seçimi

SweetCherry aynı çalışma sırasında SQLite CTB, MariaDB/MySQL ve PostgreSQL bağlantıları arasında geçebilir. `application.yml` içinde her geçiş için `spring.jpa.database-platform` değiştirmek veya uygulamayı yeniden başlatmak gerekmez.

## Varsayılan: otomatik

Bağlantı seçildiğinde JDBC metadata üzerinden veritabanı türü ve sürümü okunur. SQLite için community dialect, diğer sunucular için Hibernate'in dialect resolver'ı kullanılır. Veritabanı tanınmıyorsa seçim başarısız olur; önceki seçim korunur.

Tenant config dosyasına ekstra bir ayar yazmak gerekmez. İsterseniz açıkça belirtebilirsiniz:

```properties
custom.hibernateDialect=auto
```

Zorunlu bir açık seçim istiyorsanız desteklenen değerler `sqlite`, `mariadb`, `mysql`, `postgresql` veya bunlara karşılık gelen Hibernate dialect sınıf adlarıdır. Açık seçim gerçek bağlantının algılanan dialect'i ile eşleşmelidir; farklı motorun dialect'i zorlanamaz. Keyfi Java sınıfları yüklenmez. Sunucu sürümü açık seçimde de JDBC'den alınır.

Eski dış `application.yml` dosyasında `spring.jpa.database-platform` veya `spring.jpa.properties.hibernate.dialect` kalmış olabilir. Yeni tenant yönlendirmesi bunların yerine bağlantıya özgü seçimi kullanır; eski genel ayarları kaldırabilirsiniz. `ddl-auto=none` davranışı korunur. Bu çalışma uzak veritabanlarının şemasını oluşturmaz veya değiştirmez.

## Fabrikalar ve oturumlar

Dialect sınıfı ve sunucu sürümü aynı olan bağlantılar Hibernate fabrikasını paylaşır. MySQL/MariaDB için karakter setinin VARCHAR sınırı ve backslash SQL modu da paylaşım anahtarına dahildir. Her HTTP isteği seçili tenant'ın veri kaynağına bağlanır. Entity ve query second-level cache kapalı tutulur; farklı tenant'ların aynı node ID değerleri ortak önbellekten okunmaz. Fabrikalar ihtiyaç olduğunda hazırlanır ve uygulama kapanınca kapatılır. İlk kez farklı bir motor seçildiğinde kısa bir hazırlık süresi ve ek bellek kullanımı olabilir.

Tenant seçilmeden JPA repository/metamodel kurulumu için bağlantı açmayan SQLite başlangıç fabrikası bulunur. Bu, diğer tenant'ların SQLite dialect'i kullanacağı anlamına gelmez. Uzak sunucular kapalı olsa da uygulama başlayabilir.

Dialect metadata'sı bağlantı havuzunun yaşam döngüsü boyunca tutulur. Tenant kapatıldığında temizlenir; yeniden açıldığında tekrar okunur. Aynı veritabanına farklı ad ve ayarlarla bağlanma desteklenmeye devam eder.

## SQL uyumluluğu

- Çoğaltma kolonları SQLite `PRAGMA` yerine JDBC result metadata ile okunur.
- Gömülü nesnelerin `offset` kolon adı Hibernate/JDBC tarafından ilgili sürücüye göre alıntılanır. PostgreSQL'deki anahtar kelime ve MySQL/MariaDB'deki backtick farkları dikkate alınır.
- CTB nesne bayrakları (`has_image`, `has_table`, `has_codebox`) 0/1 integer değerleriyle eşlenir; PostgreSQL'e boolean parametresi gönderilmez. Uzak şema da senkronizasyondaki integer kolonları kullanmalıdır.
- Native sorgularda sayfalama öncesinde sorun oluşturabilecek sonda noktalı virgül kaldırılmıştır.
- SQLite şema yükseltmesi yalnız SQLite üzerinde çalışır. CTB export hedefi halen SQLite'dır; kaynak JDBC bağlantısı farklı bir motor olabilir.

`hibernate-community-dialects` bağımlılığı korunur. Sürümü Spring Boot dependency management tarafından, `hibernate-core` ile birlikte belirlenir; bunları ayrı ayrı yükseltmeyin.

## Doğrulama

Otomatik testler: metadata'dan motor/sürüm seçimi, yanlış açık seçimin reddi, Spring native EntityManager yönlendirmesi, farklı dialect fabrikaları, gerçek SQLite tenant'ları arasında işlem/sayfalama ve ayrım, uygulamanın tenant seçmeden açılması, çoğaltma ve rich-text nesne işlemleri.

Metadata/fabrika testleri gerçek MariaDB/PostgreSQL sunucusu yerine JDBC mock'ları kullanır. Canlı sunucu davranışı ayrıca kontrol edilmelidir:

1. Aynı uygulamada SQLite → MariaDB → PostgreSQL → SQLite seçin; YAML değiştirmeyin, yeniden başlatmayın.
2. Her bağlantıda doğru düğüm adlarını, shared hiyerarşisini ve nesneleri görüntüleyin; sayfalı liste ve aramayı deneyin.
3. Yedek/test veritabanında yazılabilir bağlantıyla özellik, plain text ve rich text düzenleme; nesne ekleme/taşıma; düğüm oluşturma, çoğaltma, taşıma ve silmeyi deneyin.
4. CTB/PDF/harita export'u doğrulayın. Bir tenant'taki işlemin diğer tenant'a yazmadığını kontrol edin.
5. Yanlış `custom.hibernateDialect` ile seçim başarısız olmalı ve önceki seçim korunmalıdır.
6. Sunucu erişilemiyorken uygulama açılmalı; o tenant seçildiğinde mevcut bağlantı hata mesajı gösterilmelidir.

SweetCherry otomatik veritabanı yedeği almaz. Uzak veritabanı yazma kontrollerini test kopyasında yapın.

## MySQL sürücüsüyle MariaDB bağlantısı

MySQL Connector/J ile MariaDB'ye bağlanıldığında JDBC metadata motoru MySQL veya sürümü `5.5.5-10.x-MariaDB` olarak gösterebilir. SweetCherry MariaDB işaretini gerçek sürüm metninden ayırır; işaret metadata içinde yoksa MySQL bağlantısında `SELECT VERSION()` ile kontrol eder. Uyumluluk öneki olan `5.5.5` gerçek MariaDB sürümü olarak kullanılmaz.

Bu kombinasyonda MySQL sürücüsünün `getSQLKeywords()` sorgusu MariaDB'de bulunmayan `information_schema.KEYWORDS.RESERVED` kolonunu isteyebilir. Yalnız MariaDB + MySQL sürücüsü için bu ek keyword listesi okunmaz; Hibernate'in yerleşik keyword bilgisi korunur. Gerçek MySQL, PostgreSQL ve MariaDB sürücülerindeki diğer metadata hataları gizlenmez. Mevcut tenant dosyasını değiştirmek zorunlu değildir.

Varsayılan YAML'de `spring.jpa.properties.hibernate.globally_quoted_identifiers=true` ve `globally_quoted_identifiers_skip_column_definitions=true` kullanılır. SQLite/PostgreSQL için çift tırnak, MySQL/MariaDB için backtick Hibernate tarafından seçilir. İkinci ayar `BLOB` gibi açık tip tanımlarının identifier sanılarak alıntılanmasını önler. Native SQL bu ayarlardan otomatik etkilenmez; nesne sorgularındaki JDBC quoting ayrıca korunur. Quoting ayarları yukarıdaki metadata sürücü hatasının çözümü değildir.
