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
    /** Tracks textarea UTF-16 selection as model code-point positions before toolbar focus changes. */
    function rememberSelection() {
        selection = [count(input.value.slice(0, input.selectionStart)), count(input.value.slice(0, input.selectionEnd))];
    }
    input.addEventListener('select', rememberSelection);
    input.addEventListener('keyup', rememberSelection);
    input.addEventListener('pointerup', rememberSelection);
    input.addEventListener('blur', rememberSelection);

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
    });

    /** Applies a property only to selected text; alignment covers all touched paragraphs. */
    function format(key, value, toggle = false) {
        let [start, end] = selection;
        if (start === end) { document.getElementById('selectionHint').hidden = false; return; }
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
        runs = output; render(); input.focus();
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
        for (const run of runs) {
            const span = document.createElement('span'), a = run.attributes;
            span.textContent = run.text;
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
            preview.appendChild(span);
        }
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
    render();
})();
