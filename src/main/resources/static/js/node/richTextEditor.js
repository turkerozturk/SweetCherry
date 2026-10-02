(() => {
    'use strict';
    const input = document.getElementById('richText');
    const preview = document.getElementById('richPreview');
    const form = document.getElementById('richEditForm');
    let runs = window.richEditorDocument.runs.map(run => ({text: run.text, attributes: {...run.attributes}}));
    let previous = runs.map(run => run.text).join('');
    input.value = previous;
    const count = value => Array.from(value).length;
    const sub = (value, start, end) => Array.from(value).slice(start, end).join('');
    let selection = [0, 0];
    const pendingImages = {};
    let readingImage = false;
    const plainClipboard = text => text.replace(/\uFFFC/g, '');
    const mode = document.getElementById('richVisualMode');
    const visual = () => !!(mode && mode.checked && window.RichTextVisualModel);
    function focusEditor() { if (visual()) preview.focus(); else input.focus(); }
    function restoreSelection() {
        if (visual()) window.RichTextVisualModel.restore(preview, selection[0], selection[1]);
        else input.setSelectionRange(sub(previous,0,selection[0]).length,sub(previous,0,selection[1]).length);
    }
    /** Tracks textarea UTF-16 selection as model code-point positions before toolbar focus changes. */
    function rememberSelection() {
        if (visual()) {
            const current = window.RichTextVisualModel.selection(preview);
            if (current) selection = current;
        } else selection = [count(input.value.slice(0, input.selectionStart)), count(input.value.slice(0, input.selectionEnd))];
    }
    input.addEventListener('select', rememberSelection);
    input.addEventListener('keyup', rememberSelection);
    input.addEventListener('pointerup', rememberSelection);
    input.addEventListener('blur', rememberSelection);

    const history = [];
    let historyIndex = -1;
    let composing = false;
    const snapshot = () => ({runs: runs.map(run => ({text: run.text, attributes: {...run.attributes}})),
        selection: [...selection]});

    /** Captures text and formatting as one editor state, discarding the obsolete redo branch. */
    function commitHistory() {
        const next = snapshot();
        if (historyIndex >= 0 && JSON.stringify(history[historyIndex].runs) === JSON.stringify(next.runs)) return;
        history.splice(historyIndex + 1);
        history.push(next);
        if (history.length > 200) history.shift();
        historyIndex = history.length - 1;
        updateHistoryButtons();
    }
    function updateHistoryButtons() {
        document.getElementById('richUndo').disabled = historyIndex <= 0;
        document.getElementById('richRedo').disabled = historyIndex >= history.length - 1;
    }
    function captureSelection() {
        rememberSelection();
        if (historyIndex >= 0) history[historyIndex].selection = [...selection];
    }
    /** Restores the model, textarea and selection together instead of using textarea-only native undo. */
    function restoreHistory(direction) {
        if (composing) return;
        const next = historyIndex + direction;
        if (next < 0 || next >= history.length) return;
        historyIndex = next;
        const state = history[historyIndex];
        runs = state.runs.map(run => ({text: run.text, attributes: {...run.attributes}}));
        previous = runs.map(run => run.text).join('');
        input.value = previous;
        selection = [...state.selection];
        render(); focusEditor(); restoreSelection(); updateHistoryButtons();
    }
    form.addEventListener('keydown', event => {
        if (!(event.ctrlKey || event.metaKey) || event.altKey || event.isComposing) return;
        const key = event.key.toLowerCase();
        const shortcuts = {b:['weight','heavy'],i:['style','italic'],u:['underline','single']};
        if (visual() && shortcuts[key]) {
            event.preventDefault(); rememberSelection(); format(...shortcuts[key],true); return;
        }
        if (key === 'z' || key === 'y') {
            event.preventDefault();
            restoreHistory(key === 'y' || event.shiftKey ? 1 : -1);
        }
    });
    input.addEventListener('beforeinput', event => {
        if (event.inputType === 'historyUndo' || event.inputType === 'historyRedo') {
            event.preventDefault(); restoreHistory(event.inputType === 'historyUndo' ? -1 : 1);
        } else captureSelection();
    });
    input.addEventListener('compositionstart', () => { captureSelection(); composing = true; });
    input.addEventListener('compositionend', () => { composing = false; commitHistory(); });
    document.getElementById('richUndo').addEventListener('click', () => restoreHistory(-1));
    document.getElementById('richRedo').addEventListener('click', () => restoreHistory(1));

    /** Splits runs at editing boundaries, keeping unmodified attributes and empty runs outside the edit. */
    function replace(start, end, text) {
        let protectedOffset = 0;
        for (const run of runs) {
            const finish = protectedOffset + count(run.text);
            if (run.attributes.__sweet_object && start < finish && end > protectedOffset) {
                const hint = document.getElementById('protectedHint');
                if (hint) hint.hidden = false;
                return false;
            }
            protectedOffset = finish;
        }
        const before = [], after = [];
        let offset = 0, attributes = {};
        for (const run of runs) {
            const length = count(run.text), finish = offset + length;
            if (offset <= start && finish >= start) attributes = {...run.attributes};
            if (finish <= start) before.push(run);
            else if (offset >= end) after.push(run);
            else {
                if (offset < start) before.push({text: sub(run.text, 0, start - offset), attributes: {...run.attributes}});
                if (finish > end) after.push({text: sub(run.text, end - offset), attributes: {...run.attributes}});
            }
            offset = finish;
        }
        delete attributes.__sweet_object;
        runs = [...before, ...(text ? [{text, attributes}] : []), ...after];
        return true;
    }
    input.addEventListener('input', () => {
        const oldChars = Array.from(previous), newChars = Array.from(input.value);
        let start = 0, tail = 0;
        while (start < oldChars.length && start < newChars.length && oldChars[start] === newChars[start]) start++;
        while (tail < oldChars.length - start && tail < newChars.length - start && oldChars[oldChars.length - 1 - tail] === newChars[newChars.length - 1 - tail]) tail++;
        if (!replace(start, oldChars.length - tail, newChars.slice(start, newChars.length - tail).join(''))) {
            input.value = previous; restoreSelection(); return;
        }
        previous = input.value;
        rememberSelection(); render();
        if (!composing) commitHistory();
    });

    /** Applies a property only to selected text; alignment covers all touched paragraphs. */
    function format(key, value, toggle = false) {
        let [start, end] = selection;
        if (start === end) { document.getElementById('selectionHint').hidden = false; return; }
        captureSelection();
        if (key === 'justification') {
            const chars = Array.from(input.value);
            while (start > 0 && chars[start - 1] !== '\n') start--;
            while (end < chars.length && chars[end] !== '\n') end++;
        }
        document.getElementById('selectionHint').hidden = true;
        let offset = 0;
        const output = [];
        const selected = [];
        for (const run of runs) {
            const finish = offset + count(run.text);
            if (run.attributes.__sweet_object || finish <= start || offset >= end) output.push(run);
            else {
                const from = Math.max(start, offset) - offset, to = Math.min(end, finish) - offset;
                if (from) output.push({text: sub(run.text, 0, from), attributes: {...run.attributes}});
                const middle = {text: sub(run.text, from, to), attributes: {...run.attributes}};
                output.push(middle); selected.push(middle);
                if (to < count(run.text)) output.push({text: sub(run.text, to), attributes: {...run.attributes}});
            }
            offset = finish;
        }
        const remove = toggle && selected.every(run => run.attributes[key] === value);
        for (const run of selected) {
            if (key === 'clear') {
                for (const property of ['weight','style','underline','strikethrough','family','foreground','background','scale','justification']) delete run.attributes[property];
            } else if (remove || !value) delete run.attributes[key];
            else run.attributes[key] = value;
        }
        runs = output; render(); commitHistory(); focusEditor(); restoreSelection();
    }
    document.querySelectorAll('[data-format]').forEach(button => {
        button.addEventListener('pointerdown', event => event.preventDefault());
        button.addEventListener('click', () => format(button.dataset.format, button.dataset.value, true));
    });
    document.querySelectorAll('[data-color]').forEach(control => control.addEventListener('change', () => format(control.dataset.color, control.value)));
    document.querySelectorAll('[data-choice]').forEach(control => control.addEventListener('change', () => format(control.dataset.choice, control.value)));
    document.getElementById('clearFormat').addEventListener('click', () => format('clear', ''));

    /** Builds a safe preview from text nodes and controlled styles, never from stored HTML. */
    function render() {
        preview.replaceChildren();
        let paragraph = document.createElement('div');
        paragraph.style.margin = '0';
        function flush(allowEmpty = false) {
            if (paragraph.childNodes.length || allowEmpty) {
                if (!paragraph.childNodes.length) paragraph.appendChild(document.createElement('br'));
                preview.appendChild(paragraph);
            }
            paragraph = document.createElement('div'); paragraph.style.margin = '0';
        }
        for (const run of runs) {
          if (run.attributes.__sweet_object) {
            const key = run.attributes.__sweet_object;
            const object = (window.richEditorObjects || {})[key] || {label:key};
            const slot = document.createElement('span');
            slot.dataset.richObject = key; slot.contentEditable = 'false';
            slot.style.display = 'inline-block'; slot.style.border = '1px solid #aaa'; slot.style.padding = '4px';
            slot.title = object.label;
            if (object.imageUrl) {
                const image = document.createElement('img');
                image.src = key.startsWith('new-image:') ? object.imageUrl : object.imageUrl + '?_tenantView=' + encodeURIComponent(window.richEditorTenant || '');
                image.alt = object.label; image.style.maxWidth = '100%'; image.style.maxHeight = '220px';
                image.draggable = false; slot.appendChild(image);
            } else slot.textContent = '[' + object.label + ']';
            const caret = () => {
                const span = document.createElement('span'); span.dataset.richCaret = 'true';
                span.appendChild(document.createTextNode('\u200B')); return span;
            };
            paragraph.appendChild(caret()); paragraph.appendChild(slot); paragraph.appendChild(caret());
            continue;
          }
          const lines = run.text.split('\n');
          for (let index = 0; index < lines.length; index++) {
            const span = document.createElement('span'), a = run.attributes;
            span.textContent = lines[index];
            span.dataset.richAttributes = JSON.stringify(a);
            if (a.weight === 'heavy') span.style.fontWeight = 'bold';
            if (a.style === 'italic') span.style.fontStyle = 'italic';
            span.style.textDecoration = [a.underline === 'single' ? 'underline' : '', a.strikethrough === 'true' ? 'line-through' : ''].filter(Boolean).join(' ');
            if (a.family === 'monospace') span.style.fontFamily = 'monospace';
            for (const [attribute, css] of [['foreground','color'],['background','backgroundColor']]) {
                let color = a[attribute] || '';
                if (/^#[a-f0-9]{12}$/i.test(color)) color = '#' + color.slice(1,3) + color.slice(5,7) + color.slice(9,11);
                if (/^#[a-f0-9]{6}$/i.test(color)) span.style[css] = color;
            }
            const sizes = {h1:'2em',h2:'1.5em',h3:'1.17em',h4:'1em',h5:'.83em',h6:'.67em',small:'.83em',sub:'.83em',sup:'.83em'};
            if (sizes[a.scale]) span.style.fontSize = sizes[a.scale];
            if (a.scale === 'sub' || a.scale === 'sup') span.style.verticalAlign = a.scale === 'sup' ? 'super' : 'sub';
            if (lines[index] || run.text === '') {
                const align = {left:'left',right:'right',center:'center',fill:'justify'}[a.justification] || 'left';
                paragraph.style.textAlign = align;
                paragraph.appendChild(span);
            }
            if (index < lines.length - 1) flush(true);
          }
        }
        flush(true);
    }
    /** Serializes the text model to CTB XML; the server validates and canonicalizes it again. */
    form.addEventListener('submit', event => {
        if (readingImage) { event.preventDefault(); return; }
        const imageInput = document.getElementById('richImages');
        if (imageInput) imageInput.value = JSON.stringify(Object.fromEntries(runs.filter(run=>pendingImages[run.attributes.__sweet_object]).map(run=>[run.attributes.__sweet_object,pendingImages[run.attributes.__sweet_object]])));
        const xml = document.implementation.createDocument(null, 'node');
        for (const run of runs) {
            const element = xml.createElement('rich_text');
            for (const [key, value] of Object.entries(run.attributes)) element.setAttribute(key, value);
            element.appendChild(xml.createTextNode(run.text)); xml.documentElement.appendChild(element);
        }
        document.getElementById('richXml').value = new XMLSerializer().serializeToString(xml);
    });
    /** Applies plain text input to the same model/history used by the textarea mode. */
    function visualChange(text, start = selection[0], end = selection[1]) {
        captureSelection();
        if (!replace(start,end,text)) return;
        previous = runs.map(run=>run.text).join(''); input.value = previous;
        selection = [start + count(text), start + count(text)];
        render(); focusEditor(); restoreSelection(); commitHistory();
    }
    /** Keeps file data local until Save and adds its protected slot to the shared undo/redo model. */
    async function addImage(file) {
        const status = document.getElementById('richImageStatus');
        if (readingImage) return;
        if (!['image/png','image/jpeg'].includes(file.type) || file.size > 8000000) {
            if (status) status.textContent = status.dataset.error; return;
        }
        const at = [...selection], source = JSON.stringify(runs);
        readingImage = true;
        try {
            const data = await new Promise((resolve,reject)=>{
                const reader = new FileReader(); reader.onload=()=>resolve(reader.result); reader.onerror=reject; reader.readAsDataURL(file);
            });
            // An asynchronous read must not replace text entered while the file was loading.
            if (source !== JSON.stringify(runs)) throw new Error('Editor changed');
            if (Object.keys(pendingImages).length >= 10 || Object.values(pendingImages).reduce((n,value)=>n+value.length,0)+data.length > 15000000) throw new Error('Upload too large');
            const key = 'new-image:' + crypto.randomUUID();
            if (!replace(at[0],at[1],'\uFFFC')) return;
            let offset = 0;
            for (const run of runs) {
                if (offset === at[0] && run.text === '\uFFFC' && !run.attributes.__sweet_object) {
                    run.attributes = {__sweet_object:key}; break;
                }
                offset += count(run.text);
            }
            pendingImages[key] = data;
            window.richEditorObjects = window.richEditorObjects || {};
            window.richEditorObjects[key] = {label:file.name || 'Image',imageUrl:data};
            previous = runs.map(run=>run.text).join(''); input.value=previous;
            selection=[at[0]+1,at[0]+1]; render(); focusEditor(); restoreSelection(); commitHistory();
            if (status) status.textContent='';
        } catch (_) { if (status) status.textContent=status.dataset.error; }
        finally { readingImage=false; }
    }
    const imageButton=document.getElementById('richAddImage'),imageFile=document.getElementById('richImageFile');
    if (imageButton && imageFile) {
        imageButton.addEventListener('pointerdown',event=>event.preventDefault());
        imageButton.addEventListener('click',()=>{captureSelection();imageFile.click();});
        imageFile.addEventListener('change',()=>{if(imageFile.files[0])addImage(imageFile.files[0]);imageFile.value='';});
    }
    // Clipboard object slots are not transferable; copy only their surrounding text.
    for (const target of [input,preview]) {
        target.addEventListener('copy',event=>{
            rememberSelection();event.preventDefault();event.clipboardData.setData('text/plain',plainClipboard(sub(previous,...selection)));
        });
    }
    input.addEventListener('cut',event=>{
        rememberSelection();event.preventDefault();
        event.clipboardData.setData('text/plain',plainClipboard(sub(previous,...selection)));visualChange('');
    });
    input.addEventListener('paste',event=>{
        event.preventDefault();rememberSelection();
        const file=Array.from(event.clipboardData.files || []).find(file=>file.type.startsWith('image/'));
        if(file)addImage(file);else visualChange(plainClipboard(event.clipboardData.getData('text/plain')).replace(/\r\n?/g,'\n'));
    });
    if (mode && window.RichTextVisualModel) {
        const syncMode = () => {
            input.hidden = visual();
            const title = document.getElementById('richPreviewTitle');
            title.textContent = visual() ? title.dataset.visual : title.dataset.preview;
            document.getElementById('richTextLabel').hidden = visual();
            preview.contentEditable = visual() ? 'true' : 'false';
            preview.setAttribute('role', visual() ? 'textbox' : 'region');
            preview.setAttribute('aria-multiline','true');
            render(); focusEditor(); restoreSelection();
        };
        mode.addEventListener('change',syncMode);
        preview.addEventListener('keyup',rememberSelection);
        preview.addEventListener('pointerup',rememberSelection);
        preview.addEventListener('blur',rememberSelection);
        preview.addEventListener('beforeinput',event=>{
            if (!visual()) return;
            if (event.inputType === 'historyUndo' || event.inputType === 'historyRedo') {
                event.preventDefault(); restoreHistory(event.inputType === 'historyUndo' ? -1 : 1); return;
            }
            captureSelection();
            if (!event.cancelable || event.isComposing || composing) return;
            const formats = {formatBold:['weight','heavy'],formatItalic:['style','italic'],formatUnderline:['underline','single'],formatStrikeThrough:['strikethrough','true']};
            if (formats[event.inputType]) {event.preventDefault();format(...formats[event.inputType],true);return;}
            let [start,end]=selection;
            if (event.inputType === 'insertText' || event.inputType === 'insertReplacementText') {
                event.preventDefault(); visualChange(event.data || '',start,end);
            } else if (event.inputType === 'insertParagraph' || event.inputType === 'insertLineBreak') {
                event.preventDefault(); visualChange('\n',start,end);
            } else if (event.inputType === 'deleteContentBackward' || event.inputType === 'deleteContentForward') {
                event.preventDefault();
                if (start === end) {
                    if (event.inputType === 'deleteContentBackward') start=Math.max(0,start-1);
                    else end=Math.min(count(previous),end+1);
                }
                visualChange('',start,end);
            }
        });
        preview.addEventListener('input',()=>{
            if (!visual()) return;
            const incoming=window.RichTextVisualModel.scan(preview).runs;
            const keys=list=>list.filter(run=>run.attributes.__sweet_object).map(run=>run.attributes.__sweet_object);
            if (JSON.stringify(keys(incoming)) !== JSON.stringify(keys(runs))) {render();restoreSelection();return;}
            runs=incoming;
            previous=runs.map(run=>run.text).join('');input.value=previous;
            rememberSelection(); if (!composing) commitHistory();
        });
        preview.addEventListener('compositionstart',()=>{captureSelection();composing=true;});
        preview.addEventListener('compositionend',()=>{composing=false;commitHistory();});
        preview.addEventListener('paste',event=>{
            if (!visual()) return;
            event.preventDefault(); rememberSelection();
            const file = Array.from(event.clipboardData.files || []).find(file=>file.type.startsWith('image/'));
            if (file) { addImage(file); return; }
            visualChange(plainClipboard(event.clipboardData.getData('text/plain')).replace(/\r\n?/g,'\n'));
        });
        preview.addEventListener('cut',event=>{
            if (!visual()) return;
            rememberSelection(); event.preventDefault();
            event.clipboardData.setData('text/plain',plainClipboard(sub(previous,selection[0],selection[1])));visualChange('');
        });
        // HTML/objects are deliberately not accepted through drag-and-drop in this stage.
        preview.addEventListener('drop',event=>{if(visual())event.preventDefault();});
        syncMode();
    }
    render(); commitHistory();
})();
