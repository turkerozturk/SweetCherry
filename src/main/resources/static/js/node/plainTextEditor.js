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
        index=history.length-1;buttons();
    }
    function restore(direction) {
        if(composing)return;
        const next=index+direction;if(next<0 || next>=history.length)return;
        capture();index=next;const state=history[index];
        input.value=state.text;input.focus();input.setSelectionRange(state.start,state.end);buttons();
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
    buttons();window.EditorLeaveGuard.install(form,()=>input.value!==initial);
})();
