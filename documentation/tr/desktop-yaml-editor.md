# Masaüstü YAML ayar editörü

Windows EXE ile açılan SweetCherry kontrol penceresindeki **YAML Ayarları** düğmesi editörü açar. Türkçe/İngilizce seçimi mevcut kontrol penceresi gibi işletim sistemi diline göre yapılır. Sunucu/headless kullanımı bu Swing penceresini oluşturmaz. Yeni bir JVM veya web endpoint eklenmez.

DailyTopicTracker’daki [ApplicationPropertiesEditorDialog](https://github.com/turkerozturk/springtopiccalendar/blob/main/externaltools/src/main/java/com.turkerozturk.dttlauncher/ApplicationPropertiesEditorDialog.java) yaklaşımı SweetCherry’nin YAML yapısına uyarlanmıştır; properties parser’ı kullanılmaz. SnakeYAML zaten Spring Boot bağımlılıkları içindedir.

## Dosyalar ve kaydetme

- Çalışma/uygulama klasöründeki ve doğrudan `config/` altındaki mevcut `.yml` ve `.yaml` dosyaları listelenir; alt klasörler taranmaz. Sembolik bağlantılar düzenlenmez. Dosya limiti 1 MB, liste limiti 256 dosyadır.
- JAR içindeki mevcut varsayılan dosyalar `application.yml`, `application-operations.yml`, `application-experiments.yml` listelenir. Harici karşılığı varsa onun içeriği açılır; yoksa JAR içeriği başlangıç olarak gösterilir. Başka profile ait JAR dosyaları ileride eklenirse bu varsayılan dosya listesine de eklenmelidir.
- Hiç değişiklik yoksa harici dosya oluşturulmaz. Değiştirip **Kaydet** onayı verilirse uygulama klasörüne harici dosya oluşturulur. JAR ve kaynak kod dosyaları değiştirilmez.
- Mevcut harici dosya tüm içeriğiyle açılır; içinde yazılmamış JAR varsayılan alanları o tablonun içinde birleştirilmez. Eksik bir ayar YAML metni sekmesinden eklenebilir. Bu sekme ayar ekleme/silme ve karmaşık yapılar için de kullanılabilir.
- Tablo mevcut scalar değişkenleri, iç içe alanları ve `items[0].name` gibi liste öğelerini gösterir. Tür sütununda string/boolean/number/null seçilebilir. Boolean değerler seçmeli kutudur. Null/boş alana farklı bir metin yazıldığında tür string olur. Tip doğrulaması YAML düzeyindedir; her Spring/üçüncü taraf ayarının değer aralığını bilmez.
- Tablo yalnız değiştirilen alanların metin aralıklarını değiştirir; diğer değerler, yorumlar ve sıralama korunur. Çok satırlı metin düzenlenirse escaped quoted string biçimine dönüşebilir; metin değeri korunur. Birden fazla YAML belgesi desteklenir; sonraki belgelerin alanları `[2]` gibi önekle gösterilir.
- Duplicate anahtarlar, alias/merge, özel YAML tag’leri ve çakışan flattened anahtarlar bu editörde desteklenmez. Böyle dosyaları dış metin editörüyle düzenleyin. Geçersiz YAML açıldığında düzeltmek için metin sekmesi kullanılabilir; geçerli hale getirmeden kaydedilmez.
- Dosya başka yerde değiştirilmişse eski pencere üzerine yazamaz. **Yeniden Oku** ile güncel içeriği açın. Mevcut dosya geçici dosya + atomik değiştirmeyle kaydedilir; yeni dosya başka editörün oluşturduğu dosyanın üzerine yazılmaz. Yeni dosyaya POSIX sistemlerde yalnız kullanıcı okuma/yazma izni verilir; Windows’ta klasör izinleri geçerlidir. Mevcut dosyanın POSIX/Windows ACL izinleri korunur.

## Güvenlik ve yeniden başlatma

Bu **yerel** editör login, SSL, proxy, portlar ve diğer güvenlik ayarları dahil tüm dosya içeriğini düzenler. Login gerektirmemesi, işletim sistemi kullanıcısının zaten aynı dosyaları düzenleme yetkisine dayanır. Uzak web ayar editörünün güvenlik dışı alan listesi genişletilmez.

Password/secret/token/credential/private-key benzeri adlar tabloda maskelenir. Bu ad tanıma bir şifreleme veya tüm olası gizli alanları tanıma garantisi değildir. **Gizli değerleri göster** yerel ekranda açar. **YAML Metni** tüm değerleri açık gösterdiğinden onay ister. Ayrıntılı parser hataları içerikten şifre alıntılayabileceği için gösterilmez. Şifreler dosyada önceki gibi açık metindir.

Kaydetme çalışan uygulamayı değiştirmez. **SweetCherry’yi Yeniden Başlat** ayrı onay ister; tüm oturumlar/CTB bağlantıları kapanır ve mevcut yeniden başlatma akışı kullanılır. Kaydedilmemiş değişikliklerle yeniden başlatmaya izin verilmez. Başlatma başarısızsa kontrol penceresinden editör açılarak dosya düzeltilebilir; yeniden başlatma kullanılamıyorsa uygulamayı kapatıp elle yeniden açın.

Dosyanın listelenmesi Spring’in onu otomatik yükleyeceği anlamına gelmez. Normal `application.yml`, `config/` ve aktif profile ait `application-<profil>.yml` kuralları geçerlidir; özel adlı dosya için `spring.config.import/location` gerekebilir. Profil dosyasını düzenlemek profili etkinleştirmez. Komut satırı/ortam değerleri daha öncelikli olabilir; run.sh/run.bat port ve tarayıcı argümanlarını korur. Yeni port seçildiğinde eski browser adresi artık çalışmayabilir; yeni kontrol penceresindeki adresi kullanın.
