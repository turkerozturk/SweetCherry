(() => {
    'use strict';
    const input = document.getElementById('nodeText');
    const button = document.getElementById('plainSeparator');
    let composing = false;
    input.addEventListener('compositionstart', () => { composing = true; });
    input.addEventListener('compositionend', () => { composing = false; });
    button.addEventListener('pointerdown', event => event.preventDefault());
    button.addEventListener('click', () => {
        if (composing) return;
        input.focus();
        input.setRangeText('~'.repeat(33) + '\n', input.selectionStart, input.selectionEnd, 'end');
        input.dispatchEvent(new Event('input', {bubbles:true}));
    });
})();
