(() => {
    'use strict';
    const viewport=document.getElementById('drawio-viewport'),surface=document.getElementById('drawio-surface');
    let port=null,graph=null,version=0;
    function report(type,extra={}){port?.postMessage({type,version,...extra});}
    function bounds(){return graph.getGraphBounds();}
    function size(){
        const b=bounds(),w=Math.ceil(Math.max(viewport.clientWidth,b.x+b.width+40)),h=Math.ceil(Math.max(viewport.clientHeight,b.y+b.height+40));
        surface.style.width=w+'px';surface.style.height=h+'px';
        const svg=surface.querySelector('svg');if(svg){svg.setAttribute('width',w);svg.setAttribute('height',h);}
        report('rendered',{scale:graph.view.scale});
    }
    function origin(){const b=bounds(),scale=graph.view.scale,t=graph.view.translate;graph.view.setTranslate(t.x+(24-b.x)/scale,t.y+(24-b.y)/scale);}
    function fit(horizontal=false){
        graph.view.scaleAndTranslate(1,0,0);const b=bounds();
        const scale=Math.max(.02,Math.min(horizontal?4:1,(viewport.clientWidth-48)/Math.max(1,b.width),horizontal?4:(viewport.clientHeight-48)/Math.max(1,b.height)));
        graph.view.setScale(scale);origin();size();viewport.scrollLeft=0;viewport.scrollTop=0;
    }
    function zoom(factor,reset=false){
        const old=graph.view.scale,scale=reset?1:Math.max(.02,Math.min(4,old*factor));
        const x=viewport.scrollLeft+viewport.clientWidth/2,y=viewport.scrollTop+viewport.clientHeight/2;
        graph.view.setScale(scale);origin();size();viewport.scrollLeft=Math.max(0,x*scale/old-viewport.clientWidth/2);viewport.scrollTop=Math.max(0,y*scale/old-viewport.clientHeight/2);
    }
    function render(xml,maxPageBytes){
        if(!Number.isSafeInteger(maxPageBytes)||maxPageBytes<1||maxPageBytes>67108864||typeof xml!=='string'||xml.length>maxPageBytes)throw Error('invalid');
        graph?.destroy();surface.replaceChildren();surface.style.width=viewport.clientWidth+'px';surface.style.height=viewport.clientHeight+'px';
        mxStencilRegistry.allowEval=false;mxStencil.allowEval=false;
        graph=new Graph(surface);graph.setEnabled(false);graph.setConnectable(false);graph.setTooltips(false);graph.setPanning(false);
        graph.isHtmlLabel=()=>true;graph.getLinkForCell=()=>null;graph.getImage=()=>null;graph.mathEnabled=false;
        graph.enableFlowAnimation=false;graph.foldingEnabled=false;graph.resizeContainer=false;
        const doc=mxUtils.parseXml(xml);if(doc.documentElement.nodeName!=='mxGraphModel')throw Error('invalid');
        new mxCodec(doc).decode(doc.documentElement,graph.getModel());graph.refresh();fit();
    }
    window.addEventListener('message',event=>{
        if(event.source!==window.parent||event.data?.type!=='sweetcherry-drawio-connect'||!event.ports?.[0]||port)return;
        port=event.ports[0];port.onmessage=message=>{
            const data=message.data;
            try{
                if(data?.type==='render'){version=data.version;render(data.xml,data.maxPageBytes);}
                else if(graph&&data?.type==='fit')fit();
                else if(graph&&data?.type==='fit-width')fit(true);
                else if(graph&&data?.type==='zoom')zoom(data.factor===1.2?1.2:1/1.2);
                else if(graph&&data?.type==='reset')zoom(1,true);
            }catch(_){graph?.destroy();graph=null;surface.replaceChildren();report('error');}
        };port.start();report('ready');
    });
    window.addEventListener('keydown',event=>{if(event.key==='Escape')report('escape');});
    viewport.addEventListener('click',event=>{if(event.target.closest('a'))event.preventDefault();});
    viewport.addEventListener('wheel',event=>{if(graph&&!event.shiftKey&&event.deltaY!==0){event.preventDefault();zoom(event.deltaY<0?1.2:1/1.2);}},{passive:false});
    window.addEventListener('pagehide',()=>{graph?.destroy();port?.close();});
})();
