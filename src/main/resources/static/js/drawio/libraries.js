/* SweetCherry integration. Vendor definitions stay separate; see SOURCE.txt. */
(() => {
    'use strict';
    const catalog=window.SweetCherryDrawioCatalog,icons=window.SweetCherryDrawioIcons;
    const known=new Set(catalog.groups);
    const enabled=new Set((document.body.dataset.drawioLibraries||'').split(','));
    const loaded=new Set(),warnings=new Set(),trustedImages=new Set(Object.values(icons));
    const own=(object,key)=>Object.prototype.hasOwnProperty.call(object,key);
    function warn(reason,name){
        // Bounded, deduplicated identifiers only: no labels, raw XML, URLs or attachment names.
        if(typeof name!=='string'||name.length>240||!/^[A-Za-z0-9_, .()/+\-]+$/.test(name)||warnings.size>=100)return;
        const value=reason+': '+name;if(!warnings.has(value)){warnings.add(value);console.warn('[SweetCherry Draw.io] '+value);}
    }
    function family(name){return typeof name==='string'&&name.startsWith('mxgraph.')?name.split('.')[1]:null;}
    function allowed(name){const group=family(name);if(group&&!enabled.has(group)){warn(known.has(group)?'disabled':'missing',name);return false;}return true;}
    const packedLoad=mxStencilRegistry.loadStencil;
    // No dynamic JS evaluation, remote stencil loading or fallback HTTP loader.
    mxStencilRegistry.dynamicLoading=false;mxStencilRegistry.allowEval=false;mxStencil.allowEval=false;
    mxStencilRegistry.loadStencil=function(url,callback){
        const prefix=STENCIL_PATH+'/',file=typeof url==='string'&&url.startsWith(prefix)?url.substring(prefix.length):'';
        let doc=null;if(own(catalog.files,file)&&enabled.has(catalog.files[file]))doc=packedLoad.call(this,url);
        if(callback)callback(doc);else return doc;
    };
    mxStencilRegistry.getStencil=function(name){
        if(!allowed(name))return null;
        if(own(this.stencils,name))return this.stencils[name];
        const file=own(catalog.shapes,name)?catalog.shapes[name]:null;
        if(file&&!loaded.has(file)){
            loaded.add(file);const doc=this.loadStencil(STENCIL_PATH+'/'+file);
            if(doc?.documentElement)this.parseStencilSet(doc.documentElement);
        }
        const stencil=own(this.stencils,name)?this.stencils[name]:null;
        if(!stencil&&name&&!own(mxCellRenderer.defaultShapes,name))warn('missing',name);
        return stencil;
    };
    function image(value){
        if(typeof value!=='string')return null;
        if(trustedImages.has(value))return value;
        const name=value.startsWith('sc-icon:')?value.substring(8):value.startsWith('img/lib/')?value.substring(8):null;
        if(!name)return null;
        const group=name.split('/')[0];
        if(!enabled.has(group)){warn('disabled icon',name);return null;}
        if(!own(icons,name)){warn('missing icon',name);return null;}
        return icons[name];
    }
    // Stencil image commands must use the same local catalogue as image cells.
    const canvasImage=mxSvgCanvas2D.prototype.image;
    mxSvgCanvas2D.prototype.image=function(x,y,w,h,src,...rest){const local=image(src);if(local)return canvasImage.call(this,x,y,w,h,local,...rest);};
    const getShape=mxCellRenderer.prototype.getShape;
    mxCellRenderer.prototype.getShape=function(name){return allowed(name)&&own(mxCellRenderer.defaultShapes,name)?getShape.call(this,name):null;};
    window.SweetCherryDrawioLibraries={
        begin(){warnings.clear();},warnings(){return [...warnings];},image,
        attach(graph){
            const getStyle=graph.getCellStyle;
            graph.getCellStyle=function(...args){
                const style=getStyle.apply(this,args),name=style.shape;
                if(!allowed(name))return {...style,shape:'rectangle'};
                if(name&&!own(mxCellRenderer.defaultShapes,name)&&!own(catalog.shapes,name)){
                    warn('missing',name);return {...style,shape:'rectangle'};
                }
                return style;
            };
            graph.getImage=state=>image(state?.style?.image);
            graph.getIndicatorImage=()=>null;
        }
    };
})();
