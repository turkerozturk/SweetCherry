const test=require('node:test'),assert=require('node:assert/strict');
const {read,install,key}=require('../../main/resources/static/js/node/treeReadingLayout.js');
class Events {
    constructor(){this.handlers={};}
    addEventListener(type,handler){(this.handlers[type]??=[]).push(handler);}
    dispatchEvent(event){for(const handle of this.handlers[event.type]||[])handle(event);}
}
function setup(saved,metadata=false,blocked=false){
    const win=new Events(),doc=new Events(),attrs={},values=new Map();
    if(saved!==undefined)values.set(key,JSON.stringify(saved));
    const dialog=new Events();dialog.querySelector=()=>null;
    const inputs=['title','compact','header','breadcrumbs','backlinks','toolbar','paneBar','ids','dates'].map(name=>({dataset:{treeLayoutSetting:name}}));
    const action={textContent:'Edit',dataset:{},hasAttribute:name=>name in action,setAttribute(name,value){this[name]=value;}};
    doc.documentElement={dataset:{treeMetadataEnabled:String(metadata)},setAttribute:(name,value)=>attrs[name]=value};
    doc.readyState='complete';doc.getElementById=()=>dialog;
    doc.querySelectorAll=selector=>selector==='[data-tree-layout-setting]'?inputs:selector.includes('.workspace-toolbar')?[action]:[];
    win.document=doc;win.CustomEvent=class{constructor(type){this.type=type;}};
    const storage={getItem:k=>values.get(k),setItem:(k,v)=>values.set(k,v)};
    if(blocked)Object.defineProperty(win,'localStorage',{get(){throw Error('blocked');}});else win.localStorage=storage;
    install(win);
    return {win,doc,attrs,inputs,action,values,storage,change(name,checked){doc.dispatchEvent({type:'change',target:{dataset:{treeLayoutSetting:name},checked}});}};
}
test('original layout and hidden metadata are defaults, with a single YAML metadata default',()=>{
    assert.deepEqual(read(null),{title:true,compact:false,header:true,breadcrumbs:true,backlinks:true,toolbar:true,paneBar:true,ids:false,dates:false});
    const configured=read(null,true);assert.equal(configured.ids,true);assert.equal(configured.dates,true);
});
test('only known boolean preferences are restored; corrupt and blocked storage are harmless',()=>{
    for(const storage of [null,{getItem(){throw Error();}},{getItem:()=>'{broken'},{getItem:()=> 'null'}])assert.equal(read(storage).header,true);
    const state=read({getItem:()=>JSON.stringify({header:false,ids:'true',compact:1,unknown:true})});
    assert.equal(state.header,false);assert.equal(state.ids,false);assert.equal(state.compact,false);assert.equal(state.unknown,undefined);
    assert.equal(setup(undefined,false,true).attrs['data-tree-layout-header'],'true');
});
test('each toggle persists independently and is restored when navigating to another tree document',()=>{
    const a=setup();a.change('ids',true);a.change('header',false);a.change('paneBar',false);
    const b=setup(JSON.parse(a.values.get(key)));
    assert.equal(b.attrs['data-tree-layout-ids'],'true');assert.equal(b.attrs['data-tree-layout-dates'],'false');
    assert.equal(b.attrs['data-tree-layout-header'],'false');assert.equal(b.attrs['data-tree-layout-paneBar'],'false');
    assert.equal(b.attrs['data-tree-layout-toolbar'],'true');
});
test('split pane and other tabs receive changed preferences without a reload',()=>{
    const a=setup();a.values.set(key,JSON.stringify({compact:true,backlinks:false,dates:true}));
    a.win.dispatchEvent({type:'storage',key});
    assert.equal(a.attrs['data-tree-layout-compact'],'true');assert.equal(a.attrs['data-tree-layout-backlinks'],'false');
    assert.equal(a.inputs.find(x=>x.dataset.treeLayoutSetting==='dates').checked,true);
});
test('icon-only actions keep accessible names, hints and newly loaded content labels',()=>{
    const a=setup({compact:true});assert.equal(a.action['aria-label'],'Edit');assert.equal(a.action.title,'Edit');
    delete a.action['aria-label'];delete a.action.title;
    a.doc.dispatchEvent({type:'sweetcherry:content-loaded'});assert.equal(a.action['aria-label'],'Edit');assert.equal(a.action.title,'Edit');
});

test('existing saved layouts gain a visible title by default and remember hiding it independently',()=>{
 const original=setup({compact:true,header:false});assert.equal(original.attrs['data-tree-layout-title'],'true');
 original.change('title',false);const restored=setup(JSON.parse(original.values.get(key)));
 assert.equal(restored.attrs['data-tree-layout-title'],'false');assert.equal(restored.attrs['data-tree-layout-toolbar'],'true');
 restored.values.set(key,JSON.stringify({title:true}));restored.win.dispatchEvent({type:'storage',key});
 assert.equal(restored.attrs['data-tree-layout-title'],'true');
});
