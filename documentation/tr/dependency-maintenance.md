# Maven bağımlılık bakımı ve release doğrulaması

İnceleme tarihi: 2026-10-05. Başlangıç commit'i: `dded6a0`.

Bu belge doğrudan `pom.xml` bağımlılıklarının, ilgili resmi sürüm/güvenlik duyurularının ve bazı transitif sürüm ilişkilerinin ilk incelemesini kaydeder. **Tam bir CVE taraması veya güvenlik sertifikasyonu değildir.** Eski sürüm kullanmak tek başına bir açığın uygulamada tetiklenebildiğini göstermez. Maven'ın çözdüğü transitif ağacın ve dağıtılan JAR'ın ayrıca incelenmesi gerekir. Buradaki yeni sürümler, yayın kabul testleri tamamlanana kadar adaydır.

## Sürümleme ve POM bilgileri

- Uygulama sürümü `0.5.0-SNAPSHOT`; Spring Boot parent sürümü `3.5.16`; Java hedefi ve Windows paketinin runtime'ı Java 17 olarak korunur.
- Yayın sürümüne geçiş [versioning.md](versioning.md) akışına göre yapılır.
- POM'a Git deposu (`scm`), GitHub Issues ve GitHub Actions adresleri eklendi. `scm.tag=HEAD` geliştirme içindir; yayımlanan commit Git etiketiyle ayrıca sabitlenir.
- `packaging` belirtilmediğinde Maven JAR kullanır. GitHub Release'e dosya yüklemek için Maven Central yayın ayarları veya `distributionManagement` eklemek gerekmez.
- Mevcut açıklama yorumları korunmuştur; bu yamada yorum silinmediği için yorum arşivine yeni kayıt gerekmez.

## İlk güncelleme grubu

| Bileşen | Önce | Aday sürüm | Gerekçe ve kontrol |
|---|---|---|---|
| Commons Lang | 3.14.0 | 3.21.0 | 3.18.0'da `ClassUtils.getClass` için aşırı uzun girdide recursion/StackOverflow düzeltmesi var (CVE-2025-48924 ile ilgili). Daha güncel 3.x bakım sürümü seçildi. Kullanılan EnumUtils ve HTML escaping yolları test edilmeli. |
| Commons IO | 2.15.1 | 2.21.0 | POI 5.5.1'in ilan ettiği sürümle eşleştirildi. CVE-2024-47554 için resmi etkilenen aralık 2.14.0 öncesidir; eski 2.15.1'i bu açıkla ilişkilendirmiyoruz. |
| Commons Codec | Boot yönetimi: 1.18.0 | 1.20.0 | POI 5.5.1'in ilan ettiği sürüm. Boot property override ile transitif eski sürüm seçilmesi önlenir. |
| POI ve POI OOXML | 5.2.5 | 5.5.1 | İki modül aynı property kullanır. OOXML okurken yinelenen ZIP entry adlarıyla ilgili CVE-2025-31672, 5.4.0 öncesini etkiler. Mevcut XLSX yolu üretim içindir; bu bilgi uygulamanın açığa karşı sömürülebilir olduğunu kanıtlamaz. |
| PostgreSQL JDBC | 42.7.3 | 42.7.12 | Bakım güncellemesi. `channelBinding=require` için CVE-2026-54291 düzeltmesi 42.7.12'de. 42.7.3 bu duyurudaki etkilenen 42.7.4–42.7.11 aralığında değildir. Boot 3.5.16'nın 42.7.11 sürümüne körlemesine dönülmez. |

Maven Central'da aday sürümlerin POM dosyalarının mevcut olduğu kontrol edildi. Bu, uygulama uyumluluk testinin yerine geçmez. POI POM'ları Commons IO 2.21.0 ve Commons Codec 1.20.0 kullanır; doğrudan bağımlılıklar ve Boot dependency management bu gereksinimleri eski sürümlere çekmemelidir.

## Diğer doğrudan bağımlılıklar ve sonraki gruplar

Aşağıdaki “korundu” ifadesi güvenlik onayı değil, bu yamada sürümün değiştirilmediği anlamına gelir. Önce ilk grubun kabul testleri tamamlanır; sonraki gruplar ayrı yamalarda ele alınır.

