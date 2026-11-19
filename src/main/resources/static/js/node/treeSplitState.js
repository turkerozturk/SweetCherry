(function(root,factory){const api=factory();if(typeof module==='object'&&module.exports)module.exports=api;else{root.SweetCherrySplitState=api;if(api.embedded(root))root.document.documentElement.classList.add('sc-split-embedded');}})(typeof window==='object'?window:globalThis,()=>{
    'use strict';
    function embedded(win){try{return win.parent!==win&&win.frameElement?.dataset.splitFrame==='true'&&win.parent.location.origin===win.location.origin;}catch(_){return false;}}
    function node(value){return /^\d{1,16}$/.test(String(value))&&Number.isSafeInteger(Number(value))?Number(value):0;}
    function read(storage,key){try{const value=JSON.parse(storage?.getItem(key)||'{}');return {open:value?.open===true,node:value?.node==null?null:node(value.node),active:value?.active==='right'?'right':'left',ratio:Math.max(20,Math.min(80,Number(value?.ratio)||50))};}catch(_){return {open:false,node:null,active:'left',ratio:50};}}
    return {embedded,node,read};
});
