/* Reuse the full editor in a designated same-origin tree pane; standalone pages stay unchanged. */
(() => {
    'use strict';
    let frame,host;
    try{
        frame=window.frameElement;
        if(!frame||frame.dataset.treeEditor!=='true'||window.parent.location.origin!==location.origin)return;
        host=window.parent.SweetCherryTreeEditor;if(!host?.owns(frame))return;
    }catch(_){return;}
    const html=document.documentElement;html.classList.add('sc-tree-editor');
    html.setAttribute('data-inline-compact',window.parent.document.documentElement.getAttribute('data-tree-layout-compact')==='true'?'true':'false');
    let form,invalid=false;
    const api={
        ready:()=>!!form&&!!window.SweetCherryEditorState,
        dirty:()=>window.SweetCherryEditorState?.dirty()!==false,
        busy:()=>!!form?.inert&&!invalid,
        approveLeave:()=>window.SweetCherryEditorState?.approveLeave(),
        invalidate(){invalid=true;if(form)form.inert=true;},
        async saved(destination,continuing){
            if(invalid)throw Error('invalidated editor');
            if(!await host.saved(frame,destination,continuing))throw Error('editor receipt');
            return true;
        }
    };
    window.SweetCherryInlineEditor=api;
    // Runs before the standalone leave guard to avoid two confirmation prompts for Cancel.
    document.addEventListener('click',event=>{
        if(event.defaultPrevented||event.button>0||event.ctrlKey||event.metaKey||event.shiftKey||event.altKey)return;
        const link=event.target.closest('[data-editor-cancel]');if(!link)return;
        event.preventDefault();event.stopImmediatePropagation();host.finish().catch(()=>{});
    },true);
    function ready(){
        form=document.querySelector('#richEditForm, #plainEditForm');
        if(invalid&&form)form.inert=true;
        document.querySelectorAll('.rich-savebar button,.rich-savebar a,.plain-savebar button,.plain-savebar a,#richControls button').forEach(button=>{
            const label=button.textContent.trim();if(!label)return;
            if(!button.title)button.title=label;if(!button.hasAttribute('aria-label'))button.setAttribute('aria-label',label);
        });
    }
    if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',ready,{once:true});else ready();
})();
