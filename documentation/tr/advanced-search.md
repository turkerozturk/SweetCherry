# Gelişmiş arama

`/nodesadvancedwithbinding` sayfası POST formunu kullanır; ham `node.txt` araması devam eder. CTB şeması, düğüm içeriği ve tarihler değişmez. Masaüstü ve mobil aynı arama formunu kullanır.

## Hızlı kullanım

- **Başlık ve ham içerikte ara:** Birden çok kelime yazın. **Tüm kelimeler**, her kelimenin başlıkta veya içerikte bulunmasını ister; farklı kelimeler farklı alanlarda olabilir. **Herhangi bir kelime**, en az birini bulur. **Tam ifade**, yazılan ifadenin başlık veya içerikte kesintisiz bulunmasını ister.
- **Etiketler:** Boşlukla ayrılmış etiketler yazın veya **Etiket seç** listesinden seçin. Liste içindeki arama yalnızca seçim listesini süzer; **Ara** sonuç sorgusunu çalıştırır. **Tümü / herhangi biri** tam etiketleri arar: `java`, `javascript` ile eşleşmez. **Metin içerir (eski yöntem)** etikette alt metin arar.
- **Tür:** Bir syntax adı yazın veya önerilerden seçin. Başlıklardaki Rich Text / Plain Text adları çevrilir. Tür filtresi geriye uyumlu olarak alt metin eşleşmesi yapar.
- **Diğer filtreler:** Yalnız başlıkta / yalnız ham içerikte arama, başlık başlangıcı, ID ve tarih aralıkları, ikon seçimi bulunur. Boş alan kısıtlama getirmez. Özellikle boş oluşturulma tarihi aralığı geleceğe yerleştirilmiş takvim düğümlerini de kapsar. Bitiş tarihi o günün tamamını kapsar.
- Farklı filtre alanları **VE** ile birleştirilir. Kelime eşleşmesi modu başlık ve içerik alanlarına da uygulanır; **Başlık bu tam ifade ile başlasın** seçiliyse o başlık filtresi tam ifadenin başlangıcını kontrol eder.

Örnek: `raspberry yedek` + **Tüm kelimeler**, başlığında raspberry ve içeriğinde yedek olan düğümü de bulur. `iş java` + **Seçilen tüm etiketler**, her iki etikete sahip düğümleri bulur. Kelime araması parça eşleşmesidir; tam kelime sınırı yalnızca etiketler için uygulanır.

## Sonuçlar ve sıralama

Ad, ID, oluşturulma, güncelleme, etiket veya tür sütun başlığına tıklayarak sıralayın; tekrar tıklamak yönü ters çevirir. Aynı işlem açılır sıralama listeleriyle de yapılabilir. Eşit değerlerde ağaç ID’si kararlı bir ikinci anahtardır. Filtre/sıralama değişikliği ilk sayfaya döner; önceki/sonraki sayfa filtreleri korur. Sayfa boyutu 1–100000 satırdır; varsayılan 25’tir. PDF düğmesi o anda görüntülenen sayfanın aynı filtre/sıralamadaki sonuçlarını aktarır.

Sonuçlar gerçek ve paylaşımlı **ağaç konumlarıdır**. Aynı içerik birden çok yerdeyse her konum ayrı satır olur; ID ve bağlantı o konuma aittir. Ad altında küçük yazıyla kendi ağaç yolu gösterilir. Eksik veya döngülü bir ağaç yolu `?` veya `… ↻` ile sonlandırılır. Düğüm tablosunda olup ağaçta hiçbir konumu bulunmayan yetim kayıtlar bu görünümde listelenmez.

Tarih sütunları kısa başlık ve `yyyy-MM-dd` biçimi kullanır; üzerinde bekleyince saat görünür. Dar ekranlarda tablo kendi içinde yatay kayar.

## Kayıtlı filtreler ve sınırlar

Yeni arama metni, eşleşme seçenekleri, etiket modu ve sıralama kayıtlı filtreye dahildir. Önceki sürüm kayıtları silinmez: yeni alanları olmayan kayıtlar **tam ifade**, **etiket metin içerir** ve **güncelleme azalan** varsayılanlarıyla okunur. Eski kaydı yeni seçeneklerle değiştirip aynı kayda kaydedebilirsiniz. Önceden kaydedilmiş mutlak tarih aralıkları aynı kalır.

Her metin alanı en fazla 2000 karakter, alan başına en fazla 32 arama kelimesi veya etiketi desteklenir. `%`, `_`, `!`, nokta ve SQL benzeri metinler arama metnidir; joker veya SQL olarak çalıştırılmaz. Tam ifade modunda boşlukla kelimelere bölünmez. Syntax alanı en fazla 100 karakterdir. Harf eşleşmesi veritabanının LOWER/LIKE ve dil ayarlarına bağlıdır; özellikle SQLite Türkçe büyük/küçük harfleri her durumda eşitlemez. Tırnaklı ifade, eksiyle hariç tutma veya regex sözdizimi yoktur.

Arama koşulları parametrelerle veritabanında uygulanır. İçerik render edilmez; sonuç listesi için `txt` veya resim BLOB’ları indirilmez. Sayfalandırma sıralamadan sonra veritabanında yapılır. Breadcrumbs için yalnız hiyerarşi/ad metaverisi okunur. İçerikte `%kelime%` tarzı arama yine büyük veritabanında tarama gerektirir; yeni bir tam metin indeksi eklenmez.

## Manuel kontrol

1. Kelimeleri başlık ve içerik arasında dağıtıp üç eşleşme modunu deneyin. Rich-text XML etiketleri ham aramada bulunabilmelidir.
2. `java` / `javascript`, birden çok etiket, boşluklar ve `%_!` karakterlerini deneyin.
3. Birden fazla sayfada sıralamayı, eşit tarihlerde kararlılığı ve PDF’nin geçerli sayfayı aktardığını kontrol edin.
4. Paylaşımlı düğümün ID ve breadcrumbs’ının kendi konumunu açtığını doğrulayın.
5. Eski ve yeni kayıtlı filtreleri uygulayın; nokta eklenmesi/alan çoğalması olmamalıdır.
6. Negatif/taşan ID, ters tarih aralığı ve geçersiz seçenekler sonuç sorgusu yerine doğrulama mesajı vermelidir.
7. SQLite, MariaDB ve PostgreSQL’de; masaüstü ve mobilde deneyin. Arama sonrasında düğüm değiştirilme tarihi aynı kalmalıdır.

Tarih aralıkları ve saat gösterimleri sunucunun saat dilimini kullanır.


Satır sayısı elle girilebilir; önceki 10000/100000 seçenekleri de korunur. Büyük satır sayıları sayfayı ve PDF çıktısını büyütür.
