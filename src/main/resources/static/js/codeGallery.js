(() => {
    'use strict';
    const root=document.getElementById('code-gallery');if(!root)return;
    const cards=[...root.querySelectorAll('.code-card')],dialog=document.getElementById('code-viewer');
    const stage=dialog.querySelector('[data-code-stage]'),status=dialog.querySelector('[data-code-status]'),copy=dialog.querySelector('[data-code-copy]');
    const details=document.getElementById('code-details'),wrap=dialog.querySelector('[data-code-wrap]'),requests=new AbortController();
    let current=-1,generation=0,opener=null,loadedText='';
    function settings(){root.classList.toggle('hide-details',!details.checked);dialog.querySelector('[data-code-details]').setAttribute('aria-pressed',String(details.checked));try{localStorage.setItem('sc.codeboxes.details',String(details.checked));}catch(_){}}
    try{details.checked=localStorage.getItem('sc.codeboxes.details')!=='false';wrap.checked=localStorage.getItem('sc.codeboxes.wrap')==='true';}catch(_){}
    details.addEventListener('change',settings);settings();
    function wrapping(){stage.classList.toggle('wrap-code',wrap.checked);try{localStorage.setItem('sc.codeboxes.wrap',String(wrap.checked));}catch(_){}}
    wrap.addEventListener('change',wrapping);wrapping();
    async function show(index){
        if(index<0||index>=cards.length)return;current=index;const version=++generation,card=cards[index];
        stage.replaceChildren();loadedText='';copy.disabled=true;status.textContent=root.dataset.loading;
        dialog.querySelector('[data-code-position]').textContent=root.dataset.position.replace('{0}',String(index+1)).replace('{1}',String(cards.length));
        dialog.querySelector('[data-code-prev]').disabled=index===0;dialog.querySelector('[data-code-next]').disabled=index===cards.length-1;
        dialog.querySelector('[data-code-node]').replaceChildren(card.querySelector('.code-node').cloneNode(true));
        dialog.querySelector('[data-code-info]').replaceChildren(...[...card.querySelector('.cg-info').childNodes].map(n=>n.cloneNode(true)));
        if(!dialog.open)dialog.showModal();
        try{
            const response=await fetch(card.querySelector('[data-code-open]').dataset.url,{credentials:'same-origin',cache:'no-store',signal:requests.signal});
            if(!response.ok||response.redirected||!response.headers.get('Content-Type')?.startsWith('application/json'))throw Error(response.status===409?root.dataset.stale:root.dataset.error);
            const data=await response.json();if(version!==generation||!dialog.open)return;
            if(typeof data.text!=='string'||data.text.length>1000000)throw Error(root.dataset.error);
            const code=document.createElement('code');code.textContent=data.text;
            if(root.dataset.highlighting==='true'&&data.text.length<=20000&&typeof data.language==='string'&&/^[a-z0-9-]+$/.test(data.language)){
                code.className='cherry-highlight';code.dataset.language=data.language;
            }
            stage.replaceChildren(code);stage.scrollTop=0;stage.scrollLeft=0;loadedText=data.text;copy.disabled=false;status.textContent=data.limited?root.dataset.limited:'';
            document.dispatchEvent(new CustomEvent('sweetcherry:content-loaded',{detail:{root:stage}}));
        }catch(error){if(version===generation&&dialog.open&&error.name!=='AbortError')status.textContent=error.message===root.dataset.stale?root.dataset.stale:root.dataset.error;}
    }
    cards.forEach((card,index)=>card.querySelector('[data-code-open]').addEventListener('click',event=>{opener=event.currentTarget;show(index);}));
    dialog.querySelector('[data-code-prev]').addEventListener('click',()=>show(current-1));dialog.querySelector('[data-code-next]').addEventListener('click',()=>show(current+1));
    dialog.querySelector('[data-code-close]').addEventListener('click',()=>dialog.close());
    dialog.querySelector('[data-code-details]').addEventListener('click',()=>{details.checked=!details.checked;settings();});
    copy.addEventListener('click',async()=>{if(copy.disabled)return;const version=generation;try{await navigator.clipboard.writeText(loadedText);if(version===generation&&dialog.open)status.textContent=root.dataset.copied;}catch(_){if(version===generation&&dialog.open)status.textContent=root.dataset.copyError;}});
    dialog.addEventListener('keydown',event=>{if(event.altKey||event.ctrlKey||event.metaKey||event.shiftKey||event.target.closest('pre,input'))return;if(event.key==='ArrowLeft'||event.key==='ArrowRight'){event.preventDefault();show(current+(event.key==='ArrowLeft'?-1:1));}});
    dialog.addEventListener('close',()=>{generation++;loadedText='';copy.disabled=true;stage.replaceChildren();opener?.focus();});
    window.addEventListener('pagehide',()=>requests.abort());window.addEventListener('pageshow',event=>{if(event.persisted)location.reload();});
})();
