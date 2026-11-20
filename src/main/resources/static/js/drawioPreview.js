(() => {
    'use strict';
    const root=document.getElementById('drawio-preview');if(!root)return;
    const frame=root.querySelector('iframe'),select=root.querySelector('[data-drawio-pages]'),status=root.querySelector('[data-drawio-status]');
    const title=root.querySelector('[data-drawio-filename]'),scale=root.querySelector('[data-drawio-scale]'),panel=root.querySelector('[data-drawio-panel]'),full=root.querySelector('[data-drawio-fullscreen]');
    const controller=new AbortController();let data=null,port=null,ready=false,version=0,frameTimer=null;
    const buttons=root.querySelectorAll('[data-drawio-command]');
    function render(){if(data){root.querySelector('[data-drawio-prev]').disabled=select.selectedIndex===0;root.querySelector('[data-drawio-next]').disabled=select.selectedIndex===data.pages.length-1;}if(!ready||!data)return;version++;status.textContent=root.dataset.loading;port.postMessage({type:'render',version,xml:data.pages[select.selectedIndex].xml,maxPageBytes:data.maxPageBytes});}
    frame.addEventListener('load',()=>{
        ready=false;port?.close();clearTimeout(frameTimer);frameTimer=setTimeout(()=>{if(!ready)status.textContent=root.dataset.error;},15000);const channel=new MessageChannel();port=channel.port1;
        port.onmessage=event=>{
            const message=event.data;
            if(message?.type==='ready'){clearTimeout(frameTimer);ready=true;render();}
            else if(message?.type==='escape'){panel.classList.remove('drawio-expanded');fullState();}
            else if(message?.version===version&&message.type==='rendered'){
                status.textContent='';if(Number.isFinite(message.scale))scale.textContent=Math.round(message.scale*100)+'%';buttons.forEach(button=>button.disabled=false);
            }else if(message?.version===version&&message.type==='error'){status.textContent=root.dataset.error;buttons.forEach(button=>button.disabled=true);}
        };port.start();frame.contentWindow.postMessage({type:'sweetcherry-drawio-connect'},'*',[channel.port2]);
    });
    // Attach the load handler before assigning src, including when static resources are cached.
    frame.src=frame.dataset.src;
    select.addEventListener('change',render);
    root.querySelector('[data-drawio-prev]').addEventListener('click',()=>{if(select.selectedIndex>0){select.selectedIndex--;render();}});
    root.querySelector('[data-drawio-next]').addEventListener('click',()=>{if(data&&select.selectedIndex<data.pages.length-1){select.selectedIndex++;render();}});
    buttons.forEach(button=>button.addEventListener('click',()=>{if(ready&&data)port.postMessage({type:button.dataset.drawioCommand,factor:button.dataset.factor==='in'?1.2:1/1.2});}));
    function fullState(){full.setAttribute('aria-pressed',String(document.fullscreenElement===panel||panel.classList.contains('drawio-expanded')));}
    full.addEventListener('click',async()=>{
        if(document.fullscreenElement===panel)await document.exitFullscreen();
        else if(panel.classList.contains('drawio-expanded'))panel.classList.remove('drawio-expanded');
        else{try{if(!panel.requestFullscreen)throw Error();await panel.requestFullscreen();}catch(_){panel.classList.add('drawio-expanded');}}
        fullState();
    });
    document.addEventListener('fullscreenchange',fullState);
    document.addEventListener('keydown',event=>{if(event.key==='Escape'&&panel.classList.contains('drawio-expanded')){panel.classList.remove('drawio-expanded');fullState();}});
    async function load(){
        const timer=setTimeout(()=>controller.abort(),30000);
        try{
            const response=await fetch(root.dataset.endpoint,{cache:'no-store',credentials:'same-origin',redirect:'error',signal:controller.signal,headers:{Accept:'application/json'}});
            if(!response.ok){
                if(response.status===409)throw Error('stale');
                if(response.status===422){try{const info=await response.json();if(info.error==='large'&&/^max-[a-z-]+$/.test(info.limit||''))throw Object.assign(Error('limit'),{limit:info.limit});}catch(error){if(error.message==='limit')throw error;}}
                throw Error('error');
            }
            if(!(response.headers.get('Content-Type')||'').includes('application/json'))throw Error('error');
            const result=await response.json();
            if(typeof result.filename!=='string'||!Array.isArray(result.pages)||result.pages.length<1||result.pages.length>2000||!Number.isSafeInteger(result.maxPageBytes)||result.maxPageBytes<1||result.maxPageBytes>67108864||result.pages.some(page=>typeof page.name!=='string'||typeof page.xml!=='string'||page.xml.length>result.maxPageBytes))throw Error('error');
            data=result;title.textContent=data.filename;select.replaceChildren();data.pages.forEach((page,index)=>{const option=document.createElement('option');option.value=String(index);option.textContent=(index+1)+'. '+page.name;select.append(option);});select.disabled=false;select.selectedIndex=0;render();
        }catch(error){status.textContent=error.message==='limit'&&error.limit?root.dataset.limit+' myapp.drawio-preview.'+error.limit:root.dataset[error.message]||root.dataset.error;}
        finally{clearTimeout(timer);}
    }
    load();window.addEventListener('pagehide',()=>{controller.abort();clearTimeout(frameTimer);port?.close();});
})();
