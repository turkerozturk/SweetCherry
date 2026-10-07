# Şablon PDF dışa aktarma

`/cherrytemplatenode` sayfasındaki PDF bağlantısı, şablonla eşleşen düğümleri ve şablonda tanımlanan alt düğüm değerlerini indirilebilir bir rapora dönüştürür. Önceki deneme PDF şablonu artık bu bağlantıda kullanılmaz; kaynak dosyası geçmiş çalışmayı korumak için tutulur.

- A4 yatay, üstte şablon adı, altta sayfa numarası / toplam sayfa.
- Sıra numarası %5, düğüm adı ve kimliği %20, değişkenler %75 genişliktedir.
- Son alanın içindeki değişken adı %25, değer yaklaşık %50 toplam sayfa genişliği kullanır.
- Düğüm ve değişken adlarında her iki kelimeden sonra satır kırılır. Değerler normal satır kaydırmasıyla gösterilir; rich-text değerler mevcut PDF parser hattından geçer.
- Şablonun tanımladığı fakat ilgili düğümde bulunmayan değişkenin değeri boş kalır. Şablonun mevcut proje ve değişken sıralaması korunur.
- Gömülü fontlar, resimler ve dış bağlantılar düğüm PDF çıktısıyla aynı kuralları kullanır. Dış kaynaklar ağdan indirilmez.

Bu ilk sürüm seçenek ekranı içermez. Çok uzun tek kelimeler, çok büyük iç tablolar ve tek başına bir sayfadan uzun değerler manuel olarak ayrıca kontrol edilmelidir.

## PDF bağımlılıkları

Bu çalışma sırasında kullanılmayan PDF bağımlılığı bulunmadığı için POM bağımlılıkları yorum satırına alınmadı:

- Flying Saucer: XHTML sayfa düzeni ve sayfalama.
- OpenPDF: Flying Saucer PDF üretimi ve eski arama sonuçları PDF yollarındaki HTMLWorker.
- openpdf-fonts-extra: çıktıya gömülen Liberation Sans / Mono fontları.
- PDFBox: çıktı meta bilgilerinin düzenlenmesi, eski PDF yardımcıları ve PDF doğrulama testleri.
