(function (root) {
    'use strict';
    /** Reads controlled editor DOM into text runs and records code-point selection boundaries. */
    function scan(editor) {
        const runs = [], positions = new Map();
        let offset = 0;
        const count = text => Array.from(text).length;
        const add = (text, attributes, node) => {
            runs.push({text, attributes:{...attributes}});
            if (node) positions.set(node, {start:offset, end:offset + count(text)});
            offset += count(text);
        };
        function visit(node, inherited) {
            if (node.nodeType === 3) { add(node.nodeValue, inherited, node); return; }
            if (node.nodeType !== 1) return;
            if (node.dataset && node.dataset.richObject) {
                const start = offset;
                add('\uFFFC', {__sweet_object:node.dataset.richObject});
                positions.set(node,{start,end:offset,boundaries:[start,offset]});
                return;
            }
            let attributes = inherited;
            if (node.dataset && node.dataset.richAttributes) {
                try { attributes = JSON.parse(node.dataset.richAttributes); } catch (_) { attributes = inherited; }
            }
            const start = offset, boundaries = [];
            const children = Array.from(node.childNodes);
            let hadContent = false;
            for (const child of children) {
                const block = child.nodeType === 1 && ['DIV','P'].includes(child.tagName);
                if (block && hadContent) add('\n', attributes);
                boundaries.push(offset);
                if (child.nodeType === 1 && child.tagName === 'BR') {
                    if (children.length !== 1) add('\n', attributes);
                    positions.set(child,{start:offset,end:offset});
                } else visit(child, attributes);
                hadContent = true;
            }
            boundaries.push(offset);
            // Empty attributed spans are real CTB runs; do not discard their metadata.
            if (children.length === 0 && node.dataset && node.dataset.richAttributes) add('', attributes);
            positions.set(node,{start,end:offset,boundaries});
        }
        visit(editor,{});
        return {runs,positions,length:offset};
    }
    function selection(editor) {
        const selected = editor.ownerDocument.getSelection();
        if (!selected || !selected.rangeCount || !editor.contains(selected.anchorNode) || !editor.contains(selected.focusNode)) return null;
        const model = scan(editor);
        function point(node, position) {
            const record = model.positions.get(node);
            if (!record) return 0;
            return node.nodeType === 3 ? record.start + Array.from(node.nodeValue.slice(0,position)).length
                : record.boundaries ? record.boundaries[Math.min(position,record.boundaries.length - 1)] : record.start;
        }
        const a = point(selected.anchorNode,selected.anchorOffset), b = point(selected.focusNode,selected.focusOffset);
        return [Math.min(a,b),Math.max(a,b)];
    }
    /** Places a caret/range in freshly rendered DOM without splitting a surrogate pair. */
    function restore(editor,start,end) {
        const model = scan(editor), doc = editor.ownerDocument;
        function point(position) {
            for (const [node, record] of model.positions) {
                if (node.nodeType === 3 && position >= record.start && position <= record.end) {
                    return [node,Array.from(node.nodeValue).slice(0,position-record.start).join('').length];
                }
            }
            for (const [node,record] of model.positions) {
                if (record.boundaries && record.start <= position && record.end >= position) {
                    const index = record.boundaries.findIndex(value=>value>=position);
                    return [node,index < 0 ? node.childNodes.length : index];
                }
            }
            return [editor,editor.childNodes.length];
        }
        const a=point(start),b=point(end),range=doc.createRange();
        range.setStart(a[0],a[1]);range.setEnd(b[0],b[1]);
        const selected=doc.getSelection();selected.removeAllRanges();selected.addRange(range);
    }
    const api={scan,selection,restore};
    if (typeof module !== 'undefined' && module.exports) module.exports=api;
    else root.RichTextVisualModel=api;
})(typeof window === 'undefined' ? globalThis : window);
