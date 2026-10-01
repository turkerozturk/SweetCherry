/* Delegation also covers node creation forms inserted by the desktop AJAX view. */
(() => {
    'use strict';
    document.addEventListener('submit', event => {
        const form = event.target.closest('form[data-confirm-message]');
        if (form && !window.confirm(form.dataset.confirmMessage)) event.preventDefault();
    });
})();
