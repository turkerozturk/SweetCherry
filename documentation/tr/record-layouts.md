# Özelleştirilebilir tek kayıt HTML tasarımları

Tasarım, CTB içindeki HTML kaynağına tek bir kaydın değerlerini yerleştirir. Mevcut HTML liste, HTML tablo, PDF ve XLSX görünümleri korunur. Bu adım HTML içindir; bu tasarımların PDF çıktısı sonraki adımdır.

## Örnekle başlama

1. Şablonun liste/tablo sayfasından **Kayıt tasarımları** bağlantısını açın. Kayıt yanındaki bağlantı o kaydı önceden seçer.
2. ADMIN ve yazılabilir bağlantıda **Örnek tasarım oluştur** düğmesine tıklayıp onaylayın. Eksikse şablon altında `_views`, onun altında Plain Text `Example layout` oluşturulur. Mevcut notların üzerine yazılmaz; birden fazla `_views` varsa açıklanır ve otomatik seçim yapılmaz. Tekrar gönderme, aynı adlı ikinci örneği sessizce oluşturmaz.
3. Kayıt seçip tasarım yanındaki **Tasarımla aç** düğmesine basın. **HTML kaynağını düzenle**, mevcut Plain Text editörünü açar; undo/kurtarma ve Kaydet ve devam et kullanılabilir.
4. Aynı `_views` altında başka Plain Text alt düğümler oluşturarak tasarım ekleyin. Adlar listede görünür, ID'ler aynı adlı tasarımları ayırır. Rich Text/Markdown/kod kaynakları listelenir ama HTML tasarımı olarak kullanılamaz.

```text
cherrytemplate-artifacts                  ← mevcut değişken listesi burada kalır
    _views                               ← tam ve büyük/küçük harfe duyarlı ad
        Example layout                   ← Plain Text HTML
        Dışarı verilecek rapor           ← başka bir Plain Text HTML

Örnek uygulama                           ← başka yerde; tags = cherrytemplate-artifacts
    application
    findings
```

`_views`, seçilen şablon konumunun altındadır. Paylaşımlı klasör/tasarım kullanılabilir; hiyerarşi kendi `children` satırlarıyla, kaynak içerik master'dan çözülür. Yeni tablo/sütun veya harici tasarım dosyası eklenmez. Oluşturma mevcut düğüm oluşturma tarih kurallarını kullanır; okuma/yenileme notları veya tarihlerini değiştirmez.

## Placeholder'lar

| Placeholder | Değer |
| --- | --- |
| `{{record.name}}` | Güvenli metin olarak kayıt adı |
| `{{record.id}}` | Ağaç konumu ID'si |
| `{{record.breadcrumbs}}` | Metin breadcrumb |
| `{{template.name}}`, `{{template.id}}` | Şablon adı ve seçilmiş konum ID'si |
| `{{view.name}}` | Tasarım adı |
| `{{now}}` | Sunucuda render zamanı |
| `{{field.application}}` | application değişkeninin render edilmiş içeriği |

Placeholder'lar büyük/küçük harfe duyarlıdır; yalnız **metin içeriğinde** çalışır, HTML özelliklerinde veya CSS içinde çalışmaz. `field.` sonrası anahtar şablondaki geçerli değişkenle tam eşleşir. Boşluk, nokta ve Unicode kullanılabilir; süslü parantez desteklenmez ve placeholder token'ı en fazla 256 karakterdir. Blok alanlar için başlık/paragraf yerine `<section>{{field.application}}</section>` kullanın. Alana eklenen not içeriği tekrar taranmaz: nottaki `{{now}}` metni aynen kalır.

```html
<article style="max-width:900px;margin:auto">
  <h1>{{record.name}}</h1>
  <p>{{record.id}} · {{now}}</p>
  <p>{{record.breadcrumbs}}</p>
  <div style="display:grid;grid-template-columns:1fr 2fr;gap:12px">
    <strong>Uygulama</strong>
    <section>{{field.application}}</section>
    <strong>Bulgular</strong>
    <section>{{field.findings}}</section>
  </div>
</article>
```

