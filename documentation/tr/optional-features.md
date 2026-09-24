# SweetCherry isteğe bağlı özellikler

Bu belge normal kullanıcı çalışmasında kapalı tutulan fakat geliştirme, inceleme veya ilerideki entegrasyonlar için projede korunan özellikleri hatırlatır.

## Operations profili: Actuator, Swagger ve kapatma

Normal çalışmada yalnızca aşağıdaki Actuator uçları sunulur:

- `/actuator/health`: uygulamanın ayakta olup olmadığı
- `/actuator/info`: SweetCherry adı ve proje sürümü

Swagger, ayrıntılı Actuator uçları ve kapatma uçları normal çalışmada kapalıdır. Açmak için:

```bash
cd release/SweetCherry
java -jar SweetCherry.jar \
  --server.port=8443 \
  --server.http.port=8080 \
  --spring.profiles.active=operations
```

Windows tek satır örneği:

```bat
release\SweetCherry\run.bat --spring.profiles.active=operations
```

Operations profilinde:

- `/swagger-ui/index.html`
- `/v3/api-docs`
- `/actuator/beans`
- `/actuator/mappings`
- `/actuator/scheduledtasks`
- `/actuator/shutdown`
- diğer Actuator uçları

etkinleşir. Ayrıntılı Actuator ve Swagger adresleri uygulamanın `admin` rolüyle korunur. `/actuator/shutdown` bir `POST` isteğidir ve CSRF koruması devam eder. Güvenliği düşürmemek için anonim veya sıradan bir GET bağlantısıyla kapatma açılmamıştır.

Eski `/dashboardold` sayfası geliştirme ve hatırlatma amacıyla projede tutulmaktadır. Operations profili kapalıyken health/info görünür; gelişmiş bağlantılar gizlenir.

`/shutdownContext` deneme controller'ı da yalnızca operations profilinde bean olarak oluşturulur, `POST` kullanır, CSRF korumasını sürdürür ve ayrıca `ADMIN` rolü ister.

## Experiments profili: scheduled task denemeleri

`ScheduledTasks` ve scheduling yapılandırması normal çalışmada oluşturulmaz. Denemeleri yeniden açmak için:

```bat
release\SweetCherry\run.bat --spring.profiles.active=experiments
```

Operations ile birlikte:

```bat
release\SweetCherry\run.bat --spring.profiles.active=operations,experiments
```

Mevcut scheduled task'lar örnek amaçlıdır: biri her dakika log yazar, diğeri günde bir kez e-posta deneme mesajını loglar. Gerçek e-posta gönderme satırı halen yorumdadır.

## Uzak veritabanı sürücüleri

CherryTree CTB dosyası SQLite kullanır. Bununla birlikte SweetCherry'nin veritabanları arasında export/kopyalama denemelerinde kullandığı MySQL ve PostgreSQL JDBC sürücüleri varsayılan JAR içinde korunmaktadır. Bu sürücüler tek başına bağlantı oluşturmaz; ilgili veri kaynağı ayarları yine ayrıca gerekir.

## Diğer Maven profilleri

```bat
mvnw.cmd -Plicenses -DskipTests verify
mvnw.cmd -Preports compile
mvnw.cmd -Pdocumentation generate-resources
```

- `licenses`: bağımlılık lisanslarını indirir.
- `reports`: `target/dependency-list.txt` üretir.
- `documentation`: eski ve gelecekteki Asciidoctor belgelerini HTML'e dönüştürür.
