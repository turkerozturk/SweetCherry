# Unicode karakterleri ve satır içi ikonlar

**User → Sembol / ikon…** menüsünden filtreli seçiciyi açıp kopyalayabilirsiniz. Başlığı yerinde düzenleme formunda, Node Properties ad alanında ve rich-text editör araçlarında aynı düğme ayrıca imleç/seçim konumuna **Ekle** yapar. Seçici otomatik kaydetmez; başlığı/özellikleri/içeriği normal biçimde kaydedin. Kapat/Escape, düzenleme seçimini geri getirir.

Katalog: 236 hazır Unicode karakteri, mevcut CTBICONS klasöründen 373 PNG ve uygulamanın Font Awesome Free 7.3.0 paketinden 2.157 stil/ikon birleşimi (Solid/Regular/Brands). Ad, İngilizce/Türkçe anahtar kelime, sınıf veya `U+` koduyla filtreleyin; grubu seçebilir ve sayfalar arasında gezebilirsiniz. Unicode listesi bütün standardın değil, kullanışlı karakterlerin bir seçkisidir. Emoji görünümü ve kullanılabilirliği sistem fontlarına bağlıdır.

## Metin biçimi

Unicode gerçek karakter olarak eklenir/kopyalanır: `🙂`, `✓`, `→` gibi. İkonlar normal metin işaretleriyle saklanır:

```text
Ekran görüntüsü {{fa-solid fa-image}}
Favori {{fa-regular fa-heart}}
Kaynak {{fa-brands fa-github}}
Kamera {{ctb:ct_camera}}
```

Elle yazabilir veya seçiciden kopyalayıp/ekleyebilirsiniz. Kısa FA önekleri `fas`, `far`, `fab` ve katalogda bulunan ad eşanlamları da tanınır. CTB adları büyük/küçük harf duyarsız biçimde gerçek dosyaya eşlenir (`{{ctb:CT_CAMERA}}` gibi). Yalnızca kurulu ad/aileler kabul edilir; keyfi sınıf, HTML, URL veya dosya yolu yoktur. Bilinmeyen ifadeler metin kalır. Bu `{{...}}` biçimleri, editörün `[[node name]]` internal-link çözümlemesini tetiklemez; diğer şablon ifadeleri yorumlanmaz. Üçlü/iç içe süslü parantezler de olduğu gibi kalır.

İlk sürüm **web okuma görünümü** içindir: masaüstü ağaç adları, başlık ve breadcrumb'lar, mobil gezinme/bağlam, rich-text içerik ve rich-text fragment'ler. Asenkron gezinme, başlık değişimi ve split reader ile çalışır. İkon boyutu çevresindeki metni izler; Font Awesome metin rengini, CTB PNG kendi renklerini kullanır. İçerikteki satır içi ikonlar normal ek resim gibi yeni sekmede açılmaz.

Gösterim veritabanını değiştirmez. Normal ad/metin içinde işaret saklanır; image tablosunda kayıt veya gömülü nesne oluşturulmaz. XML, arama/sıralama, indirme ve sunucu PDF/metin/mindmap export'ları saklanan metni kullanmaya devam eder. CherryTree masaüstü uygulamasında işaret metni görülür; Unicode font desteklediği yerde doğal olarak çalışır. PDF'te ikon genişletme bu değişiklikte yoktur.

## Düzenleme ve düz metin örnekler

Düzenlerken imleç/model/biçimlendirme bozulmasın diye işaretler metin olarak kalır; seçici önizlemesinde gerçek ikon görülür. Rich-text ekleme normal run/undo modelini kullanır ve tek undo/redo adımıdır. Korunan gömülü nesneler veya değişmiş/meşgul içerik bu yolla üzerine yazılamaz. Adın mevcut 255 UTF-16 birimlik sınırını aşan ekleme reddedilir.

Bir işareti aynı biçimlendirmeyle tutun; bitişik eş biçimli span’ler de desteklenir. İşaretin yarısını bold/başka renk/link yaparak span'lere bölmek tanınmasını engelleyebilir; gerekirse seçiciden yeniden ekleyin. Plain/code/Markdown/HTML düğüm içerikleri kapsam dışında; rich içerikteki pre/code/monospace ve tablo/grid metinleri de düz kalır. İkon işaretini örnek olarak gösterecekseniz code/monospace kullanın. Editör yüzeyleri ve seçici içi dönüştürülmez.

Salt okunur kullanıcı seçiciden kopyalayabilir; kaydetme için mevcut ADMIN/yazılabilir CTB/güncel tenant kontrolleri geçerlidir. Clipboard API kullanılamazsa normal kopyalama denenir; o da olmazsa Ctrl+C için değer seçilir. Pano okunmaz, dış ikon/font servisi veya yeni kütüphane indirilmez. Mevcut yerel WebJar fontları ve CTB PNG'leri kullanılır; kaynak/lisans bilgileri `distribution-notices` ve `static/js/symbols/SOURCE.txt` içindedir.

Manuel kontrol: başlıkta seçim/imleç konumuna ekleyip kaydedin; ağaç/başlık/mobil görünümü kontrol edin. Rich-text editöründe ikon/Unicode ekleyip undo/redo ve kaydetme deneyin. User menüsünden salt okunur oturumda kopyalama, filtre/grup/sayfalama ve Escape'i deneyin. Code/monospace örnek ve bilinmeyen işaret metin olarak kalmalı; içerik bağlantısı içindeki ikon PNG popup'ı açmadan bağlantıyı izlemelidir.

Katalog üretimi yalnızca geliştirme içindir: `python packaging/symbols/generate-catalog.py /path/to/font-awesome-7.3.0.jar`. Python 3 ile yerel WebJar ve mevcut CTB dosyalarından üretilir; normal build/çalıştırmada indirme veya üretim adımı yoktur.
