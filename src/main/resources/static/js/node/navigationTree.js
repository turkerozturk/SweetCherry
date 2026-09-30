/* Expand tree branches without navigating away from the selected node. */
(() => {
    const sidebar = document.getElementById('sideBarContent');
    if (!sidebar) return;
    const tenantView = sidebar.dataset.tenantView;

    function icon(name) {
        const img = document.createElement('img');
        img.src = '/img/icons/' + name + '.svg';
        img.alt = '';
        img.className = 'childiconcss';
        return img;
    }

    /** Builds only DOM nodes, so database titles remain text rather than HTML. */
    function renderChild(child) {
        const li = document.createElement('li');
        li.dataset.navigationId = String(child.nodeId);
        li.dataset.hasChildren = String(child.hasChildren);
        const link = document.createElement('a');
        link.href = '/nodes/' + encodeURIComponent(child.nodeId);
        link.title = child.name;
        link.className = 'linkinternal';
        if (child.readOnly) link.append(icon('ct_locked'));
        link.append(icon(child.iconName));
        const title = document.createElement('span');
        title.textContent = child.name;
        if (/^#[0-9a-fA-F]{6}$/.test(child.titleColor)) title.style.color = child.titleColor;
        if (child.bold) title.className = 'boldTitle';
        link.append(title);
        li.append(link);
        decorate(li);
        return li;
    }

    /** Adds an independent fold button while leaving the node link intact. */
    function decorate(li) {
        if (li.classList.contains('tree-enhanced')) return;
        const link = li.querySelector(':scope > a');
        if (!link) return;
        li.classList.add('tree-enhanced');
        if (li.dataset.hasChildren !== 'true') {
            const spacer = document.createElement('span');
            spacer.className = 'tree-fold tree-fold-spacer';
            spacer.setAttribute('aria-hidden', 'true');
            li.insertBefore(spacer, link);
            return;
        }
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'tree-fold';
        const existing = li.querySelector(':scope > ul, :scope > [data-tree-branch] > ul');
        button.setAttribute('aria-label', existing ? 'Alt düğümleri kapat' : 'Alt düğümleri aç');
        button.setAttribute('aria-expanded', String(Boolean(existing)));
        button.textContent = existing ? '▾' : '▸';
        li.insertBefore(button, link);
        button.addEventListener('click', async () => {
            let branch = li.querySelector(':scope > ul, :scope > [data-tree-branch] > ul');
            if (!branch) {
                button.disabled = true;
                try {
                    const params = new URLSearchParams({ fatherId: li.dataset.navigationId,
                        _tenantView: tenantView || '' });
                    const response = await fetch('/nodes/navigation/children?' + params, {
                        credentials: 'same-origin', cache: 'no-store',
                        headers: { Accept: 'application/json' }
                    });
                    if (response.status === 409) {
                        sidebar.textContent = 'Başka bir CTB seçildi. Bu sekmedeki sayfa artık güncel değil.';
                        return;
                    }
                    if (!response.ok) throw new Error('Navigation unavailable');
                    const children = await response.json();
                    branch = document.createElement('ul');
                    branch.className = 'tree-branch';
                    branch.hidden = true;
                    children.forEach(child => branch.append(renderChild(child)));
                    li.append(branch);
                } catch (error) {
                    button.title = 'Dallar yüklenemedi; tekrar deneyin.';
                    return;
                } finally {
                    button.disabled = false;
                }
            }
            branch.hidden = !branch.hidden;
            button.textContent = branch.hidden ? '▸' : '▾';
            button.setAttribute('aria-expanded', String(!branch.hidden));
            button.setAttribute('aria-label', branch.hidden ? 'Alt düğümleri aç' : 'Alt düğümleri kapat');
        });
    }

    sidebar.querySelectorAll('li[data-navigation-id]').forEach(decorate);
})();
