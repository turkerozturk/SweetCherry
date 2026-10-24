# Veritabanı ikonu

Tenant config dosyasında isteğe bağlı alan:

```properties
custom.databaseIcon=cherry_blue
```

`src/main/resources/static/img/ctbicons` içindeki PNG dosyasının adını `.png`
uzantısı olmadan, büyük/küçük harfleriyle aynı yazın. Görünen ikon adları ve büyük/küçük harf farkları da kabul edilir. Eksik, bilinmeyen, URL/yol
veya uzantı içeren değerlerde `ct_db` kullanılır. Örnekler: `ct_db`, `cherry_red`,
`cherry_blue`, `ct_home`. Düğüm ikonları değişmez. Ayar tenant sihirbazından da
seçilebilir/yazılabilir; config’i elle düzenlediyseniz veri kaynaklarını yeniden
yükleyin ve seçin.

İkon veritabanı adı yanında ağaç/okuyucu görünümlerinde, veri kaynağı listelerinde
ve `/tenants` tablosunda gösterilir. İkonları açtığınızda tüm veritabanını gösteren
Mermaid/Markmap haritalarının ve Freeplane/FreeMind çıktısının **sentetik veritabanı
kökünde** de kullanılır. Tek düğümden başlayan haritanın kökü o düğümün ikonudur.
İkonlar kapalıysa veritabanı ikonu da haritada bulunmaz. `.mm` referans/ZIP
seçenekleri önceki kurallarıyla çalışır; ZIP içindeki ctbicons klasörü ikonu içerir.

Veritabanı seçiminde resim desteklemeyen native `<option>` yerine klavye ve
mobil dokunmayla kullanılabilen açılır bir liste kullanılır. Bir veritabanı
satırına basmak mevcut veri kaynağı seçme işlemini gönderir.

Sihirbaz artık resimli ve aranabilir seçim sunar. Yeni yerleşimler ve kontrol
adımları: [Arayüz ve import hata ayrıntıları](interface-and-import-diagnostics.md).
