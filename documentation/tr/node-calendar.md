# Düğüm takvimi / Node calendar

## Türkçe

**Görünüm → Haftalık takvim / Aylık takvim** mevcut düğümleri `node.ts_creation` (Oluşturulma) zamanına göre gösterir. Bu ayrı bir randevu tablosu değildir; yeni tablo, süre veya bitiş tarihi eklenmez. Güncellenme zamanı (`ts_lastsave`) takvim yerleşimini belirlemez.

- Aylık görünüm altı haftalık bir ızgaradır. Günün içindeki liste dikey kayar; uzun başlıklar kutuyu genişletmez. Haftalık masaüstü görünümü 24 saatlik satırlara ayrılır; aynı saatteki çok sayıda öğe kendi hücresinde kaydırılır.
- Mobilde ay görünümü ay içindeki gün kartlarına, hafta görünümü yedi günlük bir listeye dönüşür. Masaüstü ızgarası telefona sıkıştırılmaz.
- Önceki/Sonraki, Bugün, tarih kutusu ve ay/hafta bağlantıları ile gezinilebilir. Etiket seçilmemişse **etiketsiz düğümler dahil tümü** gösterilir. Seçilen etiketler mevcut etiket kurallarına göre tam ve büyük/küçük harfe duyarlı eşleştirilir; virgül içeren bir etiket bölünmez. Herhangi biri/hepsi seçimi vardır. Filtreler tarih gezintisinde korunur.
- Takvimde içerik veya resim BLOB’ları topluca okunmaz. Düğme ikon, başlık rengi ve kalınlık özelliklerini taşır; üzerine gelindiğinde tam başlık ve ID gösterilir. Tıklama/dokunma diyaloğu tam başlığı ve kimliği gösterir. İçerik yalnız **İçeriği göster** seçildiğinde yüklenir; zengin metinde mevcut güvenli görüntüleme hattı, düz metin/kodda kaçışlı metin kullanılır. Önizleme metin sınırı 2.000.000 karakterdir; daha büyük içerik için düğümü açın.
- **Düğümü seçili görünümde aç** mevcut masaüstü/mobil tercihini kullanır. **Masaüstü ağacını aç** doğrudan `/tree` sayfasını açar. İkisi de ayrı sekmededir; görünüm tercihi sessizce değiştirilmez. Kapat düğmesi yalnız diyaloğu kapatır, düğümü silmez.

### Oluşturulma tarihini değiştirme

Diyalogda yıl, ay, gün, saat ve dakika girilebilir (0001–9999 yılları). Saat dilimi sayfada yazılıdır. Yalnız ADMIN ve yazılabilir kaynak/düğüm izin verir. Salt okunur içerik bayrağı da kontrol edilir. Gerçek bir değişiklikte `ts_creation` girilen zamana, `ts_lastsave` şimdiki Unix saniyesine güncellenir. İçerik, etiketler, özellikler ve ağaç ilişkileri değişmez. Böylece artımlı senkronizasyon güncellenmiş düğümü fark edebilir; SweetCherrySync kodu bu yamada değiştirilmemiştir.

Dakika değeri aynıysa, görünmeyen mevcut saniyeler korunur ve Güncellenme değişmez. Başka bir sekme düğümün oluşturulma/güncellenme zamanını değiştirmişse kayıt reddedilir; takvimi yenileyin. Diğer yazma işlemlerinde olduğu gibi oturum, CSRF ve `_tenantView` kaynağı kontrol edilir. Kaynak değiştirilmiş veya sunucu/oturum erişilemezse diyalog hata gösterir; başka veritabanına yazılmaz. Zaman aşımında işlem tamamlanmış da olabilir: tekrar denemeden önce sayfayı yenileyip sonucu kontrol edin.

**Paylaşımlı konumlar kendi ağaç ID’leriyle ayrı öğelerdir; oluşturulma ve güncellenme zamanı ortak gerçek düğümde tutulur. Birinin tarihini değiştirmek gerçek düğümü ve aynı içeriğe bağlı tüm paylaşımlı konumların takvim yerleşimini değiştirir.** Diyalog bu durumu bildirir. Asıl düğümden yapılan tarih değişikliği de aynı etkiyi yapar. Ağaç ID’leri veya master ilişkileri değiştirilmez.

### Ayarlar

`application.yml` içinde, uygulamayı yeniden başlattıktan sonra geçerli olur:

```yaml
myapp:
  calendar:
    first-day-of-week: MONDAY # 1=Pazartesi ... 7=Pazar; İngilizce gün adı da olur.
    time-zone: system        # JVM/işletim sistemi; örnek: Europe/Istanbul
    max-entries: 10000       # Aralık başına ağaç konumu; 1..50000
```

