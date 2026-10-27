# CTB dışa aktarma

Bu belge SweetCherry'de görüntülenen CherryTree notlarının ortak bir koleksiyon CTB'sine veya ayrı bir CTB dosyasına aktarılmasını, mevcut davranışı ve ileride ele alınacak konuları açıklar.

Düğüm sayfasındaki dört seçenek aktarım başlamadan önce onay ister; iptal dosya oluşturmaz veya değiştirmez. Dosya listesi ve silme sonucu Türkçe/İngilizcedir; sonuçtan listeye dönüş düğmesi vardır. [Arayüz ve manuel kontroller](node-id-navigation.md).

## Terimler

CherryTree'deki her not bir **node** olarak adlandırılır. SweetCherry belgelerinde geçen **not**, **not düğümü**, **düğüm** ve **node** ifadeleri aynı kavramı anlatır.

## `exportednodes.ctb` ne işe yarar?

Bir not sayfasındaki **CTB Koleksiyona Aktar** düğmesi, seçilen düğümü `exportedFiles/exportednodes.ctb` dosyasına kopyalar. Bu dosya farklı CTB veritabanlarından seçilen notların bir araya getirildiği bir sepet veya koleksiyon olarak düşünülebilir.

- `exportedFiles` klasörü paketleme sırasında hazırlanır; çalışma anında eksikse export işlemi de otomatik oluşturur.
- `exportednodes.ctb` ilk başarılı koleksiyon export işleminde oluşturulur.
- Sonraki export işlemleri aynı dosyaya yeni düğümler ekler.
- Aynı düğüm tekrar aktarılırsa halen yeni bir kopya eklenir; yinelenen içerik kontrolü yapılmaz.
- Seçilen düğümün alt düğümleri varsa mevcut controller bunları da bulup aynı export işlemine dahil eder ve ilişkilerini yeni kimliklere göre kurar.
- Export işlemi artık `POST` isteğidir. Başarılı işlemden sonra sonuç sayfasına yönlendirme yapıldığı için tarayıcının geri/ileri veya yenile hareketi export'u kendiliğinden tekrarlamaz.

## Ayrı CTB dosyasına aktarma

**CTB Ayrı Aktar** düğmesi, seçilen düğüm ve alt düğümleri için bağımsız bir CTB dosyası oluşturur. Bu seçenek hem masaüstü hem mobil düğüm görünümünde bulunur. Dosya adı şu biçimdedir:

```text
export_<tenant adı>_<nodeId>.ctb
```

Örnek:

```text
export_Demo Database_40.ctb
```

Aynı tenant ve düğüm için dosya zaten varsa mevcut dosyanın üzerine doğrudan yazılmaz. Önce tarih-saat bilgisi eklenerek `.old` uzantılı bir geçmiş kopyasına dönüştürülür, ardından yeni CTB oluşturulur:

```text
export_Demo Database_26.ctb
export_Demo Database_26.ctb__20260924155002.old
export_Demo Database_26.ctb__20260924155026.old
```

Bu export da `POST` ve sonuç sayfasına yönlendirme kullanır; tarayıcının geri/ileri hareketi yeni bir CTB veya `.old` dosyası oluşturmamalıdır.

## Dört export seçeneğinin özeti

| Seçenek | Hedef | Kapsam | Var olan hedef |
|---|---|---|---|
| Yalnız Düğümü Koleksiyona Aktar | `exportedFiles/exportednodes.ctb` | Yalnız görüntülenen düğüm | Yeni kimlikle aynı koleksiyona eklenir |
| Düğüm ve Alt Ağacı Koleksiyona Aktar | `exportedFiles/exportednodes.ctb` | Görüntülenen düğüm ve bütün alt düğümleri | Yeni kimliklerle aynı koleksiyona eklenir |
| Yalnız Düğümü Ayrı CTB'ye Aktar | `exportedFiles/export_<tenant>_<nodeId>.ctb` | Yalnız görüntülenen düğüm | Önceki hedef tarih-saatli `.old` dosyasına taşınır |
| Düğüm ve Alt Ağacı Ayrı CTB'ye Aktar | `exportedFiles/export_<tenant>_<nodeId>.ctb` | Görüntülenen düğüm ve bütün alt düğümleri | Önceki hedef tarih-saatli `.old` dosyasına taşınır |

