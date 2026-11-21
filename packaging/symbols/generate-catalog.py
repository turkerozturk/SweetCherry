#!/usr/bin/env python3
"""Generate the offline picker catalogue from the project's pinned Font Awesome WebJar and existing CTB PNGs.
Usage: python packaging/symbols/generate-catalog.py /path/to/font-awesome-7.3.0.jar
No downloads, new fonts or artwork. Requires only Python 3.
"""
from pathlib import Path
import sys,zipfile,json,re,hashlib,unicodedata
root=Path(__file__).resolve().parents[2];jar=Path(sys.argv[1]);version=re.search(r'<sweetcherry.font-awesome.version>([^<]+)',(root/'pom.xml').read_text(encoding='utf-8'))[1]
with zipfile.ZipFile(jar) as z:
 raw=z.read(f'META-INF/resources/webjars/font-awesome/{version}/metadata/icons.json');fa=json.loads(raw)
items=[]
blocks=[('😀 😃 😄 😁 😆 😅 😂 🙂 🙃 😉 😊 😇 😍 🤩 😘 😎 🤔 🤗 🤫 😐 😕 😢 😭 😡 😴 🤖 👋 👍 👎 👏 🙏 💪 ✍️ 👀 🧠 👤 👥 👩‍💻 👨‍💻','emoji face smile happy sad people person yuz gulumse insan kisi'),('❤️ 🧡 💛 💚 💙 💜 🖤 🤍 💔 ⭐ 🌟 ✨ ☀️ 🌙 ☁️ ⛅ 🌧️ ❄️ ⚡ 🔥 💧 🌱 🌳 🌸 🍒 🍎 🍊 ☕ 🎂 🎁 🎉 🎵 🎶','nature star heart weather food doga yildiz kalp hava dogum gunu muzik'),('✅ ✔️ ☑️ ☐ ☒ ❌ ❎ ⚠️ ⛔ 🚫 ℹ️ ❓ ❗ 🔴 🟠 🟡 🟢 🔵 🟣 ⚫ ⚪ 🟩 🟥 🟦 🏁 🚩 📌 📍 🔔 🔕 🔒 🔓 🔑 🛡️','status check warning information lock durum onay uyari bilgi kilit'),('📁 📂 🗂️ 📄 📃 📋 📌 📝 📓 📔 📕 📗 📘 📙 📚 🔖 🏷️ 🔗 📎 ✂️ 🔍 🔎 🖼️ 📷 🎥 🎬 💻 🖥️ ⌨️ 🖱️ 🖨️ 💾 💿 🔌 🔋 ⚙️ 🛠️ 🔧 🧰 📡 🌐 🏠 🏢 📅 ⏰ ⏱️ ⌛ 📈 📉 📊','file folder note book image camera computer network calendar tools dosya klasor not kitap resim kamera bilgisayar ag takvim arac'),('← → ↑ ↓ ↔ ↕ ↖ ↗ ↘ ↙ ⇐ ⇒ ⇑ ⇓ ⇔ ↩ ↪ ↳ ➜ ➤ ▶ ◀ ▲ ▼ ⏩ ⏪ ⏭️ ⏮️ ⏯️ ⏸️ ⏹️ ⏺️ ⟳ ⟲','arrow direction navigation media ok yon gezinme'),('• ◦ ▪ ▫ ◆ ◇ ● ○ ★ ☆ ✓ ✗ © ® ™ ° ± × ÷ ≈ ≠ ≤ ≥ ∞ √ ∑ ∫ π μ Ω € £ ¥ ₺ $ % ‰ ½ ¼ ¾ ² ³ ⁂ § ¶ † ‡','symbol math currency punctuation simge sembol matematik para isaret')]
seen=set()
for block,terms in blocks:
 for value in block.split():
  if value in seen:continue
  seen.add(value);label=' + '.join(unicodedata.name(ch,'UNICODE') for ch in value if ord(ch)!=0xfe0f)
  items.append({'kind':'unicode','value':value,'label':label.title(),'terms':terms+' '+' '.join(f'U+{ord(ch):04X}' for ch in value)})
