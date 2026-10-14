(() => {
    function initialize() {
        document.querySelectorAll('[data-icon-picker]').forEach(picker => {
            const search = picker.querySelector('[data-icon-search]');
            const details = picker.querySelector('[data-icon-details]');
            const choices = Array.from(picker.querySelectorAll('.icon-choice'));
            const selected = picker.querySelector('[data-icon-selected]');
            const normalize = value => value.normalize('NFKD').replace(/\p{M}/gu, '').toLowerCase();
            function filter() {
                const term = normalize(search.value.trim());
                choices.forEach(choice => {
                    choice.hidden = !normalize(choice.querySelector('[data-icon-label]').textContent).includes(term);
                });
                if (term) details.open = true;
            }
            function updateSelected() {
                selected.replaceChildren();
                choices.filter(choice => choice.querySelector('input').checked).forEach(choice => {
                    const badge = document.createElement('span');
                    badge.className = 'badge text-bg-secondary d-inline-flex align-items-center gap-1';
                    const label = choice.querySelector('[data-icon-label]').textContent;
                    badge.title = label;
                    const source = choice.querySelector('[data-icon-src]');
                    if (source) {
                        const image = document.createElement('img');
                        image.src = source.dataset.iconSrc;
                        image.width = 20; image.height = 20; image.alt = '';
                        badge.append(image);
                    }
                    // Text nodes preserve safety even if an icon label contains markup.
                    badge.append(document.createTextNode(label));
                    selected.append(badge);
                });
            }
            details.addEventListener('toggle', () => {
                if (details.open) picker.querySelectorAll('[data-icon-src]').forEach(image => {
                    if (!image.hasAttribute('src')) image.src = image.dataset.iconSrc;
                });
            });
            search.addEventListener('input', filter);
            search.addEventListener('keydown', event => { if (event.key === 'Enter') event.preventDefault(); });
            picker.addEventListener('change', event => {
                if (event.target.matches('input[type="radio"],input[type="checkbox"]')) updateSelected();
            });
            updateSelected();
        });
    }
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', initialize);
    else initialize();
})();
