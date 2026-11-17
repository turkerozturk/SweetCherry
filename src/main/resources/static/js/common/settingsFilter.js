/* Filtering changes visibility only: every setting remains enabled and is submitted. */
(() => {
    'use strict';
    const search = document.querySelector('[data-settings-search]'); if (!search) return;
    const form = search.closest('form'), rows = [...form.querySelectorAll('[data-setting-row]')];
    const count = form.querySelector('[data-settings-count]'), empty = form.querySelector('[data-settings-empty]');
    const fold = value => value.normalize('NFKD').toLowerCase().replace(/\p{M}/gu,'').replace(/ı/g,'i');
    function apply() {
        const terms = fold(search.value).trim().split(/\s+/).filter(Boolean); let visible = 0;
        for (const row of rows) {
            const cells = row.querySelectorAll('td');
            const values = [...row.querySelectorAll('input,select')].map(input => input.value + ' ' + (input.selectedOptions?.[0]?.textContent || ''));
            const text = fold((cells[0]?.textContent || '') + ' ' + (cells[2]?.textContent || '') + ' ' + values.join(' '));
            row.hidden = !terms.every(term => text.includes(term)); if (!row.hidden) visible++;
        }
        count.textContent = visible + ' / ' + rows.length + ' ' + count.dataset.label; empty.hidden = visible !== 0;
    }
    function clear() { search.value = ''; apply(); search.focus(); }
    search.addEventListener('input',apply); search.addEventListener('keydown',event => { if(event.key === 'Escape'){event.preventDefault();clear();} });
    form.querySelector('[data-settings-clear]').addEventListener('click',clear);
    form.addEventListener('change',apply);
    form.addEventListener('invalid',event => { if(event.target.closest('[data-setting-row]')?.hidden){clear();event.target.focus();} },true);
    form.querySelector('[data-settings-filter]').hidden = false; apply();
})();
