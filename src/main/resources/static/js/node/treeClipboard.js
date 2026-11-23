(() => {
    'use strict';
    const feedback = document.getElementById('tree-clipboard-status');
    const dialog = document.getElementById('tree-clipboard-dialog');
    if (!feedback || !dialog) return;

    const textArea = dialog.querySelector('textarea');
    dialog.addEventListener('close', () => { textArea.value = ''; });

    async function copyText(text) {
        try {
            await navigator.clipboard.writeText(text);
            feedback.textContent = feedback.dataset.copied;
        } catch {
            textArea.value = text;
            if (!dialog.open) dialog.showModal();
            textArea.focus();
            textArea.select();
        }
    }

    document.addEventListener('click', async event => {
        const button = event.target.closest('[data-copy-node-id], [data-copy-node]');
        if (!button || button.disabled) return;
        const id = button.dataset.copyNodeId;
        if (id) {
            if (window.confirm(feedback.dataset.confirm + '\n' + id)) await copyText(id);
            return;
        }

        button.disabled = true;
        try {
            const response = await fetch(button.dataset.copyNode, {cache: 'no-store', credentials: 'same-origin'});
            if (!response.ok || response.redirected || !response.headers.get('Content-Type')?.startsWith('text/plain')) {
                throw new Error('Copy content unavailable');
            }
            const text = await response.text();
            if (button.isConnected) await copyText(text);
        } catch {
            if (button.isConnected) window.alert(feedback.dataset.failed);
        } finally {
            button.disabled = false;
        }
    });
})();
