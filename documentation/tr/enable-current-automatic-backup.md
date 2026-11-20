# Aktif CTB için otomatik yedekleme

ADMIN olarak CTB seçip `/system/backups` sayfasını açın. Mevcut manuel yedekleme kontrollerinin yanındaki **Aktif veritabanını otomatik yedeklemeyi etkinleştir** düğmesi seçili bağlantı adını `myapp.ctb-backup.automatic.tenants` listesine ekler ve `myapp.ctb-backup.automatic.enabled` değerini `true` yapar.

Mevcut aralık, yeniden deneme süresi, saklama sayısı ve diğer veritabanı adları korunur. Ortak otomatik zamanlayıcı açılınca listede önceden bulunan veritabanları da yeniden etkin olur; onay mesajı bunu açıklar. Yapılandırma revision kontrolüyle aktif harici YAML dosyasına kaydedilir, ardından yeniden başlatmadan çalışan zamanlayıcıya uygulanır. Sonraki kontrol bir dakika içinde yapılır; yedek hemen zorlanmaz, mevcut plana göre zamanı geldiyse alınır. Devam eden iş etkilenmez.

ADMIN, CSRF, güncel CTB görünüm belirteci, uygun yerel SQLite kaynağı ve yazılabilir aktif harici `application.yml`/`application.yaml` gerekir. Genel yedekleme özelliği açık olmalıdır. Yapılandırma düzenlenemiyorsa veya seçili ad zaten listedeyken zamanlayıcı açıksa düğme pasiftir. Hatalı/eski revision ile kayıt başarısız olursa çalışan plan değiştirilmez. Planlı bağlantılar mevcut `/tenants` kontrollerinden çıkarılabilir.
