# Optional Draw.io image replacements / İsteğe bağlı Draw.io resim karşılıkları

All standard packaged Draw.io shapes/icons, CTB icons and PDF icon fonts are already inside `SweetCherry.jar`. No extraction, online download or manual library installation is required.

This folder is only for your own local PNG/JPEG replacements for unavailable Draw.io shapes/images. The example `images.properties` has no active entries. To use it:

1. Put only images you intend to show to SweetCherry readers in this folder.
2. Edit `images.properties` with a `shape.<shape-name>` or `image.<SHA-256-key>` from the missing-image placeholder; use relative image filenames.
3. Set `myapp.drawio-preview.local-images-directory` under System settings to this folder's absolute path (for example `C:/SweetCherry/drawio-images`). The default empty setting keeps replacements disabled.
4. Restart SweetCherry; repeat the restart after changing mappings or pictures.

Do not add a second `myapp:` YAML section: use System settings or edit the existing setting. Diagrams/external URLs are not fetched to populate this folder. The replacement map is application-wide, so do not put private pictures here unless all relevant readers should see them. See `../documentation/en/drawio-viewer.md` for supported formats, limits and security boundaries.

Maven release preparation and the Windows setup create seed files only when absent, preserving your edited manifest and images. A ZIP extraction tool has its own overwrite behavior: extract a new release into a new writable folder and transfer your settings/data deliberately. Back up this folder separately from CTB backups.

**Türkçe:** Hazır shape/ikonlar JAR içindedir; bu klasör yalnızca sizin ekleyeceğiniz PNG/JPEG karşılıkları içindir. `images.properties` örneği yorum satırlarından oluşur. Resimleri ekleyip eşlemeyi düzenleyin, System settings üzerinden bu klasörün mutlak yolunu ayarlayın ve yeniden başlatın. Varsayılan boş ayar özelliği kapalı tutar. Klasör uygulama genelinde okuyuculara sunulabilir; yalnızca göstermek istediğiniz dosyaları koyun. Maven hazırlığı ve Windows kurucusu mevcut dosyaları korur; ZIP'i yeni klasöre açın. Ayrı yedekleyin. Ayrıntılar: `../documentation/tr/drawio-viewer.md`.
