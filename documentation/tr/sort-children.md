# Doğrudan alt düğümleri sırala

Bir düğümün içeriğini görüntüleyin, ardından **Uygulama → Alt düğümleri sırala…** menüsünü açın. Alt menüde oluşturulma, değiştirilme tarihi ve ada göre önce azalan (↓), sonra artan (↑) seçenekleri bulunur. Popup içinde düğüm adı, alt düğüm sayısı ve sıralama ölçütünü onaylayın. Bu onay, oluşturma/çoğaltma/silme onay tercihlerinden bağımsızdır.

Yalnız doğrudan çocukların sırası değişir. Daha alt seviyeler, içerik, resimler, bookmark’lar, master referansları ve çocukların tarihleri korunur. Shared çocukların adı/tarihleri master düğümden okunur; konum kimlikleri değişmez. Shared bir üst düğümün kendi çocukları da sıralanabilir. Eksik tarihler sıfır sayılır; ad sıralaması büyük/küçük harf duyarsız, sözlükseldir; sayısal veya dile özel sıralama yapılmaz. Eşit değerlerde mevcut kardeş sırası korunur.

ADMIN, yazılabilir bağlantı, güncel veritabanı belirteci ve CSRF gerekir. Kilitli üst düğüm, korumalı sistem konteynerleri ve altları reddedilir. Veritabanı kökü veya içerik düğümü seçilmemiş sayfa sıralanamaz. Önce açık editörü kapatın. İkiden az çocuk veya zaten istenen sıradaki liste değişiklik yapmaz. Onay açıkken liste/ad/tarih değişmişse istek reddedilir; yenileyip tekrar deneyin.

Sıra mevcut `children.sequence` değerlerine tek transaction içinde yazılır; şema değişmez. Gerçek sıra değişikliğinde yalnız üst düğümün değiştirilme tarihi güncellenir. CTBLOGS açıksa `tree.sort` kaydı tutulur. Başarıdan sonra seçili içerik ve ağaç yenilenir; split view’da etkin paneldeki düğüm kullanılır. Sıra CTB’ye yazıldığı için CherryTree’de de görünür.
