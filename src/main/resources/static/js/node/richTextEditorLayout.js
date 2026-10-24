(() => {
    'use strict';
    const shell=document.getElementById('richEditorShell');if(!shell)return;
    const toggle=document.getElementById('richToolsToggle'),close=document.getElementById('richToolsClose');
    const tools=document.getElementById('richControls'),splitter=document.getElementById('richToolsSplitter'),backdrop=document.getElementById('richToolsBackdrop');
    const mobile=window.matchMedia('(max-width:899px)');let ticket=null,width=280,dragging=false;
    try{const saved=Number(localStorage.getItem('sweetcherry.richToolsWidth'));if(saved>=220&&saved<=480)width=saved;}catch(_){}
    const capture=()=>{ticket=window.RichTextSelectionBridge?.capture()||ticket;};
    const restore=()=>window.RichTextSelectionBridge?.restore(ticket);
    function setWidth(value){width=Math.max(220,Math.min(480,Math.max(220,shell.clientWidth-300),value));shell.style.setProperty('--rich-tools-width',width+'px');splitter.setAttribute('aria-valuenow',String(Math.round(width)));}
    function setOpen(open,focusEditor=true){
        document.body.classList.toggle('rich-tools-open',open&&mobile.matches);
        toggle.setAttribute('aria-expanded',String(open&&mobile.matches));
        tools.setAttribute('aria-hidden',String(mobile.matches&&!open));backdrop.hidden=!(open&&mobile.matches);
        if(!open&&focusEditor){capture();restore();}
    }
    toggle.addEventListener('pointerdown',event=>{event.preventDefault();capture();});
    toggle.addEventListener('click',()=>{capture();const open=!document.body.classList.contains('rich-tools-open');setOpen(open,!open);});
    close.addEventListener('pointerdown',event=>event.preventDefault());
    close.addEventListener('click',()=>setOpen(false));backdrop.addEventListener('click',()=>setOpen(false));
    document.addEventListener('keydown',event=>{if(event.key==='Escape'&&document.body.classList.contains('rich-tools-open')&&!document.querySelector('dialog[open]')){event.preventDefault();setOpen(false);}});
    splitter.addEventListener('pointerdown',event=>{if(mobile.matches)return;capture();event.preventDefault();dragging=true;splitter.setPointerCapture(event.pointerId);document.body.classList.add('rich-resizing');});
    splitter.addEventListener('pointermove',event=>{if(dragging)setWidth(event.clientX-shell.getBoundingClientRect().left);});
    function finish(){if(!dragging)return;dragging=false;document.body.classList.remove('rich-resizing');try{localStorage.setItem('sweetcherry.richToolsWidth',String(width));}catch(_){}restore();}
    splitter.addEventListener('pointerup',finish);splitter.addEventListener('pointercancel',finish);
    splitter.addEventListener('keydown',event=>{if(event.key==='ArrowLeft'||event.key==='ArrowRight'){event.preventDefault();setWidth(width+(event.key==='ArrowRight'?20:-20));try{localStorage.setItem('sweetcherry.richToolsWidth',String(width));}catch(_){}}});
    mobile.addEventListener('change',()=>{setOpen(false,false);setWidth(width);});window.addEventListener('resize',()=>setWidth(width));
    setWidth(width);setOpen(false,false);
})();
