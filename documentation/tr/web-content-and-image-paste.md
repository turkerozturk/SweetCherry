# Pano resimleri ve URL’den içerik ekleme

## Ekran görüntüsü yapıştırma

Zengin metin editöründe imleci yerleştirip Ctrl+V kullanın. SweetCherry hem panonun `items` hem de `files` listesindeki PNG/JPEG resmi kontrol eder. Panoda aynı resmin HTML, metin ve dosya biçimleri birlikte bulunabilir. SweetCherry’nin kendi metin/biçim kopyası önceliklidir; yalnız resim içeren panoda resim yolu seçilir. Web sayfasından biçimli metin/tablo yapıştırma yolu korunur.

Araç çubuğundaki **📋 Panodan resim yapıştır** düğmesi tarayıcının izin isteyen Clipboard API’sini ayrıca deneyebilir. Kullanıcı tıklaması, HTTPS veya localhost ve tarayıcı desteği gerekir. Normal Ctrl+V, aynı API’ye bağlı değildir; yerel ağda HTTP üzerinden de denenebilir. İzin verilmezse veya desteklenen resim yoksa açıklama gösterilir. Birden çok resim temsili varsa ilk desteklenen resim kullanılır; aynı ekran görüntüsü iki kez eklenmez.

**Flameshot için kesin uyumluluk garantisi yoktur:** işletim sisteminin yerel pano biçimlerini tarayıcı PNG/JPEG olarak sunmuyorsa web uygulaması bunları okuyamaz. CherryTree gibi yerel bir uygulamanın okuyabilmesi, web tarayıcısının da aynı biçime erişeceği anlamına gelmez. Pano boş/uyumsuzsa tarayıcının bildirdiği biçim adları açıklamaya eklenir; pano metninin içeriği loglanmaz. Böyle bir durumda görüntüyü PNG kaydedip **Resim Ekle** ile seçin veya çalışan başka bir ekran görüntüsü aracını kullanın. SweetCherry kullanıcının bilgisayarındaki yerel panoyu sunucudan okumaya çalışmaz.

## URL’den içerik ekleme

1. Zengin metin editöründe imleci veya değiştirilecek metni seçin.
2. **🌐 URL’den içerik ekle** düğmesine basın.
3. Giriş gerektirmeyen bir `http://` veya `https://` sayfa adresi girin.
4. **Yükle ve incele** ile metin önizlemesini ve resim/tablo sayılarını görün. Önizleme, özgün sayfanın çalıştırılabilir HTML’si değildir.
5. **İmleç konumuna ekle** ile desteklenen içeriği ekleyin. İptal, editör içeriğini değiştirmez.
6. Sonucu düzenleyip **Kaydet** ile CTB’ye yazın. Eklenen metin, resim ve tablolar tek geri al/yinele adımıdır; Kaydet öncesinde nesneler de yalnız editörün belleğinde bekler.

Sayfanın ilk `article`, yoksa `main`/`role=main`, yoksa `body` içeriği alınır. Menü/form/script gibi bölümler çıkarılır. Desteklenen yazı biçimleri, inline renkler, HTTP(S) bağlantılar ve satır sonları mevcut yapıştırma dönüştürücüsünden geçer. Resimler normalize edilmiş PNG olarak CTB’ye eklenir; göreli resim/bağlantı adresleri alınan son sayfa URL’sine göre çözülür. `src`, yaygın `data-src`/`data-original` adresleri desteklenir.

Basit dikdörtgen tablolar grid nesnesidir; ilk satır başlıktır. Birleştirilmiş hücreler ve iç içe tablolar metne dönüşür. Hücre içi biçimler düz metindir; tabloların içindeki desteklenen resimler tablonun ardından eklenir. Özgün sitenin CSS tasarımı, class’ları, özel fontları, JavaScript’i ve sayfa yerleşimi aktarılmaz.

