# Markdown düğümleri / Markdown nodes

## Türkçe

`node.syntax` tam olarak `markdown` olduğunda, düğümün `node.txt` alanı Markdown kaynağı olarak yorumlanır. Kaynak veritabanında **değiştirilmeden düz metin** kalır. `plain-text`, diğer kod dilleri ve rich-text içindeki **codebox nesneleri** bu kurala dahil değildir; codebox.syntax=markdown eski kod gösterimini sürdürür.

### Görüntüleme

Başlıklar, paragraflar, kalın/italik/üstü çizili metin, listeler, görev kutuları (salt okunur), kod blokları, tablolar, resim adresleri ve bağlantılar desteklenir. Normal masaüstü/mobil düğüm, hızlı içerik, shared düğüm, takvim önizlemesi ve şablon veri hücreleri aynı güvenli render katmanını kullanır. Shared konum kendi ağaç kimliğini korur; kaynağı master düğümden alır.

Düğüm/alt ağaç PDF ve şablon PDF çıktılarında da Markdown render edilir. Başlıklar mevcut PDF outline/yer imi mekanizmasını kullanır. Freeplane içerik notları Markdown işaretleri yerine düz metne dönüştürülür; bağlantıların adresleri ve resim açıklamaları eklenir. CTB export/import ve veritabanı senkronizasyonu kaynağı dönüştürmez. İçerik filtresinde arama eskisi gibi **raw node.txt** üzerinde kalır.

Markdown içindeki harici resim adresleri tarayıcı tarafından yüklenebilir. Sunucu Markdown render/önizleme sırasında internetten resim veya başka kaynak **indirmez**. PDF'nin mevcut kaynak güvenlik politikası korunur: gömülü PNG gösterilir, uzak/yerel dosya adresleri PDF'de `[Image not embedded]` olur. Otomatik resim indirme veya dosya sistemi erişimi eklenmemiştir.

### Düzenleme

Mevcut `/nodes/content/{id}` adresi Markdown düğümünde Markdown kaynak editörü açar. Düzenle menüsündeki bağlantı bu durumda **Markdown** adını taşır. Biçimlendirme düğmeleri Markdown işaretleri ekler; kaynağın kendisi textarea'da görülebilir. Önizleme ayrı sütunda, küçük ekranda kaynak alanının altında gösterilir. Otomatik önizleme kapatılabilir veya Yenile ile elle alınabilir.

Önizleme kayıt yapmaz, oluşturulma/güncellenme tarihine dokunmaz. Kaydet ve Kaydet ve devam et mevcut kayıt servisini kullanır; syntax korunur. İçerik gerçekten değişmemişse Güncellenme değişmez. Undo/redo, 33~, sayfadan çıkış uyarısı ve tarayıcı kurtarma taslakları aynı düz metin editörünün parçasıdır. Zengin metin editörü değiştirilmemiştir. Shared konumlar için mevcut gerçek düğümü düzenleme kuralı sürer.

Özellikler sayfasında rich text olmayan bir düğümü Markdown yapmak için mevcut **Kod dili** listesinden `markdown` seçilebilir. Bu bir içerik dönüştürme işlemi değildir; zaten Markdown biçiminde yazılmış kaynağın nasıl gösterileceğini seçer.

### Güvenlik ve kapsam

Flexmark 0.64.8'in çekirdeği ve yalnız tablo/strikethrough/task-list modülleri kullanılır; jsoup 1.23.2 korunur. `flexmark-all` ve EasyMDE eklenmemiştir. Ayrı bir editör eklemek yerine mevcut geçmiş/taslak davranışı korunmuştur. `NodeMarkupRenderer` arayüzü ve `NodeMarkup` kaydı ileride başka node markup syntax'ları eklenebilecek biçimdedir; bilinmeyen syntax'lar mevcut kod görünümünde kalır.