| Grup | Mevcut sürümler | Sonraki işlem |
|---|---|---|
| Spring Boot ve yönetilen modüller | Parent 3.5.16; web, JPA, security, validation, actuator, test, cache, mail, websocket, devtools | Spring/Hibernate sürümlerini ayrı ayrı yükseltmeyin. Parent/BOM güncellemesi ayrı test grubu; güvenlik duyuruları ve destek durumu yayın günü tekrar kontrol edilir. |
| SQLite JDBC | 3.44.1.0 | Native sürücü güncellemesi ayrı grup. Olmayan CTB'nin oluşturulmaması, pool kapatma, dosya kilidinin bırakılması, rich-text nesne kayıtları ve transaction rollback Windows/Raspberry Pi'de test edilmeli. |
| MySQL JDBC | 9.0.0 | Boot yönetimindeki sürümle uyumluluk ve MySQL/MariaDB kullanım kapsamı ayrıca incelenir. Gerçek sunucu bağlantısı test edilmeden “uyumlu” sayılmaz. |
| PDFBox | 3.0.8 (önce 3.0.1) | İkinci grupta 3.0.8 seçildi. Güvenlik sayfasındaki 2026 path-traversal duyuruları `examples` modülünü ilgilendirir; core kullanımımız otomatik olarak bu açık sayılmaz. Font/görsel/PDF çıktısı ayrı test grubu. |
| OpenPDF ve extra fonts | 2.0.2 / 2.0.2 | Birlikte tutulur. 2.0.x Java 17; 2.1.x ve sonrası Java 21 ister. Yeni ana sürümler paket adı değişikliği de içerir. Java 17 dağıtımı korunurken doğrudan en yeni ana sürüme geçilmez. |
| Flying Saucer PDF | 9.7.1 | OpenPDF ile çözülen transitif sürüm ve kullanılan HTML→PDF API'leri birlikte incelenir. |
| jsoup | 1.17.2 | Resmi 1.23.2 adayı mevcut. XML/HTML serileştirme davranışı rich-text okuma/önizleme/rendering çıktılarını etkileyebilir; node 53 ve boş/alias/plain-text senaryoları karşılaştırılır. |
| Thymeleaf layout | 3.3.0 | Yerleşim ve fragment davranışlarıyla ayrı kontrol. |
| Thymeleaf security extras | 3.1.1.RELEASE | Eski yorumda geçici bug override'ı var. Boot BOM'da 3.1.5.RELEASE görülüyor; admin/user görünürlük ve sunucu yetki testleriyle ayrı güncelleme adayı. Eski yorum ancak neden çözüldüğü doğrulanınca arşivlenerek değiştirilir. |
| WebJars | locator 0.52; Bootstrap 5.3.3; bootstrap-select 1.13.18; Popper 2.11.7; jQuery 3.7.1; jQuery UI 1.13.2; Font Awesome 6.5.2; htmx 1.9.12; hyperscript 2.0.2 | Kullanılan template/API yollarını belirleyip güncelleyin; eski dashboard/help yolları da kontrol edilir. Büyük sürüm geçişleri otomatik yapılmaz. |
| Astronomi ve zaman | commons-suncalc 3.10; time4j-base/sqlxml 5.9.1; time4j-tzdata 5.0-2022a | Paketli zaman dilimi verisi eski. Gerçekte kullanılan provider ve Java runtime tzdata ilişkisi incelenmeden yalnız artifact adına göre güncelleme yapılmaz. Kutup yazı ve timezone testleri korunur. |
| OpenAPI | springdoc 2.8.13 | Spring Boot 3.x uyumluluğu ve endpoint erişim politikasıyla kontrol edilir. |

POM dışındaki `src/main/resources/static` altında bulunan JavaScript/CSS kopyaları WebJar yükseltilince kendiliğinden güncellenmez. Ayrı envanter ve lisans kontrolü gerekir.

## Build eklentileri

Mevcut sürümler: kaynak kopyalama 3.3.1, git-commit-id 8.0.2, antrun 3.2.0, license 2.4.0, dependency reports 3.9.0, Asciidoctor 3.0.0, Launch4j 2.7.0. Spring Boot Maven plugin sürümünü parent belirler.

Uygulama paketleme eklentileri korunur. İsteğe bağlı rapor eklentisi `maven-dependency-plugin` 3.2.0 → 3.9.0 olarak güncellenmiştir. 3.9.0 Java 8 ve Maven 3.6.3 veya üzerini ister; Java 17 hedefimizle ve Maven 3.9.x wrapper ile uyumludur. Launch4j'in `windows-launcher` profili varsayılan build'de çalışmaz; Windows dağıtım workflow'u gerçek EXE üretimini doğrular. IntelliJ'de “plugin not found” görülürse Maven projelerini yeniden yükleyin; offline ayarını, Maven settings/proxy/mirror ve yerel çözümleme kayıtlarını kontrol edin. Normal `clean package` başarısı isteğe bağlı profilin çalıştırıldığını göstermez.

`xmlns:unless="ant:unless"` ve `unless:set` geçerli Ant kullanımıdır. Namespace, indirilebilir XSD adresi değildir. IntelliJ'nin şema kaydı uyarısı sebebiyle bu koşulları kaldırmayın: mevcut demo CTB ve tenant config dosyalarını build sırasında korurlar.

## Otomatik ve manuel doğrulama

