# Düğüm taşıma

Yeni masaüstü ağaç görünümü ve mobil okuyucuda, admin kullanıcı writable CTB üzerinde dört yön düğmesini kullanabilir. Düğümün içerik read-only bayrağı yapısal taşımayı engellemez; içerik düzenlenmez.

| Yön | İşlem |
| --- | --- |
| Yukarı / aşağı | Aynı parent altındaki önceki / sonraki sibling ile yer değiştirir. |
| Sağ | Önceki sibling gerçek düğüm ise onun son çocuğu olur. |
| Sol | Parent'inin hemen sonrasına, onun sibling'i olarak çıkar. |

Masaüstünde `Alt+Shift+Yön` seçili içerik düğümü için aynı işlemi yapar. Metin giriş alanlarında çalışmaz. Tarayıcı veya işletim sistemi kısayolu yakalarsa düğmeler kullanılabilir. Mobilde seçenekler işlemler panelindedir. Geçersiz yönler pasiftir; seçenekler yüklenemezse sayfayı yenileyin.

Shared node kendi `children` kaydıyla taşınır; master ve diğer alias'lar taşınmaz. Shared node parent olamaz. Gerçek düğüm taşındığında alt ağacı ona bağlı kalır.

İşlem yalnızca `children.father_id` ve `children.sequence` alanlarını günceller. Etkilenen sibling gruplarında sıra 1'den başlayarak yeniden numaralanır. İçerik, bookmark, master bağlantısı ve içerik kayıt zamanları değişmez. Taşıma sonrasında aynı düğüm yeni görünümde açılır; ağaç yeniden yüklenir. Ek bir hedef seçerek taşıma yöntemi şimdilik planlanmıyor. Tek düğüm ve alt ağaç çoğaltma ayrı Çoğalt menüsündedir.

POST isteği admin yetkisi, writable CTB, CSRF ve `_tenantView` denetimlerine tabidir. Hiyerarşi özeti eskiyse taşıma reddedilir. Parent zincirindeki eksik kayıt, döngü veya alias altında child varsa taşıma uygulanmaz. Aynı CTB'yi başka uygulamayla eşzamanlı düzenlemek yerine, değişiklik sonrası görünümü yenileyin.

## CTB kopyasında manuel kontrol

- Top-level sibling'leri yukarı/aşağı taşıyın; ilk/son sınırları kontrol edin.
- Sağ ile önceki gerçek sibling altına, sol ile parent sonrasına taşıyın.
- Alt düğümleri olan gerçek düğümü taşıyın; içerik ve alt ağacın korunduğunu doğrulayın.
- Shared node taşıyın; master yerinde kalmalı, shared node altına taşıma pasif olmalı.
- Eski sekmeden taşıma ve veri kaynağı değişimi sonrası taşıma reddedilmeli.
- CTB'yi CherryTree ile açıp sıralamayı, bookmark'ları ve nesneli içerikleri kontrol edin.
