# Ek dosyalarda metin, hex ve arşiv önizleme

Attachments sayfasındaki veya düğüm içindeki ekin yanındaki **Dosyayı incele**
bağlantısını kullanın. Var olan Word/Excel önizlemeleri ve orijinal indirme
bağlantıları da kullanılabilir. **Metin**, **Hex** ve **Arşiv içeriği** seçenekleri
vardır. Bilinmeyen dosyalar da metin veya hex olarak açılabilir.

Bilinen kod/ayar/metin uzantıları metin, desteklenen arşivler arşiv tablosu,
diğer dosyalar hex olarak açılır. Varsayılan seçim dosya adına göre yapılır;
arşiv okuyucu ayrıca kapsayıcıyı kontrol eder. UTF-8 varsayılandır; UTF-16/LE/BE,
Windows-1254, ISO-8859-9, Windows-1252, ISO-8859-1 ve US-ASCII de seçilebilir.
Otomatik encoding tahmini yapılmaz. Başlangıç BOM'u gösterilmez; kodlamaya uymayan
byte ve görünmeyen kontrol karakterleri yalnız gösterimde değiştirilir ve uyarı
çıkar. Başka kodlama deneyin; gerçek byte değerlerini hex görünümünden inceleyin.

Metin düz ve kaçırılmış içerik olarak gösterilir: HTML/SVG/script/Markdown
çalıştırılmaz veya belge olarak render edilmez. Bilinen kaynak dosyaları mevcut
yerel highlight.js ile renklendirilir; uygulamanın syntax highlighting ayarı
kapalıysa veya dil yüklenemezse düz metin kalır. Büyük dosyanın yalnız sınırlı
başlangıcı gösterilir. Sınırda yarım kalan çok byte'lı karakter atlanır; Unicode
karakterleri ortadan bölünmez.

Hex görünümü her satırda 16 byte gösterir: hex offset, unsigned byte değerleri
ve ASCII sütunu. Yazdırılabilir ASCII dışındaki byte'lar noktayla gösterilir.
Önceki/sonraki sayfa kaynak belirtecini korur. Uygulama JDBC akışından sınırlı
byte okur; hex sayfası için ilgili konuma atlar. Bazı veritabanı sürücüleri yine
de tek BLOB'un tamamını kendi içlerinde belleğe alabilir.

## Arşivler

ZIP, JAR, WAR, EAR, TAR, TAR.GZ ve TGZ desteklenir. ZIP tabanlı DOCX, XLSX, PPTX,
ODT, ODS ve EPUB kapsayıcıları da incelenebilir. RAR/7z gibi diğer formatlar bu
arşiv görünümünde desteklenmez; metin/hex görünümü kullanılabilir.

Tabloda yol/ad, tür, boyut, sıkıştırılmış boyut, UTC değiştirilme tarihi, CRC32,
ZIP yöntem numarası, şifreleme bayrağı ve varsa not gösterilir. TAR bağlantısının
hedefi düz metindir. ZIP yönteminde 0 sıkıştırmasız, 8 deflate anlamındadır.
Olmayan alanlar boş veya tire görünür; ad/not/hedef en fazla 512 karakter gösterilir.

Bu görünüm meta bilgi listesidir; arşivi veya içerik checksumlarını doğrulamaz.
ZIP üyeleri açılmaz; şifreli dosyaların başlıkları okunabiliyorsa listelenebilir.
Şifreli/okunamayan dizin bilgileri listelemeyi engelleyebilir. TAR.GZ verisi byte
sınırı altında açılıp listelenir. Üyeler diske çıkartılmaz, bağlantılar izlenmez,
tek tek açılmaz/indirilmez, iç içe arşivler taranmaz ve kod çalıştırılmaz.
`../` içeren veya tekrarlı adlar yalnız metindir. Uzun liste açık uyarıyla kısaltılır.

## Ayarlar

Mevcut `myapp` bölümüne birleştirip değişiklikten sonra yeniden başlatın:

```yaml
myapp:
  file-preview:
    max-text-bytes: 262144
    max-characters: 200000
    hex-bytes-per-page: 512
    max-archive-bytes: 5242880
    max-expanded-bytes: 33554432
    max-entries: 1000
```

Metin byte sınırı 1024–1048576; karakter sınırı 1–1000000 Unicode code point.
Hex sayfası 128–4096 byte ve 16'nın katı olmalı. Arşiv girdisi 1024–20971520,
açılmış TAR.GZ boyutu 1024–67108864 byte; satır sınırı 1–2000 olabilir.
Karakter ve arşiv satır sınırları System settings sayfasından da değiştirilebilir.
Orijinal dosyayı indirmek bu gösterim sınırlarından etkilenmez.

USER ve salt okunur kaynaklarda kullanılabilir. Güncel tenant belirteci zorunlu;
yanıtlar `no-store` ve `nosniff` kullanır. Başka veritabanı seçildikten sonra eski
sekmenin bağlantısı reddedilir. Dosya byte'ları, düğümün tarih/etiket/özellikleri
ve CTB şeması değişmez. Mevcut Commons Compress ve highlight.js kullanılır;
yeni kütüphane veya harici servis eklenmez.

## Doğrulama

```bat
mvnw.cmd -Dtest=FilePreviewServiceTest,FilePreviewTemplateTest,FilePreviewSecurityTest,WordPreviewTemplateTest,RichTextObjectHtmlRendererTest,AppearanceSettingsTest test
mvnw.cmd clean package
```

Manuel: UTF-8 Türkçe/emoji, Windows-1254, UTF-16, HTML/kod, bilinmeyen binary,
boş dosya, hex sayfalama ve büyük metin sınırını deneyin. ZIP/TAR/TGZ meta bilgileri,
bozuk/büyük arşivler, liste ve düğüm bağlantıları, mobil/koyu tema, USER/salt okunur
kaynak ve veritabanı değiştirildikten sonra eski sekme erişimini kontrol edin.