Giriş isteyen, bot korumalı veya JavaScript ile oluşan sayfalar alınamayabilir. PDF veya başka dosyaların URL’leri bu özellikte web sayfası olarak açılmaz. WebP/AVIF/SVG gibi diğer resim biçimleri ve alınamayan görseller, varsa alternatif metniyle kalır; sayısı özette belirtilir. Sadece `srcset` kullanan görseller bu ilk sürümde desteklenmez.

### Sınırlar ve sunucu davranışı

- Yalnız ADMIN, seçili/yazılabilir veri kaynağı ve düzenlenebilir düğüm. POST isteği mevcut CSRF ve eski sekme korumalarından geçer.
- `myapp.web-import-enabled: false` ile sunucuda kapatılabilir; varsayılan `true`. Değişiklikten sonra yeniden başlatın.
- Yalnız standart HTTP/HTTPS portları 80/443. Yerel dosya, localhost, özel/yerel/ayrılmış ağlar ve kullanıcı/parola içeren URL’ler kabul edilmez. Her yönlendirme ve resim için kontrol tekrarlanır. DNS yanıtlarının tümü kontrol edilir ve bağlantıya sabitlenir.
- Sunucu, kendi internet bağlantısıyla sayfayı/resimleri alır; tarayıcının oturum çerezleri, giriş bilgileri veya proxy kimlik bilgileri aktarılmaz. JavaScript çalıştırılmaz.
- HTML en çok 2 MB; bir işlemde toplam ağ verisi 20 MB; en çok 8 resim konumu ve toplam 10 MB normalize PNG. Tek resim için mevcut tenant dosya boyutu/piksel sınırları da geçerlidir. Büyük/uyumsuz resimler atlanır.
- İşlem için 30 saniyelik ağ okuma bütçesi, adım başına en çok 8 saniyelik bağlantı/okuma beklemesi, en çok 4 yönlendirme ve aynı anda en çok 2 aktarım vardır. İşletim sistemi DNS çözümleme süresi ayrıca etkili olabilir.
- Mevcut editör sınırları korunur: kaydetmeden eklenmiş toplam 10 yeni nesne, metin/HTML karmaşıklığı ve toplam gönderim boyutu. Daha çok nesneli bir sayfa eklenemiyorsa daha küçük bir sayfa/içerik kullanın. Eklenemeyen aktarım editörün önceki içeriğini değiştirmez.

## Manuel kontrol

1. IrfanView/Greenshot/Flameshot’tan kopyalanan bir görüntüyü Ctrl+V ve 📋 ile ayrı ayrı deneyin. İzin reddi, boş pano ve HTTP/HTTPS farklarını kontrol edin. Flameshot başarısızsa açıklamadaki **biçim adlarını**, tarayıcı ve Flameshot sürümünü kaydedin; pano içeriğini paylaşmanız gerekmez.
2. Biçimli web metni/tablosu ve SweetCherry içindeki metni kopyalayıp yapıştırın: önceki davranış korunsun.
3. Statik, küçük bir sayfayı 🌐 ile alın; metin/resim/tablo sırası, göreli bağlantılar ve Türkçe karakterleri kontrol edin. Ekle öncesi İptal hiç değişiklik yapmasın.
4. Ekle sonrası geri al hepsini çıkarsın; yinele geri getirsin. Kaydet, düğümü yeniden açın ve CherryTree’de kontrol edin.
5. Mevcut resim/attachment/table işaretçisini kapsayan seçime aktarım yapmak engellensin; işaretçi ve asıl nesne korunmalı.
6. `http://127.0.0.1/`, yerel ağ adresi, farklı port, `file:` URL veya erişilemeyen sayfada anlaşılır hata olsun; düzenleme korunmalı.
7. Salt okunur düğüm/kaynak veya USER ile endpoint’e erişim ağ indirmesi başlatmasın.

Kaynaklar:
- https://developer.mozilla.org/en-US/docs/Web/API/Clipboard/read
- https://www.w3.org/TR/clipboard-apis/
- https://hc.apache.org/httpcomponents-client-5.5.x/current/httpclient5/apidocs/
