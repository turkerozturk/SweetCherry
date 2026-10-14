# SweetCherry 1.1 ekran görüntüleri / screenshot capture plan

Bu klasörde henüz ekran görüntüsü yoktur. Aşağıdaki dosyalar gerçek uygulamadan alındıktan sonra README'ye veya ayrı galeriye eklenebilir. Resim varmış gibi çalışan link/placeholder oluşturulmadı. Görüntüler yayın için zorunlu değildir; eklenecekse etiket öncesinde commit edin.

## Dosyalar ve içerik

| Dosya | Gösterilecek içerik | İngilizce kısa açıklama |
| --- | --- | --- |
| `01-node-tree-en.png` | Sol ağaç ve sağ içerik: gerçek/shared düğüm, biçimli metin, tek resim | Browse CherryTree notes in a desktop web view. |
| `02-rich-text-editor-en.png` | Metin, dış bağlantı, resim ve tablo içeren açık editör | Edit supported rich text and embedded objects. |
| `03-markmap-en.png` | 10–20 okunabilir düğüm, birkaç ikon; uygun zoom | Explore the selected branch as a mind map. |
| `04-pdf-export-en.png` | PDF görüntüleyicide içerik ve solda hiyerarşik PDF navigasyonu | Export a branch with hierarchical PDF navigation. |
| `05-tenant-wizard-en.png` | Gerçekçi örnek CTB bağlantısı; şifre/yol gizli | Create and edit database connections with a wizard. |
| `06-desktop-control-en.png` | Windows kontrol penceresi: ready durumu, browser/stop düğmeleri | Start and stop SweetCherry from its desktop control window. |
| `07-mobile-reader-en.png` | Tek düğüm ve mobil toolbar; okunabilir içerik | Read notes on a phone-sized browser view. |

README'de ilk üç görüntü yeterlidir; diğerleri galeride veya kılavuzda kullanılabilir. Türkçe görüntü gerekirse aynı adın `-tr.png` karşılığı kullanılır. Aynı dosya içinde dil karıştırmayın.

## Çekim standardı

- PNG kullanın; masaüstünde tarayıcı zoom %100 ve yaklaşık 1440×900 pencere. Mobilde yaklaşık 390×844 görünüm. Uygulama içeriğini kırpabilirsiniz; oranı bozmadan saklayın.
- Görüntü başına 300–800 KB makul hedeftir; okunabilirliği bozacak küçültme/renk azaltma yapmayın. JPEG arayüz yazılarını bozabilir; animasyon gerekiyorsa kısa video tercih edin.
- Aynı dil, tema ve pencere ölçüsü kullanın. Menüyü ancak o özellik gösterilecekse açın; imleci önemli içeriğin üzerine koymayın.
- demo.ctb veya kopyasında gerçekçi, kişisel olmayan kısa içerik kullanın: Project Notes, Reading List, Meeting Notes gibi. Dağıtılan demo dosyasını değiştirmek zorunlu değildir.
- Parola, gerçek IP/hostname, kişisel dosya yolu ve notlar görünmemeli. Mümkünse sahte örnek veri kullanın; hassas veri kaldıysa bulanıklaştırma yerine tam kapatın.
- Tanıtım için temiz görüntü ve altta kısa açıklama kullanın. Rehberde 1–3 numaralı işaret/ok yeterli; uzun metinleri görüntünün içine gömmeyin.
- Flameshot işaretlenmiş kopyalar, IrfanView kayıpsız küçültme, GIMP sosyal medya için çerçeve/gölge oluşturma işlerinde kullanılabilir. Ham orijinali ve GIMP XCF dosyasını yerelinizde saklayın; depoya yayın PNG'sini koyun.

## README'ye ekleme

Önce gerçek dosyaları ekleyin. Ardından kök README'de örneğin şu biçimi kullanın:

```markdown
![SweetCherry node tree and note content](documentation/images/1.1/01-node-tree-en.png)

Browse CherryTree notes in a desktop web view.
```

Görüntüde işaretleme varsa alt açıklama işareti anlatmalıdır. Alt text görselin içeriğini açıklasın; yalnız “screenshot” yazmayın. Çok sayıda ekranı tek dev kolaja sıkıştırmayın.
