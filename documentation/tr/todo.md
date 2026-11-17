# Görevler (TODO): ilk aşama

**View Mode → Görevler (TODO)** veya `/nodes/todo` adresini açın.

- Mevcut bir düğümün etiketlerine küçük harflerle tam olarak `todo` ekleyin.
  Düğüm özellikleri veya mevcut hızlı etiket düzenleyicisi kullanılabilir.
- `todo` içeren, `done` içermeyen düğüm açık görevdir.
- **Tamamlandı işaretle**, `todo` ve diğer etiketleri koruyarak `done` ekler.
- **Yeniden aç**, yalnız `done` etiketini kaldırır.
- `done` tek başına görev oluşturmaz. `TODO` ve `todo-later`, `todo` yerine geçmez.
- Görev listesinden çıkarmak için düğümün `todo` etiketini kaldırın.

Açık / tamamlanmış / tümü filtreleri, düğüm adı–etiket–ID araması ve sayfalama
vardır. Sıralama düğüm adına göredir; eşitliklerde içerik ID kullanılır.
Üstteki toplamlar arama filtresinden bağımsızdır.

Aynı içeriğin paylaşılan yerleşimleri tek görev sayılır. Etkin yerleşim sayısı
ayrıca gösterilir. Bağlantı öncelikle gerçek düğümü, o yerleşim çöp kutusundaysa
ya da erişilebilir bir ağaç yolu yoksa etkin paylaşılan yerleşimi açar.
Çöp kutusu ve CTBLOGS dallarının altındaki yerleşimler, ağaçta yer almayan
kayıtlar ve bozuk/döngülü yollar listeye alınmaz. Başka bir etkin yerleşim varsa
aynı içerik görev olarak kalır. Paylaşılan görevde tamamlanma tüm yerleşimlere
aynı içeriğin etiketleri üzerinden yansır.

USER ve salt okunur veri kaynağı listeyi görebilir. Durum değişikliği ADMIN,
yazılabilir veri kaynağı, CSRF ve geçerli tenant belirteci gerektirir. Etiket
alanı ve değiştirilme zamanı için revizyon kontrolü vardır; eski form başka
bir düzenlemeyi ezmez. İşlem yalnız etiketleri ve değiştirilme zamanını
günceller; içerik, nesneler ve oluşturulma tarihi korunur. Etiket alanı mevcut
etiket düzenleyicisiyle aynı kurallarla normalleştirilir. CTBLOGS etkinse görev
durumu değişiklikleri `todo.update` olarak aynı işlem içinde kaydedilir.

Bu ilk aşamada son tarih, öncelik, tekrar veya alarm alanı oluşturulmaz.
Sonraki aşama: düğüme bağlı hatırlatıcı formu ve yaklaşan/geciken kayıtlar;
ardından yıllık tekrar, ortak takvim ve görsel/sesli alarm.

## Kontrol

```bat
mvnw.cmd -Dtest=TodoServiceTest,TodoControllerTest,TodoSecurityTest,TodoTemplateTest,AuditIntegrationTest test
mvnw.cmd clean package
```

Manuel deneme: bir düğüme `todo proje` ekleyin, listeden tamamlayın,
tamamlanmış filtresinden yeniden açın. Paylaşılan düğümün tek satır olduğunu,
USER ve salt okunur kaynakta işlem düğmelerinin olmadığını kontrol edin.
Başka sekmede etiketleri değiştirdikten sonra eski formun reddedildiğini,
başka CTB seçilince eski sayfadan işlem yapılamadığını ve CTBLOGS açıksa
durum değişikliğinin geçmişe yazıldığını doğrulayın.
