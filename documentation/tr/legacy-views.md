# Eski düğüm görünümleri ve güncel menüler

Bakımı yapılan düğüm görünümleri artık Masaüstü (`/tree`, `viewMode=tree`) ve Mobil (`/nodes/{id}`, `viewMode=reader`). Görünüm değiştirme seçili gerçek/shared düğümün ağaç ID’sini ve `_tenantView` token’ını korur. Eski `desktop` seçimi `tree`, eski `mobile` seçimi `reader` olarak değerlendirilir. `/changeView` çereze güncel değeri yazar. Bilinmeyen görünüm seçimleri Mobil olarak değerlendirilir.

`node/node.html` ve `node/nodeMobile.html` silinmedi; başlarına RETIRED açıklaması eklendi. HTTP controller’larında bunları render eden dal kalmadı. `/nodes/{id}` ve ana sayfa adresleri ortak olduğundan kapatılmadı; güncel görünümlere hizmet ederler. Workspace içerik hazırlığı özel bir metoda ayrıldı: AJAX içeriği hazırlarken eski görünümü yeniden etkinleştirmek gerekmez. Java’nın `@Deprecated` annotation’ı HTML şablonlarına uygulanamaz. Hâlâ kullanılan ortak controller metotları da deprecated olarak işaretlenmedi.

Bu yaklaşım geçmiş kodu incelemeyi mümkün kılar; ileride şablonlar silinse bile Git geçmişi korunur. Eski şablonlar aktif ekran değildir. Kodun korunması tek başına genel bir güvenlik garantisi değildir; ortak servislerin rol, CSRF, yazılabilir CTB ve sekme token’ı kontrolleri çalışmaya devam eder.

Görünüm menüsünde sırasıyla Masaüstü, Mobil, Düşünce Haritası 1 (Markmap), Düşünce Haritası 2 (Mermaid) ve SweetCherry Özel 1 bulunur. Veri kaynağı gerektiren harita/özel ekran seçenekleri CTB seçildikten sonra görünür. Haritalar önceki davranıştaki gibi yeni sekmede açılır.

Kullanıcı menüsünde kullanıcı adı tıklanamaz bilgi olarak gösterilir. Oturum Bilgisi ve POST ile Oturumu Kapat seçeneklerinin altında Eski Dashboard ve Eski Yardım bulunur. Dashboard’un mevcut ADMIN erişim kuralı korunur; bağlantı da sadece admin için gösterilir. Yardım ve dashboard kodları bu düzenlemede kaldırılmadı; gelecek inceleme için tutuluyor.

Ay fazı göstergesi artık bir düğmedir: tıklama, dokunma veya klavyeyle odaklama önceki/sonraki faz adı ve tarihlerini, ayın aydınlanma oranını ve ayarlanmış zaman dilimini açılan kutuda gösterir. Güneş/ay kutusuyla aynı popover mekanizmasını kullanır. Kutuların bilgileri mevcut offline hesaplama ve önbellekten gelir; harici servis kullanılmaz.
