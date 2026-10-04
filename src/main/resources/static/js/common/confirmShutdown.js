/* Confirm menu shutdown while leaving POST authorization and CSRF enforcement on the server. */
(function () {
    if (window.sweetCherryShutdownConfirmationInstalled) return;
    window.sweetCherryShutdownConfirmationInstalled = true;
    document.addEventListener('submit', function (event) {
        const form = event.target;
        if (form instanceof HTMLFormElement && form.hasAttribute('data-shutdown-confirm') &&
                !window.confirm(form.dataset.shutdownConfirm)) event.preventDefault();
    });
}());
