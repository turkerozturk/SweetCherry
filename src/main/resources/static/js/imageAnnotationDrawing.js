(function(root,factory){const api=factory();if(typeof module==='object'&&module.exports)module.exports=api;else root.SweetCherryImageDrawing=api;})(typeof window==='object'?window:globalThis,()=>{
 'use strict';
 function point(x,y,rect,width,height){return {x:Math.max(0,Math.min(width,(x-rect.left)*width/rect.width)),y:Math.max(0,Math.min(height,(y-rect.top)*height/rect.height))};}
 function history(){let marks=[],past=[],future=[];const remember=()=>{past.push(marks);if(past.length>100)past.shift();future=[];};return {
  get marks(){return marks;},get undoable(){return past.length>0;},get redoable(){return future.length>0;},
  add(mark){if(marks.length>=300)return false;remember();marks=[...marks,mark];return true;},
  undo(){if(!past.length)return;future.push(marks);marks=past.pop();},redo(){if(!future.length)return;past.push(marks);marks=future.pop();},
  reset(){if(marks.length){remember();marks=[];}}
 };}
 function draw(ctx,shape){
  const a=shape.points[0],b=shape.points.at(-1);if(!a||!b)return;
  ctx.save();ctx.strokeStyle=shape.color;ctx.fillStyle=shape.color;ctx.lineWidth=shape.width;ctx.lineCap='round';ctx.lineJoin='round';ctx.beginPath();
  if(shape.tool==='rect')ctx.rect(Math.min(a.x,b.x),Math.min(a.y,b.y),Math.abs(b.x-a.x),Math.abs(b.y-a.y));
  else if(shape.tool==='ellipse')ctx.ellipse((a.x+b.x)/2,(a.y+b.y)/2,Math.abs(b.x-a.x)/2,Math.abs(b.y-a.y)/2,0,0,2*Math.PI);
  else if(shape.tool==='pen'){
   if(shape.points.length===1){ctx.arc(a.x,a.y,shape.width/2,0,2*Math.PI);ctx.fill();ctx.restore();return;}
   ctx.moveTo(a.x,a.y);for(const p of shape.points.slice(1))ctx.lineTo(p.x,p.y);
  }else{
   ctx.moveTo(a.x,a.y);ctx.lineTo(b.x,b.y);
   if(shape.tool==='arrow'){const angle=Math.atan2(b.y-a.y,b.x-a.x),head=Math.min(Math.hypot(b.x-a.x,b.y-a.y)/2,Math.max(12,shape.width*4));
    for(const delta of [-Math.PI/6,Math.PI/6]){ctx.moveTo(b.x,b.y);ctx.lineTo(b.x-head*Math.cos(angle+delta),b.y-head*Math.sin(angle+delta));}
   }
  }
  ctx.stroke();ctx.restore();
 }
 return {point,history,draw};
});