Ham HTML çalıştırılmaz; escape edilir ve sonuç ayrıca Jsoup allowlist'ten geçer. Script, iframe, olay işleyicileri, tehlikeli URL şemaları ve SVG data URI'leri kabul edilmez. Markdown başlık kimlikleri uygulama kontrolleriyle çakışmaması için `md-` öneki alır; yerel başlık bağlantıları buna göre güncellenir. `/nodes/ID` biçimindeki bağlantılara canlı görünümde `_tenantView` eklenir. Markdown içindeki `/nodes/ID` URL’leri CherryTree internal-link nesneleri değildir; CTB import/export ID eşleştirmesine dahil değildir. Harici bağlantılar ayrı sekmede `noopener noreferrer` ile açılır. Önizleme ADMIN, yazılabilir düğüm/kaynak, CSRF ve güncel tenant token kontrollerinden geçer. Desteklenen kaynak üst sınırı **2.000.000 Java karakteridir**; daha büyük kaynak, mevcut düz metin editöründe açılıp küçültülebilir ama render/önizleme 413 ile reddedilir.

### Manuel kontroller

1. Markdown syntax'lı gerçek bir düğüme Türkçe/İngilizce başlıklar, liste, tablo, kod bloğu, görev kutuları, URL ve resim adresi ekleyin; masaüstü/mobil görünümü kontrol edin.
2. Aynı master'a bağlı shared konumu açın. Aynı render, farklı ağaç ID'si ve doğru navigasyon korunmalı.
3. Düzenle → Markdown ile kaynağı açın. Kalın/liste/bağlantı düğmelerini, undo/redo, 33~, önizlemeyi kapatma ve Yenile'yi deneyin. Kaydetmeden önce DB değişmemeli.
4. Kaydet, Kaydet ve devam et, değişiklik yapmadan kaydet ve kurtarma taslağını geri yüklemeyi deneyin. Syntax aynı kalmalı; sadece değişen kaynak Güncellenme'yi etkilemeli.
5. Ham HTML `<script>`/`<img onerror>` veya `javascript:` bağlantısı ile JavaScript çalışmadığını kontrol edin. Başlık/başlık bağlantısı ve `/nodes/ID` bağlantısını deneyin.
6. Takvimde İçeriği göster, düğüm PDF, alt ağaç PDF, Freeplane içerik notları ve şablon veri hücrelerini kontrol edin. Uzak resimler PDF sırasında sunucu tarafından indirilmemeli.
7. Rich text içine syntax=markdown olan bir **codebox** ekleyin; `# Heading` kaynak olarak kalmalı, başlığa dönüşmemeli. Normal plain-text/kod düğümleri değişmemeli.
8. Salt okunur kaynak, USER hesabı ve başka sekmede kaynak değişiminde mevcut korumalar sürmeli; önizleme hatası textarea'yı değiştirmemeli.

Kaynak: https://github.com/vsch/flexmark-java — sürüm ve lisans ayrıntıları `dependency-maintenance.md` ve dağıtımdaki THIRD-PARTY-NOTICES.md dosyasındadır.

## English

Nodes with the exact syntax `markdown` are rendered through a shared node-markup registry; CTB text remains the original Markdown source. Rich-text codeboxes, plain-text/code nodes, raw search and CTB import/export remain unchanged. The renderer covers the normal/shared node views, quick content, calendar preview, template cells, node/subtree/template PDF, and plain-text Freeplane content notes.

The existing plain-text editor becomes a Markdown source editor with formatting tools and a safe, optionally automatic preview. It retains its undo/redo, 33~, save/save-and-continue, unsaved-change guard and browser recovery drafts. Preview does not write anything. Existing ADMIN/writable/CSRF/source-token checks remain in force. Select `markdown` in the existing code-language property list; selecting syntax never converts the stored text.

Flexmark core plus tables, strikethrough and task lists (0.64.8) are used with existing jsoup 1.23.2. Raw HTML is escaped, output is allowlisted, unsafe URLs are removed and task boxes are disabled. No server-side resource fetching is added. Browser images may load external URLs; PDF retains its embedded-PNG-only policy and displays placeholders for external images. Markdown rendering is capped at 2,000,000 Java characters. Larger source can still be reduced in the source editor. Generated heading IDs use an `md-` prefix and safe links retain their behavior. See the Turkish checklist above for acceptance scenarios and the third-party notices for license/source references.