Üyelik hâlâ **tags alanının tamamının** şablon adına eşitliğidir. Değerler yalnız adı eşleşen doğrudan alt düğümlerden gelir. [Şablon raporlarındaki](template-reports.md) sıralı alan modeli, eksik/boş/çoklu eşleşme ve tekrarlı tanım açıklamaları kullanılır. Tek kayıt isteğinde bütün kayıtlar render edilmez. Paylaşımlı kayıt konumuna doğrudan konum ID'siyle erişilebilir; normal seçim listesi mevcut gerçek kayıt listesini korur.

## Dinamik zaman ayarı

```yaml
myapp:
  custom-lists:
    views:
      date-time-format: "yyyy-MM-dd HH:mm:ss"
      time-zone: ""  # JVM/sistem bölgesi; Europe/Istanbul veya UTC de olabilir
      max-design-characters: 131072
      max-rendered-characters: 16777216
```

`MM` ay, `mm` dakika, `HH` 24 saat biçimidir. Bunlar Java DateTimeFormatter kalıplarıdır; JavaScript/PHP biçimleri değildir. Geçersiz biçim/bölge ve sınırlar ayar doğrulamasında reddedilir; sessizce başka anlam kullanılmaz. YAML değişince yeniden başlatın. `{{now}}`, **Görünümü/zamanı yenile** veya sayfa yenilemede hesaplanır; sürekli çalışan tarayıcı saati değildir ve Created/Modified tarihlerini değiştirmez.

## HTML, bağlantılar ve sınırlar

Başlık, bölüm, normal metin biçimlendirmeleri, listeler, tablolar, bağlantılar, gömülü PNG/JPEG resimler, stiller ve satır içi CSS desteklenir. Script, olay özellikleri, form, iframe, object ve Thymeleaf/SpEL gibi ifadeler çalıştırılmaz. Temizlenmiş sonuç script/same-origin/form izni olmayan sandbox iframe'de gösterilir. İçerik politikası otomatik dış istekleri engeller; uzak resimler çıkarılır. Alanlardaki gömülü CTB resimleri/LaTeX kullanılabilir. HTTP/HTTPS ve dahili düğüm bağlantıları ayrı sekmede açılır; desteklenmeyen URL'ler kaldırılır.

Bu adımda script motoru yoktur; `{{now}}` sunucunun sağladığı adlandırılmış değerdir. Desteklenmeyen placeholder, kod çalıştırmak yerine açıklama gösterir. Kaynak düğüm temizlenmiş HTML ile değiştirilmez. Varsayılan tasarım sınırı 128 Ki karakter, oluşturulan HTML sınırı 16 Mi karakterdir; en fazla 64 tasarım alt düğümü listelenir. Sınır aşılırsa içerik kesilmez, işlem durur. YAML üst sınırları tasarım için 1 Mi, çıktı için 64 Mi karakterdir. Bunlar Java string karakter sayısıdır; upload baytı değildir. Tüm yollar seçili bağlantı korumalarına tabidir. Örnek oluşturmak ADMIN, yazılabilir bağlantı, geçerli sayfa belirteci ve CSRF gerektirir.

## Manuel kontrol

1. Onayla örnek oluşturun; iki düğümün normal Plain Text CTB düğümü olduğunu CherryTree'de de görün.
2. HTML'yi iki sütunlu yapıp ikinci tasarım ekleyin; farklı kayıtları açarak ad, ID ve nesnelerin doğru olduğunu kontrol edin.
3. Eksik/boş/çoklu eşleşen alan ve bilinmeyen placeholder deneyin. Not içine `{{now}}` yazın; yeniden yorumlanmasın.
4. Tarih biçimi/bölgesini değiştirip yeniden başlatın. Yenilemenin notların Created/Modified tarihlerini değiştirmediğini kontrol edin.
5. Deneme tasarımında script/form/uzak resim girişimleri çalışmasın ve dış kaynak istemesin. Desteklenen bağlantılar ayrı açılsın. Gösterim kaynak HTML'yi değiştirmesin.
6. USER/salt okunur erişim, yanlış şablon/tasarım/kayıt kombinasyonları, birden fazla `_views` ve bağlantı değişmiş eski sekmeyi deneyin; izinsiz oluşturma/yeni kaynağa yanlış yazma olmasın.
