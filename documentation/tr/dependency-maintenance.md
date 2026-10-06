# Maven bağımlılık bakımı ve release doğrulaması

İlk inceleme: 2026-10-05 (`dded6a0`). Son sürüm kaydı: 2026-10-06, `pom.xml` commit'i `9908748d625bc5e0f93e2407abf1917fcac26d78`.

Güncel sürüm tablosu aşağıdaki POM durumunu gösterir; sonraki tarihsel bölümler önceki güncelleme adımlarını korur.

Bu belge doğrudan `pom.xml` bağımlılıklarının, ilgili resmi sürüm/güvenlik duyurularının ve bazı transitif sürüm ilişkilerinin ilk incelemesini kaydeder. **Tam bir CVE taraması veya güvenlik sertifikasyonu değildir.** Eski sürüm kullanmak tek başına bir açığın uygulamada tetiklenebildiğini göstermez. Maven'ın çözdüğü transitif ağacın ve dağıtılan JAR'ın ayrıca incelenmesi gerekir. POM’da seçilmiş sürümler ile tamamlanmış kabul kontrolleri ayrı kaydedilir; bir sürümün tabloda bulunması bütün platformlarda doğrulandığı anlamına gelmez.

## Sürümleme ve POM bilgileri

- Uygulama sürümü `0.5.0-SNAPSHOT`; Spring Boot parent sürümü `3.5.16`; Java hedefi ve Windows paketinin runtime'ı Java 17 olarak korunur.
- Yayın sürümüne geçiş [versioning.md](versioning.md) akışına göre yapılır.
- POM'a Git deposu (`scm`), GitHub Issues ve GitHub Actions adresleri eklendi. `scm.tag=HEAD` geliştirme içindir; yayımlanan commit Git etiketiyle ayrıca sabitlenir.
- `packaging` belirtilmediğinde Maven JAR kullanır. GitHub Release'e dosya yüklemek için Maven Central yayın ayarları veya `distributionManagement` eklemek gerekmez.
- Mevcut açıklama yorumları korunmuştur; bu yamada yorum silinmediği için yorum arşivine yeni kayıt gerekmez.

## İlk güncelleme grubu

| Bileşen | Önce | POM sürümü | Gerekçe ve kontrol |
|---|---|---|---|
| Commons Lang | 3.14.0 | 3.21.0 | 3.18.0'da `ClassUtils.getClass` için aşırı uzun girdide recursion/StackOverflow düzeltmesi var (CVE-2025-48924 ile ilgili). Daha güncel 3.x bakım sürümü seçildi. Kullanılan EnumUtils ve HTML escaping yolları test edilmeli. |
| Commons IO | 2.15.1 | 2.21.0 | POI 5.5.1'in ilan ettiği sürümle eşleştirildi. CVE-2024-47554 için resmi etkilenen aralık 2.14.0 öncesidir; eski 2.15.1'i bu açıkla ilişkilendirmiyoruz. |
| Commons Codec | Boot yönetimi: 1.18.0 | 1.20.0 | POI 5.5.1'in ilan ettiği sürüm. Boot property override ile transitif eski sürüm seçilmesi önlenir. |
| POI ve POI OOXML | 5.2.5 | 5.5.1 | İki modül aynı property kullanır. OOXML okurken yinelenen ZIP entry adlarıyla ilgili CVE-2025-31672, 5.4.0 öncesini etkiler. Mevcut XLSX yolu üretim içindir; bu bilgi uygulamanın açığa karşı sömürülebilir olduğunu kanıtlamaz. |
| PostgreSQL JDBC | 42.7.3 | 42.7.12 | Bakım güncellemesi. `channelBinding=require` için CVE-2026-54291 düzeltmesi 42.7.12'de. 42.7.3 bu duyurudaki etkilenen 42.7.4–42.7.11 aralığında değildir. Boot 3.5.16'nın 42.7.11 sürümüne körlemesine dönülmez. |

İlk grup incelemesinde Maven Central'da seçilen sürümlerin POM dosyalarının mevcut olduğu kontrol edildi. Bu, uygulama uyumluluk testinin yerine geçmez. POI POM'ları Commons IO 2.21.0 ve Commons Codec 1.20.0 kullanır; doğrudan bağımlılıklar ve Boot dependency management bu gereksinimleri eski sürümlere çekmemelidir.

