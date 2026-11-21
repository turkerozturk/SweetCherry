(() => {
    'use strict';
    const viewport=document.getElementById('drawio-viewport'),surface=document.getElementById('drawio-surface');
    let port=null,graph=null,version=0;
    function report(type,extra={}){port?.postMessage({type,version,...extra});}
    function bounds(){return graph.getGraphBounds();}
    function size(minWidth=0,minHeight=0){
        const b=bounds(),w=Math.ceil(Math.max(minWidth,viewport.clientWidth,b.x+b.width+40)),h=Math.ceil(Math.max(minHeight,viewport.clientHeight,b.y+b.height+40));
        surface.style.width=w+'px';surface.style.height=h+'px';
        const svg=surface.querySelector('svg');if(svg){svg.setAttribute('width',w);svg.setAttribute('height',h);}
        report('rendered',{scale:graph.view.scale,missing:window.SweetCherryDrawioLibraries?.warnings()||[]});
    }
    function origin(){const b=bounds(),scale=graph.view.scale,t=graph.view.translate;graph.view.setTranslate(t.x+(24-b.x)/scale,t.y+(24-b.y)/scale);}
    function fit(horizontal=false){
        graph.view.scaleAndTranslate(1,0,0);const b=bounds();
        const scale=Math.max(.02,Math.min(horizontal?4:1,(viewport.clientWidth-48)/Math.max(1,b.width),horizontal?4:(viewport.clientHeight-48)/Math.max(1,b.height)));
        graph.view.setScale(scale);origin();size();viewport.scrollLeft=0;viewport.scrollTop=0;
    }
    function zoom(factor,reset=false,pointer=null){
        const old=graph.view.scale,scale=reset?1:Math.max(.02,Math.min(4,old*factor)),t=graph.view.translate;
        const anchorX=pointer?.x??viewport.clientWidth/2,anchorY=pointer?.y??viewport.clientHeight/2;
        const modelX=(viewport.scrollLeft+anchorX)/old-t.x,modelY=(viewport.scrollTop+anchorY)/old-t.y;
        let tx=t.x,ty=t.y,left=(modelX+tx)*scale-anchorX,top=(modelY+ty)*scale-anchorY;
        // Leave room for the anchor even when zooming out would otherwise clamp a scrollbar to zero.
        if(left<0){tx-=left/scale;left=0;}if(top<0){ty-=top/scale;top=0;}
        graph.view.scaleAndTranslate(scale,tx,ty);size(left+viewport.clientWidth,top+viewport.clientHeight);
        viewport.scrollLeft=left;viewport.scrollTop=top;
    }
    function placeholders(show){document.body?.classList?.toggle('sc-hide-placeholders',show===false);}
    function safeLink(value){
        if(typeof value!=='string'||value.length>4096||/[\u0000-\u001f\u007f\\]/.test(value))return null;
        if(value.startsWith('data:page/id,'))return value.substring(13).trim()&&value.length<=268?value:null;
        if(!/^https?:\/\//i.test(value)||/\s/.test(value))return null;
        try{const url=new URL(value);return (url.protocol==='http:'||url.protocol==='https:')&&!url.username&&!url.password&&!/%(?:0[0-9a-f]|1[0-9a-f]|7f)/i.test(value)?url.href:null;}catch(_){return null;}
    }
    function navigate(value){
        const link=safeLink(value);if(!link)return null;
        if(link.startsWith('data:page/id,')){report('navigate',{link});return null;}
        // Called synchronously by the vendor's trusted mouse/touch gesture handler.
        if(window.navigator?.userActivation?.isActive===false)return null;
        return window.open(link,'_blank','noopener,noreferrer');
    }
    function installLinks(current){
        // The vendor registers a document-level mouseleave handler; release it when changing pages.
        const listeners=[],add=mxEvent.addListener,destroy=current.destroy;
        current.destroy=function(){for(const [node,type,handler] of listeners)mxEvent.removeListener(node,type,handler);listeners.length=0;return destroy.call(this);};
        try{
            mxEvent.addListener=function(node,type,handler,...rest){if(node===document||node===window)listeners.push([node,type,handler]);return add.call(this,node,type,handler,...rest);};
            current.addClickHandler('#3584e4',(event,link)=>{if(link?.startsWith('data:page/id,')){if(event.isTrusted)navigate(link);mxEvent.consume(event);}});
        }finally{mxEvent.addListener=add;}
    }
    function render(xml,maxPageBytes){
        if(!Number.isSafeInteger(maxPageBytes)||maxPageBytes<1||maxPageBytes>67108864||typeof xml!=='string'||xml.length>maxPageBytes)throw Error('invalid');
        graph?.destroy();surface.replaceChildren();surface.style.width=viewport.clientWidth+'px';surface.style.height=viewport.clientHeight+'px';
        window.SweetCherryDrawioLibraries?.begin();
        mxStencilRegistry.allowEval=false;mxStencil.allowEval=false;
        graph=new Graph(surface);graph.setEnabled(false);graph.setConnectable(false);graph.setTooltips(false);graph.setPanning(false);
        graph.isHtmlLabel=()=>true;graph.getLinkForCell=cell=>safeLink(cell?.scLink);graph.getAbsoluteUrl=value=>safeLink(value);graph.openLink=navigate;graph.linkPolicy="blank";graph.getImage=()=>null;graph.mathEnabled=false;
        window.SweetCherryDrawioLibraries?.attach(graph);
        graph.enableFlowAnimation=false;graph.foldingEnabled=false;graph.resizeContainer=false;
        const doc=mxUtils.parseXml(xml);if(doc.documentElement.nodeName!=='mxGraphModel')throw Error('invalid');
        new mxCodec(doc).decode(doc.documentElement,graph.getModel());
        installLinks(graph);
        graph.refresh();fit();
    }
    window.addEventListener('message',event=>{
        if(event.source!==window.parent||event.data?.type!=='sweetcherry-drawio-connect'||!event.ports?.[0]||port)return;
        port=event.ports[0];port.onmessage=message=>{
            const data=message.data;
            try{
                if(data?.type==='render'){placeholders(data.showPlaceholders);version=data.version;render(data.xml,data.maxPageBytes);}
                else if(data?.type==='placeholders'){placeholders(data.show);graph?.refresh();if(graph)size();}
                else if(graph&&data?.type==='fit')fit();
                else if(graph&&data?.type==='fit-width')fit(true);
                else if(graph&&data?.type==='zoom')zoom(data.factor===1.2?1.2:1/1.2);
                else if(graph&&data?.type==='reset')zoom(1,true);
            }catch(_){graph?.destroy();graph=null;surface.replaceChildren();report('error');}
        };port.start();report('ready');
    });
    window.addEventListener('keydown',event=>{if(event.key==='Escape')report('escape');});
    viewport.addEventListener('click',event=>{
        const anchor=event.target.closest('a');if(!anchor)return;
        const link=safeLink(anchor.getAttribute('href')||anchor.getAttribute('xlink:href'));
        if(!event.isTrusted||!link){event.preventDefault();return;}
        if(link.startsWith('data:page/id,')){event.preventDefault();navigate(link);}
        else{anchor.setAttribute('href',link);anchor.setAttribute('target','_blank');anchor.setAttribute('rel','noopener noreferrer');}
    });
    viewport.addEventListener('wheel',event=>{if(graph&&!event.shiftKey&&event.deltaY!==0){event.preventDefault();const rect=viewport.getBoundingClientRect?.()||{left:0,top:0};
        const pointer=Number.isFinite(event.clientX)&&Number.isFinite(event.clientY)?{x:Math.max(0,Math.min(viewport.clientWidth,event.clientX-rect.left)),y:Math.max(0,Math.min(viewport.clientHeight,event.clientY-rect.top))}:null;zoom(event.deltaY<0?1.2:1/1.2,false,pointer);}},{passive:false});
    window.addEventListener('pagehide',()=>{graph?.destroy();port?.close();});
})();
