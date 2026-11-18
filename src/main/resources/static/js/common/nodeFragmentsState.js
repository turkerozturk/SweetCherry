(function(root,factory){const api=factory();if(typeof module==='object'&&module.exports)module.exports=api;else root.SweetCherryNodeFragments=api;})(typeof window==='object'?window:globalThis,()=>{
    'use strict';
    const id=value=>typeof value==='string'&&/^[1-9][0-9]{0,18}$/.test(value)&&BigInt(value)<=9223372036854775807n;
    function target(href,base,nodePath='/nodes/'){
        if(typeof href!=='string'||href.startsWith('#'))return null;
        try{
            const url=new URL(href,base),origin=new URL(base);
            if(url.origin!==origin.origin||!['http:','https:'].includes(url.protocol)||url.username||url.password||!url.pathname.startsWith(nodePath))return null;
            const value=url.pathname.slice(nodePath.length);return id(value)?{id:value,href:url.href}:null;
        }catch(_){return null;}
    }
    function valid(fragment){return fragment&&id(fragment.contentId)&&typeof fragment.name==='string'&&typeof fragment.syntax==='string'&&typeof fragment.html==='string'&&fragment.html.length<=2097152;}
    function budget(maximum,characters=4194304){let count=0,used=0;return {reserve(){if(count>=maximum)return false;count++;return true;},accept(length){if(!Number.isSafeInteger(length)||length<0||length>characters-used)return false;used+=length;return true;}};}
    return {id,target,valid,budget};
});
