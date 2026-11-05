(function(root) {
    'use strict';
    /** Use full subtree sizes so folding never moves a branch to the opposite side. */
    function partition(children) {
        function weight(node) { return 1 + (node.children || []).reduce((sum, child) => sum + weight(child), 0); }
        const sides = new Map(), totals = {left: 0, right: 0};
        children.map((node, index) => ({node, index, weight: weight(node)}))
            .sort((a,b) => b.weight-a.weight || a.index-b.index).forEach(item => {
                const side = totals.right <= totals.left ? 'right' : 'left';
                sides.set(item.node, side); totals[side] += item.weight;
            });
        return {left: children.filter(node => sides.get(node) === 'left'),
            right: children.filter(node => sides.get(node) === 'right')};
    }
    function path(source, target, left, lineWidth) {
        const a=source.state.rect,b=target.state.rect;
        const sx=left?a.x:a.x+a.width, tx=left?b.x+b.width:b.x;
        const sy=a.y+a.height+lineWidth(source)/2, ty=b.y+b.height+lineWidth(target)/2;
        const middle=(sx+tx)/2;
        return `M${sx},${sy}C${middle},${sy} ${middle},${ty} ${tx},${ty}`;
    }
    /** Adapter for the bundled Markmap: use its own measured flex layout once per side. */
    function createClass(Markmap) {
        return class BilateralMarkmap extends Markmap {
            _relayout() {
                const root=this.state.data;
                if(!root)return;
                const children=root.children, groups=partition(children || []);
                this.branchSides=new WeakMap();
                const visible=[];
                const collect=(node,left)=>{
                    this.branchSides.set(node,left);
                    const rect={...node.state.rect};
                    if(left)rect.x=root.state.rect.width-rect.x-rect.width;
                    node.state.rect=rect;visible.push(rect);
                    if(!node.payload?.fold)(node.children || []).forEach(child=>collect(child,left));
                };
                try {
                    for(const side of ['right','left']) {
                        root.children=groups[side];
                        super._relayout();
                        if(!root.payload?.fold)root.children.forEach(child=>collect(child,side==='left'));
                    }
                } finally { root.children=children; }
                visible.push(root.state.rect);
                this.state.rect={x1:Math.min(...visible.map(r=>r.x)),y1:Math.min(...visible.map(r=>r.y)),
                    x2:Math.max(...visible.map(r=>r.x+r.width)),y2:Math.max(...visible.map(r=>r.y+r.height))};
            }
            async renderData(originData) {
                await super.renderData(originData);
                if(!this.state.data)return;
                const isLeft=node=>this.branchSides?.get(node)===true;
                const width=this.options.lineWidth;
                // ForeignObject labels remain upright; only coordinates and connectors change.
                this.g.selectAll('g.markmap-node > circle').attr('cx',node=>isLeft(node)?0:node.state.rect.width);
                const active=new Set();
                const visit=node=>{active.add(node);if(!node.payload?.fold)(node.children || []).forEach(visit);};
                visit(this.state.data);
                // Exiting paths keep Markmap's removal transition when a branch is folded.
                this.transition(this.g.selectAll('path.markmap-link').filter(link=>active.has(link.target)))
                    .attr('stroke',link=>this.options.color(link.target))
                    .attr('stroke-width',link=>width(link.target))
                    .attr('d',link=>path(link.source,link.target,isLeft(link.target),width));
            }
        };
    }
    const api={partition,path,createClass};
    if(typeof module!=='undefined' && module.exports)module.exports=api;
    else root.SweetCherryMarkmapLayout=api;
})(typeof window==='undefined'?globalThis:window);
