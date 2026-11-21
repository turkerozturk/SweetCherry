/* Canvas labels and strict Mermaid labels use bounded, local icon bitmaps. No remote URLs. */
(() => {
 'use strict';
 const core=window.SweetCherryInlineSymbols;if(!core)return;
 const base=document.currentScript?.dataset.ctbBase||'/img/ctbicons/',cache=new Map();
 const font=item=>(item.kind==='solid'?'900':'400')+' 48px "Font Awesome 7 '+(item.kind==='brands'?'Brands':'Free')+'"';
 function loadImage(src){return new Promise((resolve,reject)=>{const image=new Image();image.onload=()=>resolve(image);image.onerror=()=>reject(Error('Local symbol unavailable'));image.src=src;});}
 async function icon(item,color='#20242a'){
  if(!item||core.resolve(item.value?.slice(2,-2))!==item)throw Error('Unknown symbol');
  if(!/^#[0-9a-f]{6}$/i.test(color))color='#20242a';
  const key=item.value+color;if(cache.has(key))return cache.get(key);
  const pending=(async()=>{
   const canvas=document.createElement('canvas');canvas.width=64;canvas.height=64;const ctx=canvas.getContext('2d');
   if(item.kind==='ctb'){
    const image=await loadImage(base+encodeURIComponent(item.name)+'.png');ctx.drawImage(image,0,0,64,64);
   }else{
    await document.fonts.load(font(item),item.glyph);ctx.font=font(item);ctx.fillStyle=color;
    const m=ctx.measureText(item.glyph),width=m.actualBoundingBoxLeft+m.actualBoundingBoxRight,height=m.actualBoundingBoxAscent+m.actualBoundingBoxDescent;
    ctx.fillText(item.glyph,(64-width)/2+m.actualBoundingBoxLeft,(64-height)/2+m.actualBoundingBoxAscent);
   }
   return canvas.toDataURL('image/png');
  })();
  // Bounded cache; old diagram labels do not accumulate throughout navigation.
  if(cache.size>=256)cache.delete(cache.keys().next().value);cache.set(key,pending);
  try{return await pending;}catch(error){cache.delete(key);throw error;}
 }
 async function label(text,{color='#20242a',bold=false,width=155,height=24}={}){
  const parts=core.tokens(String(text)),canvas=document.createElement('canvas');canvas.width=width*2;canvas.height=height*2;
  const ctx=canvas.getContext('2d');ctx.scale(2,2);ctx.font=(bold?'bold ':'')+'13px system-ui';ctx.fillStyle=/^#[0-9a-f]{6}$/i.test(color)?color:'#20242a';ctx.textBaseline='middle';
  const images=await Promise.all(parts.map(async p=>typeof p==='string'?null:loadImage(await icon(p.item,color))));
  let x=0;for(let i=0;i<parts.length;i++){
   const p=parts[i];if(typeof p!=='string'){if(x+14>width)break;ctx.drawImage(images[i],x,(height-14)/2,14,14);x+=16;continue;}
   for(const ch of p){const size=ctx.measureText(ch).width;if(x+size>width-8){ctx.fillText('…',x,height/2);return canvas.toDataURL('image/png');}ctx.fillText(ch,x,height/2);x+=size;}
  }
  return canvas.toDataURL('image/png');
 }
 async function ready(){await Promise.all(['solid','regular','brands'].map(kind=>document.fonts.load(font({kind}),String.fromCodePoint(kind==='brands'?0xf09b:0xf03e))));}
 window.SweetCherrySymbolImages={icon,label,ready};
})();
