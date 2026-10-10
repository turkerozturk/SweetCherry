# Eski CTB şemalarını açma

2019 CherryTree el kitabı veritabanında `children.master_id` yoktur. Paylaşımlı düğümlerden önceki bu şemada konumların tamamı gerçek düğümdür. SweetCherry, SQLite veri kaynağı seçilirken kullandığı temel tabloların/sütunların mevcut olduğunu kontrol eder. Eksik varsa önceki veri kaynağı seçimi korunur, havuz kapatılır ve seçim ekranında eksikler gösterilir. Loga eksik alanlar ve desteklenen işlem yazılır.

Şimdilik tek otomatik geçiş:

```sql
ALTER TABLE children ADD COLUMN master_id INTEGER DEFAULT 0;
```

Mevcut düğüm içerikleri, kimlikleri, hiyerarşi ve yer işaretleri değişmez. Yeni sütundaki 0, gerçek düğüm konumu anlamına gelir. İşlem transaction içinde uygulanır; SQL hatasında rollback yapılır. Sonraki seçimlerde sütun zaten varsa işlem tekrarlanmaz.

## İzin verme

1. CherryTree ve aynı CTB'yi kullanan diğer uygulamaları kapatın. **Önce dosyanın kopyasını kendiniz alın; SweetCherry yedek almaz.**
2. `allTenants` klasöründeki ilgili tenant config dosyasına ekleyin:

   ```properties
   custom.allowLegacySchemaUpgrade=true
   ```

   `1` de kabul edilir. Ayar yoksa, `false` veya başka bir değer ise izin kapalıdır.
3. Kaydedip arayüzden **Veri kaynaklarını yeniden yükle** seçeneğine tıklayın.
4. **ADMIN** hesabıyla veri kaynağını yeniden seçin. USER hesabı geçişi uygulayamaz.
5. Güncelleme tamamlanınca ayarı `false` yapıp veri kaynaklarını tekrar yükleyebilirsiniz.

Bu izin `custom.isWritable` içerik düzenleme izninden ayrıdır: içerik salt okunur olsa bile açıkça verilen şema geçiş izni dosyayı değiştirir. Dosya sistemi/JDBC bağlantısı gerçekten salt okunursa işlem hata verir ve veri kaynağı seçilmez.

Diğer eksik sütunlar veya tablolar tahminle oluşturulmaz. Ekran bunları listeler ve otomatik geçiş olmadığını bildirir. MySQL/MariaDB/PostgreSQL kaynaklarında SQLite şema geçişi çalıştırılmaz; mevcut bağlantı davranışı korunur. Mevcut dosya bulunmazsa yine yeni boş SQLite dosyası oluşturulmaz.

Kaynak örneği: https://giuspen.net/cherrytreemanual/cherrytree_manual.ctb . Bu dosya test sırasında indirilip bir kopyada incelendi; depoya üçüncü taraf CTB eklenmez. Tekrarlanabilir eski şema fixture'ı `src/test/resources/fixtures/legacy-ctb-schema.sql` dosyasıdır.

SQL davranışının resmi açıklaması: https://www.sqlite.org/lang_altertable.html#alter_table_add_column .
