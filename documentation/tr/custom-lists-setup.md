# Özel listelerin kurulumu

`/cherrytemplatetasks` sayfası, veritabanının herhangi bir yerindeki yapılandırılmış ada sahip **tek Plain Text düğümü** arar. Sayfayı açmak veri oluşturmaz/değiştirmez. Birden fazla aynı ad veya yanlış içerik türü açıklanır; ilk eşleşme sessizce seçilmez.

## Ayarlar

`application.yml` varsayılanları:

```yaml
myapp:
  custom-lists:
    main-node-name: cherrytemplatetasks
    template-prefix: cherrytemplate
```

Bağlantıya özel tenant config değerleri önceliklidir:

```properties
custom.customListsNodeName=listelerim
custom.templatePrefix=sablonum
```

Eksik, boş veya geçersiz tenant değeri global ayara döner. Global adlar boş olamaz, en fazla 255 UTF-16 karakteridir. Önek zaten `-` ile bitmiyorsa ayırıcı eklenir. Tenant config düzenlendikten sonra yapılandırmaları yenileyin; global YAML için uygulamayı yeniden başlatın. Ayarlar mevcut düğümleri yeniden adlandırmaz. Sihirbaz, varolan bağlantı düzenlenirken bu ek değişkenleri korur.

## Tanımlar ve oluşturma

Ana düğüme her satırda anahtar, isteğe bağlı TAB ve görünen ad yazılır:

```text
projects	Projelerim
devices	Cihaz Listesi
#software	Yazılımlar
```

Örnekte gerçek TAB vardır. Editördeki TAB düğmesini kullanabilirsiniz; klavyedeki Tab normal odak geçişini sürdürür. Boş satırlar atlanır; baştaki boşluklardan sonra `#` ile başlayan satırlar aranmaz. Tekrarlanan etkin anahtarlar açıklanır. Varsayılanda `projects`, `cherrytemplate-projects` düğümünü arar. Eşleşme tam ada ve büyük/küçük harfe duyarlıdır.

Tabloda düğüm ID bağlantısı ile mevcut şablon raporu bağlantısı ayrı gösterilir. ADMIN ve yazılabilir bağlantıda, eksik ana/şablon düğümü onayla oluşturulabilir. Yeni düğüm kökte son sıraya eklenen boş Plain Text düğümdür; içeriği sonradan doldurulur. Oluşturma öncesinde tanımlar ve seçili bağlantı tekrar kontrol edilir. Varolan, belirsiz veya yorum satırındaki tanımlar oluşturulmaz. Salt okunur bağlantılar ve USER kullanıcıları oluşturamaz.

İki yardım sayfası Türkçe/İngilizce görsel örnekler ve geçerli adlarla uygulamada kalır. Mevcut HTML/XLSX/PDF rapor motoru ve özel alt düğüm kuralları değiştirilmez. Özellikle eski motor, proje seçerken **`node.tags` alanının tamamını** şablon düğümü adıyla karşılaştırır; tag parçaları üzerinde arama yapmaz. Alan seçimi ve eksik değer davranışı bu adımın kapsamı dışındadır.

## Düğüm adları ve editörler

Özellikler, hızlı oluşturma, normal oluşturma ve yeni çoğaltma sırasında adın sınırlarındaki boşluklar (NBSP dahil), kontrol ve Unicode biçim karakterleri temizlenir. İç boşluklar/görünür metin korunur. Mevcut adlar topluca değiştirilmez; import/export özgün adları korur. Sadece içerik kaydetmek düğümü yeniden adlandırmaz. Çoğaltmada yalnız yeni kopyanın adı temizlenir.

Plain Text/Markdown araç çubuğunda ☐, ☑, ☒, • ve TAB; görsel Rich Text editöründe mevcut işaretlerin yanında TAB bulunur. Ekleme, seçimi ve mevcut undo/redo/kurtarma akışını kullanır; klavyedeki Tab yakalanmaz.

## Manuel kontrol

1. Ana düğüm yokken sayfayı açın, onayla oluşturun; adını, Plain Text türünü ve kök konumunu kontrol edin.
2. TAB içeren örneği kaydedin: projects/devices aransın, software aranmasın. Eksik şablonu oluşturup ID ve rapor bağlantılarını açın.
3. Aynı isimli ana/şablon düğümleri, tekrarlı etkin satır ve yanlış ana düğüm türü deneyin; otomatik seçim yapılmadığını görün.
4. Global ayarları ve tenant önceliğini, iki dilde yardım sayfalarını ve rapor içindeki yardımı kontrol edin.
5. USER/salt okunur görünümünde oluşturma olmasın; başka sekmede bağlantı değiştikten sonra eski form yeni veritabanına yazamasın.
6. İki editörde TAB/işaret ekleme, undo/redo ve kaydetmeyi deneyin. Ad sınırlarında boşluk, NBSP ve görünmez biçim karakterlerinin temizlenip iç boşlukların korunduğunu görün.


[Sıralı rapor alanları ve çıktı kuralları](template-reports.md)
