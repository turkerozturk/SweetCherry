# Üçüncü taraf lisans ve atıf envanteri

About sayfası kısa tutulur. Bütün bağımlılıkların uzun listesi bu sayfaya gömülmek yerine dağıtımda ayrı bir lisans/atıf envanteri olarak hazırlanmalıdır. SweetCherry’nin kendi lisansı GNU GPL v3 veya sonraki sürümlerdir; bu bilgi bütün üçüncü taraf bileşenlerin aynı lisansa sahip olduğu anlamına gelmez.

Release öncesinde:

1. `mvnw.cmd -Preports compile` ile Maven bağımlılık raporunu, `mvnw.cmd -Plicenses -DskipTests verify` ile mevcut lisans toplama profilini çalıştırın. Linux’ta `sh ./mvnw` kullanın. Çıktılar başlangıç envanteridir; otomatik sonuçların tamlığı varsayılmamalıdır.
2. Doğrudan ve geçişli JAR bağımlılıklarını; ayrıca Maven dışındaki JavaScript, CSS, font, ikon ve diğer kopyalanmış kaynakları inceleyin. CherryTree’den alınan ikonlar da bu kapsamda değerlendirilmelidir.
3. Her bileşen için ad, sürüm veya kaynak commit, kaynak URL, lisans, gerekli atıf/NOTICE metni ve ilgili lisans dosyasının konumunu kaydedin. Belirsiz kayıtları doğrulanmış gibi işaretlemeyin.
4. Doğrulanmış `THIRD-PARTY-NOTICES` ve gerekli lisans metinlerini dağıtıma ekleyin. About’tan yayımlanmış envantere bağlantı eklemek sonraki adımdır; henüz oluşmamış bir rapora çalışan bağlantı varmış gibi davranılmamalıdır.

Bu belge tam lisans envanteri veya tamamlanmış lisans incelemesi değildir. Mevcut lisans başlıkları ve dosyaları bu arayüz sadeleştirmesinde kaldırılmamıştır.
