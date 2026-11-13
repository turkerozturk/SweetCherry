(() => {
 'use strict';
 const page=document.getElementById('linkGraphPage');if(!page)return;
 const byId=id=>document.getElementById(id),data=page.dataset,status=byId('graphStatus'),warning=byId('graphWarning');
 const back=byId('graphBack'),forward=byId('graphForward'),hierarchy=byId('graphHierarchy'),limit=byId('graphLimit'),frame=byId('graphPreview'),open=byId('graphOpen');
 const dark=document.documentElement.dataset.bsTheme==='dark',text=dark?'#e5eaf0':'#20242a';let cy,controller,sequence=0,current=data.focus,history=[],cursor=-1;
 const link=(base,id,suffix='')=>{const url=new URL(base+encodeURIComponent(id)+suffix,location.href);url.searchParams.set('_tenantView',data.token);return url.href;};
 function positions(nodes){const groups=new Map();nodes.forEach(n=>{if(n.zone!=='focus'){if(!groups.has(n.zone))groups.set(n.zone,[]);groups.get(n.zone).push(n);}});const result=new Map();result.set(current,{x:0,y:0});
  for(const [zone,items] of groups){items.forEach((n,i)=>{const offset=(i-(items.length-1)/2)*85;result.set(n.id,zone==='parent'?{x:offset*2.7,y:-250}:zone==='child'?{x:offset*2.7,y:250}:{x:zone==='in'?-460:460,y:offset});});}return result;
 }
 function fit(){
  if(!cy)return;const focus=cy.getElementById(current);if(!focus.length)return;
  const center=focus.position();let x=120,y=40;cy.nodes().forEach(n=>{const p=n.position();x=Math.max(x,Math.abs(p.x-center.x)+115);y=Math.max(y,Math.abs(p.y-center.y)+40);});
  cy.zoom(Math.max(.05,Math.min(1,(cy.width()-50)/(2*x),(cy.height()-50)/(2*y))));cy.center(focus);
 }
 function draw(graph){
  const list=byId('graphList');list.replaceChildren();
  for(const n of graph.nodes){const li=document.createElement('li'),button=document.createElement('button'),img=document.createElement('img');button.type='button';button.className='btn btn-sm btn-outline-secondary';img.src=data.icons+encodeURIComponent(n.icon)+'.png';img.alt='';button.append(img,document.createTextNode((n.name||n.id)+' ['+n.id+'] — '+(n.zone==='focus'?data.focusLabel:(data[n.zone]||data.focusLabel))+(n.shared?' · '+data.shared:'')));button.addEventListener('click',()=>load(n.id,true));li.append(button);list.append(li);}
  if(typeof cytoscape!=='function')return; // The accessible list remains usable if canvas/WebGL is unavailable.
  cy?.destroy();const pos=positions(graph.nodes);
  cy=cytoscape({container:byId('linkGraphCanvas'),layout:{name:'preset',padding:35},minZoom:.05,maxZoom:2,wheelSensitivity:.2,elements:[
   ...graph.nodes.map(n=>({data:{id:n.id,label:n.name||n.id,icon:data.icons+encodeURIComponent(n.icon)+'.png',color:/^#[0-9a-f]{6}$/i.test(n.color)&&n.color!=='#000000'?n.color:text,bold:n.bold,focus:n.id===current},position:pos.get(n.id)})),
   ...graph.edges.map(e=>({data:{...e,label:e.count>1?String(e.count):''}}))],style:[
    {selector:'node',style:{shape:'round-rectangle',width:190,height:48,'background-color':dark?'#293340':'#e9f1fa','border-color':dark?'#91c5ff':'#4777aa','border-width':1,label:'data(label)',color:'data(color)','text-wrap':'ellipsis','text-max-width':155,'font-size':13,'text-valign':'center','text-halign':'center','background-image':'data(icon)','background-width':18,'background-height':18,'background-position-x':'5%','background-position-y':'50%'}},
    {selector:'node[?bold]',style:{'font-weight':'bold'}},{selector:'node[?focus]',style:{'border-width':3,width:215,height:60}},
    {selector:'edge',style:{width:2,label:'data(label)',color:text,'font-size':11,'line-color':dark?'#91c5ff':'#4777aa','target-arrow-color':dark?'#91c5ff':'#4777aa','target-arrow-shape':'triangle','curve-style':'bezier'}},
    {selector:'edge[kind="tree"]',style:{'line-style':'dashed','line-color':'#a594c7','target-arrow-color':'#a594c7'}}
   ]});cy.on('tap','node',event=>load(event.target.id(),true));fit();
 }
 async function load(id,push=false,refresh=false,historyIndex=null){
  const request=++sequence;controller?.abort();controller=new AbortController();status.textContent=data.loading;warning.hidden=true;frame.removeAttribute('srcdoc');open.hidden=true;
  const timer=setTimeout(()=>controller.abort(),30000);
  try{const url=new URL(data.api,location.href);url.search=new URLSearchParams({nodeId:id,_tenantView:data.token,hierarchy:String(hierarchy.checked),limit:limit.value,refresh:String(refresh)});
   const response=await fetch(url,{cache:'no-store',credentials:'same-origin',headers:{Accept:'application/json'},signal:controller.signal});
   if(!response.ok||response.redirected||!(response.headers.get('Content-Type')||'').includes('application/json'))throw Error('unavailable');
   const graph=await response.json();if(request!==sequence)return;
   if(!Array.isArray(graph.nodes)||!Array.isArray(graph.edges)||!graph.nodes.some(n=>n.id===graph.focus))throw Error('invalid');
   current=graph.focus;if(historyIndex!==null)cursor=historyIndex;else if(push||cursor<0){if(history[cursor]!==current){history=history.slice(0,cursor+1);history.push(current);cursor=history.length-1;}}
   back.disabled=cursor<=0;forward.disabled=cursor>=history.length-1;byId('graphId').value=current;
   const focus=graph.nodes.find(n=>n.id===current);byId('graphFocusName').textContent=focus.name+' ['+current+']';open.href=link(data.node,current);open.hidden=false;
   draw(graph);status.textContent=graph.nodes.length===1?data.empty:data.focusLabel+': '+focus.name;
   const warnings=[];if(graph.more)warnings.push(data.more);if(graph.partial||graph.unreadable||graph.broken)warnings.push(data.warning+' ('+graph.unreadable+' / '+graph.broken+')');warning.textContent=warnings.join(' ');warning.hidden=!warnings.length;
   try{
    const preview=await fetch(link(data.content,current,'/content'),{credentials:'same-origin',cache:'no-store',headers:{Accept:'application/json'},signal:controller.signal});
    if(!preview.ok||preview.redirected)throw Error('preview');const body=await preview.json();if(request!==sequence)return;
    if(typeof body.html!=='string')throw Error('preview');
    const doc=new DOMParser().parseFromString(body.html,'text/html');doc.querySelectorAll('a[href]').forEach(a=>{a.target='_blank';a.rel='noopener noreferrer';});
    frame.srcdoc='<!doctype html><html><head><meta charset="utf-8"><meta http-equiv="Content-Security-Policy" content="default-src \'none\'; img-src '+location.origin+' data:; style-src '+location.origin+' \'unsafe-inline\'; font-src '+location.origin+'; frame-src \'self\'; base-uri \'none\'; form-action \'none\'"><style>body{background:#fff;color:#20242a;font:16px system-ui;padding:.6rem;overflow-wrap:anywhere}img{max-width:100%;height:auto}pre{white-space:pre-wrap}table,td,th{border:1px solid #777;border-collapse:collapse}iframe{width:100%;min-height:60vh}</style></head><body>'+doc.body.innerHTML+'</body></html>';
   }catch(error){if(request===sequence){byId('graphFocusName').textContent=focus.name+' — '+data.error;}}

  }catch(error){if(request===sequence){status.textContent=data.error;cy?.destroy();cy=null;byId('graphList').replaceChildren();}}
  finally{clearTimeout(timer);}
 }
 back.addEventListener('click',()=>{if(cursor>0)load(history[cursor-1],false,false,cursor-1);});forward.addEventListener('click',()=>{if(cursor+1<history.length)load(history[cursor+1],false,false,cursor+1);});
 byId('graphJump').addEventListener('submit',event=>{event.preventDefault();const id=byId('graphId').value.trim();if(/^[1-9][0-9]{0,18}$/.test(id))load(id,true);});
 hierarchy.addEventListener('change',()=>load(current));limit.addEventListener('change',()=>load(current));byId('graphRefresh').addEventListener('click',()=>load(current,false,true));byId('graphFit').addEventListener('click',fit);
 new ResizeObserver(()=>{cy?.resize();fit();}).observe(byId('linkGraphCanvas'));window.addEventListener('pagehide',()=>{controller?.abort();cy?.destroy();});load(current,true);
})();