## Güncel diğer doğrudan bağımlılıklar ve kalan kontroller

Bu tablo `9908748` commit’indeki POM ile eşleştirilmiştir. “Korundu” ifadesi güvenlik onayı değil, ilgili güncelleme grubunda sürümün değiştirilmediği anlamına gelir. Son test ve manuel kontrol durumu aşağıdaki 2026-10-06 kaydındadır.

| Grup | Mevcut sürümler | Sonraki işlem |
|---|---|---|
| Spring Boot ve yönetilen modüller | Parent 3.5.16; web, JPA, security, validation, actuator, test, cache, mail, websocket, devtools | Spring/Hibernate sürümlerini ayrı ayrı yükseltmeyin. Parent/BOM güncellemesi ayrı test grubu; güvenlik duyuruları ve destek durumu yayın günü tekrar kontrol edilir. |
| SQLite JDBC | 3.53.4.0 | 2026-10-06 tarihinde güncellendi; 258 test başarılı. Native sürücü için Olmayan CTB'nin oluşturulmaması, pool kapatma, dosya kilidinin bırakılması, rich-text nesne kayıtları ve transaction rollback Windows/Raspberry Pi'de test edilmeli. |
| MySQL JDBC | 9.0.0 | Boot yönetimindeki sürümle uyumluluk ve MySQL/MariaDB kullanım kapsamı ayrıca incelenir. Gerçek sunucu bağlantısı test edilmeden “uyumlu” sayılmaz. |
| PDFBox | 3.0.8 (önce 3.0.1) | İkinci grupta 3.0.8 seçildi. Güvenlik sayfasındaki 2026 path-traversal duyuruları `examples` modülünü ilgilendirir; core kullanımımız otomatik olarak bu açık sayılmaz. Font/görsel/PDF çıktısı ayrı test grubu. |
| OpenPDF ve extra fonts | 2.0.5 / 2.0.5 | Birlikte tutulur. 2.0.x Java 17; 2.1.x ve sonrası Java 21 ister. Yeni ana sürümler paket adı değişikliği de içerir. Java 17 dağıtımı korunurken doğrudan en yeni ana sürüme geçilmez. |
| Flying Saucer PDF | 9.13.3 | OpenPDF ile çözülen transitif sürüm ve kullanılan HTML→PDF API'leri birlikte incelenir. |
| jsoup | 1.23.2 | 2026-10-06 tarihinde güncellendi; 258 test başarılı. XML/HTML serileştirme davranışı rich-text okuma/önizleme/rendering çıktılarını etkileyebilir; node 53 ve boş/alias/plain-text senaryoları karşılaştırılır. |
| Thymeleaf layout | 3.3.0 | Yerleşim ve fragment davranışlarıyla ayrı kontrol. |
| Thymeleaf security extras | 3.1.1.RELEASE | Eski yorumda geçici bug override'ı var. Boot BOM'da 3.1.5.RELEASE görülüyor; admin/user görünürlük ve sunucu yetki testleriyle ayrı güncelleme adayı. Eski yorum ancak neden çözüldüğü doğrulanınca arşivlenerek değiştirilir. |
| WebJars | locator 0.52; Bootstrap 5.3.8; bootstrap-select 1.13.18; Popper 2.11.7; jQuery 3.7.1; jQuery UI 1.14.2+1; Font Awesome 7.3.0; htmx 4.0.0; hyperscript 2.0.2 | 2026-10-06 güncellemeleri ve genel sayfa kontrolü kaydedildi. Gerçekte yüklenen WebJar/statik dosya yolları ve etkileşimler ayrıca kontrol edilir; genel gezinme tüm yeni API’lerin kullanıldığını kanıtlamaz. |
| Astronomi ve zaman | commons-suncalc 3.11; time4j-base/sqlxml 5.9.4; time4j-tzdata 5.0-2026b | 2026-10-06 tarihinde güncellendi; 258 test başarılı. Gerçekte kullanılan provider ve Java runtime tzdata ilişkisi ayrı incelenir. Kutup yazı ve timezone testleri korunur. |
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


## Sürüm property düzeni ve güncelleme yöntemi

