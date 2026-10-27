# Zengin metin içinde kod kutusu düzenleme / Codeboxes in rich text

Kod kutusu, bir düğümün tamamının kod olmasıyla aynı şey değildir. Zengin metin içindeki ayrı `codebox` kaydıdır. **Kod kutusu ekle** düğmesi imleç konumunda yeni nesne oluşturur. Mevcut nesnenin **Kod kutusunu düzenle** düğmesi metni ve dili değiştirir. Dil listesinde Plain Text ilk sıradadır; diğer seçenekler alfabetik, ikonlu ve metinle filtrelenebilirdir. Editör içinde kod düz metindir; kaydedildikten sonra mevcut sözdizimi vurgulama ayarı uygulanır. Tanınmayan eski bir dil, değiştirilmedikçe korunur.

Yeni nesne varsayılanları: `justification=left`, `width=300`, `height=150`, `is_width_pix=1`, `do_highl_bra=1`, `do_show_linenum=0`. Mevcut nesnede yalnız `txt` ve `syntax` düzenlenir; boyutlar, hizalama ve bayraklar korunur. Nesne konumu, metnin Unicode karakter konumuna göre diğer nesnelerle birlikte güncellenir.

Değişiklikler ana **Kaydet / Kaydet ve devam et** işlemine kadar yalnız editördedir. Diyaloğu iptal etmek yazmaz. Geri alma/yeniden yapma nesne ekleme ve düzenlemeyi kapsar. Değişiklik yapmadan kaydetmek düğümün güncelleme tarihini değiştirmez. Silme mevcut kilitli nesne yönetimi üzerinden yapılır. Salt okunur düğüm/veri kaynağı, yetki ve eski revizyon kontrolleri aynen geçerlidir. Kod çalıştırılmaz, HTML olarak yorumlanmaz.

Sınırlar: kod kutusu başına 200.000 karakter; istek başına en fazla 20 kod kutusu düzenlemesi ve 2.000.000 karakter JSON; resim, dosya, tablo ve kod kutusu birlikte en fazla 10 yeni nesne. Kod dilleri mevcut paketlenmiş vurgulayıcı ve `CodeIcon` eşlemelerinden alınır; ayrı indirme yapılmaz.

## Manuel kontrol

1. Unicode metnin arasına varsayılan düz metin ve Python/Java kod kutuları ekleyin; kaydedip tekrar açın.
2. Mevcut kutunun metnini/dilini değiştirin; boyut ve diğer bayraklarının aynı kaldığını doğrulayın.
3. Ekleme/düzenleme ardından geri alma, yeniden yapma, diyalog iptali ve sayfadan çıkma uyarısını deneyin.
4. Kaydet ve devam et, nesne silme, resim/tablo/LaTeX ile birlikte metin düzenleme ve değişikliksiz kaydetmeyi deneyin.
5. SQLite, MariaDB ve PostgreSQL üzerinde kaydetme; sonrasında CherryTree'de CTB'yi açma veya CTB export üzerinden görünümü kontrol edin.

## English

A codebox is an embedded `codebox` object, distinct from a node whose entire content is code. **Add codebox** inserts at the cursor; **Edit codebox** on an existing object edits its plain source and syntax. Plain Text is first; other languages are sorted, searchable and illustrated with bundled icons. Existing unknown syntax is preserved unless replaced. Editing has no live highlighting; normal display uses the existing highlighting setting.

New defaults: left alignment, 300×150 pixels, `is_width_pix=1`, `do_highl_bra=1`, line numbers off. Existing dimensions/flags remain unchanged. All changes commit atomically with the main editor save, using existing revision, permission, object protection and Unicode offset checks. Cancel does not write; undo/redo covers codebox changes; an unchanged save does not touch the modified timestamp. Code is never executed or treated as HTML. Limits: 200,000 characters per codebox, 20 edits/2,000,000 JSON characters per request, and at most 10 new embedded objects across all object types.
