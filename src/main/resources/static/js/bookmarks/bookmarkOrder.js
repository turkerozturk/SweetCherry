(() => {
    'use strict';
    const form=document.getElementById('bookmarkOrderForm');
    const body=document.querySelector('.bookmarks-table tbody');
    if(!form || !body)return;
    const rows=()=>Array.from(body.querySelectorAll('[data-bookmark-id]'));
    let drag=null,busy=false;
    function save(original) {
        const order=rows().map(row=>row.dataset.bookmarkId).join(',');
        if(order===original.map(row=>row.dataset.bookmarkId).join(','))return;
        busy=true;form.elements.namedItem('order').value=order;
        body.querySelectorAll('.bookmark-drag').forEach(button=>button.disabled=true);
        form.requestSubmit();
    }
    body.addEventListener('pointerdown',event=>{
        const handle=event.target.closest('.bookmark-drag');
        if(!handle || busy || drag || event.button!==0)return;
        event.preventDefault();handle.focus();
        drag={handle,row:handle.closest('[data-bookmark-id]'),original:rows(),pointer:event.pointerId};
        drag.row.classList.add('bookmark-dragging');handle.setPointerCapture(event.pointerId);
    });
    document.addEventListener('pointermove',event=>{
        if(!drag || event.pointerId!==drag.pointer)return;
        event.preventDefault();
        const target=document.elementFromPoint(event.clientX,event.clientY)?.closest('[data-bookmark-id]');
        if(target && target!==drag.row && target.parentElement===body) {
            const rect=target.getBoundingClientRect();
            body.insertBefore(drag.row,event.clientY<rect.top+rect.height/2?target:target.nextSibling);
            drag.handle.setPointerCapture(event.pointerId);
        }
        if(event.clientY<60)window.scrollBy(0,-20);
        else if(event.clientY>window.innerHeight-60)window.scrollBy(0,20);
    },{passive:false});
    document.addEventListener('pointerup',event=>{
        if(!drag || event.pointerId!==drag.pointer)return;
        const current=drag;drag=null;current.row.classList.remove('bookmark-dragging');save(current.original);
    });
    document.addEventListener('pointercancel',event=>{
        if(!drag || event.pointerId!==drag.pointer)return;
        drag.row.classList.remove('bookmark-dragging');drag.original.forEach(row=>body.appendChild(row));drag=null;
    });
    body.addEventListener('keydown',event=>{
        const handle=event.target.closest('.bookmark-drag');
        if(!handle || busy || drag || !['ArrowUp','ArrowDown'].includes(event.key))return;
        event.preventDefault();const original=rows(),row=handle.closest('[data-bookmark-id]'),at=original.indexOf(row);
        if(event.key==='ArrowUp' && at>0)body.insertBefore(row,original[at-1]);
        else if(event.key==='ArrowDown' && at<original.length-1)body.insertBefore(original[at+1],row);
        else return;
        save(original);
    });
})();
