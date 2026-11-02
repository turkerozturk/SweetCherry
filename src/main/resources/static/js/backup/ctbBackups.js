(() => {
    'use strict';
    const job = document.getElementById('ctb-backup-job');
    if (!job || job.dataset.running !== 'true') return;
    const phase = document.getElementById('ctb-backup-phase');
    const progress = document.getElementById('ctb-backup-progress');
    async function poll() {
        try {
            const response = await fetch(job.dataset.statusUrl, {credentials: 'same-origin', cache: 'no-store', headers: {Accept: 'application/json'}});
            if (!response.ok || !response.headers.get('content-type')?.includes('application/json')) throw new Error('Unavailable');
            const status = await response.json();
            if (status.state !== 'RUNNING') { window.location.reload(); return; }
            phase.textContent = job.dataset[status.phase.toLowerCase()] || '';
            if (status.total > 0) progress.value = Math.max(0, Math.min(99, Math.floor(100 * (status.total - status.remaining) / status.total)));
            else progress.removeAttribute('value');
            window.setTimeout(poll, 2000);
        } catch (_) { phase.textContent = job.dataset.paused; }
    }
    window.setTimeout(poll, 1000);
})();
