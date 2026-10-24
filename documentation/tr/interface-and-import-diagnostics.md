# İkon seçimi, editör panelleri ve import hata ayrıntıları

## Tenant ikonu

Sihirbazda düğüm özellikleri sayfasındakiyle aynı görselli, aranabilir ikon
seçicisi kullanılır. Seçilen `databaseIcon` alanı formdan kayda aktarılır.
Config elle düzenlenirken `custom.databaseIcon` için PNG adı veya ikonun
`NodeIcon.iconNameForHuman` görünen adı kullanılabilir. Büyük/küçük harf ve
boşluk/alt çizgi/tire farkları tolere edilir. Kaydedilen değer gerçek PNG adıdır;
örneğin `CHERRY_BLUE` → `cherry_blue`, `W Night Clear` → `ct_W-night-clear`.
Bazı mevcut PNG adlarında büyük `W` bulunduğundan gerçek dosya adı korunur.
Yanlış veya bilinmeyen değer `ct_db` olur.

## Rich text ve ağaç görünümü

- Masaüstünde araçlar soldadır; dikey ayırıcı fareyle veya sol/sağ okla genişliği
  değiştirir. Yalnız panel genişliği tarayıcıda hatırlanır; içerik kaydedilmez.
- Mobilde üstteki **Araçlar** düğmesi yan panel açar. Yazım alanı bağımsız kayar.
  Kapat, dış alana dokunma veya Escape paneli kapatır. Seçim editör modelinde
  tutulur; biçimlendirme ve link/nesne diyalogları önceki mekanizmayı kullanır.
- `/tree` içeriğinde önce breadcrumbs ve düğmeler, sonra ikonuyla düğüm başlığı
  gelir. Breadcrumbs’ın başındaki ikon kaldırılmıştır; ağaçtaki veritabanı ikonu
  ve düğüm ikonları korunur.
- Görüntülenen rich text tablo ve hücrelerinde 1 px siyah çerçeve vardır.

## Veri kaynağı sırası ve tarih

Seçim listelerinde önce CTB kaynakları, dosyanın dosya sistemindeki **son değişim
zamanı azalan sırada** gösterilir. Okunamayan/eksik CTB’nin tarihi boş kalır ve
CTB grubunun sonuna yerleşir. Ardından sunucu veritabanları görünen ada göre
A–Z sıralanır. Eşit tarihler adla belirlenir.

`/tenants` tablosu her zaman görünen ada göre A–Z sıralıdır. URL ile kimlik
doğrulama sütunu arasında CTB dosyasının tarihi, `yyyy-MM-dd HH-mm-ss` biçiminde
**SweetCherry sunucusunun saat dilimiyle** gösterilir. Bu bir düğüm tarihi veya
uzak veritabanının son işlem zamanı değildir. SQLite bağlantısı açılmaz, eksik
dosya oluşturulmaz. Sıralama sayfayı tekrar yüklediğinizde güncellenir.

## Import önizlemesi

Sınır aşıldığında CTB dosyasının bayt boyutu, tablolardaki toplam alan verisi,
altı tablonun toplam kayıt sayısı ve geçerli iki limit gösterilir. Sayfa ayarların
Sistem → Uygulama Ayarları, masaüstü YAML editörü veya `application.yml` içindeki
`myapp.ctb-import.max-bytes` / `max-records` üzerinden değiştirileceğini anlatır.
Değişiklikten sonra restart ve yeni önizleme gerekir. Ölçüm aşamasında resim ve
ek dosya BLOB’ları Java belleğine yüklenmez; SQL uzunlukları/sayıları okunur.

Dahili düğüm bağlantısı kaynağın dışında kalıyorsa kaynak içerik düğümünün ID’si,
başlığı, alanı (`node.txt` veya `image.link` ve offset’i) ve bulunamayan hedef ID
listelenir. Bir master’ın içeriğini kullanan paylaşımlı konumlarda kaynak ID,
içeriğin saklandığı master ID’sidir. Aynı alandaki aynı hedef bir kez gösterilir.
Düğümü silmek yerine bağlantıyı düzeltmek veya kaldırmak yeterlidir.
İçe aktarma onayı verilmez; kaynak veya hedefe yazılmaz.

## Manuel kontrol

1. Tenant sihirbazında arama yapıp farklı ikon seçin, kaydedin, config ve görünümü
   kontrol edin; geri açıldığında aynı ikon seçili olmalı.
2. Mobilde birden fazla kelime seçin, Araçlar’ı açıp bold/renk/link uygulayın;
   seçimin ve undo’nun çalıştığını, panel kapanınca yazmaya devam edebildiğinizi
   kontrol edin. Masaüstünde ayırıcıyı sürükleyerek de deneyin.
3. Yeni/eski iki CTB ve iki uzak kaynakla seçim sırasını ve `/tenants` A–Z sırasını
   karşılaştırın; dosya değiştikten sonra sayfayı yenileyin.
4. Düşük import limitiyle önizleyin: ölçümler ve ayar yolu görünmeli. Kaynakta
   olmayan hedeflere birkaç dahili link ekleyin: her sorunlu kaynak başlığı ve
   ID’si, hedef ID’siyle gösterilmeli. İptalde hiçbir veri değişmemeli.
