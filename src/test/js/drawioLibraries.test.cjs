const test=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs'),path=require('node:path'),zlib=require('node:zlib'),crypto=require('node:crypto');
const folder=path.join(__dirname,'../../main/resources/static/js/drawio');
const read=name=>fs.readFileSync(path.join(folder,name),'utf8');
const groups=read('groups.txt').trim().split(/\r?\n/).map(name=>name.trim());
function fixture(enabled=groups,localImages={}){
 const warnings=[];class Stencil{constructor(node){this.desc=node;}}
 class Renderer{getShape(name){return Renderer.defaultShapes[name];}}
 Renderer.defaultShapes={rectangle:class{},'mxgraph.infographic.ribbon':class{}};
 const registry={stencils:Object.create(null),loadStencil(){throw Error('Network fallback forbidden');},parseStencilSet(root){for(let n=root.firstChild;n;n=n.nextSibling)if(n.nodeName==='shape')this.stencils[(root.getAttribute('name')+'.'+n.getAttribute('name').replace(/ /g,'_')).toLowerCase()]=new Stencil(n);}};
 class Canvas{image(...args){return args;}}
 const window={SweetCherryDrawioLocalImages:localImages,setTimeout:fn=>fn(),SweetCherryDrawioIcons:Object.create(null)};
 const context=vm.createContext({window,document:{body:{dataset:{drawioLibraries:enabled.join(',')}}},console:{warn:message=>warnings.push(message)},mxStencilRegistry:registry,mxStencil:Stencil,mxCellRenderer:Renderer,mxSvgCanvas2D:Canvas,STENCIL_PATH:'local',atob:s=>Buffer.from(s,'base64').toString('binary'),pako:{inflateRaw:bytes=>new Uint8Array(zlib.inflateRawSync(bytes))},Uint8Array,DataView,TextDecoder,Map,Set});
 for(const script of ['stencils.min.js','catalog.js',...fs.readdirSync(path.join(folder,'icons')).map(f=>'icons/'+f),'libraries.js'])vm.runInContext(read(script),context);
 const api=window.SweetCherryDrawioLibraries;
 return {api,registry,context,warnings,Canvas,icons:window.SweetCherryDrawioIcons,catalog:window.SweetCherryDrawioCatalog};
}
test('real Cabinets, EIP, flowchart and networking definitions resolve without network or eval',()=>{
 const s=fixture();for(const name of ['mxgraph.cabinets.cb_1p','mxgraph.eip.aggregator',...Object.keys(s.catalog.shapes).filter(n=>n.startsWith('mxgraph.flowchart.')||n.startsWith('mxgraph.cisco.')).slice(0,5)]){
  const shape=s.registry.getStencil(name);assert(shape,name);assert(shape.desc.getElementsByTagName('foreground').length>0,name);
 }
 assert.equal(s.registry.dynamicLoading,false);assert.equal(s.registry.allowEval,false);assert.equal(s.warnings.length,0);
});
test('every packaged stencil set decodes, registers, and resolves all its catalogue entries',()=>{
 const s=fixture();assert.equal(Object.keys(s.catalog.files).length,204);assert(Object.keys(s.catalog.shapes).length>9000);
 for(const [name] of Object.entries(s.catalog.shapes))assert(s.registry.getStencil(name),name);
 assert.equal(s.warnings.length,0);
});
test('disabled sets and JS-only groups fall back while other groups continue to work',()=>{
 const s=fixture(groups.filter(g=>!['cabinets','infographic'].includes(g)));
 assert.equal(s.registry.getStencil('mxgraph.cabinets.cb_1p'),null);assert(s.registry.getStencil('mxgraph.eip.aggregator'));
 const graph={getCellStyle:cell=>({shape:cell})};s.api.attach(graph);
 assert.equal(graph.getCellStyle('mxgraph.infographic.ribbon').shape,'rectangle');assert.equal(graph.getCellStyle('rectangle').shape,'rectangle');
 assert(s.warnings.some(w=>w.includes('disabled: mxgraph.cabinets')));assert(s.warnings.some(w=>w.includes('disabled: mxgraph.infographic')));
});
test('unknown stencils cannot reach the upstream loader and logs are bounded and deduplicated',()=>{
 const s=fixture();for(let n=0;n<1000;n++)assert.equal(s.registry.getStencil('mxgraph.unknown.symbol_'+n),null);
 assert.equal(s.warnings.length,100);s.registry.getStencil('mxgraph.unknown.symbol_0');assert.equal(s.warnings.length,100);
 s.api.begin();assert.equal(s.api.warnings().length,0);s.registry.getStencil('mxgraph.unknown.symbol_0');assert.equal(s.api.warnings().length,1);
 assert.equal(s.registry.loadStencil('https://example.com/evil.xml'),null);
});
test('all 1910 pinned icons are local image documents; arbitrary data and external URLs stay blocked',()=>{
 const s=fixture();assert.equal(Object.keys(s.icons).length,1910);
 const hashes=JSON.parse(read('asset-sha256.json'));
 for(const [name,data] of Object.entries(s.icons))assert.equal(crypto.createHash('sha256').update(Buffer.from(data.split(',')[1],'base64')).digest('hex'),hashes['img/lib/'+name],name);
 const name=Object.keys(s.icons).find(k=>k.startsWith('azure2/')),data=s.api.image('sc-icon:'+name);assert.match(data,/^data:image\/svg\+xml;base64,/);
 const canvas=new s.Canvas();assert.equal(canvas.image(0,0,30,30,data,true,false,false)[4],data);
 for(const unsafe of ['https://example.com/test.svg','javascript:alert(1)','data:image/svg+xml;base64,PHN2Zz4=','img/lib/../../test.svg'])assert.equal(s.api.image(unsafe),null);
 assert.equal(canvas.image(0,0,10,10,'https://example.com/test.svg'),undefined);
 const disabled=fixture(groups.filter(g=>g!=='azure2'));assert.equal(disabled.api.image('sc-icon:'+name),null);
});
test('unknown names and prototype keys use ordinary rectangles',()=>{
 const s=fixture(),graph={getCellStyle:cell=>({shape:cell})};s.api.attach(graph);
 for(const name of ['constructor','__proto__','unknown','mxgraph.eip.future_shape'])assert.equal(graph.getCellStyle(name).shape,'rectangle');
 assert.equal(graph.getImage({style:{image:'https://example.com/image.svg'}}),null);
});

