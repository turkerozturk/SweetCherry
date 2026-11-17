(() => {
    'use strict';
    const widget = document.getElementById('reminderAlarms'), state = window.SweetCherryAlarmState;
    if (!widget || !state) return;
    const enable = widget.querySelector('[data-alarm-enable]'), sound = widget.querySelector('[data-alarm-sound]');
    const cards = widget.querySelector('[data-alarm-cards]'), status = widget.querySelector('[data-alarm-status]');
    const csrf = widget.querySelector('form'), prefs = 'sweetcherry-reminder-enabled:' + widget.dataset.scope;
    const soundPrefs = 'sweetcherry-reminder-sound:' + widget.dataset.scope;
    const soundAllowed = widget.dataset.soundAllowed !== 'false';
    let soundWanted = soundAllowed && widget.dataset.defaultSound !== 'false';
    let enabled = widget.dataset.defaultEnabled !== 'false', stopped = false, running = false, soundOn = false, pageActive = true, audio, controller, currentFeed, signature = '';
    let memory = Object.create(null), generation = 0, actions = 0; const tones = new Set();
    const text = name => widget.dataset[name] || '';
    try { enabled = localStorage.getItem(prefs) !== 'false'; soundWanted = soundWanted && localStorage.getItem(soundPrefs) !== 'false'; } catch (_) { }
    function load(store) {
        try { return state.merge(memory[store], JSON.parse(localStorage.getItem('sweetcherry-reminder-ledger:' + store) || '{}')); }
        catch (_) { return state.ledger(memory[store]); }
    }
    function save(store, receipts) {
        memory[store] = receipts;
        try { localStorage.setItem('sweetcherry-reminder-ledger:' + store, JSON.stringify(receipts)); } catch (_) { }
    }
    function labels() { enable.textContent = text(enabled ? 'stop' : 'start'); sound.disabled = !enabled || !soundAllowed; sound.textContent = text(soundOn ? 'soundOff' : 'soundOn'); }
    function silence() { soundOn = false; for (const tone of tones) { try { tone.stop(); } catch (_) { } } tones.clear(); if (audio) audio.suspend().catch(() => {}); labels(); }
    function stop(message) { generation++; stopped = true; controller?.abort(); silence(); cards.replaceChildren(); status.textContent = message; clearInterval(timer); enable.disabled = true; sound.disabled = true; }
    async function request(method, item, action) {
        const url = new URL(widget.dataset.endpoint + (item ? '/' + encodeURIComponent(item.id) : ''), location.href);
        url.searchParams.set('_tenantView', widget.dataset.token);
        const local = new AbortController(); controller = local; const timeout = setTimeout(() => local.abort(), 12000);
        const body = method === 'POST' ? new FormData(csrf) : undefined;
        if (body) { body.set('key', item.key); body.set('action', action); body.set('_tenantView', widget.dataset.token); }
        try {
            const response = await fetch(url, {method, body, credentials:'same-origin', cache:'no-store', redirect:'error', signal:local.signal, headers:{Accept:'application/json'}});
            if (!(response.headers.get('Content-Type') || '').includes('application/json')) throw Error('error');
            const data = await response.json();
            if (!response.ok) {
                if (response.status === 404 && data.error === 'disabled') { stop(text('disabled')); throw Error('disabled'); }
                if (response.status === 401 || response.status === 409 && data.error === 'stale-source') { stop(text('stale')); throw Error('stale'); }
                throw Error(response.status === 403 ? 'readonly' : 'error');
            }
            return data;
        } finally { clearTimeout(timeout); }
    }
    async function lock(name, work, wait = false) {
        if (navigator.locks?.request) return navigator.locks.request(name, wait ? {} : {ifAvailable:true}, found => found ? work() : undefined);
        if (document.hasFocus()) return work();
    }
    function beep() {
        if (!soundOn || !audio || audio.state !== 'running') return;
        const start = audio.currentTime;
        for (const offset of [0, .24]) {
            const tone = audio.createOscillator(), gain = audio.createGain();
            tone.type = 'sine'; tone.frequency.value = 880; gain.gain.setValueAtTime(0,start+offset);
            gain.gain.linearRampToValueAtTime(.035,start+offset+.02); gain.gain.linearRampToValueAtTime(0,start+offset+.18);
            tones.add(tone); tone.connect(gain); gain.connect(audio.destination); tone.start(start+offset); tone.stop(start+offset+.2);
            tone.onended = () => { tones.delete(tone); tone.disconnect(); gain.disconnect(); };
        }
    }
    async function action(item, kind, button) {
        generation++; actions++; button.disabled = true;
        try {
            if (!currentFeed?.canAct && kind === 'dismiss') {
                await lock('sweetcherry-reminder-receipts:' + currentFeed.storeId, () => { save(currentFeed.storeId, state.dismiss(item, load(currentFeed.storeId), Date.now())); signature = ''; render(currentFeed); }, true); return;
            }
            await request('POST', item, kind); signature = ''; await refresh();
        } catch (error) { if (!stopped) status.textContent = text(error.message === 'readonly' ? 'readonly' : 'error'); }
        finally { actions--; if (button.isConnected) button.disabled = false; }
    }
    function render(feed) {
        const presentation = state.present(feed.items, load(feed.storeId), Date.now());
        const audible = soundOn && audio?.state === 'running';
        const soundClaim = audible ? state.claimSound(presentation.rows,presentation.ledger,Date.now()) : {claimed:false,ledger:presentation.ledger};
        save(feed.storeId, soundClaim.ledger); currentFeed = feed;
        const next = JSON.stringify([presentation.rows,feed.canAct,feed.total]);
        if (next !== signature) {
            signature = next; cards.replaceChildren();
            for (const item of presentation.rows) {
                const card = document.createElement('section'); card.className = 'reminder-alarm-card';
                const title = document.createElement('strong'); title.textContent = item.name;
                const note = document.createElement('p'); note.className = 'reminder-alarm-note'; note.textContent = item.message;
                const date = document.createElement('small'); date.textContent = item.dueText;
                const buttons = document.createElement('div'); buttons.className = 'reminder-alarm-actions';
                const open = document.createElement('a'), url = new URL(widget.dataset.nodes + encodeURIComponent(item.nodeId), location.href);
                url.searchParams.set('_tenantView',widget.dataset.token); open.href = url.pathname+url.search; open.textContent = text('open'); open.className = 'btn btn-sm btn-outline-primary'; buttons.append(open);
                for (const kind of feed.canAct ? ['snooze','dismiss'] : ['dismiss']) {
                    const button = document.createElement('button'); button.type = 'button'; button.className = 'btn btn-sm btn-outline-secondary'; button.textContent = text(kind);
                    button.addEventListener('click', () => action(item,kind,button)); buttons.append(button);
                }
                card.append(title,note,date,buttons); cards.append(card);
            }
        }
        status.textContent = feed.total > feed.items.length ? text('more') : presentation.rows.length ? text('due') : text('quiet');
        if (presentation.fresh || soundClaim.claimed) widget.open = true;
        if (soundClaim.claimed) beep();
    }
    async function refresh() {
        if (!enabled || stopped || !widget.isConnected) return;
        const current = generation, feed = await request('GET'); if (!state.valid(feed)) throw Error('error');
        if (!enabled || stopped || current !== generation) return;
        if (!feed.storeId) { signature = ''; currentFeed = feed; cards.replaceChildren(); status.textContent = text('quiet'); return; }
        await lock('sweetcherry-reminder-receipts:' + feed.storeId, () => { if (current === generation && enabled && !stopped) render(feed); });
    }
    async function poll() {
        if (!pageActive || !enabled || stopped || running || actions) return;
        if (!widget.isConnected) { stop(''); return; }
        running = true;
        try { await lock('sweetcherry-reminder-poll:' + widget.dataset.token, refresh); }
        catch (_) { if (!stopped && enabled) status.textContent = text('error'); }
        finally { running = false; }
    }
    function activate(value, persist) {
        if (stopped) return;
        generation++; enabled = value; if (persist) { try { localStorage.setItem(prefs,String(enabled)); } catch (_) { } }
        labels(); signature = ''; if (enabled) { widget.open = true; armSound(); poll(); }
        else { controller?.abort(); cards.replaceChildren(); silence(); status.textContent = text('disabled'); }
    }
    enable.addEventListener('click', () => activate(!enabled,true));
    function rememberSound(value) {
        soundWanted = value && soundAllowed;
        try { localStorage.setItem(soundPrefs,String(soundWanted)); } catch (_) { }
    }
    async function armSound(test = false) {
        if (!pageActive || !enabled || stopped || !soundWanted || !soundAllowed) return;
        try {
            const Context = window.AudioContext || window.webkitAudioContext; if (!Context) throw Error();
            audio ||= new Context(); await audio.resume();
            if (!pageActive || !enabled || stopped || !soundWanted) return;
            soundOn = audio.state === 'running'; labels();
            if (!soundOn) throw Error();
            if (test) beep();
            poll();
        } catch (_) { if (enabled && !stopped && soundWanted) { soundOn = false; labels(); status.textContent = text('soundBlocked'); } }
    }
    window.addEventListener('storage', event => {
        if (event.key === prefs || event.key === null) activate(event.newValue !== 'false',false);
        if (event.key === soundPrefs || event.key === null) {
            soundWanted = soundAllowed && event.newValue !== 'false';
            if (soundWanted) armSound(); else silence();
        }
    });
    sound.addEventListener('click', () => {
        if (!enabled || stopped || !soundAllowed) return;
        if (soundOn) { rememberSound(false); silence(); return; }
        rememberSound(true); return armSound(true);
    });
    function unlock(event) {
        if (event.isTrusted === false || event.target === sound || event.target === enable) return;
        if (!soundOn) armSound();
    }
    document.addEventListener('pointerdown', unlock);
    document.addEventListener('keydown', unlock);
    window.addEventListener('pagehide', () => { pageActive = false; generation++; controller?.abort(); silence(); });
    window.addEventListener('pageshow', () => { pageActive = true; armSound(); poll(); }); document.addEventListener('visibilitychange', poll); window.addEventListener('focus', poll);
    labels(); const timer = setInterval(poll,20000); if (enabled) { armSound(); poll(); } else status.textContent = text('disabled');
})();
