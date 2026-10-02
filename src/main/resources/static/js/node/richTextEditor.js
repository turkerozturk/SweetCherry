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
        runs = [...before, ...(text ? [{text, attributes}] : []), ...after];
    }
    input.addEventListener('input', () => {
        const oldChars = Array.from(previous), newChars = Array.from(input.value);
        let start = 0, tail = 0;
        while (start < oldChars.length && start < newChars.length && oldChars[start] === newChars[start]) start++;
        while (tail < oldChars.length - start && tail < newChars.length - start && oldChars[oldChars.length - 1 - tail] === newChars[newChars.length - 1 - tail]) tail++;
        replace(start, oldChars.length - tail, newChars.slice(start, newChars.length - tail).join(''));
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
            if (finish <= start || offset >= end) output.push(run);
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
    form.addEventListener('submit', () => {
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
        replace(start,end,text);
        previous = runs.map(run=>run.text).join(''); input.value = previous;
        selection = [start + count(text), start + count(text)];
        render(); focusEditor(); restoreSelection(); commitHistory();
    }
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
            runs=window.RichTextVisualModel.scan(preview).runs;
            previous=runs.map(run=>run.text).join('');input.value=previous;
            rememberSelection(); if (!composing) commitHistory();
        });
        preview.addEventListener('compositionstart',()=>{captureSelection();composing=true;});
        preview.addEventListener('compositionend',()=>{composing=false;commitHistory();});
        preview.addEventListener('paste',event=>{
            if (!visual()) return;
            event.preventDefault();rememberSelection();visualChange(event.clipboardData.getData('text/plain').replace(/\r\n?/g,'\n'));
        });
        preview.addEventListener('cut',event=>{
            if (!visual()) return;
            rememberSelection(); event.preventDefault();
            event.clipboardData.setData('text/plain',sub(previous,selection[0],selection[1]));visualChange('');
        });
        // HTML/objects are deliberately not accepted through drag-and-drop in this stage.
        preview.addEventListener('drop',event=>{if(visual())event.preventDefault();});
        syncMode();
    }
    render(); commitHistory();
})();
