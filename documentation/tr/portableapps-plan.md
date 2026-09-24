# SweetCherry PortableApps.com paketleme planı

PortableApps.com paketi, `release/SweetCherry` klasörünü yalnızca yeniden adlandırmaktan ibaret değildir. Windows'a özel bir launcher, standart dizin yapısı, uygulama metadatası, simgeler ve PortableApps.com Installer ile üretilen `.paf.exe` paketi gerekir.

## Neden ikinci aşama?

SweetCherry'nin mevcut ZIP paketi Windows, Linux ve macOS için aynı JAR'ı ve scriptleri kullanır. PortableApps.com Format ise Windows odaklı ayrı bir dağıtım kanalıdır. Bu nedenle önerilen sıra:

1. Önce sürüm numaralı GitHub ZIP paketini yayımla.
2. Aynı JAR sürümünü temel alan `SweetCherryPortable` geliştirme paketini hazırla.
3. Paketi farklı Windows bilgisayarlarda ve farklı sürücü harflerinde dene.
4. PortableApps.com topluluğunun test/review sürecine sun.

Formatı karşılayan bir `.paf.exe` hazırlamak, uygulamanın PortableApps.com uygulama listesine otomatik kabul edileceği anlamına gelmez.

## Hedef dizin yapısı

Planlanan yapı:

```text
SweetCherryPortable/
├── SweetCherryPortable.exe
├── App/
│   ├── AppInfo/
│   │   ├── appinfo.ini
│   │   ├── appicon.ico
│   │   └── Launcher/
│   │       └── SweetCherryPortable.ini
│   └── SweetCherry/
│       └── SweetCherry.jar
├── Data/
│   └── SweetCherry/
│       ├── CTBDATA/
│       ├── allTenants/
│       ├── exportedFiles/
│       └── myapp.log
└── Other/
    ├── Help/
    └── Source/
```

İlk kurulumdaki `demo.ctb` ve `demo.txt`, kullanıcının verisi sayıldığı için uygulama ikili dosyalarının yanında değil varsayılan veri şablonunda tutulmalıdır. Launcher uygulamayı `Data/SweetCherry` çalışma diziniyle açar; böylece CTB tanımları, exportlar ve log uygulama güncellendiğinde korunur.

## Java kararı

Resmî launcher Java desteğini etkinleştirerek `java.exe`/`javaw.exe` yolunu taşınabilir Java kurulumuna yönlendirebilir. SweetCherry için launcher taslağı şu esaslara dayanmalıdır:

- `[Activate] Java=require`
- `ProgramExecutable=java.exe` veya konsolsuz kullanım istenirse `javaw.exe`
- `CommandLineArguments=-jar "%PAL:AppDir%\SweetCherry\SweetCherry.jar" ...`
- `WorkingDirectory=%PAL:DataDir%\SweetCherry`
- Java doğrudan çalıştırıldığı için `SingleAppInstance=false`
- `appinfo.ini` içinde `UsesJava=true`

Burada iki seçenek ayrıca test edilmelidir:

1. Kullanıcıdan PortableApps.com Java kurulumu istemek.
2. Lisans ve paket boyutu uygun ise desteklenen bir Java çalışma zamanını paketlemek.

Mevcut GitHub ZIP paketindeki sistem Java'sı yaklaşımı PortableApps paketine doğrudan taşınmamalıdır; aksi halde paket yalnızca klasörü taşınabilir olur, çalışma zamanı açısından bağımsız olmaz.

## Hazırlanacak dosyalar

- Güncel Format sürümüne göre `App/AppInfo/appinfo.ini`
- Launcher ayarı: `App/AppInfo/Launcher/SweetCherryPortable.ini`
- `appicon.ico` ile gerekli PNG simge boyutları
- Installer ayarı ve varsayılan veri şablonu
- GPL kaynak/lisans bildirimleri
- PortableApps.com Launcher ile üretilen `SweetCherryPortable.exe`
- Güncel PortableApps.com Installer ile üretilen `SweetCherryPortable_<sürüm>.paf.exe`

`PackageVersion` dört sayısal alandan oluşmalıdır; örneğin uygulama sürümü `0.5.0` ise paket sürümü `0.5.0.0` olabilir. Görünen sürüm ayrı `DisplayVersion` alanında tutulur.

## Taşınabilirlik testleri

- Paketi boş bir PortableApps dizinine kurma.
- Boş `Data` ile demo verisinin yalnızca ilk kurulumda oluşması.
- CTB ekleme, tenant seçme, export, indirme ve silme.
- Uygulamayı kapatıp farklı sürücü harfine taşıma ve yeniden açma.
- Mutlak yol içeren kullanıcının tenant dosyası ile göreli demo yolunun davranışı.
- Güncelleme kurulumunda `Data` içeriğinin, logların ve exportların korunması.
- İki kez başlatma, port çakışması ve kontrollü kapatma.
- PortableApps Java yokken anlaşılır hata.
- Varsayılan dinleme adresinin yalnızca `127.0.0.1` olması.

## Resmî listeleme için yaklaşım

Önce geliştirme testi olarak yayımlamak, geri bildirimlerle launcher ve veri taşıma davranışını düzeltmek daha gerçekçidir. Resmî listelenme hedefi değerlidir; ancak kabul, yalnızca klasör biçimine değil lisans, marka, güncelleme, taşınabilirlik ve topluluk incelemesine de bağlıdır.

PortableApps prototipine başlamadan önce GitHub sürüm numarası, Windows simgesi ve Java dağıtım seçimi kesinleştirilmelidir.
