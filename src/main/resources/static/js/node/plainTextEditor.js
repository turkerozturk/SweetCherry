(() => {
    'use strict';
    const input=document.getElementById('nodeText'), form=document.getElementById('plainEditForm');
    const initial=input.value, undo=document.getElementById('plainUndo'), redo=document.getElementById('plainRedo');
    let composing=false, history=[snapshot()], index=0;
    function snapshot() {return {text:input.value,start:input.selectionStart,end:input.selectionEnd};}
    function buttons() {undo.disabled=index===0;redo.disabled=index===history.length-1;}
    function capture() {history[index].start=input.selectionStart;history[index].end=input.selectionEnd;}
    function commit() {
        if(history[index].text===input.value)return;
        history.splice(index+1);history.push(snapshot());
        if(history.length>200)history.shift();
        index=history.length-1;buttons();form.dispatchEvent?.(new Event("editor:changed"));
    }
    function restore(direction) {
        if(composing)return;
        const next=index+direction;if(next<0 || next>=history.length)return;
        capture();index=next;const state=history[index];
        input.value=state.text;input.focus();input.setSelectionRange(state.start,state.end);buttons();form.dispatchEvent?.(new Event("editor:changed"));
    }
    input.addEventListener('beforeinput',event=>{
        if(event.inputType==='historyUndo'||event.inputType==='historyRedo') {event.preventDefault();restore(event.inputType==='historyUndo'?-1:1);}
        else capture();
    });
    input.addEventListener('input',()=>{if(!composing)commit();});
    input.addEventListener('compositionstart',()=>{capture();composing=true;});
    input.addEventListener('compositionend',()=>{composing=false;commit();});
    input.addEventListener('keydown',event=>{
        if(!(event.ctrlKey||event.metaKey)||event.altKey||event.isComposing||composing)return;
        const key=event.key.toLowerCase();
        if(key==='z'||key==='y'){event.preventDefault();restore(key==='y'||event.shiftKey?1:-1);}
    });
    [[undo,()=>restore(-1)],[redo,()=>restore(1)],[document.getElementById('plainSeparator'),()=>{
        if(composing)return;capture();input.focus();
        input.setRangeText('~'.repeat(33)+'\n',input.selectionStart,input.selectionEnd,'end');commit();
    }]].forEach(([button,action])=>{button.addEventListener('pointerdown',event=>event.preventDefault());button.addEventListener('click',action);});
    window.PlainTextEditorBridge={replace(start,end,text){
        if(composing)return false;capture();input.focus();input.setRangeText(text,start,end,'end');commit();return true;
    }};
    buttons();
    window.EditorRecovery?.install(form,{
        base:initial,
        dirty:()=>input.value!==initial,
        busy:()=>composing,
        snapshot:()=>{capture();return {history,index,text:input.value,start:input.selectionStart,end:input.selectionEnd};},
        restore:state=>{
            if(!state || typeof state.text!=='string' || !Array.isArray(state.history) || state.history.length<1 || state.history.length>200 || !Number.isInteger(state.index) || state.index<0 || state.index>=state.history.length || state.history.some(item=>typeof item.text!=='string'))throw new Error('draft');
            history=state.history;index=state.index;input.value=state.text;
            input.focus();input.setSelectionRange(state.start,state.end);buttons();form.dispatchEvent?.(new Event("editor:changed"));
        }
    });
    window.EditorLeaveGuard.install(form,()=>input.value!==initial);
})();
