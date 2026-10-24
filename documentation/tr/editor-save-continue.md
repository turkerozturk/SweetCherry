# Kaydet ve devam et

Düz metin ve zengin metin editörlerinde **Kaydet**, **Kaydet ve devam et**, **İptal**
seçenekleri bulunur.

- **Kaydet** mevcut davranışı korur: kaydeder ve düğüm içeriğine döner.
- **Kaydet ve devam et** aynı kayıt servisini kullanır; başarılı kayıttan sonra
  aynı düğümün aynı editörünü yeniden açar. Yeni rich text revision, nesne ID’leri
  ve kayıtlı veriler sunucudan yeniden okunur. Sayfa yeniden yüklendiğinden eski
  undo/redo geçmişi ve imleç konumu korunmaz.
- İzin, salt okunur durum, eski sayfa/revision ve nesne doğrulamaları iki kaydetme
  seçeneğinde aynıdır. Kaydetme hatası başarı gibi gösterilmez.
- İçerik değişmemişse tarih güncellenmez; var olan tarih kuralları geçerlidir.
- İptal ve sayfadan ayrılma uyarıları önceki davranışı korur.

Manuel kontrol: düz metni değiştirip devam ederek kaydedin; aynı editörde yeni
metin açılmalı. Rich text’te resim veya dosya ekleyip aynı işlemi yapın; yeni nesne
artık kayıtlı nesne olarak açılmalı ve sonraki kayıt çalışmalı. Sonra içerikte
hiçbir değişiklik yapmadan kaydedip güncellenme tarihinin aynı kaldığını doğrulayın.

Araç panellerinin yeni düzeni: [Arayüz rehberi](interface-and-import-diagnostics.md).