Windows CMD:

```bat
mvnw.cmd clean package
mvnw.cmd -Preports compile
```

İkinci komut normal derlemeye ek olarak `target/dependency-list.txt` ve `target/dependency-tree.txt` oluşturur. Tree çıktısında `omitted for conflict` satırlarıyla seçilen sürümleri ayırın. Özellikle POI 5.5.1, Commons IO 2.21.0, Codec 1.20.0, Lang 3.21.0 ve PostgreSQL 42.7.12 seçildiğini doğrulayın. Raporlar uygulama dağıtımına eklenmez.

İsteğe bağlı effective POM:

```bat
mvnw.cmd help:effective-pom -Doutput=target/effective-pom.xml
```

Bu komut plugin metadata indirilebilir; gereken eklenti makinede mevcut değilse internet ister. Effective POM ve dependency raporlarını herkese açık paylaşmadan önce settings/profil kaynaklı özel yollar veya bilgiler içerip içermediklerini kontrol edin.

İlk grubun manuel kabul listesi:

1. Java 17 Windows bundle workflow'unu çalıştırın; kurulumu, launcher açılışını, login ve demo CTB yüklemeyi deneyin.
2. Rich-text node 53'ü okuyun; düzenleyip kaydedin. Syntax highlighting/HTML escaping, linkler, resimler ve tabloları kontrol edin.
3. SweetCherry Özel/TemplateNode üzerinden XLSX üretin. Excel veya LibreOffice'te açın; Türkçe karakterleri, başlıkları, hücreleri ve sayısal değerleri kontrol edin. POI testleri olmayan bu eski özellik için manuel test önemlidir.
4. Kullanılıyorsa PostgreSQL tenant'ını açın; bağlantı ve transaction/okuma-yazma yollarını kontrol edin. SQLite testleri uzak PostgreSQL'i doğrulamaz. Test edilmediyse bunu kayda geçirin.
5. Veri kaynağını kapatın, uygulamayı durdurun ve yeniden başlatın. Mevcut release klasöründeki kullanıcı CTB/tenant dosyalarının build sırasında korunmasını kontrol edin.

Mevcut test sayısının değişmesi beklenmez; bu yama konfigürasyon ve belge değişikliğidir. Release öncesinde gerçek çözülen bağımlılık ağacı için güncel advisory taraması yapılmalıdır. GitHub dependency graph/Dependabot kullanılabilir; ayrıca OWASP Dependency-Check veya OSV tabanlı bir tarama çıktısında transitif bulguların uygulanabilirliği incelenebilir. Bu araçları normal kullanıcı build'ine zorunlu ağ/ağır indirme adımı olarak eklemiyoruz.

## Resmi kaynaklar

- Maven POM: https://maven.apache.org/pom.html
- Ant koşul namespace'leri: https://ant.apache.org/manual/ifunless.html
- Launch4j Maven plugin: https://github.com/orphan-oss/launch4j-maven-plugin
- Boot 3.5.16 BOM: https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/3.5.16/spring-boot-dependencies-3.5.16.pom
- Commons Lang sürüm notları: https://commons.apache.org/proper/commons-lang/changes.html
- Commons IO sürüm notları: https://commons.apache.org/proper/commons-io/changes.html
- Commons IO güvenlik: https://commons.apache.org/proper/commons-io/security.html
- POI duyuruları ve güvenlik: https://poi.apache.org/
- POI 5.5.1 POM: https://repo.maven.apache.org/maven2/org/apache/poi/poi/5.5.1/poi-5.5.1.pom
- POI OOXML 5.5.1 POM: https://repo.maven.apache.org/maven2/org/apache/poi/poi-ooxml/5.5.1/poi-ooxml-5.5.1.pom
- PostgreSQL JDBC güvenlik: https://jdbc.postgresql.org/security/
- PostgreSQL 42.7.12: https://github.com/pgjdbc/pgjdbc/releases/tag/REL42.7.12
- PDFBox güvenlik: https://pdfbox.apache.org/security.html
- PDFBox sürümleri: https://pdfbox.apache.org/download.html
- OpenPDF Java gereksinimleri: https://github.com/LibrePDF/OpenPDF
- jsoup sürümleri: https://jsoup.org/news/
- SQLite JDBC sürümleri: https://github.com/xerial/sqlite-jdbc/releases

## Rapor eklentisi düzeltmesi

İlk `reports` denemesinde normal `clean package` ve 258 test başarılı olmuş, fakat dependency plugin 3.2.0 `tree` goal'ü `Attribute value can not be null` hatasıyla sonlanmıştır. Tam stack trace olmadan null değerin kaynağı kesinleştirilmiş sayılmaz. Eski raporlama eklentisi yerine Maven Central'da doğrulanan 3.9.0 kullanılır; uygulama bağımlılıkları ve rapor dosya adları korunur.

