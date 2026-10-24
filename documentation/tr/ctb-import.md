# Dışa aktarılmış CTB’yi aktif veritabanına içe aktarma

`/exportedFiles` sayfasında bir `.ctb` dosyasının **İçe aktar** düğmesini seçin.
ADMIN oturumu ve yazılabilir aktif veri kaynağı gerekir. Dosya yükleme değildir:
SweetCherry’nin `myapp.exportingFolderName` klasöründe listelenen dosyalar kullanılır.
`.old` dosyaları ve sembolik bağlantılar içe aktarılmaz.

Önce kaynak dosya, hedef bağlantı adı ve `node`, `children`, `bookmark`, `image`,
`grid`, `codebox` tablolarının aktarılacak kayıt sayıları gösterilir. Bu aşamada
veritabanına yazılmaz. **Onayla ve içe aktar** aktarımı başlatır; iptal veri yazmaz.
Onay 10 dakika geçerlidir, bir kez kullanılabilir. Kaynak veri veya aktif seçim
bu arada değişirse yeniden önizleme/onay gerekir. Başarılı işlemden sonra aynı
dosyayı tekrar onaylamak yeni bir kopya oluşturur; birleştirme/senkronizasyon değildir.

## Veri kuralları

- Mevcut düğümlere dokunulmaz. Kaynak kökler, kendi sıralarını koruyarak hedefteki
  son kökün arkasına eklenir; tek bir yapay üst düğüm oluşturulmaz.
- Kaynağın tüm gerçek/paylaşımlı konumlarına, hedefin hem `node` hem `children`
  tablolarındaki en büyük ID’den sonra yeni ID verilir. Atama işleminin tamamı
  yazmadan önce hazırlanır; ID taşması hata olur.
- `children.node_id`, `father_id`, `master_id`; `node.node_id`; nesne tabloları ve
  `bookmark.node_id` aynı eşleme ile değiştirilir. `father_id=0`, `master_id=0`
  özel değerleri korunur. Paylaşımlı konumların kendi çocukları korunur.
- Kaynak yer işaretleri kaynak sıralarıyla mevcut yer işaretlerinin sonuna eklenir.
- Adlar, renk/ikon/kalınlık/salt okunur özellikleri, içerikler, gömülü resimler,
  dosyalar, tablolar, kod kutuları ve **oluşturulma/güncellenme tarihleri korunur**.
  Var olan hedef düğümlerin tarihleri değişmez.
- Zengin metin XML’indeki ve `image.link` alanındaki `node ID [çapa]` bağlantıları
  yeni ID’ye çevrilir. Kaynak dışında kalan bir düğüm bağlantısı varsa yanlış bir
  hedefe gitmesini önlemek için aktarım reddedilir. Web/dosya bağlantıları korunur;
  harici dosyaların kendileri kopyalanmaz.
- Eski CTB’de yalnız `children.master_id` eksikse sanal olarak `0` okunur; kaynak
  şema değiştirilmez. Altı standart tablo ve diğer standart alanlar gereklidir.
  Ek özel tablolar/alanlar aktarılmaz. Bozuk veya yetim bağlantılar reddedilir.
- Kaynak salt okunur SQLite işlemiyle tutarlı bir anlık görüntü olarak okunur.
  Kaynak dosyaya yazılmaz. Hedef SQLite, PostgreSQL veya MySQL/MariaDB olabilir.
  MySQL/MariaDB’de altı tablonun da **InnoDB** olması gerekir: işlemin tamamını
  geri alabilmek için transaction desteklemeyen tablolar reddedilir.
- Tek hedef transaction’ı kullanılır; bir kayıt yazılamazsa tüm eklemeler geri
  alınır. Eşzamanlı ID çakışması da üzerine yazma yerine işlemi iptal eder.
- Varsayılan (ayarlanabilir) sınır: kaynak dosya ve açılmış alan verileri için 64 MiB, altı tablonun
  toplamı için 200.000 kayıt. Büyük arşivleri küçük CTB’lere bölerek aktarın.
- Aktif dosya aynı zamanda kaynak seçilmişse önce okuma tamamlanıp kapatılır,
  sonra hedefe yeni ID’lerle kopyalanır.

Java paket adı `com.turkerozturk.importing.exportednodes` seçilmiştir;
`import`, Java anahtar kelimesi olduğundan paket adı olarak kullanılamaz.

## Manuel kontrol

1. Gerçek ve paylaşımlı düğümler, paylaşımlı parent altında çocuk, yer işaretleri,
   resim/dosya/tablo/kod kutusu içeren CTB’yi `/exportedFiles` altında hazırlayın.
2. Yazılabilir hedefi seçin; sayıları kontrol edip iptal edin. Hedef değişmemeli.
3. Yeniden önizleyip onaylayın. Kökler sonda; alt hiyerarşi aynı; paylaşımlı
   içerikler kendi yeni master’ına bağlı olmalı. Dahili bağlantıları açın.
4. Nesneleri ve tarihleri karşılaştırın; mevcut hedef içerikler değişmemeli.
5. Salt okunur hedefte düğme görünmemeli; doğrudan istek de yazamamalı.
6. Önizlemeden sonra kaynak içeriğini veya aktif seçimi değiştirin: eski onayla
   aktarım yapılmamalı. İki kez onay/gönderim yalnız bir kez yazmalı.
7. SQLite, PostgreSQL ve InnoDB MariaDB hedeflerde aynı örneği deneyin.

Otomatik kontroller: `CtbImportTest`, `CtbImportControllerTest`,
`CtbImportSecurityTest`, `CtbImportTemplateTest`.

## Ayarlanabilir sınırlar ve HTTP

`application.yml` varsayılanları:

```yaml
myapp:
  ctb-import:
    max-bytes: 67108864 # 64 MiB; hem kaynak dosya hem toplam açılmış alan verisi
    max-records: 200000 # altı tablonun toplamı
```

Pozitif değerler kullanın. Sistem → Uygulama Ayarları veya masaüstü YAML
editöründen de değiştirilebilir; yeni değerler için yeniden başlatın. Önizleme
sayfası o anda geçerli sınırları gösterir. Bellekte tutulan Java nesneleri, diziler,
metinler ve JDBC sürücüsü nedeniyle gereken heap bu bayt sınırından büyük olabilir.

CTB bu işlem sırasında istemciden yüklenmez: `/exportedFiles` altındaki sunucu
dosyası okunur. HTTP isteği yalnız dosya adı ve onay bilgilerini taşır; dolayısıyla
`server.tomcat.max-http-form-post-size` veya multipart upload sınırını CTB boyutu
kadar artırmaya gerek yoktur. CTB’yi başka bir upload özelliğiyle sunucuya
getiriyorsanız **o ayrı işlemin** ve varsa reverse proxy’nin sınırları uygulanır.

Rich text resim/dosya ekleme için limitler farklıdır; bkz.
[Bellek, HTTP ve dosya sınırları](memory-and-transfer-limits.md).
