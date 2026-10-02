# Rich text editör: sonraki adımlar

2 Ekim 2026: İlk görsel editör manuel olarak doğrulandı. Textarea + önizleme alternatifi de
çalışıyor; iki görünüm şimdilik korunacak. İleride alternatif görünüm silinmeden deaktive
edilebilir. Mevcut plain-text paste metin ve Unicode simgelerini alıyor; resim/tablo/link
aktarımı yapmıyor. Tarayıcıda görünen bazı simgelerin CherryTree'de görünümü font desteğine bağlı olabilir.

## Öncelik 1: mevcut ve yeni resimler

- Nesne içeren düğümleri açmadan önce, editör modelinde nesneleri kalıcı kimlikleriyle
  temsil etmek. Metin düzenlenince resim konumlarını yeniden hesaplamak.
- İlk destek PNG resimlerle sınırlı olsun. Aynı düğümdeki diğer nesneler korunmuş,
  düzenlenemeyen nesneler olarak tutulabilmeli; desteklenmeyen durum sessizce atılmamalı.
- Clipboard veya dosyadan resim ekleme; gerekirse PNG'ye dönüştürme.
- Resim seçme/silme; düzenleme sırasında silmeyi undo/redo ile geri getirebilme.
- Veritabanı değişiklikleri yalnızca Kaydet'te ve tek transaction'da uygulanmalı.
  İptal hiçbir binary kaydı değiştirmemeli.
- Metin XML'i, nesnelerin final-buffer offset'leri ve has_image/lastsave gibi metadata
  birlikte tutarlı kaydedilmeli. Başka sekmede değişmiş içerik overwrite edilmemeli.
- CherryTree ile save/reopen ve resim byte/konum kontrolleri yapılmalı.

Image tablosu yalnızca resim içermez: anchor, attachment ve özel ekler de bulunur.
`anchor`, `filename`, binary `png`, `link` ve diğer alanlar birlikte değerlendirilmelidir.
Resim eklemek/silmek aynı tablodaki attachment/anchor kayıtlarını etkilememeli.

## Sonraki resim adımı: web içeriğinden alma

- İnternet sayfasından kopyalanan HTML içindeki resim URL'sini kaynak bilgisi olarak
  korumak ile binary resmi CTB'ye eklemek ayrı işlemler olarak tasarlanmalı.
- URL kaydı tek başına CherryTree'de taşınabilir resim anlamına gelmez; seçilen resmin
  binary içeriği image tablosuna yazılmalı. Kaynak URL CTB'nin mevcut `link` alanıyla
  karıştırılmamalı; bu alan resme atanmış tıklama bağlantısını da temsil edebilir.
- HTML img bilgileriyle resmin alınıp alınamadığı kullanıcıya anlaşılır gösterilmeli.
  Dış kaynak indirme, HTML paste ve yerel dosya/clipboard resmi farklı giriş yollarıdır.

## Daha sonra

- Editör içindeki copy/cut/paste için metin ile biçim modelini birlikte taşıyan clipboard
  formatı. Dış uygulamaya copy düz metin/HTML sunabilir; iç paste bilinmeyen attribute'ları
  korumalı. Cut/paste ve undo/redo birlikte test edilmeli.
- Web HTML paste içinden link ve desteklenen biçimleri CTB modeline dönüştürme.
- HTML tabloyu grid XML'ine dönüştürüp grid tablosuna kaydetme. Header/storage-row sırası,
  hücre metni ve offset'ler korunmalı. İlk aşamada düz metin tablo paste yeterli kabul edilir.
- Tablo düzenleme, codebox düzenleme ve diğer nesne türlerinin oluşturulması daha sonra.

Normal görüntüleme parser'ı, resim zoom/büyüteç, tablo yatay kaydırma ve bağımsız
image/attachment/grid/codebox browse sayfaları korunacak. Bu belge planı kaydeder;
mevcut sürümün nesneli düğüm düzenleme engelini kaldırmaz.
