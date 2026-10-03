/* Bookmark occurrence IDs are independent from their content master IDs. */
(() => {
    'use strict';
    /** Checks the current bookmark set before enabling Add; failure leaves writes disabled. */
    async function initialize(form) {
        if(form.dataset.bookmarkReady)return;
        form.dataset.bookmarkReady='loading';const button=form.querySelector('button[type="submit"]');
        try {
            const url=new URL(form.dataset.stateUrl,window.location.href);
            url.searchParams.set('_tenantView',form.elements._tenantView.value);
            const response=await fetch(url,{credentials:'same-origin',cache:'no-store',headers:{Accept:'application/json'}});
            if(!response.ok || response.redirected)throw new Error('Bookmark state unavailable');
            const state=await response.json();if(typeof state.bookmarked!=='boolean')throw new Error('Invalid bookmark state');
            button.disabled=state.bookmarked;form.dataset.bookmarkReady=state.bookmarked?'exists':'ready';
            if(state.bookmarked)form.querySelector('[data-bookmark-label]').textContent=form.dataset.existing;
        } catch(error) {form.dataset.bookmarkReady='failed';form.querySelector('[data-bookmark-error]').textContent=form.dataset.error;}
    }
    document.addEventListener('submit',event=>{
        const form=event.target;if(!form.matches('[data-bookmark-add]'))return;
        if(form.dataset.bookmarkReady!=='ready'){event.preventDefault();return;}
        form.dataset.bookmarkReady='submitting';form.querySelector('button[type="submit"]').disabled=true;
    });
    function scan(){document.querySelectorAll('[data-bookmark-add]').forEach(initialize);}
    new MutationObserver(scan).observe(document.body,{childList:true,subtree:true});scan();
})();