test('missing shapes and icons show escaped identifiers while ordinary captions survive',()=>{
 const s=fixture(),graph={getCellStyle:cell=>cell.style,getLabel:cell=>cell.value};s.api.attach(graph);
 const cell={style:{shape:'mxgraph.local.missing'},value:'Caption'};
 assert.match(graph.getLabel(cell),/Caption<br><span class="sc-drawio-placeholder">\[Shape: mxgraph.local.missing\]/);
 const icon={style:{shape:'image',image:'sc-icon:azure2/missing.svg'},value:''};assert.match(graph.getLabel(icon),/Image: azure2\/missing.svg/);
});
test('administrator local images replace missing shapes and source-key images without permitting remote sources',()=>{
 const png='data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+j1ioAAAAASUVORK5CYII=';
 const hash='a'.repeat(64),s=fixture(groups,{'shape.mxgraph.local.server':png,['image.'+hash]:png});
 const graph={getCellStyle:cell=>cell.style,getLabel:cell=>cell.value};s.api.attach(graph);
 let style=graph.getCellStyle({style:{shape:'mxgraph.local.server'}});assert.equal(style.shape,'image');assert.equal(graph.getImage({style}),png);
 const cell={style:{shape:'rectangle',scImageKey:hash},value:'Caption<br><span class="sc-drawio-placeholder" title="local">[Image: server.png]</span>'};
 style=graph.getCellStyle(cell);assert.equal(style.shape,'image');assert.equal(graph.getImage({style}),png);assert.equal(graph.getLabel(cell),'Caption');
 assert.equal(s.api.image('https://example.invalid/server.png'),null);
});
