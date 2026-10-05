# Düğüm çoğaltma

Yeni masaüstü ve mobil görünümde admin + `custom.isWritable=true` için **Çoğalt** menüsü vardır. İşlem onay ister ve yeni düğümün sayfasını açar.

- **Düğümü çoğalt:** Seçilen gerçek düğümü içerik ve özellikleriyle kopyalar; çocuklarını kopyalamaz. Shared node seçiliyse aynı gerçek düğüme yeni bir başvuru oluşturur.
- **Alt düğümlerle çoğalt:** Gerçek düğümü ve onun altındaki ağaç kayıtlarını kopyalar. Shared node için bu seçenek yoktur. Desteklenmeyen bir alt ağaç varsa seçenek pasif olabilir.

Yeni kök, özgün düğümün hemen sonrasına aynı parent altında eklenir. Yeni ID'ler hem `node` hem `children` tablolarındaki en büyük kimliğin üzerinden atanır. Kopyalanan gerçek düğümlerin `ts_creation` ve `ts_lastsave` alanları güncel Unix saniyesidir. Ad, etiket, syntax, renk, ikon, kalınlık ve içerik read-only bayrağı korunur. Read-only içerikli düğüm çoğaltılabilir; özgün içeriği değiştirilmez.

`node.txt` parse edilmeden kopyalanır. `image`, `grid`, `codebox` satırları SQL üzerinden kopyalanır; binary veri, SQL NULL, offset, justification, dosya adı ve diğer metadata korunur. Aynı tablolarda ek CTB sütunları varsa bunlar da kopyalanır. Bookmark'lar ve uygulama ayarları kopyalanmaz.

Alt ağaç içindeki shared node'nin master'ı da kopyalanıyorsa yeni master ID'ye bağlanır. Master dışarıdaysa özgün master'a bağlı kalır. Seçilen alt ağacın dışındaki alias'lar kopyalanmaz. Metindeki internal node linkleri ve image tablosundaki link metadata'sı aynen korunur; bunlar kopya düğümlere otomatik yönlendirilmez.

Çoğaltma tek tenant transaction'ında yapılır. Eksik gerçek düğüm/master, döngü, shared parent, shared node altında çocuk, eski hiyerarşi özeti veya yeni ID için alan kalmaması durumunda işlem reddedilir. Başarısız yazma transaction'ı geri alınır. Admin, writable CTB, CSRF ve güncel `_tenantView` şartları sunucuda doğrulanır. Kaynak içerik işlem sırasında CTB'deki güncel haliyle kopyalanır; özgün içerik hiçbir zaman üzerine yazılmaz.

## Harici değişikliklerin ağaca yansıması

Yeni masaüstü görünümü düğüm seçerken ağacın güncel özetini kontrol eder. CherryTree'deki taşıma, ekleme/silme veya başlık/ikon/renk değişikliği özeti değiştirmişse yüklü dallar yeniden alınır. Açık dallar ve scroll konumu mümkün olduğunca korunur; seçilen düğümün yeni parent yolu açılır. Değişiklik yoksa dallar yeniden yüklenmez. Arka planda polling yapılmaz.

Mobil okuyucuda tam ağaç paneli her açıldığında güncel kayıtlarla yüklenir. Açık dallar korunur. Shared node altında gerçek düğüm bulunan özel durum henüz desteklenmez; `documentation/shared-nodes_tr.md` uyarısına bakın.

## CTB kopyasında manuel test

1. Nesneli bir düğümü tek başına çoğaltın; metin, dosya indirme, resim, çapa, tablo ve codebox'ı karşılaştırın.
2. Gerçek düğümler, dahili ve dış master'lara bağlı alias'lar içeren alt ağacı çoğaltın. Kopya ID'leri ve master bağlantılarını kontrol edin.
3. Shared node'yi çoğaltın; yeni bir gerçek node satırı oluşmamalı, mevcut master ve diğer alias'lar değişmemeli.
4. Onayı iptal edin; yeni kayıt oluşmamalı. Veri kaynağı değiştirildikten sonra eski sekmeden çoğaltma reddedilmeli.
5. CherryTree'de bir düğümü taşıyın; SweetCherry'de düğüme tıklayın. Ağaç yeni yerleşimi göstermeli; değişmeyen ağaçta fold/scroll durumu korunmalı.
6. CTB'yi CherryTree'de açıp kopyaları, sıralamayı ve içeriği doğrulayın.

## Paylaşımlı düğüm oluşturma

Yeni masaüstü ve mobil okuyucularda Çoğalt menüsündeki **Paylaşımlı Düğüm Oluştur**, seçili occurrence'ın hemen ardından aynı parent altında kardeş oluşturur. Yeni node_id hem node hem children kimliklerinden büyük seçilir. Yalnız children kaydı eklenir; master_id gerçek düğümü gösterir. Kaynak zaten paylaşımlıysa onun gerçek master'ı kullanılır; alias zinciri oluşturulmaz. İçerik/obje satırları, zaman damgaları ve bookmarklar kopyalanmaz. Kardeş sıraları normalleştirilir. Yönetici ve yazılabilir tenant gerekir; CSRF, tenant görünüm token'ı ve ağaç revision kontrolü korunur. Paylaşımlı düğüm içeriği master ile ortaktır; bağımsız içerik isteniyorsa gerçek düğüm çoğaltılır. Desteklenmeyen paylaşımlı parent yerleşimleri ve eski ağaç revision'ı reddedilir.
