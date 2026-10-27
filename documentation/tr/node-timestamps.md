# Düğüm tarihleri ve incremental senkronizasyon

Bu kurallar SweetCherry'nin yazma işlemleri içindir; CherryTree ile birebir aynı değildir. Amaç SweetCherrySync gibi araçların değişen içerik gruplarını daha tutarlı bulmasını sağlamaktır. CTB'deki `node.ts_creation` oluşturulma, `node.ts_lastsave` son değişiklik tarihidir. Değerler Unix epoch **saniyesidir**, tarihlerin ekranda gösterimi saat dilimine göre değişebilir.

## Kurallar

| İşlem | Mevcut düğümün son değişiklik tarihi | Yeni düğüm / oluşturulma tarihi |
| --- | --- | --- |
| Düz metin veya kod içeriğini kaydet (`/nodes/content/{id}`) | Yalnız metin değişmişse şimdi | Oluşturulma korunur |
| Zengin metni kaydet (`/nodes/richtext/edit/{id}`) | Metin, biçim, nesne konumu, yükleme/silme veya tablo hücreleri değişmişse şimdi | Oluşturulma korunur |
| İçerikte değişiklik yapıp Geri Al ile başlangıç içeriğine dön, Kaydet | Korunur | Korunur |
| Özellikleri kaydet (`/nodes/properties/{id}`) | Alanlar aynı olsa bile başarılı Kaydet işleminde şimdi | Oluşturulma korunur |
| İptal / editörü açma / görüntüleme / reddedilen işlem | Korunur | Korunur |
| Düğümü taşı (`/nodes/move/{id}`) | Taşınan düğümde şimdi | Oluşturulma korunur |
| Çoğalt (`/nodes/duplicate/{id}`), yalnız veya alt ağaç | Orijinaller korunur | Yeni gerçek düğümlerin iki tarihi de şimdi |
| Ortak CTB'ye veya yeni CTB'ye export; yalnız veya alt ağaç | Kaynakta korunur | Hedefte kopyalanan düğümlerin iki tarihi aynen korunur; yeni kapsayıcının iki tarihi şimdi |
| Kardeş düğüm oluştur (`/nodes/siblings/{id}`) | Seçilen düğümde şimdi; kök olmayan parent da yeni çocuk aldığı için güncellenir | Yeni düğümün iki tarihi de şimdi |
| Alt düğüm oluştur (`/nodes/children/{id}`) | Parent düğümde şimdi | Yeni düğümün iki tarihi de şimdi |
| Kök düğüm oluştur | Başka düğüm güncellenmez | Yeni düğümün iki tarihi de şimdi |
| Bookmark ekle / kaldır / sıra değiştir | Bookmark'ın bağlı olduğu gerçek içerik düğümünde şimdi | Oluşturulma korunur |
| Shared konum oluştur / taşı / sil | İlgili master düğümde şimdi | Master oluşturulma tarihi korunur |

**Shared düğümün `node` tablosunda ayrı satırı ve bağımsız tarihi yoktur.** Seçilen `children.node_id`, `master_id` üzerinden gerçek içerik düğümüne çözülür. Alt ağaç silinirken kaldırılan shared konumların hayatta kalan master'ları da güncellenir. Master silinip dışarıdaki shared konum gerçek düğüme yükseltilirse oluşturulma tarihi korunur, değişiklik tarihi güncellenir. Grubun tamamı silindiyse güncellenecek hayatta kalan satır yoktur.

Çoğaltma farklı bir işlemdir: yeni gerçek içerik kopyalarının tarihleri yenidir; kopyalanan shared konumlar mevcut içerik grubuna bağlı kalabilir. **Çoğaltma orijinallerin tarihlerini değiştirmez.** Shared bir kopyanın kendisine ait yeni tarihler yazılacak `node` satırı yoktur.

Taşıma/ekleme/çoğaltma nedeniyle yalnız sıra numarası yeniden düzenlenen komşuların tarihleri topluca güncellenmez. Bu belge tüm yapısal farkların yalnız `ts_lastsave` sorgusuyla yakalanabileceği anlamına gelmez.

## Değişikliksiz rich text kaydı

XML declaration, attribute sırası veya aynı biçimdeki metnin birden fazla parçaya bölünmesi içerik değişikliği sayılmaz. Metin, biçimlendirme attribute'ları, nesne offset'leri ve tablo hücreleri karşılaştırılır. Hiçbir şey değişmediyse tarih dışında **orijinal XML ve nesne satırları da yazılmaz**. Plain text'in rich text'e dönüştürülmesi içerik türü değişikliğidir ve tarihi günceller. Biçim değiştirmek, metin aynı kalsa bile değişikliktir.

Aynı bookmark'ı tekrar eklemek, zaten kaldırılmış bookmark'ı tekrar kaldırmak veya aynı sıra değerini tekrar kaydetmek tarih değiştirmez. Sahipsiz bookmark kaldırılabilir; olmayan master için satır oluşturulmaz.

## Bookmark sıra güncelleme

