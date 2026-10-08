# Gelişmiş arama

`/nodesadvancedwithbinding` içerik filtresi halen ham `node.txt` üzerinde SQL `LIKE` araması yapar. Rich-text XML etiketleri eşleşebilir; biçim etiketiyle bölünen görünen bir kelime ise tek parça sorguda bulunmayabilir. Bu sürümde arama semantiği değiştirilmemiştir.

İçerik filtresinin ham metin üzerinde hızlı çalışan mevcut davranışı korunur; render edilmiş metin aramasına geçiş planlanmıyor.

Arama formu önceki POST ve Spring `FormSearch` binding akışını kullanır. POST sonrasında GET sonuç adresine yönlendirme kaldırılmıştır. Arama yaptıktan sonra tarayıcıyla geri dönmek veya POST sonuç sayfasını yenilemek, tarayıcıya göre formu yeniden gönderme/“document expired” mesajı oluşturabilir. Bu gezinme konusu release sonrasına bırakılmıştır. PDF düğmesinin ayrı POST işlemi korunur.

Türlerin görünen adları Türkçe/İngilizce Rich Text ve Plain Text karşılıklarıdır; veritabanındaki `custom-colors` ve `plain-text` değerleri değişmez. Content Type Filter metin kutusu halen ham syntax değeriyle arar. İkon seçimi resimli ve çoklu checkbox listesidir; hiçbir seçim olmaması ikon filtresi uygulanmaması anlamına gelir.

Masaüstü alt durum çubuğu içerik düğümünün kimliğini ve paylaşımlı konumdaysa onun ayrı ağaç kimliğini gösterir.
