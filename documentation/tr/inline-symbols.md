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

İkonlar web üzerindeki düğüm adı görünümlerinde açılır: ağaç/mobil okuyucu, breadcrumb, bookmarks, gelişmiş arama, attachments/galeriler, takvim, TODO/hatırlatıcılar, raporlar ve hedef düğüm seçicileri. Mermaid/Markmap adları yerleşim hesaplanmadan önce dönüştürür; link grafiği yerel canvas etiketleri kullanır. Asenkron gezinme ve başlık değişiklikleri izlenir. Normal DOM ikonları çevresindeki metin boyutunu, Font Awesome metin rengini izler; CTB PNG kendi rengini korur. Tarayıcının sekme başlığı, native tooltip ve select option'larına ikon yerleştirilemediği için işaret yerine okunabilir `[İkon adı]` gösterilir. Girdi/kaynak alanlarında saklanan metin korunur.

Düğüm, alt ağaç, rapor ve filtrelenmiş liste PDF'lerinde de işaretler açılır. Solid/Regular/Brands için yerel fontlar, CTB için PNG'ler PDF'e gömülür; alt ağaç başlıkları ve içindekiler dahildir. Kod/pre örnekleri ve rich grid metinleri değiştirilmez; tablodaki açıkça düğüm adı olarak işaretlenen hücreler dönüştürülür. Gerçek Unicode karakterleri metin kalır ve tarayıcı/PDF fontlarının desteğine bağlıdır; emoji görsel paketi eklenmez.

Gösterim veritabanını değiştirmez; image tablosunda kayıt veya gömülü nesne oluşturulmaz. Tanınan ikon işaretleri **yalnızca düğüm adından dosya/klasör adı üretilirken** temizlenir: text-tree ZIP klasör/dosyaları, PDF ve mind-map indirme dosyalarının adları. Boş ad yedekleri ve çakışma çözümü korunur. Gerçek Unicode ile ilgisiz `{{ifadeler}}` silinmez. CTB içeriği, XML, metin/mindmap içerikleri, arama/sıralama ve CherryTree masaüstü gösterimi saklanan ad ve işaretleri korur.

Scriptsiz record-view çerçevelerindeki ad placeholder'ları yerel PNG olarak gömülür; mevcut data-only CSP korunur. Placeholder'dan eklenen düğüm adları tekrar rapor şablonu olarak değerlendirilmez.

## Düzenleme ve düz metin örnekler

Düzenlerken imleç/model/biçimlendirme bozulmasın diye işaretler metin olarak kalır; seçici önizlemesinde gerçek ikon görülür. Rich-text ekleme normal run/undo modelini kullanır ve tek undo/redo adımıdır. Korunan gömülü nesneler veya değişmiş/meşgul içerik bu yolla üzerine yazılamaz. Adın mevcut 255 UTF-16 birimlik sınırını aşan ekleme reddedilir.

Bir işareti aynı biçimlendirmeyle tutun; bitişik eş biçimli span’ler de desteklenir. İşaretin yarısını bold/başka renk/link yaparak span'lere bölmek tanınmasını engelleyebilir; gerekirse seçiciden yeniden ekleyin. Plain/code/Markdown/HTML düğüm içerikleri kapsam dışında; rich içerikteki pre/code/monospace ve tablo/grid metinleri de düz kalır. İkon işaretini örnek olarak gösterecekseniz code/monospace kullanın. Editör yüzeyleri ve seçici içi dönüştürülmez.

Salt okunur kullanıcı seçiciden kopyalayabilir; kaydetme için mevcut ADMIN/yazılabilir CTB/güncel tenant kontrolleri geçerlidir. Clipboard API kullanılamazsa normal kopyalama denenir; o da olmazsa Ctrl+C için değer seçilir. Pano okunmaz, dış ikon/font servisi veya yeni kütüphane indirilmez. Mevcut yerel WebJar fontları ve CTB PNG'leri kullanılır; kaynak/lisans bilgileri `distribution-notices` ve `static/js/symbols/SOURCE.txt` içindedir.

Manuel kontrol: başlıkta seçim/imleç konumuna ekleyip kaydedin; ağaç/başlık/mobil görünümü kontrol edin. Rich-text editöründe ikon/Unicode ekleyip undo/redo ve kaydetme deneyin. User menüsünden salt okunur oturumda kopyalama, filtre/grup/sayfalama ve Escape'i deneyin. Code/monospace örnek ve bilinmeyen işaret metin olarak kalmalı; içerik bağlantısı içindeki ikon PNG popup'ı açmadan bağlantıyı izlemelidir.

Katalog üretimi yalnızca geliştirme içindir: `python packaging/symbols/generate-catalog.py /path/to/font-awesome-7.3.0.jar`. Python 3 ile yerel WebJar ve mevcut CTB dosyalarından üretilir; normal build/çalıştırmada indirme veya üretim adımı yoktur.

Ek kontroller: her iki mind map, link graph, bookmarks, arama ve attachments üzerinde ikonlu adı inceleyin. Her FA stili ve CTB işaretiyle düğüm/alt ağaç/liste PDF'i alın. Yalnızca ikon içeren ve temizlendiğinde çakışan adlarla text-tree ZIP/mindmap export deneyin. PDF belge başına en fazla 5.000 satır içi ikon işler.

Sunucu kataloğu da aynı script ile üretilir. Font kopyalarını yeniden oluşturmak için `--pdf-fonts` ekleyin; bu yalnızca geliştirmede kullanılan dönüşüm `fonttools[woff]` (Brotli) gerektirir. Uygulamanın derlenmesi/çalışması için Python veya ek Java bağımlılığı gerekmez.