Başlangıç commit'i: `907902b`. İstenen 16 bağımlılığın mevcut sürümleri 14 adet
`sweetcherry.*.version` property’sine taşınmıştır. OpenPDF ve extra fonts aynı;
Time4J base ve sqlxml aynı property’yi kullanır. tzdata kendi sürüm çizgisindedir.
Bu düzenleme sürüm yükseltmez, scope değiştirmez veya Spring Boot BOM'una yeni
override eklemez. Önceki incelemede bilinçli eklenen Commons/PostgreSQL override’ları
korunur. POM yorumları silinmemiştir.

### Her kütüphaneyi tek tek mi güncellemek gerekir?

Her seferinde tüm uygulamayı elle dolaşmak gerekmez. Değişiklik birimi **bir
bağımsız bağımlılık veya birbiriyle uyumlu olması gereken grup** olmalıdır:

| Grup | Birlikte değerlendirme | Hedefli kabul kontrolü |
|---|---|---|
| Spring Boot parent/BOM | Spring, Security, Hibernate ve Boot tarafından yönetilen modüller | Başlatma, login/yetki, tenant ve temel okuma/yazma |
| PDF | OpenPDF + extra fonts; Flying Saucer'ın istediği OpenPDF ve Java/API sınırı | Türkçe font, resim/tablo içeren PDF |
| POI | poi + poi-ooxml; gerekli Commons transitifleri | XLSX üretme ve açma |
| Time4J | base + sqlxml; ayrı sürümlenen tzdata | Astronomi, tarih/saat ve zaman dilimi |
| SQLite JDBC | Tek sürücü; native platform ve transaction davranışı | Windows/Pi açma, kayıt, rollback, pool kapatma/dosya kilidi |
| jsoup | Bağımsız güncelleme; HTML/XML dönüşüm davranışı | Rich-text node 53, düzenleme/kaydetme ve yapıştırma |
| Web arayüzü | Bootstrap/jQuery eklentileri ve gerçekten yüklenen statik dosyalar | Menü, editör, tablo ve mobil görünüm |
| springdoc / Thymeleaf eklentileri | Spring Boot/Thymeleaf ana sürümüyle uyumluluk | Uygulama açılışı, şablonlar ve API belgeleri erişimi |

1. Çalışan commit'ten ayrı branch açın; mevcut test sonucunu başlangıç kabul edin.
2. Resmi sürüm notlarını, Java minimumunu ve framework uyumluluk matrisini okuyun.
   “En yüksek sürüm” ile “bu proje için uygun sürüm” aynı şey değildir.
3. Bir grup güncellenir; `mvnw.cmd clean package` ve `mvnw.cmd -Preports compile`
   çalıştırılır. Seçilen transitif sürümler önceki raporla karşılaştırılır.
4. Gerçek paketli JAR başlatılır; ilgili özelliğin hedefli manuel kabul testi yapılır.
   Testler uygulamanın tam Spring context açılışını kapsamıyorsa build başarısı tek
   başına runtime başarısı değildir. Native/launcher değişikliklerinde Windows
   bundle ve Raspberry Pi kontrolü ayrıca gerekir.
5. Başarılı grup ayrı commit edilir. Başarısızlıkta ilk anlamlı `Caused by` satırı,
   güncelleme farkı ve dependency-tree ile neden bulunur; o grup geri alınabilir.
   Daha geniş bir grup başarısızsa adaylar küçültülerek hata kaynağı izole edilir.

Patch/minor numarası uyumluluk garantisi değildir; major geçişler genellikle
ayrı migration işi olarak planlanır. Örneğin springdoc 3.x Spring Boot 4 içindir;
SweetCherry Boot 3.5 ile 2.x çizgisinde kalır. OpenPDF 2.1.x ve sonrası Java 21
ister; 3.x ayrıca `com.lowagie` → `org.openpdf` paket adı geçişi içerir.

### Otomatik araçlar nasıl yardımcı olur?

