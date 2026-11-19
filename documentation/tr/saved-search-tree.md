# Kayıtlı aramaların sanal ağaç dalları

Masaüstü `/tree` görünümünde gerçek düğümlerin üstünde **Kayıtlı aramalar** grubu bulunur. Burayı doldurmak için **Gelişmiş Arama** üzerinden adlandırılmış bir filtre kaydedin. Mevcut kullanıcı/veritabanı kapsamındaki kayıtlı filtreler kullanılır; ikinci bir filtre türü veya CTB içinde sanal düğüm oluşturulmaz.

- Dal açılınca arama çalışır ve eşleşen düğümler listelenir. Dallar başlangıçta kapalıdır; ağaç açıldığında bütün kayıtlı sorgular çalıştırılmaz.
- Her sayfada en fazla 50 sonuç bulunur. **Önceki** ve **Sonraki** sunucudan ilgili sayfayı ister. Filtrenin kayıtlı sayfa boyutu bu dar görünümde kullanılmaz.
- Kayıtlı eşleştirme, tag, tarih, ikon ve sıralama seçimleri korunur. Tarihler mutlak tarihlerdir. Gelişmiş Arama ile aynı sorgu motoru kullanılır; shared occurrence ve sistem yardımcı düğümlerinin davranışı da aynıdır.
- Sonuca tıklamak özgün düğümü açar; ikon, başlık biçimi ve tag işareti korunur. Hover bilgisinde konum yolu görünür. Sonuç bir kopya, yeni üst düğüm veya shared node değildir; gezinme kısayoludur. Sonrasında düzenleme, taşıma ve silme normal izin ve kontrollerle asıl düğüme uygulanır.
- Split view'de seçim etkin panelde açılır. Odaklanılmış sonuç bağlantılarında yön tuşları kullanılabilir; içerik ve editör tuşları etkilenmez. Sanal dal başlıkları ve sayfalama düğmeleri Tab/Enter/Space ile kullanılır.
- Dalı yeniden açmak veya dalın yenileme düğmesi ilk sayfayı tekrar sorgular. Grubun yenileme düğmesi, örneğin başka sekmede filtre değiştirildikten sonra, kayıtlı filtre listesini tekrar okur. Liste yenilenince dallar kapanır; normal düğüm gezinmesinde açık kalırlar. Sonuçlar yeniden sorgulanana kadar son sorgunun anlık görüntüsüdür.

Varsayılan açıktır. Grubu gizlemek ve salt okuma sonuç API'lerini kapatmak için harici `application.yml` içinde aşağıdaki ayarı değiştirin veya `/system/settings` sayfasındaki **Ağaçta kayıtlı aramaları sanal dallar olarak göster** ayarını kapatıp yeniden başlatın:

```yaml
myapp:
  tree:
    saved-searches-enabled: false
```

Bu görünümü kapatmak filtreleri silmez; Gelişmiş Arama üzerinden kaydetme/uygulama devam eder. Kayıtlar mevcut özel `saved-filters/` dizininde, oturum açan kullanıcı ve bağlantı kimliği kapsamında tutulur. Eksik/eski veritabanı belirteçleri ve süresi dolan oturumlar mevcut kontrollerle engellenir. Filtre saklama hataları normal ağacı kaldırmak yerine yalnız bu grubu etkiler. Sonuç satırlarına yalnız metadata yüklenir; metin/BLOB içerikleri taşınmaz. Metin koşullarını değerlendirmek için mevcut arama motoru kayıtlı metni tarayabilir.