enums=(root/'src/main/java/com/turkerozturk/helpers/NodeIcon.java').read_text(encoding='utf-8');labels={m[0]:(m[1],m[2]) for m in re.findall(r'\(\d+,\s*\d+,\s*"([^"]+)",\s*"([^"]+)",\s*"([^"]+)"',enums)}
for file in sorted((root/'src/main/resources/static/img/ctbicons').glob('*.png')):
 name=file.stem
 if not re.fullmatch('[A-Za-z0-9_-]{1,80}',name):raise ValueError('Unsafe filename')
 label,category=labels.get(name,(name.removeprefix('ct_').replace('_',' ').replace('-',' ').title(),'other'))
 items.append({'kind':'ctb','name':name,'value':'{{ctb:'+name+'}}','label':label,'terms':name+' '+category+' CherryTree CTBICONS'})
for name,icon in sorted(fa.items()):
 for style in icon.get('free',[]):
  if style not in ('solid','regular','brands'):continue
  if not re.fullmatch('[a-z0-9-]+',name):raise ValueError('Unsafe Font Awesome name')
  items.append({'kind':style,'name':name,'value':'{{fa-'+style+' fa-'+name+'}}','label':icon.get('label',name),'terms':' '.join(icon.get('search',{}).get('terms',[]))+' Font Awesome fa-'+style,'aliases':[n for n in icon.get('aliases',{}).get('names',[]) if re.fullmatch('[a-z0-9-]+',n)]})
translations={'smile':'gulumse gulumsemek mutlu yuz','grinning':'gulumse mutlu yuz','joy':'gulmek nese','face':'yuz','camera':'kamera','image':'resim gorsel','picture':'resim gorsel','file':'dosya','folder':'klasor','note':'not','book':'kitap','heart':'kalp','arrow':'ok yon','check':'onay tamam','clock':'saat','calendar':'takvim','trash':'cop sil','star':'yildiz','warning':'uyari','information':'bilgi','info':'bilgi','lock':'kilit','lightbulb':'ampul fikir','user':'kullanici kisi','people':'insanlar kisiler','computer':'bilgisayar','network':'ag','globe':'dunya web','code':'kod','music':'muzik','bell':'zil hatirlatici','sitemap':'agac hiyerarsi','cherries':'kiraz','cherry':'kiraz','map':'harita','home':'ev ana sayfa','birthday':'dogum gunu','cake':'pasta dogum gunu','gift':'hediye','cloud':'bulut','sun':'gunes','moon':'ay','rain':'yagmur','snow':'kar','dollar':'dolar para','euro':'avro euro para','lira':'lira para'}
for item in items:
 if item['kind']=='unicode':item['terms']='Unicode '+' '.join(re.findall(r'U\+[0-9A-F]+',item['terms']))
 haystack=(item['label']+' '+item.get('name','')+' '+item['terms']).lower().replace('_',' ').replace('-',' ')
 for english,turkish in translations.items():
  if re.search(r'\b'+english+r's?\b',haystack):item['terms']+=' '+turkish
data={'fontAwesomeVersion':version,'items':items}
out=root/'src/main/resources/static/js/symbols';out.mkdir(parents=True,exist_ok=True)
(out/'catalog.js').write_text('/* Generated offline catalogue; see SOURCE.txt. */\nwindow.SweetCherrySymbolCatalog='+json.dumps(data,separators=(',',':'),ensure_ascii=True)+';\n',encoding='utf-8',newline='\n')
(out/'SOURCE.txt').write_text('Font Awesome '+version+' metadata/icons.json from the existing org.webjars:font-awesome dependency.\nMetadata SHA-256: '+hashlib.sha256(raw).hexdigest()+'\nIcon/font/code terms remain as recorded in the WebJar LICENSE and distribution-notices.\nCTB entries reference existing static/img/ctbicons/*.png files without copying/changing artwork.\nUnicode entries are a curated native-text list; no emoji artwork or additional font is bundled.\nRegenerate with packaging/symbols/generate-catalog.py and the pinned local WebJar.\n',encoding='utf-8')
print({kind:sum(i['kind']==kind for i in items) for kind in ['unicode','ctb','solid','regular','brands']})