Dependabot ve Renovate yeni sürümleri izleyip inceleme için PR açabilir.
Dependabot varsayılan olarak tek bağımlılık için PR açar; grup kurallarıyla
birlikte ele alınacak modüller, güncelleme türleri ve takvim tanımlanabilir.
Renovate de grup/uyumluluk kuralları ve CI sonucu ile yönetilebilir. Bot önerisi
uygulama uyumluluğu onayı değildir. SweetCherry için haftalık öneri, sınırlı PR
sayısı, major geçişlerde manuel inceleme ve otomatik merge olmaması uygun bir
başlangıç politikasıdır. Bu yamada bot veya workflow konfigürasyonu eklenmemiştir.

Yaygın bakım akışı: bot PR'ı → sürüm notu/uyumluluk incelemesi → CI test ve paketleme
→ hedefli kullanım kontrolü → merge. BOM yönetimindeki modüller tek tek rastgele
sabitlenmez; gerektiğinde belgelenmiş override veya parent güncellemesi yapılır.

Kaynaklar:
- https://springdoc.org/ (uyumluluk matrisi)
- https://github.com/LibrePDF/OpenPDF (Java ve paket adı gereksinimleri)
- https://docs.github.com/en/code-security/reference/supply-chain-security/dependabot-options-reference
- https://docs.renovatebot.com/key-concepts/automerge/


## 2026-10-06 sürüm güncellemeleri ve doğrulama kaydı

Kaynak POM commit'leri:
- https://github.com/turkerozturk/SweetCherry/commit/638d95921173cbc54abf4c594a90297110584ace
- https://github.com/turkerozturk/SweetCherry/commit/9908748d625bc5e0f93e2407abf1917fcac26d78

| Bağımlılık | Önce | Güncel POM sürümü | Property |
|---|---|---|---|
| htmx.org | 1.9.12 | 4.0.0 | `sweetcherry.htmx.version` |
| sqlite-jdbc | 3.44.1.0 | 3.53.4.0 | `sweetcherry.sqlite-jdbc.version` |
| jsoup | 1.17.2 | 1.23.2 | `sweetcherry.jsoup.version` |
| jquery-ui | 1.13.2 | 1.14.2+1 | `sweetcherry.jquery-ui.version` |
| font-awesome | 6.5.2 | 7.3.0 | `sweetcherry.font-awesome.version` |
| commons-suncalc | 3.10 | 3.11 | `sweetcherry.suncalc.version` |
| time4j-base | 5.9.1 | 5.9.4 | `sweetcherry.time4j.version` |
| time4j-sqlxml | 5.9.1 | 5.9.4 | `sweetcherry.time4j.version` |
| time4j-tzdata | 5.0-2022a | 5.0-2026b | `sweetcherry.time4j-tzdata.version` |
| bootstrap | 5.3.3 | 5.3.8 | `sweetcherry.bootstrap.version` |

POM'daki artifact adı `time4j-sqlxml`’dir. jQuery UI sürümündeki `+1` eki
WebJar sürümünün parçasıdır ve tabloda aynen korunmuştur.

### Bildirilen sonuçlar

Kullanıcı bu güncellemeleri push etmiş ve her iki aşama için de
`Tests run: 258, Failures: 0, Errors: 0, Skipped: 0` sonucunu bildirmiştir.
Uygulama sayfalarında genel gezinme yapılmış, görünür bir problem gözlenmemiştir.
Bu sonuçlar build/test ve genel gezinme doğrulamasıdır; her frontend etkileşiminin,
PDF çıktısının veya native sürücünün bütün platformlarda sınandığı anlamına gelmez.

İlk güncelleme grubu için daha önce bildirilen Windows x64 bundled distribution,
rich-text düzenleme ve XLSX dışa aktarma başarıları tarihsel olarak korunur.
Bu son sürüm seti için yeni Windows bundle / Raspberry Pi, PDF veya uzak veritabanı
kabul sonucu bildirilmemiştir; önceki sonuçlar otomatik olarak yeni sete aktarılmaz.

### Release öncesinde kalan hedefli kontroller

- Yeni `dependency-tree` raporunda seçilen sürümleri ve transitif çakışmaları kontrol edin.
- SQLite: CTB açma, rich-text nesne kaydı, rollback, kaynağı kapattıktan sonra dosya
  kilidinin bırakılması ve olmayan dosyanın oluşturulmaması; Windows ve Raspberry Pi.