Dört seçenek de hem masaüstü hem mobil düğüm görünümünde bulunur.

## Düğüm kimlikleri neden değişir?

Ortak CTB’ye aktarırken yeni kimlikler hedefteki `node` ve `children` tablolarının en büyük kimliğinden sonra, ağaç sırasıyla ayrılır. `father_id`, `master_id`, bookmark ve nesne kimlikleri bu eşlemeyi kullanır. Ayrı CTB’de aktarılan konumların kaynak kimlikleri korunur. Her iki durumda kapsayıcı düğüme, aktarılan konumlarla çakışmayan ayrı bir kimlik verilir.

## Dosyayı indirme ve silme

`/exportedFiles` sayfası oluşturulan ortak ve ayrı CTB dosyaları ile `.old` geçmiş kopyalarını listeler.

- **Download**, seçilen CTB veya `.old` dosyasını bilgisayarda seçilen konuma indirir.
- **Delete**, kullanıcı onayından sonra dosyayı siler. Silme işlemi veri değiştirdiği için `POST` isteği kullanır.
- Dosya silindikten sonra yeni bir düğüm export edilirse boş bir `exportednodes.ctb` yeniden oluşturulur.

Silmeden veya CherryTree ile düzenlemeden önce gerekli dosyaların yedeğini alın.

## Paylaşımlı konumların CTB’ye aktarılması

Dört seçenek gerçek ve paylaşımlı konumlarda kullanılabilir. Kapsam, seçilen `children.node_id` ve alt ağaç seçilmişse onun `father_id` ile bağlı kendi çocuklarıdır. Master’ın başka konumdaki çocukları aktarılmaz.

| Kaynak durumu | Hedefteki sonuç |
| --- | --- |
| Yalnız bir shared konum aktarılır | Master’ın içerik ve nesneleri bu konumun kimliğiyle bağımsız gerçek düğüm olarak kopyalanır |
| Master da seçilen alt ağaç içindedir | Master ve shared konumlar kopyalanır; shared bağlantılar hedefteki master kimliğine çevrilir |
| Master dışarıda, aynı gruptan birden fazla shared konum içeridedir | Ağaç sırasındaki ilk konum gerçek master olur; diğer konumlar hedefte ona bağlanır |

**Hedef CTB’de dışarıda kalan kaynak master’a bağımlı shared bağlantı bırakılmaz.** İlk konumda master içeriği materialize edilir; aynı grubun her konumu için ayrı içerik kopyası oluşturulmaz. Kaynak veritabanı değişmez. Hedefin içeriği kaynaktan bağımsızdır; kaynaktaki sonraki düzenlemeler export dosyasını değiştirmez.

Ortak CTB’ye eklerken kimlikler `node` ve `children` tablolarındaki en büyük kimlikten sonra ayrılır. Parent, master, nesne ve bookmark kimlikleri aynı eşlemeye göre yazılır. Her export yeni bir bağımsız grup ekler; önceki export grubuyla birleştirme yapılmaz. Yeni ayrı CTB’de kaynak ağaç kimlikleri korunur; seçilen kökün ebeveyni yeni kapsayıcı düğümdür. Yalnız kapsayıcı `father_id=0` ile eklenir.

İçerik ve nesneler JDBC üzerinden kopyalanır; binary resim/attachment verileri ve SQL NULL değerleri korunur. Hedefe yazma transaction içindedir. Ortak CTB’de hata varsa eklenen satırlar geri alınır. Ayrı CTB geçici dosyada tamamlanır; ancak başarılı olduğunda mevcut çıktı `.old` olarak taşınır ve yeni çıktı yerleştirilir. Bu geçmiş export dosyaları uygulamanın not veritabanını otomatik yedeklediği anlamına gelmez.

