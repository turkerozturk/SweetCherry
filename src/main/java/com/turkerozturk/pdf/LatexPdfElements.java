package com.turkerozturk.pdf;

import com.turkerozturk.richtext.latex.LatexRenderer;
import org.xhtmlrenderer.extend.*;
import org.xhtmlrenderer.simple.extend.FormSubmissionListener;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.pdf.*;
import org.xhtmlrenderer.render.*;
import org.w3c.dom.Element;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Uses the normal PNG for layout/fallback and glyph outlines for PDF painting. */
final class LatexPdfElements implements ReplacedElementFactory {
    private final ReplacedElementFactory delegate;
    LatexPdfElements(ReplacedElementFactory delegate) { this.delegate=delegate; }
    @Override public ReplacedElement createReplacedElement(LayoutContext context,BlockBox box,UserAgentCallback user,int width,int height) {
        var fallback=delegate.createReplacedElement(context,box,user,width,height);
        Element element=box.getElement();
        if (!(fallback instanceof ITextImageElement image) || element==null || !element.hasAttribute("data-sc-latex")) return fallback;
        try {
            String encoded=element.getAttribute("data-sc-latex");
            if(encoded.length()>90000)return fallback;
            var drawing=LatexRenderer.render(new String(Base64.getDecoder().decode(encoded),StandardCharsets.UTF_8));
            return new ITextImageElement(image.getImage()) {
                @Override public void paint(RenderingContext c,ITextOutputDevice output,BlockBox block) {
                    try {
                        var template=output.getWriter().getDirectContent().createTemplate(drawing.width(),drawing.height());
                        var graphics=template.createGraphicsShapes(drawing.width(),drawing.height());
                        try { LatexRenderer.paint(drawing.math(),graphics); }
                        finally { graphics.dispose(); }
                        var vector=new ITextFSImage(com.lowagie.text.Image.getInstance(template));
                        var scaled=vector.scale(getIntrinsicWidth(),getIntrinsicHeight());
                        var bounds=block.getContentAreaEdge(block.getAbsX(),block.getAbsY(),c);
                        output.drawImage(scaled,bounds.x,bounds.y);
                    } catch (Exception unavailable) { super.paint(c,output,block); }
                }
            };
        } catch (IllegalArgumentException invalid) { return fallback; }
    }
    @Override public void reset(){delegate.reset();}
    @Override public void remove(Element element){delegate.remove(element);}
    @Override public void setFormSubmissionListener(FormSubmissionListener listener){delegate.setFormSubmissionListener(listener);}
}
