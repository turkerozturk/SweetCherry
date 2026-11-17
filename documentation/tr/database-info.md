# Veritabanı bilgileri ve iç bağlantı raporu

Seçili veritabanında **Datasources → Veritabanı bilgileri** menüsünü açın.
Sayfa `/database-info` adresindedir; ADMIN ve USER kullanıcıları salt okunur
veritabanlarında da görüntüleyebilir. Veritabanı seçilmemişse ana sayfaya dönülür;
eski bir sekmenin tenant belirteciyle yapılan gezinme reddedilir.

İstatistikler tüm kayıtları kapsar: düğüm içerikleri, ağaç yerleşimleri,
paylaşılan yerleşimler, kökler, yer imleri, image tablosundaki nesneler
(resim/ek dosya/çapa), kod kutuları, tablolar ve sözdizimine göre içerik sayıları.
Paylaşılan yerleşimler aynı içeriği yeniden kullanır; içerik sayısını artırmaz.
Ağaç dışında kalan kayıtlar varsa ilgili tablo toplamlarında yer alır.
İstatistik sorguları içerik veya BLOB yüklemez; bağlantı adresi ve parola gösterilmez.

Alt bölüm mevcut Internal Links raporunu kullanır ve başlangıçta yalnız sorunlu
bağlantıları gösterir: eksik hedef düğüm, eksik çapa ve geçersiz internal link.
Kaynak düğüm bağlantısından içeriği açıp bağlantıyı elle silin veya düzeltin.
Arama, durum filtresi ve sayfalama bu sayfada kalır. Düzenleme sonrası **Göster**
düğmesi yeniden tarar. Raporun taraması önbelleğe alınmaz; büyük veritabanlarında
sayfanın açılması zaman alabilir. Okunamayan veya boyut sınırını aşan içerikler
uyarı olarak ayrıca gösterilir; bu durumda rapor bütün bağlantıları kapsamayabilir.
Rapor CTB üzerinde değişiklik yapmaz.

## Doğrulama

```bat
mvnw.cmd -Dtest=DatabaseInfoServiceTest,DatabaseInfoControllerTest,DatabaseInfoSecurityTest,InternalLinkReportTemplateTest,InternalLinkReportServiceTest test
mvnw.cmd clean package
```

Manuel kontrol: boş ve dolu veritabanları; paylaşılan düğümler; eksik hedef ve
çapa içeren bağlantılar; filtre ve sonraki sayfa; kaynak düğümden bağlantı düzeltme
ve yeniden tarama; USER ve salt okunur erişim; başka CTB seçildikten sonra eski
sekmede filtreleme.