Varsayılan hafta Pazartesi başlar; `SUNDAY` veya `7` Pazar yapar. `0` desteklenmez. Geçersiz gün/saat dilimi ayarında bir uyarı yazılır ve Pazartesi/sistem saat dilimi kullanılır. İki cihazda aynı görünüm isteniyorsa aynı açık saat dilimini ayarlayın; tarayıcının saat dilimi yerine bu değer kullanılır. Sayfa üstündeki saat dilimi kaydı da açıklar.

Yaz saati geçişindeki var olmayan yerel saatler reddedilir. Aynı yerel saatin iki kez yaşandığı geçişte ilk/erken UTC ofseti kullanılır. Örneğin Europe/Berlin 2026-10-25 02:30, UTC+02:00 olarak yorumlanır. Ekrandaki takvim yerel gün sınırlarını kullanır; veritabanı Unix saniyesi tutmaya devam eder.

Aralık sınırı aşılırsa ilk öğeler gösterilir ve görünür uyarı çıkar; etiketlerle daraltın veya `max-entries` değerini bilinçli artırın. Tarih 0 olan eski düğümler yerel saat dilimine göre 1970 civarında görünür. Takvimde sürükle-bırak ve silme işlemi yoktur; tarih yalnız açık diyalogdaki Kaydet ile değişir.

### Manuel kontroller

1. Düz ve zengin metin, kod, paylaşımlı ve etiketsiz düğümlerle ay/hafta görünümünü açın. Tam başlık, ikon, renk/kalınlık ve kendi konum ID’sini kontrol edin.
2. Aynı güne ve saate çok sayıda öğe koyun. Ay kutuları ve saat hücreleri büyümemeli; liste dikey kaymalı, yatay taşmamalı. Uzun başlığı tıklayıp okuyabilmelisiniz.
3. Diyalogda içeriği açın; resim/LaTeX/tablo ve bağlantıları kontrol edin. İki görünüm bağlantısı yeni sekmede doğru ağaç konumuna gitmeli.
4. Tarihi geçmiş ve gelecekteki bir güne/saatine değiştirin. Oluşturulma değişmeli, Güncellenme şimdi olmalı; içerik ve ağaç ilişkileri aynı kalmalı. Aynı dakikayı kaydetmek ve Kapat değişiklik yapmamalı.
5. Bir paylaşımlı konumun tarihini değiştirin; gerçek düğümün ve diğer paylaşımlı konumların da aynı tarihi aldığını kontrol edin.
6. Salt okunur kaynak/düğüm ve USER hesabında görüntüleme çalışmalı; tarih düzenleme sunulmamalı/yetkisiz POST reddedilmeli.
7. Başka sekmede kaynak değiştirip eski diyalogdan kaydetmeyi deneyin; yeni kaynak etkilenmemeli. Aynı düğümü başka sekmede değiştirdikten sonra eski takvimden kaydetmek çakışma vermeli.
8. Etiket filtresinde `Tag`/`tag`, `one,two`, herhangi biri/hepsi ve seçim olmamasını kontrol edin. Tarih gezintisinde seçim korunmalı.
9. `first-day-of-week: 7` ve `time-zone: Europe/Istanbul` ile yeniden başlatıp ilk sütunu ve yerel gün/saat yerleşimini kontrol edin. Mobilde günlük kartları deneyin.

## English

**View Mode → Weekly calendar / Monthly calendar** places existing tree occurrences by `node.ts_creation`, never by Modified. The desktop month uses a six-week grid with independently scrolling days; the desktop week uses hourly cells. On mobile, both become day lists. Titles truncate inside cells and expand in a dialog. Optional exact tag filtering supports ANY/ALL; no selection includes untagged nodes as well.

The dialog loads content only on request and opens the node’s preferred view or its desktop tree in a new tab. ADMIN users can change year/month/day/hour/minute when both source and node are writable. A real change updates Created and sets Modified to now for incremental sync detection; an unchanged minute preserves existing seconds and Modified. No content, hierarchy or schema is changed. Shared occurrences retain their own tree IDs but **all share the original node’s timestamps**. Their calendar date changes together.

`myapp.calendar.first-day-of-week` accepts ISO numbers 1=Monday through 7=Sunday or English day names (default MONDAY); 0 is not supported. `time-zone` defaults to `system` or accepts an IANA zone such as `Europe/Istanbul`. The zone is displayed, used for both placement and editing, and is independent of the browser zone. DST gaps are rejected and overlaps use the earlier offset. Restart after changing configuration. `max-entries` defaults to 10000 (clamped to 1..50000) with an explicit truncation warning.

The standard source-token, role, read-only and CSRF checks protect writes; Created/Modified comparisons reject stale updates. A server/session/network failure keeps the dialog open. After a timeout, reload to check whether the write completed. Years 0001–9999 are accepted subject to the database’s timestamp capacity. This patch does not change SweetCherrySync; the updated Modified value lets a synchronization process discover the edit.
