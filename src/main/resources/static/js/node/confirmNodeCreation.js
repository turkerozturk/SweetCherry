/* Delegation also covers node creation forms inserted by the desktop AJAX view. */
(() => {
    'use strict';
    if (window.sweetCherryConfirmFormsInstalled) return;
    window.sweetCherryConfirmFormsInstalled = true;
    document.addEventListener('submit', event => {
        const form = event.target.closest('form[data-confirm-message]');
        if (form && (!form.dataset.confirmKind || window.SweetCherryPreferences?.confirmationEnabled(form.dataset.confirmKind) !== false) && !window.confirm(form.dataset.confirmMessage)) event.preventDefault();
    });
})();
