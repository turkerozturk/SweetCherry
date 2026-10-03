/* Structural moves are opt-in controls in the new readers, never content editing shortcuts. */
(() => {
    'use strict';
    /** Fetches a snapshot for each newly inserted content fragment; failure leaves all controls disabled. */
    async function initialize(form) {
        if (form.dataset.moveReady) return;
        form.dataset.moveReady = 'loading';
        const buttons = [...form.querySelectorAll('[data-move]')];
        try {
            const url = new URL(form.dataset.stateUrl, window.location.href);
            url.searchParams.set('_tenantView', form.elements._tenantView.value);
            const response = await fetch(url, {credentials:'same-origin',cache:'no-store',headers:{Accept:'application/json'}});
            if (!response.ok || response.redirected) throw new Error('Move state unavailable');
            const state = await response.json();
            if (typeof state.revision !== 'string') throw new Error('Invalid move state');
            form.elements.revision.value = state.revision;
            buttons.forEach(button => {button.disabled = state[button.dataset.move.toLowerCase()] !== true;});
            form.dataset.moveReady = 'ready';
        } catch (error) {
            form.dataset.moveReady = 'failed';
            form.querySelector('[data-move-error]').textContent = form.dataset.error;
        }
    }
    function scan() { document.querySelectorAll('[data-node-move]').forEach(initialize); }
    /** Freezes a single submitted move and includes its direction even after disabling the clicked button. */
    document.addEventListener('submit', event => {
        const form = event.target;
        if (!form.matches('[data-node-move]')) return;
        const button = event.submitter;
        if (form.dataset.moveReady !== 'ready' || !button || button.disabled || !button.dataset.move) {
            event.preventDefault(); return;
        }
        form.elements.direction.value = button.dataset.move;
        form.dataset.moveReady = 'submitting';
        form.querySelectorAll('[data-move]').forEach(control => {control.disabled = true;});
    });
    document.addEventListener('keydown', event => {
        if (!document.getElementById('tree-workspace') || !event.altKey || !event.shiftKey || event.ctrlKey || event.metaKey || event.repeat) return;
        if (event.target.closest('input,textarea,select,[contenteditable="true"],dialog,[role="textbox"]')) return;
        const direction = {ArrowUp:'UP',ArrowDown:'DOWN',ArrowLeft:'LEFT',ArrowRight:'RIGHT'}[event.key];
        if (!direction) return;
        const form = document.querySelector('#workspace-content [data-node-move]');
        if (!form || form.dataset.moveReady !== 'ready') return;
        event.preventDefault();
        const button = form.querySelector('[data-move="'+direction+'"]');
        if (button && !button.disabled) form.requestSubmit(button);
    });
    new MutationObserver(scan).observe(document.body,{childList:true,subtree:true});
    scan();
})();
