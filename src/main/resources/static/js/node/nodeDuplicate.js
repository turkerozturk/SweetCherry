/* Explicit duplicate commands in the new readers; cancellation never posts a write. */
(() => {
    'use strict';
    /** Captures the current hierarchy identity before enabling a duplication command. */
    async function initialize(form) {
        if(form.dataset.duplicateReady) return;
        form.dataset.duplicateReady='loading';
        try {
            const url=new URL(form.dataset.stateUrl,window.location.href);
            url.searchParams.set('_tenantView',form.elements._tenantView.value);
            const response=await fetch(url,{credentials:'same-origin',cache:'no-store',headers:{Accept:'application/json'}});
            if(!response.ok || response.redirected) throw new Error('Duplicate state unavailable');
            const state=await response.json();
            if(typeof state.revision!=='string' || typeof state.shared!=='boolean' || !Number.isInteger(state.subtreeCount)) throw new Error('Invalid duplicate state');
            form.elements.revision.value=state.revision;
            form.dataset.subtreeCount=String(state.subtreeCount);
            form.querySelectorAll('[data-duplicate]').forEach(button=>{button.disabled=button.dataset.duplicate==='subtree' && (state.shared || state.subtreeCount<1);});
            form.dataset.duplicateReady='ready';
        } catch(error) {
            form.dataset.duplicateReady='failed';form.querySelector('[data-duplicate-error]').textContent=form.dataset.error;
        }
    }
    function scan(){document.querySelectorAll('[data-node-duplicate]').forEach(initialize);}
    /** Locks repeated submissions only after the user confirms the selected copy operation. */
    document.addEventListener('submit',event=>{
        const form=event.target;if(!form.matches('[data-node-duplicate]'))return;
        const button=event.submitter;
        if(form.dataset.duplicateReady!=='ready' || !button || button.disabled || !['single','subtree'].includes(button.dataset.duplicate)) {event.preventDefault();return;}
        const subtree=button.dataset.duplicate==='subtree';
        const message=subtree?form.dataset.confirmSubtree.replace('%COUNT%',form.dataset.subtreeCount):form.dataset.confirmSingle;
        if(!window.confirm(message)){event.preventDefault();return;}
        form.elements.withSubnodes.value=String(subtree);form.dataset.duplicateReady='submitting';
        form.querySelectorAll('[data-duplicate]').forEach(control=>{control.disabled=true;});
    });
    new MutationObserver(scan).observe(document.body,{childList:true,subtree:true});scan();
})();