Bu taban sürümde arayüzden sıra değiştirme işlemi bulunmuyordu. Yazma servisine ve ADMIN kontrollü `POST /bookmarks/{id}/sequence` endpoint'ine `sequence` parametresiyle sıra anahtarı güncellemesi eklendi. Yer işareti sayfasında artık sıra hücresindeki tutamaçla mouse/dokunma veya klavye kullanılarak toplu sıralama yapılabilir; `POST /bookmarks/order` endpoint’i kullanılır. [Sıralama rehberi](timeline-and-bookmark-order.md). İstek normal oturum, writable tenant, tenant-view ve CSRF kontrollerine tabidir. Pozitif sıra değeri gerekir; eşit sıra anahtarları mevcut node ID ikincil sıralamasıyla gösterilir. Diğer bookmark'lar otomatik kaydırılmaz.

## Manuel doğrulama

Önce ayrı bir CTB kopyası kullanın. SQL kontrolünü DB Browser'da açık yazma transaction'ı bırakmadan yapın; SweetCherry'yi test ederken başka uygulamadan aynı dosyaya yazmayın. Gerçek ve shared parent'lar için `src/test/resources/fixtures/shared-node-tree.sql` dosyasından oluşturulan CTB de kullanılabilir.

```sql
SELECT c.node_id AS occurrence_id, c.father_id, c.sequence, c.master_id,
       n.node_id AS content_id, n.name, n.ts_creation, n.ts_lastsave
FROM children c
JOIN node n ON n.node_id = CASE WHEN COALESCE(c.master_id,0)=0
                              THEN c.node_id ELSE c.master_id END
ORDER BY c.node_id;
```

Her testte önce iki sayısal tarihi kaydedin. Saniye hassasiyeti nedeniyle başlangıç değerinin alındığı andan **en az iki saniye sonra** işlem yapın; görünümdeki dakika bilgisinden değil ham değerlerden karşılaştırın.

1. Düz metin editörünü açıp hiçbir şey değiştirmeden Kaydet: iki tarih aynı. Metni değiştirip geri alarak ilk hâline döndürüp Kaydet: yine aynı. Gerçek değişiklik: yalnız `ts_lastsave` değişir.
2. Rich text'te aynısını yapın. Resim ve tablo içeren düğümü de deneyin. Kutuyu üç kez tıklayıp başlangıç işaretine dönmek değişiklik sayılmamalı. Tabloyu açıp hücreleri değiştirmeden onaylamak tarihi değiştirmemeli.
3. Rich text'te yalnız rengi/kalınlığı, tablo hücresini veya nesne konumunu değiştirin; sonra nesne ekleme/silmeyi deneyin: her gerçek değişiklikte modified güncellenmeli. İptal hiçbir tarihi değiştirmemeli.
4. Özellikleri hiç değiştirmeden Kaydet: modified değişmeli. İptal: değişmemeli.
5. Gerçek ve shared konumu yukarı/aşağı/içeri/dışarı taşıyın: seçilenin gerçek içerik satırında modified değişmeli, creation korunmalı. Geçersiz veya stale istek yazmamalı.
6. Yalnız ve alt ağaç çoğaltmayı deneyin: yeni gerçek satırlarda iki tarih eşit ve yeni; kaynak satırların tarihleri aynı. Shared konumlarda ayrı tarih olmadığını dikkate alın.
7. Dört CTB export seçeneğini deneyin. Kaynak ve hedefte karşılık gelen içeriklerin iki tarihi aynı olmalı. ID eşleştirmesi değişebileceği için yalnız node ID ile karşılaştırmayın.
8. Gerçek/shared konumdan kardeş ve alt düğüm oluşturun. Seçilen/orijinal içerik satırının modified değeri ve yeni düğümün iki tarihi kontrol edilmeli. Shared parent kendi ağacını korumalı.
9. Bookmark ekleyin, kaldırın, tekrar ekleme/kaldırma deneyin. Gerçek değişiklikte master modified değişmeli; tekrar istekleri tarih değiştirmemeli. Sıra endpoint'ini test eden kişi aynı ve farklı sequence değerlerini de deneyebilir.
10. Shared oluşturun, taşıyın, silin. Master modified değişmeli. Alt ağaç silme ve master terfisi senaryolarında hayatta kalan grupları ve creation değerlerini kontrol edin.
11. Aynı kontrolleri uygun test veritabanlarıyla SQLite, MariaDB ve PostgreSQL üzerinde tekrarlayın.

## SweetCherrySync açısından sınırlar

- Aynı saniyede birden fazla gerçek değişiklik aynı zaman değerini alabilir. Yapay olarak geleceğe tarih atılmaz; sync sınırdaki eşit zamanları da yeniden değerlendirmeli veya karşılaştırma/hash kullanmalıdır.
- Silinmiş düğümlerin tarihi okunamaz. Silmeleri bulmak için hedef/kaynak kimlik karşılaştırması gibi ayrı mekanizma gerekir.
- `children` ve `bookmark` ayrı tablolardır. Sıra normalizasyonu, çoğaltma, export ile eski tarihli yeni kayıt eklenmesi ve dış uygulama yazmaları için yalnız `node.ts_lastsave > son_zaman` filtresi yeterli değildir. Yapısal tabloların/kimliklerin ayrıca karşılaştırılması gerekir.
- Saatin geri alınması veya makineler arası saat farkı watermark tabanlı sync'i etkileyebilir. Bu yama SweetCherrySync'in algoritmasını değiştirmez ve otomatik yedek özelliği eklemez.
