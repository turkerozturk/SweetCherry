(function(root,factory){const api=factory();if(typeof module==='object'&&module.exports)module.exports=api;else root.SweetCherryTreeKeyboard=api;})(typeof window==='object'?window:globalThis,()=>{
    'use strict';
    /** A successful move may restore focus once, after the moved path has been revealed. */
    function focusNode(tree,id){
        if(!/^[1-9][0-9]{0,18}$/.test(String(id)))return false;
        const doc=tree.ownerDocument,active=doc.activeElement;
        if(doc.querySelector('dialog[open], #richEditForm, #plainEditForm')||doc.defaultView?.getSelection()?.isCollapsed===false)return false;
        if(active&&active!==doc.body&&active!==doc.documentElement&&!tree.contains(active))return false;
        const link=tree.querySelector('li[data-node-id="'+id+'"] > .workspace-tree-row > a');
        if(!link||link.closest('[hidden]'))return false;
        link.focus({preventScroll:true});link.scrollIntoView({block:'nearest',inline:'nearest'});return true;
    }
    function install({tree,select,toggle,stopped=()=>false,onError=()=>{}}){
        const doc=tree.ownerDocument;let busy=false;
        const linkOf=li=>li?.querySelector(':scope > .workspace-tree-row > a');
        function go(link){if(!link||!tree.contains(link))return;link.focus({preventScroll:true});link.scrollIntoView({block:'nearest',inline:'nearest'});select(link.closest('li[data-node-id]').dataset.nodeId);}
        tree.addEventListener('keydown',async event=>{
            if(stopped()||busy||event.defaultPrevented||event.isComposing||event.altKey||event.ctrlKey||event.metaKey||event.shiftKey)return;
            if(!['ArrowUp','ArrowDown','ArrowLeft','ArrowRight','Home','End'].includes(event.key))return;
            // This listener owns only node-name links, never content, buttons, editors or modal dialogs.
            const link=event.target.closest('.workspace-tree-row > a');
            if(!link||!tree.contains(link)||doc.querySelector('dialog[open], #richEditForm, #plainEditForm')||doc.defaultView?.getSelection()?.isCollapsed===false)return;
            event.preventDefault();
            const li=link.closest('li[data-node-id]');
            const fold=li.querySelector(':scope > .workspace-tree-row > button');
            const open=fold?.getAttribute('aria-expanded')==='true';
            try{
                if(event.key==='ArrowRight'){
                    if(fold&&!open){busy=true;await toggle(li,true);}
                    else go(li.querySelector(':scope > ul:not([hidden]) > li > .workspace-tree-row > a'));
                }else if(event.key==='ArrowLeft'){
                    if(open){busy=true;await toggle(li);}
                    else go(linkOf(li.parentElement?.closest('li[data-node-id]')));
                }else{
                    const links=[...tree.querySelectorAll('.workspace-tree-row > a')].filter(item=>!item.closest('[hidden]'));
                    const index=links.indexOf(link);
                    const next=event.key==='Home'?0:event.key==='End'?links.length-1:index+(event.key==='ArrowUp'?-1:1);
                    if(next>=0&&next<links.length&&next!==index)go(links[next]);
                }
            }catch(error){onError(error);}finally{busy=false;}
        });
    }
    return {install,focusNode};
});
