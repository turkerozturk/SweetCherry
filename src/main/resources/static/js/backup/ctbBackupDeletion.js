(() => {
    'use strict';
    const form = document.getElementById('ctb-backup-delete-selection');
    if (!form) return;
    const boxes = [...document.querySelectorAll('input[data-backup-select]')];
    const all = document.getElementById('ctb-backup-select-all');
    const count = document.getElementById('ctb-backup-selected-count');
    function update() {
        const selected = boxes.filter(box => box.checked).length;
        count.textContent = String(selected);
        if (all) { all.checked = boxes.length > 0 && selected === boxes.length; all.indeterminate = selected > 0 && selected < boxes.length; }
    }
    boxes.forEach(box => box.addEventListener('change', update));
    all?.addEventListener('change', () => { boxes.forEach(box => { box.checked = all.checked; }); update(); });
    update();
})();
