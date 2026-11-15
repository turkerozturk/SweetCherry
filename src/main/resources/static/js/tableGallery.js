(() => {
    'use strict';
    const root=document.getElementById('table-gallery');if(!root)return;
    const dialog=document.getElementById('table-viewer'),cards=[...root.querySelectorAll('.table-card')];
    const stage=dialog.querySelector('[data-table-full]'),status=dialog.querySelector('[data-table-status]');
    const details=document.getElementById('table-details'),requests=new AbortController();
    let current=-1,generation=0,opener=null,running=0;const queue=[];
    const width=Number(root.dataset.cellWidth);
    if(!Number.isInteger(width)||width<20||width>480)return;
    function settings(){root.classList.toggle('hide-details',!details.checked);dialog.querySelector('[data-table-details]').setAttribute('aria-pressed',String(details.checked));try{localStorage.setItem('sc.tables.details',String(details.checked));}catch(_){}}
    try{details.checked=localStorage.getItem('sc.tables.details')!=='false';}catch(_){}
    details.addEventListener('change',settings);settings();
    async function load(url){
        for(let n=0;n<3;n++){
            const r=await fetch(url,{credentials:'same-origin',cache:'no-store',signal:requests.signal});
            if(r.status===429&&n<2){await new Promise(resolve=>setTimeout(resolve,700));continue;}
            if(!r.ok||r.redirected||!r.headers.get('Content-Type')?.startsWith('application/json'))throw Error(r.status===409?root.dataset.stale:root.dataset.error);
            return r.json();
        }
    }
    function table(data){
        if(!Number.isInteger(data.columns)||data.columns<0||data.columns>100||!Array.isArray(data.rows)||!Array.isArray(data.header)||data.rows.length>1000||data.rows.length*Math.max(1,data.columns)>10000)throw Error(root.dataset.error);
        const fragment=document.createDocumentFragment();
        if(!data.columns){const p=document.createElement('p');p.textContent=root.dataset.empty;fragment.append(p);return fragment;}
        const result=document.createElement('table');result.className='grid-table';result.style.width=String(data.columns*width)+'px';
        const head=document.createElement('thead'),body=document.createElement('tbody');
        function row(cells,header){
            if(!Array.isArray(cells)||cells.length!==data.columns||cells.some(c=>typeof c!=='string'))throw Error(root.dataset.error);
            const tr=document.createElement('tr');
            cells.forEach(text=>{const td=document.createElement(header?'th':'td');if(header)td.scope='col';const content=document.createElement('div');content.className='table-cell';content.textContent=text;td.append(content);tr.append(td);});return tr;
        }
        head.append(row(data.header,true));data.rows.forEach(cells=>body.append(row(cells,false)));result.append(head,body);fragment.append(result);return fragment;
    }
    function summary(data){return root.dataset.summary.replace('{0}',String(data.totalRows)).replace('{1}',String(data.totalColumns))+(data.limited?' · '+root.dataset.limited:'');}
    function pump(){
        while(running<2&&queue.length&&!requests.signal.aborted&&!dialog.open){
            const card=queue.shift(),preview=card.querySelector('[data-preview]');running++;
            load(preview.dataset.preview).then(data=>{preview.replaceChildren(table(data));card.querySelector('.table-summary').textContent=summary(data);}).catch(error=>{if(error.name!=='AbortError')preview.textContent=error.message;}).finally(()=>{running--;pump();});
        }
    }
    function enqueue(card){queue.push(card);pump();}
    if('IntersectionObserver' in window){const observer=new IntersectionObserver(entries=>entries.filter(e=>e.isIntersecting).forEach(e=>{observer.unobserve(e.target);enqueue(e.target);}),{rootMargin:'300px'});cards.forEach(card=>observer.observe(card));}
    else cards.forEach(enqueue);
    async function show(index){
        if(index<0||index>=cards.length)return;current=index;const version=++generation,card=cards[index];
        stage.replaceChildren();status.textContent=root.dataset.loading;
        dialog.querySelector('[data-table-position]').textContent=root.dataset.position.replace('{0}',String(index+1)).replace('{1}',String(cards.length));
        dialog.querySelector('[data-table-prev]').disabled=index===0;dialog.querySelector('[data-table-next]').disabled=index===cards.length-1;
        dialog.querySelector('[data-table-node]').replaceChildren(card.querySelector('a').cloneNode(true));
        dialog.querySelector('[data-table-info]').replaceChildren(...[...card.querySelector('.tg-info').childNodes].map(n=>n.cloneNode(true)));
        if(!dialog.open)dialog.showModal();
        try{const data=await load(card.querySelector('[data-table-open]').dataset.full);if(version!==generation||!dialog.open)return;stage.replaceChildren(table(data));stage.scrollTop=0;stage.scrollLeft=0;status.textContent=summary(data);}
        catch(error){if(version===generation&&dialog.open&&error.name!=='AbortError')status.textContent=error.message;}
    }
    cards.forEach((card,index)=>card.querySelector('[data-table-open]').addEventListener('click',event=>{opener=event.currentTarget;show(index);}));
    dialog.querySelector('[data-table-prev]').addEventListener('click',()=>show(current-1));dialog.querySelector('[data-table-next]').addEventListener('click',()=>show(current+1));
    dialog.querySelector('[data-table-close]').addEventListener('click',()=>dialog.close());
    dialog.querySelector('[data-table-details]').addEventListener('click',()=>{details.checked=!details.checked;settings();});
    dialog.addEventListener('keydown',event=>{if(event.altKey||event.ctrlKey||event.metaKey||event.shiftKey||event.target.closest('.table-scroll'))return;if(event.key==='ArrowLeft'||event.key==='ArrowRight'){event.preventDefault();show(current+(event.key==='ArrowLeft'?-1:1));}});
    dialog.addEventListener('close',()=>{generation++;stage.replaceChildren();opener?.focus();pump();});
    window.addEventListener('pagehide',()=>requests.abort());window.addEventListener('pageshow',event=>{if(event.persisted)location.reload();});
})();
