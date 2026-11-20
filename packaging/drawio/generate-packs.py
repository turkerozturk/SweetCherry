#!/usr/bin/env python3
"""Rebuild offline Draw.io resources from a local, clean clone at the pinned commit.
Requires Python 3 and Node.js, without additional packages. No network performed.
Usage: python packaging/drawio/generate-packs.py /path/to/drawio
"""
import pathlib,json,re,base64,hashlib,shutil,subprocess,sys,xml.etree.ElementTree as ET
root=pathlib.Path(__file__).resolve().parents[2];out=root/'src/main/resources/static/js/drawio';up=pathlib.Path(sys.argv[1]).resolve()
commit=subprocess.check_output(['git','-C',str(up),'rev-parse','HEAD'],text=True).strip()
if commit!='f411a11f4f6b8994d869bdbc45f4d4d9e2284f34':raise ValueError('Review and pin updated upstream assets/licences before regenerating')
subprocess.run(['git','-C',str(up),'diff','--quiet','HEAD','--','src/main/webapp/js/stencils.min.js','src/main/webapp/img/lib'],check=True)
bundle=up/'src/main/webapp/js/stencils.min.js'
c=json.loads(subprocess.check_output(['node',str(root/'packaging/drawio/decode-catalog.cjs'),str(bundle)]));viewer=(out/'viewer-static.min.js').read_text(encoding='utf-8');groups=set(re.findall(r'mxgraph\.([a-zA-Z0-9_]+)',viewer));index={};files={}
for row in c:
 g=row['namespace'].split('.')[1];groups.add(g);files[row['file']]=g
 for name in row['shapes']: index[(row['namespace']+'.'+name.replace(' ','_')).lower()]=row['file']
icons={};hashes={}
for p in sorted((up/'src/main/webapp/img/lib').rglob('*')):
 if not p.is_file():continue
 rel=p.relative_to(up/'src/main/webapp/img/lib').as_posix();group=rel.split('/')[0];groups.add(group)
 data=p.read_bytes();hashes['img/lib/'+rel]=hashlib.sha256(data).hexdigest()
 if p.suffix=='.svg':
  # Pinned vendor icons are isolated as image documents; reject active/remote content at packaging time.
  doc=ET.fromstring(data)
  for el in doc.iter():
   if el.tag.rsplit('}',1)[-1].lower() in ['script','foreignobject','iframe','object','embed']:raise ValueError(rel+' active SVG')
   for k,v in el.attrib.items():
    if k.rsplit('}',1)[-1].lower().startswith('on'):raise ValueError(rel+' handler')
    if k.rsplit('}',1)[-1].lower() in ['href','src'] and not v.startswith(('#','data:image/')):raise ValueError(rel+' external image '+v)
  for match in re.finditer(r'url\(\s*[\"\']?([^\)\"\']+)',data.decode('utf-8'),re.I):
   if not match[1].startswith(('#','data:image/')):raise ValueError(rel+' external CSS reference')
  if re.search(r'@import',data.decode('utf-8'),re.I):raise ValueError(rel+' CSS import')
  mime='image/svg+xml'
 else:mime='image/png'
 icons.setdefault(group,{})[rel]='data:'+mime+';base64,'+base64.b64encode(data).decode()
if len(hashes)!=1910 or len(c)!=204 or len(index)!=9089:raise ValueError('Incomplete pinned upstream resources')
(out/'icons').mkdir(exist_ok=True)
for g,items in icons.items():(out/'icons'/f'{g}.js').write_text('/* Pinned Draw.io icons; see ../SOURCE.txt and distribution-notices. */\nObject.assign(window.SweetCherryDrawioIcons, '+json.dumps(items,separators=(',',':'))+');\n',encoding='utf-8',newline='\n')
shutil.copyfile(bundle,out/'stencils.min.js')
(out/'catalog.js').write_text('/* Generated from pinned Draw.io stencil bundle; see SOURCE.txt. */\nwindow.SweetCherryDrawioCatalog='+json.dumps({'shapes':index,'files':files,'groups':sorted(groups)},separators=(',',':'))+';\n',encoding='utf-8',newline='\n')
(out/'groups.txt').write_text('\n'.join(sorted(groups))+'\n',encoding='utf-8',newline='\n')
(out/'asset-sha256.json').write_text(json.dumps(hashes,indent=2)+'\n',encoding='utf-8',newline='\n')
print('families',len(groups),'stencil shapes',len(index),'icon packs',len(icons),'icons',len(hashes));print(sorted(groups))
