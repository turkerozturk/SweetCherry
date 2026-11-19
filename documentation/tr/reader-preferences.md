# Okuyucu tercihleri ve yeniden başlatma

**User** menüsünde yatay ayıracın altında üç bağımsız onay anahtarı bulunur. Üçü de varsayılan olarak açıktır:

- Oluşturma: kardeş, çocuk, kök ve shared node oluşturma.
- Çoğaltma: tek düğüm veya alt düğümleriyle birlikte çoğaltma.
- Silme: düğüm silme onay sayfası; yapılandırılmışsa çöp kutusuna taşıma dahil.

Tercihler değiştirilene kadar bu tarayıcıda saklanır ve sekmeler arasında eşitlenir. Tarayıcı depolaması engelliyse değişiklik yalnız mevcut sayfada geçerlidir. Bu anahtarlar düğüm işlemlerinin onayını etkiler; export, yedek, veritabanı, kapatma ve kaydedilmemiş editör değişikliklerinin uyarıları mevcut davranışını korur.

Silme onayı kapalıyken açıkça verilen Sil komutu, mevcut hazırlık sayfasına gider ve POST formunu otomatik gönderir. Tenant belirteci, CSRF koruması, writable/admin ve çöp kutusu revision kontrolleri devam eder. İşaretlenmemiş silme URL’sini açmak kendiliğinden form göndermez. Geçici hızlı silme URL belirteci gönderimden önce kaldırılır; sayfayı yenilemek otomatik gönderimi tekrarlamaz.

**Düğüm içeriği arka plan rengi** seçeneği renk seçici ve aranabilir standart CSS renk adlarını içeren pencereyi açar. Hex değerleri de yazılabilir (`#rgb`, `#rgba`, `#rrggbb`, `#rrggbbaa`). Uygula ile ağaç, mobil okuyucu, hızlı okuyucu ve bağlı içeriklerde hemen görünür. Kaydedilen tarayıcı tercihi uygulama varsayılanından önceliklidir; **Uygulama varsayılanını kullan** bu tercihi kaldırır. İçerikte yazılmış metin renkleri, vurgular ve gömülü nesneler korunur. Editör ve PDF çıktıları etkilenmez.

Uygulama varsayılanı **System settings** sayfasından veya mevcut YAML eşlemesinden ayarlanabilir:

```yaml
myapp:
  appearance:
    node-background-color: ivory # Veya "#fff8dc" gibi tırnaklı hex değeri.
```

Yapılandırma dosyasındaki değişiklik yeniden başlatmadan sonra uygulanır. Geçersiz yapılandırma rengi beyaza döner; ayar editörü geçersiz değeri reddeder. Tarayıcı menüsündeki renk tercihi yeniden başlatma gerektirmez.

**Restart SweetCherry** sonrasında durum sayfası, süresi dolan oturum çerezini göndermeden küçük bir readiness endpoint’ini yoklar. Yeni uygulama örneğinin başlangıcı bitince ana sayfayı otomatik açar. Yeniden başlatma oturumları kapattığı için giriş gerekebilir. Başlangıç uzun sürerse veya başarısız olursa beklemeye devam eder ve elle açılabilecek ana sayfa bağlantısını gösterir.

Mind-map ve ZIP import dosya alanlarının Göz at/Dosya seçilmedi metinleri uygulamanın seçili dilini kullanır. Açılan yerel dosya seçme penceresinin dili işletim sistemi/tarayıcı tarafından belirlenir. JavaScript kapalıyken özgün yerel dosya alanı kullanılabilir.

## Ağaçta klavye ile gezinme

`/tree` üzerinde düğüm adına tıklayın veya Tab ile odaklanın. Yukarı/Aşağı önceki/sonraki görünür düğümü, Home/End ilk/son görünür düğümü açar. Sağ kapalı dalı açar; dal açıksa ilk çocuğa gider. Sol açık dalı kapatır; dal kapalıysa üst düğüme gider. Odak ağaçta kalır.

Yalnız ağaçtaki düğüm adı bağlantılarında basılan değiştiricisiz tuşlar işlenir. İçerik kaydırma, metin seçimi, düğmeler, editörler, pencereler ve tarayıcı kısayolları normal davranışını korur. Mevcut Alt+Shift+ok taşıma komutları değişmez. Rich text başlangıcındaki boş satırlar kayıtlı olduğu gibi gösterilir.
