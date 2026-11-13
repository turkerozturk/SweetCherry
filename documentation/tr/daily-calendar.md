# Günlük takvim

Görünüm menüsünden veya takvim araç çubuğundan **Günlük takvim** seçilir. Hafta/ay görünümündeki tarih başlığına tıklayarak da o güne geçilebilir. Adres `/nodes/calendar/day?date=2026-11-13`; kısa `/calendar/day` adresi de desteklenir.

Kartlar Oluşturulma zamanına göre soldan sağa, yukarıdan aşağı sıralanır. Geniş ekranda üç, tablette iki, mobilde tek sütun vardır. Boş saat kutuları gösterilmez. Her kartta saat, ikon ve düğüm başlığı stili, düz metin önizlemesi ve varsa ek dosya/görsel/tablo/kod kutusu adetleri bulunur. Başlık mevcut takvim diyaloğunu açar; **Düğümü aç** bağlantısı tam nota gider. Etiket filtreleri ve tarih değiştirme kontrolleri korunur.

`application.yml` içindeki mevcut `myapp.calendar` bölümüne şu ayarlar eklenebilir; ardından uygulama yeniden başlatılır:

```yaml
myapp:
  calendar:
    day-max-entries: 300
    day-preview-max-characters: 600
    day-preview-max-lines: 8
```

Aynı YAML bölümünü ikinci kez oluşturmayın. Ayarlar yoksa varsayılanlar kullanılır. Karakter sınırı 1–20000, satır sınırı 1–100, günlük kart sınırı 1–2000 aralığına alınır. Günlük kart sınırı ayrıca mevcut `max-entries` ile sınırlanır. Emoji bölünmez; kısaltma işareti karakter sınırına dahildir. Satır sınırı hem kaynak satırlarına hem tarayıcıdaki sarılmış satırlara uygulanır.

Her farklı ana düğüm için en fazla 65536 kaynak karakteri okunur. Görsel ve ek dosya BLOB'ları, tablo gövdeleri ve kod kutusu metinleri önizleme için alınmaz. Paylaşılan düğümler ana düğümün aynı önizlemesini ve adetlerini kullanır. Rich-text anchor kayıtları görsel sayılmaz. HTML/Markdown ve zengin metin yalnızca okunabilir düz metne çevrilir; kartlarda ham HTML çalıştırılmaz ve uzak kaynak yüklenmez. Geçersiz/yasaklı XML için açıklayıcı mesaj gösterilir.

Büyük HTML/Markdown kaynaklarında metin içindeki görsel/tablo sayımı yalnızca incelenen bölümü kapsar; kartta bu sınırlama belirtilir. Veritabanındaki nesne adetleri tam sayılır.

Gün sınırı `myapp.calendar.time-zone` saat dilimindeki iki gece yarısıdır; yaz saati geçişlerinde 23/25 saatlik günler doğru ele alınır. Veritabanı şeması değişmez; ay/hafta görünümleri yalnızca metaveri okumaya devam eder.
