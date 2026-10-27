/* Delegation also covers node creation forms inserted by the desktop AJAX view. */
(() => {
    'use strict';
    if (window.sweetCherryConfirmFormsInstalled) return;
    window.sweetCherryConfirmFormsInstalled = true;
    document.addEventListener('submit', event => {
        const form = event.target.closest('form[data-confirm-message]');
        if (form && !window.confirm(form.dataset.confirmMessage)) event.preventDefault();
    });
})();
