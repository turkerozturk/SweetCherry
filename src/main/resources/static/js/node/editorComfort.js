/* Shared presentation preferences for text/code/Markdown/HTML and rich-text editing. */
(() => {
 'use strict';
 const key='sweetcherry.editorTextZoom';let percent=100,adapter=null,ticket=null;
 const valid=value=>Number.isInteger(value)&&value>=70&&value<=200&&value%10===0;
 try{const saved=Number(localStorage.getItem(key));if(valid(saved))percent=saved;}catch(_){}
 const capture=()=>{try{return adapter?.capture()||null;}catch(_){return null;}};
 function gap(surface){if(surface?.clientHeight>0)surface.style.setProperty('--editor-writing-gap',Math.max(32,Math.min(240,surface.clientHeight*.35))+'px');}
 function bottom(surface){if(!surface)return;gap(surface);surface.scrollTop=surface.scrollHeight;}
 function apply(){
  document.documentElement.style.setProperty('--editor-text-scale',String(percent/100));
  document.querySelectorAll('[data-editor-zoom-value]').forEach(label=>label.textContent=percent+'%');
  document.querySelectorAll('[data-editor-zoom]').forEach(button=>button.disabled=(button.dataset.editorZoom==='out'&&percent===70)||(button.dataset.editorZoom==='in'&&percent===200));
 }
 document.addEventListener('pointerdown',event=>{
  if(!event.target.closest('[data-editor-zoom]'))return;ticket=capture();event.preventDefault();
 },true);
 document.addEventListener('click',event=>{
  const button=event.target.closest('[data-editor-zoom]');if(!button)return;
  event.preventDefault();event.stopPropagation();const selection=ticket||capture();ticket=null;
  percent=button.dataset.editorZoom==='reset'?100:Math.max(70,Math.min(200,percent+(button.dataset.editorZoom==='in'?10:-10)));
  try{localStorage.setItem(key,String(percent));}catch(_){}apply();
  if(selection)adapter?.restore(selection);
  gap(adapter?.surface());if(adapter?.atEnd())bottom(adapter.surface());
 },true);
 window.addEventListener('storage',event=>{
  if(event.key!==key)return;const surface=adapter?.surface(),selection=document.activeElement===surface?capture():null;
  const value=Number(event.newValue);percent=valid(value)?value:100;apply();if(selection){adapter.restore(selection);if(adapter.atEnd())bottom(adapter.surface());}
 });
 window.SweetCherryEditorComfort={
  register(value){
   adapter=value;const initial=JSON.stringify(capture());
   const settle=()=>requestAnimationFrame(()=>requestAnimationFrame(()=>{
    const surface=adapter.surface();gap(surface);
    // Do not steal a draft's restored cursor, a user's selection, or an opened dialog's focus.
    if(document.activeElement!==surface||JSON.stringify(capture())!==initial)return;
    bottom(surface);surface.scrollIntoView?.({block:'end',behavior:'instant'});
    window.scrollTo?.({top:document.scrollingElement?.scrollHeight||0,behavior:'instant'});
   }));
   if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',settle,{once:true});else settle();
   const refresh=()=>{gap(adapter.surface());};
   const observer=new ResizeObserver(refresh);for(const surface of value.surfaces)observer.observe(surface);refresh();
   // Textareas use the browser's caret layout. At the end, keep the intentional writing gap visible.
   for(const surface of value.surfaces)surface.addEventListener('input',()=>requestAnimationFrame(()=>{
    if(document.activeElement===surface&&surface===adapter.surface()&&adapter.atEnd())bottom(surface);
   }));
  },
  bottom
 };
 apply();
})();