Tekrar denemek için `mvnw.cmd -Preports compile` yeterlidir. İlk çalıştırma yeni plugin bağımlılıklarını indirebilir. Sürenin 12 dakika olması, rapor hesaplamasının tek başına 12 dakika aldığı anlamına gelmez; indirme ve ağ bekleme süreleri logdan ayrıca incelenmelidir.

3.9.0 plugin descriptor'ü `tree` için `verbose` ve `outputFile` parametrelerini destekler. `list` goal'ünde eski Java modül adı çıkarma parametreleri bulunmaz; 3.2.0 logundaki “Can't extract module name” mesajları uygulamanın classpath ile çalışmasına engel değildi.

Jansi native-access uyarısı Maven'ı çalıştıran JDK'ya aittir; bu rapor hatasının nedeni olarak değerlendirilmez. `git.dirty=true` değişiklikler henüz commit edilmeden derlendiğinde beklenir.

Kaynak: https://maven.apache.org/plugins-archives/maven-dependency-plugin-3.9.0/plugin-info.html


## İkinci güncelleme grubu: PDFBox

Başlangıç commit'i: `b607b60`. İlk grubun Windows x64 paketleme workflow'u,
rich-text düzenleme ve XLSX dışa aktarma kontrolleri kullanıcı tarafından başarılı
olarak bildirilmiştir. PostgreSQL sunucu testi bu bildirimde yer almamaktadır.

PDFBox 3.0.1 → 3.0.8 olarak güncellenir. `pdfbox.version` property’si sürümü
tek yerde tutar; `fontbox` ve `pdfbox-io` PDFBox'ın transitif bağımlılıklarıyla aynı
3.0.8 sürümünde çözülmelidir. Resmi Java 8 minimum gereksinimi Java 17 ile uyumludur.
Bu değişiklik, uygulamada belirli bir güvenlik açığının mevcut olduğunu iddia etmez.

Maven Central'daki üç modülün yayımlanmış POM'ları karşılaştırılmıştır:
3.0.1'de `junit-jupiter` için scope verilmediğinden `compile` kabul edilir;
3.0.8'de açıkça `test` yazılmıştır. Önceki dependency-tree raporunda görülen
JUnit compile/runtime bağımlılıklarının bu yoldan gelmesi böylece giderilir.
Uygulamanın kendi `spring-boot-starter-test` bağımlılığı korunur. Ek exclusion
veya elle fontbox sürümü zorlaması gerekmez.

OpenPDF / extra fonts 2.0.2 ve Flying Saucer 9.7.1 bu grupta korunur.
OpenPDF 2.1.x ve sonrası Java 21 gerektirdiğinden Java 17 paketine doğrudan
geçiş yapılmaz; bu bileşenlerin bakım kararı ayrı değerlendirilir.

### Kabul kontrolleri

```bat
mvnw.cmd clean package
mvnw.cmd -Preports compile
```

- `target/dependency-tree.txt`: PDFBox, fontbox ve pdfbox-io 3.0.8 seçilmeli;
  JUnit uygulama `compile` / `runtime` scope'uyla çözülmemelidir. Test scope'u normaldir.
- Üretilen `target/SweetCherry.jar` ZIP olarak incelendiğinde `BOOT-INF/lib` altında
  `junit-*`, `junit-platform-*` veya `opentest4j-*` bulunmamalıdır.
- Login ve CTB seçimi ardından `/download-pdf` açılıp PDFBox örnek PDF'i indirilir;
  Türkçe karakterler ve dosyanın açılması kontrol edilir. Bu eski örnek yolun font
  dosyası erişimiyle ilgili bir hata varsa sürüm farkından ayrı değerlendirilir.
- Düğümün PDF düğmesiyle de çıktı alınır. `/export-node-to-pdf/{nodeId}`
  `PDFFromHTMLHelper` üzerinden OpenPDF kullanır; bu kontrol tek başına PDFBox
  doğrulaması değildir. Türkçe metin, tablo ve görsel içeren düğüm denenir.
- Windows Java 17 bundle workflow'u çalıştırılır; açılış ve PDF indirme kontrol edilir.

Bu grupta Java kaynakları ve mevcut POM yorumları değiştirilmemiştir.
Burada Maven çalıştırılmamıştır; build ve manuel kabul sonuçları ayrıca kaydedilir.

POM kaynakları:
- https://repo.maven.apache.org/maven2/org/apache/pdfbox/pdfbox/3.0.8/pdfbox-3.0.8.pom
- https://repo.maven.apache.org/maven2/org/apache/pdfbox/fontbox/3.0.8/fontbox-3.0.8.pom
- https://repo.maven.apache.org/maven2/org/apache/pdfbox/pdfbox-io/3.0.8/pdfbox-io-3.0.8.pom
