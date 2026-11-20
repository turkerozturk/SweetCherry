(() => {
 'use strict';
 const root=document.getElementById('image-annotation'),core=window.SweetCherryImageDrawing;if(!root||!core)return;
 const canvas=root.querySelector('[data-annotation-canvas]'),ctx=canvas.getContext('2d'),status=root.querySelector('[data-annotation-status]'),controls=root.querySelector('[data-annotation-controls]');
 const undo=root.querySelector('[data-annotation-undo]'),redo=root.querySelector('[data-annotation-redo]'),preview=root.querySelector('[data-annotation-preview]'),dialog=root.querySelector('[data-annotation-dialog]'),save=root.querySelector('[data-annotation-save]'),cancel=root.querySelector('[data-annotation-cancel]');
 let edits=core.history(),image,revision,gesture=null,pointer=null,ready=false,saving=false,preparing=false,leaving=false,pending=null,previewUrl=null,generation=0,maxPoints=5000;
 const loadController=new AbortController();
 const url=()=>{const value=new URL(root.dataset.endpoint,location.href);value.searchParams.set('_tenantView',root.dataset.tenantView);return value;};
 const dirty=()=>edits.marks.length>0;
 function buttons(){undo.disabled=!ready||saving||!edits.undoable;redo.disabled=!ready||saving||!edits.redoable;preview.disabled=!ready||saving||!dirty();}
 function render(){if(!image)return;ctx.clearRect(0,0,canvas.width,canvas.height);ctx.drawImage(image,0,0);for(const mark of edits.marks)core.draw(ctx,mark);if(gesture)core.draw(ctx,gesture);buttons();}
 function position(event){return core.point(event.clientX,event.clientY,canvas.getBoundingClientRect(),canvas.width,canvas.height);}
 function message(response){return response.status===409?root.dataset.conflict:response.status===403?root.dataset.readonly:[413,422].includes(response.status)?root.dataset.unsupported:root.dataset.error;}
 async function initialize(){
  try{
   const response=await fetch(url(),{credentials:'same-origin',cache:'no-store',redirect:'error',signal:loadController.signal,headers:{Accept:'application/json'}});
   if(!response.ok||response.redirected)throw Error(message(response));
   if(!(response.headers.get('Content-Type')||'').includes('application/json'))throw Error(root.dataset.error);
   const data=await response.json();
   if(!/^[a-f0-9]{64}$/.test(data.revision)||!Number.isInteger(data.width)||!Number.isInteger(data.height)||data.width<1||data.height<1||data.width>8192||data.height>8192||data.width*data.height>12000000||typeof data.png!=='string'||data.png.length>23000000||!data.png.startsWith('data:image/png;base64,'))throw Error(root.dataset.error);
   const source=new Image();await new Promise((resolve,reject)=>{source.onload=resolve;source.onerror=()=>reject(Error(root.dataset.error));source.src=data.png;});
   if(loadController.signal.aborted)return;
   if(source.naturalWidth!==data.width||source.naturalHeight!==data.height)throw Error(root.dataset.unsupported);
   canvas.width=data.width;canvas.height=data.height;image=source;revision=data.revision;ready=true;controls.disabled=false;status.textContent='';render();
  }catch(error){if(error.name!=='AbortError')status.textContent=error.message||root.dataset.error;}
 }
 canvas.addEventListener('pointerdown',event=>{
  if(!ready||saving||preparing||dialog.open||pointer!==null||event.button!==0)return;
  if(edits.marks.length>=300){status.textContent=root.dataset.error;return;}
  maxPoints=Math.min(5000,30000-edits.marks.reduce((sum,mark)=>sum+mark.points.length,0));if(maxPoints<2)return;
  event.preventDefault();canvas.focus({preventScroll:true});pointer=event.pointerId;canvas.setPointerCapture(pointer);
  gesture={tool:root.querySelector('[data-annotation-tool]').value,color:root.querySelector('[data-annotation-color]').value,width:Number(root.querySelector('[data-annotation-width]').value),points:[position(event)]};render();
 });
 canvas.addEventListener('pointermove',event=>{
  if(pointer!==event.pointerId||!gesture)return;
  const next=position(event);
  if(gesture.tool==='pen'){if(gesture.points.length<maxPoints)gesture.points.push(next);}else gesture.points=[gesture.points[0],next];render();
 });
 function finish(event,cancelled=false){if(pointer!==event.pointerId||!gesture)return;const value=gesture;gesture=null;pointer=null;
  if(!cancelled){if(value.tool!=='pen')value.points=[value.points[0],position(event)];edits.add(value);}render();
 }
 canvas.addEventListener('pointerup',event=>finish(event));canvas.addEventListener('pointercancel',event=>finish(event,true));
 canvas.addEventListener('lostpointercapture',event=>finish(event,true));
 undo.addEventListener('click',()=>{edits.undo();render();});redo.addEventListener('click',()=>{edits.redo();render();});
 root.querySelector('[data-annotation-reset]').addEventListener('click',()=>{edits.reset();render();});
 canvas.addEventListener('keydown',event=>{if(!ready||saving||preparing||dialog.open)return;if((event.ctrlKey||event.metaKey)&&!event.altKey&&['z','y'].includes(event.key.toLowerCase())){event.preventDefault();if(event.shiftKey||event.key.toLowerCase()==='y')edits.redo();else edits.undo();render();}});
 function closePreview(){if(saving)return;generation++;pending=null;if(previewUrl){URL.revokeObjectURL(previewUrl);previewUrl=null;}dialog.close();}
 preview.addEventListener('click',async()=>{
  if(!ready||saving||preparing||gesture||!dirty())return;const current=++generation;preparing=true;controls.disabled=true;preview.disabled=true;
  try{const blob=await new Promise(resolve=>canvas.toBlob(resolve,'image/png'));if(current!==generation)return;
   if(!blob||blob.size>16777216)throw Error(root.dataset.unsupported);pending=blob;previewUrl=URL.createObjectURL(blob);root.querySelector('[data-annotation-result]').src=previewUrl;dialog.showModal();
  }catch(error){status.textContent=error.message||root.dataset.error;}finally{preparing=false;controls.disabled=saving||!ready;buttons();}
 });
 cancel.addEventListener('click',closePreview);dialog.addEventListener('cancel',event=>{event.preventDefault();closePreview();});
 save.addEventListener('click',async()=>{
  if(!pending||saving)return;
  const csrf=root.querySelector('[data-annotation-csrf] input[name="_csrf"]');if(!csrf){status.textContent=root.dataset.error;return;}
  saving=true;save.disabled=true;cancel.disabled=true;controls.disabled=true;
  try{
   const target=url();target.searchParams.set('revision',revision);target.searchParams.set('confirmed','true');
   const response=await fetch(target,{method:'POST',credentials:'same-origin',cache:'no-store',redirect:'error',headers:{'Content-Type':'image/png','X-CSRF-TOKEN':csrf.value},body:pending});
   if(!response.ok||response.redirected)throw Error(message(response));
   // Keep the just-saved bitmap visible; further edits require a fresh guarded snapshot.
   edits=core.history();ready=false;status.textContent=root.dataset.saved;dialog.close();
  }catch(error){status.textContent=error.message||root.dataset.error;dialog.close();}
  finally{saving=false;save.disabled=false;cancel.disabled=false;controls.disabled=!ready;pending=null;if(previewUrl){URL.revokeObjectURL(previewUrl);previewUrl=null;}buttons();}
 });
 root.querySelector('[data-annotation-reload]').addEventListener('click',()=>{if(saving||dirty()&&!window.confirm(root.dataset.leave))return;leaving=true;location.reload();});
 window.addEventListener('beforeunload',event=>{if(!leaving&&(dirty()||saving)){event.preventDefault();event.returnValue='';}});
 window.addEventListener('pageshow',()=>{leaving=false;});
 document.addEventListener('click',event=>{if(event.defaultPrevented||event.button>0||event.ctrlKey||event.metaKey||event.shiftKey||event.altKey)return;const link=event.target.closest('a[href]');if(link&&!link.hasAttribute('download')&&link.target!=='_blank'){if(saving){event.preventDefault();return;}if(dirty()){if(!window.confirm(root.dataset.leave)){event.preventDefault();event.stopImmediatePropagation();}else leaving=true;}}},true);
 window.addEventListener('pagehide',()=>{loadController.abort();if(previewUrl)URL.revokeObjectURL(previewUrl);});initialize();
})();
