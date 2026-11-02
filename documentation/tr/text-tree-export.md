# Klasör ağacı / metin ZIP dışarı aktarımı

[CherryTree #2914](https://github.com/giuspen/cherrytree/issues/2914) isteğindeki klasör yapısını koruyan düz metin aktarımı, SweetCherry içinde bağımsız olarak uygulanmıştır.

Masaüstü veya mobil düğüm görünümünde **Dışarı Aktar → Klasör ağacı / metin dosyaları (.zip)** seçin. Alt düğümleri dahil edip etmeyeceğinizi seçip **ZIP indir** düğmesine basın. Giriş yapmış USER/ADMIN kullanıcıları, salt okunur bağlantılarda da kullanabilir. Kaynak veritabanına, şemasına veya tarihlerine yazılmaz. PDF, dört mevcut CTB aktarımı ve Freeplane artık aynı **Dışarı Aktar** menüsündedir; Markmap/Mermaid görüntülemeleri **Harita** menüsünde kalır.

- ZIP seçilen düğümden başlar; onun üstündeki atalar eklenmez. Sıra ve hiyerarşi `children` tablosuna göre korunur. Paylaşımlı konum, master içeriğini ama kendi alt düğümlerini kullanır.
- Alt düğümü olan düğüm klasör olur. Kendi metni boş değilse o klasörün `_content.txt` dosyasına yazılır. Boş klasör düğümünde fazladan boş içerik dosyası oluşturulmaz.
- Yaprak dosyanın adı düğüm adıdır. Mevcut uzantı ve `.env` gibi noktayla başlayan adlar korunur; nokta içermeyen adlara `.txt` eklenir. Kod dilinden uzantı tahmin edilmez. Alt düğümler dahil edilmezse parent düğüm de tek dosya olur.
- Düz metin/kod/Markdown kaynakları ek başlık olmadan, UTF-8/BOM’suz yazılır; metnin satır sonları korunur. Zengin metin mevcut nesne farkındalıklı dönüştürücüyle düzleştirilir: bağlantı hedefleri eklenir, tablolar sekmeli satırlara, kod kutuları metne dönüşür. Resim/ekli dosya/anchor açıklama olarak yer alır; ikili dosyalar ZIP’e konmaz.

Bir yol hem dosya hem klasör olamayacağından, örnekteki `set_system_settings.ansible` alt düğümleri varsa klasör olur; kodu `set_system_settings.ansible/_content.txt`, alt dosyaları `set_system_settings.ansible/files/...` yollarına yazılır. Bu dosya sistemi kısıtını aşmak içindir; kaynak düğüm değiştirilmez.

Geçersiz yol karakterleri, `..`, Windows aygıt adları ve uzun bileşenler düzeltilir. Unicode desteklenir. Büyük/küçük harf duyarsız çakışmalarda (dosya/klasör ve `_content.txt` dahil) uzantıdan önce konum ID’si eklenir. Çok uzun toplam yollar aktarımı iptal eder; işletim sisteminin arşiv açma/yol sınırları yine geçerlidir.

Harici `application.yml` dosyasında mevcut `myapp` altında değiştirip yeniden başlatabilirsiniz:

```yaml
myapp:
  text-tree-export:
    max-nodes: 10000
    max-depth: 64
    max-node-characters: 8388608
    max-text-bytes: 67108864
```

Pozitif değerler gerekir. Kök derinliği sıfırdır. Bayt sınırı ZIP boyutu veya HTTP upload sınırı değil, sıkıştırılmamış UTF-8 metin toplamıdır. Zengin metin nesnelerinin metinleri de sınırlandırılır. Ad bileşenleri yaklaşık 180 UTF-8 baytı, toplam yollar 2.048 bayt ile sınırlıdır. Sınır, bozuk ağaç veya eksik master durumunda veri sessizce atlanmaz; indirme başlamadan işlem iptal edilir ve formda bilgi gösterilir.

Aynı anda bir ZIP hazırlanır. Arşiv Java geçici klasöründe oluşturulur ve indirme/ele alınan hata sonrasında silinir; tüm ZIP bellekte tutulmaz. Geçici diskte boş yer gerekir. Süreç çökerse `sweetcherry-text-tree-*.zip` dosyaları OS/yönetici temizliği için kalabilir. Kaynak değişimi belirteci ve normal POST/CSRF koruması geçerlidir. Bu bir metin aktarımıdır; ikili nesnelerle tam yedek için CTB aktarımı/yedekleme kullanılmalıdır.

Manuel kontrol: uzantılı/uzantısız, Unicode ve CRLF içerikli yaprakları; kendi metni olan parent ve boş klasörleri; çakışan/geçersiz adları deneyin. Shared fixture’da 11’i aktarırken 12–19 alt konumları bulunmalı, master 2’nin diğer çocuğu 3 bulunmamalıdır. Zengin metin bağlantı/nesne açıklamalarını, yalnız düğüm seçimini, sınırları, salt okunur bağlantıyı, eski sekmeyi ve mobil menüyü kontrol edin.
