# Düğüm ve alt düğümleri PDF olarak indirme

Yeni masaüstü ve mobil görünümlerde PDF → **PDF indir - Düğüm ve Alt Düğümler** aynı PDF sihirbazını açar. Seçili düğüm ve alt ağacı tek dosyada, ağaç sırasıyla aktarılır. Her düğüm yeni sayfadan başlar.

İki navigasyon seçeneği birbirinden bağımsızdır:

- **PDF okuyucusunda başlıklar:** Sol başlık panelinde düğüm/alt düğüm hiyerarşisi korunur. İçerikteki h1–h6 başlıklar ait oldukları düğümün altında yer alır. Başlık tıklaması ilgili sayfaya gider.
- **İçindekiler:** PDF başına düğüm adlarından oluşan, girintili ve tıklanabilir bir içindekiler bölümü ekler. Varsayılan kapalıdır. Bu seçenek yalnız alt ağaç sihirbazında gösterilir.

Sayfa boyutu, yönü, renkler, düğüm adını gösterme, header/footer ve güvenli meta bilgi seçenekleri tek düğüm PDF’siyle ortaktır. Başarılı indirmeden sonra seçimler oturum boyunca hatırlanır. Düğüm adını görünür başlık olarak kapatmak, açık tutulmuş PDF navigasyonunu kaldırmaz.

## Paylaşımlı düğümler

Paylaşımlı düğüm master’ın içeriğini kullanır; PDF başlığının konumu ve bağlantı kimliği kendi ağaç kaydına aittir. Mevcut okuyucularla aynı şekilde yaprak olarak işlenir. Master’ın alt ağacı veya paylaşımlı düğüm altında veritabanında bulunabilecek gizli kayıtlar genişletilmez. Aynı içeriğin farklı ağaç konumları PDF’de ayrı bölümlerdir. Sayfa içi çapa kimlikleri bu bölümler arasında çakışmayacak şekilde ayrılır.

## Sınırlar

Raspberry Pi gibi cihazlarda büyük işlerin bellek tüketimini sınırlamak için tek seferde en fazla 512 düğüm, 64 alt seviye ve toplam 64 Mi karakter işlenmiş HTML kabul edilir. Tek düğümün metni için mevcut 8 Mi karakter sınırı korunur. Sınır aşımında aktarım reddedilir; kırpılmış PDF verilmez. Bozuk döngülü hiyerarşi de reddedilir. Tek düğüm ve alt ağaç PDF üretimleri aynı eşzamanlı iş kilidini kullanır.

CTB değişmez ve dış kaynaklar ağdan indirilmez. Font, resim, rich-text ve meta bilgi kuralları mevcut düğüm PDF hattından gelir.