### CherryTree kaynak incelemesi (2026-10-08)

- [`ct_storage_sqlite.cc`](https://github.com/giuspen/cherrytree/blob/master/src/ct/ct_storage_sqlite.cc): `_write_node_to_db`, yalnız düğüm export’unda shared master bilgisini sıfırlar; alt ağaç export’unda verilen master eşlemesini uygular. Kendi çocuklarını ağaç üzerinden gezer.
- [`ct_storage_control.cc`](https://github.com/giuspen/cherrytree/blob/master/src/ct/ct_storage_control.cc): `CURRENT_NODE_AND_SUBNODES` için master dışarıda kalıyorsa ilk aktarılacak shared konumu `expo_master_reassign` ile yeni master seçer.

SweetCherry aynı grup yaklaşımını bağımsız JDBC koduyla uygular; CherryTree kaynak kodu kopyalanmamıştır.

### Metin içindeki linkler ve anchor’lar

Dört CTB seçeneği de her başarılı aktarım için **bir kapsayıcı düğüm** oluşturur. Ortak dosyada her işlem yeni bir kapsayıcı ekler; önceki aktarımlarla birleştirme yapılmaz. Seçilen düğüm, kendi alt ağacı korunarak kapsayıcının tek çocuğu olur. Kapsayıcı kökte son sıradadır (`father_id=0`), kalın başlıklıdır ve rich-text içeriğe sahiptir.

Başlık: `yyyy-mm-dd hh:mm:ss from:<kaynak veritabanının görünen adı> [node:<seçilen kaynak konum ID’si>]`.
Başlıktaki tarih/saat uygulamanın çalışma saat dilimindedir. Kapsayıcının oluşturulma ve güncellenme tarihleri aynı güncel Unix saniyesidir. **Kopyalanan düğümlerin iki tarihi, kaynak içerikleri ve kaynak bağlantıları değişmez.** Sonuç ekranındaki düğüm sayısı, kaynakta seçilen konum sayısıdır; ek kapsayıcı bu sayıya dahil değildir.

Rich-text `link="node ID ..."` attribute’ları ve resim nesnelerinin `image.link` alanları işlenir:

- Hedef konum da bu aktarımın içindeyse bağlantı onun **hedefteki konum ID’sine** çevrilir. Shared konumun kendi kimliği kullanılır; varsa anchor eki korunur.
- Kaynakta bulunmayan hedef (**orphan**) veya kaynakta bulunup bu aktarımın dışında kalan gerçek/shared hedef için bağlantı kapsayıcıya çevrilir. Eski anchor eki kaldırılır; raporda olmayan bir anchor’a gitmeye çalışılmaz.
- Hedef CTB’de aynı sayısal ID’ye sahip eski bir düğüm olması bu kararı değiştirmez. Başka bir export grubuna yanlışlıkla bağlantı kurulmaz.
- Master’ı dışarıda kalmış bir shared konumun içeriğinin aktarılması, dışarıdaki master konumunu da aktarılmış yapmaz. O master’a giden metin bağlantısı da kapsayıcıya yönlenir.
- Web URL’leri, dosya/klasör bağlantıları ve plain-text içindeki benzer yazılar değiştirilmez. Görünen bağlantı metni, biçimi ve nesne offset’leri korunur. Geçersiz rich-text XML varsa eksik bir aktarım üretmek yerine transaction geri alınır.

Kapsayıcı içeriği, bağlantının bulunduğu **kaynak içerik düğümünün** ID’si/başlığı altında şu listeyi gösterir:

`Source type | Source ID | Source target name | Internal link text`

Türler `orphan`, `node`, `shared node` değerleridir. ID ve ad, **bağlantının kaynaktaki hedefini** anlatır; shared hedefte ad master’dan gelir. Orphan hedefin bilinmeyen adı `—` olur. Birden fazla biçimli metin parçasına yayılan aynı bağlantının görünür metni birleştirilir. Birden çok ayrı bağlantı ayrı satırlarda kalır. Resim bağlantısında metin yerine nesne offset’i ve varsa dosya adı yazılır. Kaynakta sorunlu/kapsam dışı bağlantı yoksa rapor bunu belirtir; kapsayıcı yine oluşturulur. Rapor taşınabilir CTB içeriği olduğu için İngilizcedir ve kaynak bağlantılarını tıklanabilir olarak yeniden üretmez.

Önceki export dosyaları geriye dönük değiştirilmez.

### Manuel kabul listesi

[`src/test/resources/fixtures/shared-node-tree.sql`](../../src/test/resources/fixtures/shared-node-tree.sql) dosyasından [test CTB’si oluşturun](shared-node-operations.md#sqlden-manuel-test-ctbsi-oluşturma).

Önceki sürümle üretilmiş export dosyaları kendiliğinden onarılmaz. Manuel doğrulamaya boş bir ortak CTB ile başlayın; gerekiyorsa eski `exportednodes.ctb` dosyasını ayrı bir yere taşıyın. Kaynak test CTB’sini değiştirmeyin.

1. Shared 11’de dört seçeneği deneyin: yalnız düğümde kapsayıcı altında bir gerçek düğüm, alt ağaçta kapsayıcı altında 11, 12, 13, 14, 15, 18, 19 bulunmalı. Master 2’nin çocuğu 3 gelmemeli.
2. Çıktıları CherryTree’de açın. 18’in altında 19 kalmalı; hedefte olmayan master’a referans bulunmamalı.
3. 10’u 11’in altına taşıyıp alt ağacı aktarın: 18 ve 10 aynı hedef içeriğe bağlanmalı, biri gerçek master olmalı.
4. Gerçek 1’i 11’in altına taşıyıp aktarın: 18 ve 10 bu gerçek düğümün hedefteki kimliğine bağlanmalı.
5. Kaynak master’a resim/attachment, tablo, kod kutusu ekleyin; aktarılan gerçek master’da nesneleri kontrol edin. Shared konuma eklenen bookmark kendi hedef konumunda kalmalı.
6. Ortak CTB’ye aynı dalı iki kez ekleyin; iki bağımsız kapsayıcı kök ve geçerli parent/master bağlantıları bulunmalı.

### Kapsayıcı ve internal linkler için manuel kontrol

1. Kaynak düğümde kendi alt düğümüne, dışarıdaki gerçek düğüme, dışarıdaki shared konuma ve var olmayan bir ID’ye rich-text link ekleyin. Bir bağlantıyı kalın/italik parçalara bölün; Türkçe, `&` ve `<` içeren metin de kullanın.
2. Dört seçeneği deneyin. Kapsayıcı başlığı, kalınlığı, iki güncel tarihi, kökteki sırası ve kendi altındaki seçilen dalı kontrol edin. Tek düğüm aktarımında onun kaynak çocuklarına giden linkler de kapsam dışıdır.
3. CherryTree veya SweetCherry’de çıktıyı açın: dal içi link doğru gerçek/shared konuma gitmeli; diğerleri kapsayıcıya gitmeli. Raporda hedef türü, kaynak ID’si, hedef adı ve görünen link metni doğru olmalı. Web bağlantıları ve görünür metin değişmemeli.
4. Ortak CTB’ye aynı dalı iki kez ekleyin. Her kopyanın linkleri kendi dalına/kapsayıcısına gitmeli; başka gruptaki aynı/eski ID’ye gitmemeli. Ortak CTB aktif kaynakken de tekrar deneyin.
5. Resme internal link atayın; görüntü, dosya, offset ve yerleşim korunurken link aynı kurala uymalı. Kaynak XML ve tarihlerinin hiç değişmediğini kontrol edin.
6. Hiç internal link içermeyen düğümde de kapsayıcı ve boş rapor açıklaması bulunmalı. Çıktıyı tekrar içe aktarma önizlemesi kapsayıcıya yönlenen linkleri orphan saymamalı.

### English summary — export containers

Every single-node/subtree export to a new or common CTB creates a bold, rich-text, top-level container named `yyyy-mm-dd hh:mm:ss from:<source database name> [node:<source occurrence ID>]`. Its creation/modification timestamps are current; copied node timestamps are preserved. The selected branch is placed under it without changing its internal hierarchy or shared groups.

Rich-text and image node links within the selected branch are remapped to exported occurrence IDs. Missing source targets and existing targets outside the export point to the container, with their old anchor suffix removed. The report groups these links by source content node and lists source target type (`orphan`, `node`, `shared node`), target ID, target/master name and visible link text. Exported aliases do not implicitly include their external master occurrence. External URLs, visible text, object offsets and source data remain unchanged. Repeated exports have separate containers; the reported exported-node count excludes the container. Existing export files are not retroactively repaired.

## SweetCherry içinde veri kaynağı olarak açma

`exportednodes.ctb`, başka CTB dosyaları gibi `allTenants` altında bir bağlantı tanımı veya tenant sihirbazı ile SweetCherry'ye tanıtılabilir. Görüntülemek, düzenlemek ve içindeki bir dalı tekrar ortak CTB'ye kopyalamak mümkündür. Aktif kaynak dosyası ile ortak export hedefi aynı dosyaysa kopyalama ikinci bağlantı açmadan mevcut transaction bağlantısında yapılır; yeni kimliklerle bağımsız bir kopya eklenir. Hata halinde bu kopyaya ait eklemeler geri alınır.

**Aynı dosyaya export yapmak aktif tenant'ın `custom.isWritable=true` ayarını gerektirir.** Salt okunur bir bağlantı üzerinden aynı CTB'ye yazılamaz. Bu değişiklik başka uygulamaların veya oturumların eşzamanlı yazma kilitlerini ortadan kaldırmaz; böyle bir durumda işlemi tekrar deneyin. Otomatik yedekleme yapılmaz.

Bağlantı tanım dosyalarını `allTenants` klasörüne yüklemek için SweetCherry'de bir yükleme arayüzü de bulunmaktadır.

## Kalan işler

- İsteğe bağlı yinelenen export kontrolü.
- Export hedefinin ayrıca aktif tenant veya CherryTree’de açık olması senaryosunun doğrulanması. Export dosyasını incelemeden/değiştirmeden önce kullanmakta olan programda kapatın.

## Freeplane / FreeMind dışa aktarma

Yeni masaüstü ve mobil düğüm sayfalarının Export menüsünde **Freeplane / FreeMind (.mm)** bulunur. Ortak sayfa şablonundaki `/mindmap-export?nodeId=<ağaç-kimliği>` formu ID alanını doldurur; doğrudan açıldığında kimlik elle girilebilir. Tenant kapalıysa önce veri kaynağı seçimi gerekir. Görünüm token'ı ve POST CSRF kontrolü korunur. Paylaşımlı düğümde ID, shared occurrence'ın kendi kimliğidir; görünen ad/stiller master'dan okunur. Shared occurrence kendi çocuklarıyla genişletilir; master'ın başka konumdaki çocukları onun alt ağacı gibi dışa aktarılmaz. İkonlar seçilirse `.mm` dosyasının yanında eşleşen PNG ikonları içeren `ctbicons` klasörü gerekir.


## Düşünce Haritası menüsü ve ikon paketi

Yeni masaüstü ve mobil düğüm sayfalarında Düşünce Haritası menüsü sırasıyla
Markmap, Mermaid ve Freeplane / FreeMind (.mm) seçeneklerini içerir.
Freeplane bağlantısı Export grubundan bu gruba taşınmıştır. Paylaşımlı düğüm
bağlantıları kendi ağaç kimliğini kullanır.

`/mindmap-export` sayfasında düğüm ID’si boş bırakılırsa tüm veritabanı aktarılır.
Veritabanı adı sanal kök (`ID_0`) olur; `father_id=0` kayıtları altında sıralı
olarak yer alır. Veritabanı boşsa yalnız bu kök oluşur. ID girilirse önceki gibi
ilgili dal aktarılır. Paylaşımlı düğüm master içeriğini gösterir; master'ın alt
ağacı o paylaşımlı konumda genişletilmez. Seviye 0 yalnız kökü verir; tüm
veritabanı aktarımında üst düzey gerçek düğümler seviye 1'dir.

CherryTree ikonları varsayılan olarak dahil edilmez; indirme yalnız `.mm` olur.
İkon seçimi üç ayrı indirme biçimi sunar:

| Seçenek | İndirme | XML içinde ikon referansı |
|---|---|---|
| İkonsuz (.mm) | Yalnız `.mm` | Yok |
| İkon referanslı (.mm) | Yalnız `.mm` | Var; `ctbicons/` yanında olmalı |
| İkonlarla birlikte (.zip) | `.mm` + ikon klasörü | Var |

ZIP seçilirse istek sırasında `.zip` oluşturulur: kökte `.mm`, yanında
`ctbicons/` altında paketli PNG ikonları ve İngilizce `readme.txt` bulunur.
İkonlar kaynak ağacına veya disk üzerindeki sabit geliştirme yoluna ihtiyaç
olmadan, çalışmakta olan JAR'ın classpath kaynaklarından alınır. Kaynak ikon
klasörüne readme yazılmaz; açıklama yalnız indirme arşivinde oluşturulur.

İlk ikonlu harita için ZIP’i açıp `.mm` dosyasını o klasörden Freeplane ile açın.
Sonraki dışa aktarımlarda **İkon referanslı (.mm)** seçip indirilen dosyayı
aynı klasöre koyabilirsiniz; ikonları tekrar indirmeniz gerekmez. İkonları dahil etmeden indirilen `.mm` ikon referansı içermez;
mevcut ikon klasöründen otomatik ikon eklemez. Yeni uygulama sürümlerinde ikon
seti değişirse ZIP'teki ikon klasörünü de yenileyin.

Manuel kabul: masaüstü/mobil menü sırası, tek dal ve shared dal, boş ID ile tüm
veritabanı ve boş veritabanı, level 0/1, ikonlu ZIP’i Freeplane'de açma ve ikonları
harita yanında tutma. Paketli Windows dağıtımında da ZIP indirmesi denenmelidir.


### Freeplane biçim tanıma

Freeplane kaynak kodundaki `MFileManager.loadTreeImpl` dosyanın başlangıcını
`MapVersionInterpreter.getVersionInterpreter` metoduna iletir. Tanıma,
`<map version="...` önekiyle `startsWith` karşılaştırması yapar. XML bildirimi
önce geldiğinde geçerli XML olsa bile “unknown program” uyarısı oluşabilir.
Dışa aktarma XML bildirimi olmadan doğrudan `<map version="freeplane 1.12.15">`
ile başlar. SweetCherry üretici bilgisi map içindeki açıklama yorumundadır;
Freeplane tarafından üretildiği iddia edilmez.

Kaynak incelemesi: https://github.com/freeplane/freeplane
- `freeplane/src/main/java/org/freeplane/features/url/mindmapmode/MFileManager.java`
- `freeplane/src/main/java/org/freeplane/features/url/MapVersionInterpreter.java`

Kabul: yeni indirilen `.mm` ve ZIP içindeki `.mm` ilk kez açılırken unknown-program
uyarısını kontrol edin; eski dosyalar yeniden dışa aktarılmalıdır. Üç ikon seçeneği
tek düğüm, shared düğüm ve tüm veritabanı aktarımında kullanılabilir. Eski
`includeIcons=true` isteği uyumluluk için ZIP davranışını korur; yeni form
`iconMode=none/references/bundle` kullanır.

## Dışa aktarılan CTB’yi geri içe aktarma

`/exportedFiles` sayfasındaki **İçe aktar** işlemi, bir CTB’nin tüm kayıtlarını
yazılabilir aktif veritabanına önizleme ve onayla ekler. ID ve paylaşımlı düğüm
eşleme kuralları ile manuel kontroller: [CTB içe aktarma](ctb-import.md).
