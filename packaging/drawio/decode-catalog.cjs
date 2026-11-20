// Build-time decoder for the pinned upstream packed stencil format. No network.
const fs=require('fs'),vm=require('vm'),zlib=require('zlib');
const source=fs.readFileSync(process.argv[2],'utf8');const names=[...source.matchAll(/f\['([^']+)'\]/g)].map(m=>m[1]);
const registry={loadStencil(){throw Error('Unexpected network');}};
vm.runInNewContext(source,{mxStencilRegistry:registry,STENCIL_PATH:'local',window:{setTimeout:fn=>fn()},atob:s=>Buffer.from(s,'base64').toString('binary'),pako:{inflateRaw:bytes=>new Uint8Array(zlib.inflateRawSync(bytes))},Uint8Array,DataView,TextDecoder,Map});
const result=[];for(const file of names){const doc=registry.loadStencil('local/'+file);const root=doc.documentElement;const group=root.getAttribute('name');const shapes=root.getElementsByTagName('shape').map(shape=>shape.getAttribute('name'));result.push({file,namespace:group,shapes});}
process.stdout.write(JSON.stringify(result));