- jsoup: node 53 okuma/düzenleme/kaydetme, HTML yapıştırma, link/resim/tablo ve boş içerik.
- Web arayüzü: gerçekten yüklenen dosya sürümleri, mobil/masaüstü menüler, editör,
  jQuery UI kullanılan ekranlar ve Font Awesome ikonları. Statik kopyalar POM ile değişmez.
- Astronomi: widget, kutup senaryoları, saat dilimi ve tarih/saat gösterimi.
- Java 17 Windows bundle, PDFBox örneği ve düğüm PDF çıktısı için önceki kabul listeleri.

Bu belge güncellemesi POM'a veya uygulama koduna müdahale etmez.


## 2026-10-07 PDF bağımlılık grubu

Başlangıç commit'i `0bebdf6`. OpenPDF / extra fonts 2.0.2 → 2.0.5 ve
Flying Saucer 9.7.1 → 9.13.3 olarak eşleştirilir. PDFBox 3.0.8 korunur.
Bu satırlar 2026-10-06 POM sürüm kaydının ardından gelen yeni gruptur.

Maven Central metadata'da yayımlanan son 2.0.x OpenPDF/font paketi 2.0.5,
son 9.x Flying Saucer PDF 9.13.3'tür. Yayımlanmış parent POM'ları Java 17
hedefini doğrular; Flying Saucer parent 9.13.3 `openpdf.version=2.0.5` kullanır.
OpenPDF 2.1+ ve Flying Saucer 10+ Java 21 istediği için bu gruba alınmaz.
Bu seçim “Java 17'ye uygun son yayımlanmış sürüm”dür; ilgili eski dal için
gelecekte güvenlik backport garantisi veya tam güvenlik onayı anlamına gelmez.

Flying Saucer 9.12.1 sürüm notları XMLResource'ta dış entity erişiminin
kapatıldığını kaydeder; 9.13.3 bu düzeltmeyi içerir. 10.4.0'da ayrıca
DocumentBuilderFactory sertleştirmesi vardır; Java 17 dalına aynı düzeltmenin
backport edildiği varsayılmaz. Yeni PDF yolunda kendi güvenli DOM hazırlığımız
ve kapalı kaynak erişim politikamız gerekir. Transitive advisory taraması
release kabulünün ayrı adımıdır.

`PdfLibraryCompatibilityTest`, Flying Saucer → OpenPDF üretiminin PDFBox ile
okunabildiğini, sayfa/metin ve dış bağlantı annotation'ını kontrol eder.
Bu test henüz Unicode font, resim, TOC veya tam node export testi değildir.
Mevcut HTMLWorker endpoint'i bu yamada değiştirilmez. Hiçbir PDF kütüphanesi
silinmez veya comment edilmez; mevcut Java kodu onları hâlâ kullanır.

Kabul: `mvnw.cmd clean package`, `mvnw.cmd -Preports compile`; ağaçta
OpenPDF 2.0.5, extra fonts 2.0.5, Flying Saucer core/pdf 9.13.3 ve PDFBox
3.0.8 seçildiğini kontrol edin. Mevcut düğüm PDF ve `/download-pdf` örneğini,
Java 17 Windows bundle açılışını da deneyin. Bu grupta burada Maven çalıştırılmadı.

Kaynaklar:
- https://repo.maven.apache.org/maven2/com/github/librepdf/openpdf/maven-metadata.xml
- https://repo.maven.apache.org/maven2/com/github/librepdf/openpdf-fonts-extra/maven-metadata.xml
- https://repo.maven.apache.org/maven2/com/github/librepdf/openpdf-parent/2.0.5/openpdf-parent-2.0.5.pom
- https://repo.maven.apache.org/maven2/org/xhtmlrenderer/flying-saucer-pdf/maven-metadata.xml
- https://repo.maven.apache.org/maven2/org/xhtmlrenderer/flying-saucer-parent/9.13.3/flying-saucer-parent-9.13.3.pom
- https://github.com/flyingsaucerproject/flyingsaucer/blob/main/CHANGELOG.md
- https://github.com/flyingsaucerproject/flyingsaucer (Java gereksinimleri)
- https://pdfbox.apache.org/security.html
- [PDF dışa aktarma geliştirme planı](pdf-export-plan.md)
