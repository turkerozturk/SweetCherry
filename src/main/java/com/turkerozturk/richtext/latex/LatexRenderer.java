package com.turkerozturk.richtext.latex;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.scilab.forge.jlatexmath.*;

/** Offline, bounded math rendering. Full TeX documents contribute only their document body. */
public final class LatexRenderer {
    private LatexRenderer() { }
    public static final String FILENAME = "__ct_special.tex";
    private static final int MAX_SOURCE = 16384;
    private static final Pattern UNSAFE = Pattern.compile("\\\\(?:includegraphics|input|include|openin|openout|read|write|special|catcode|csname|def|gdef|edef|xdef|let|newcommand|renewcommand|providecommand|newenvironment|renewenvironment|definecolor|Declare[A-Za-z]*|jlm[A-Za-z]*|usepackage|documentclass)\\b");
    private static final Set<String> ENVIRONMENTS = Set.of("align","align*","aligned","alignedat","alignat","alignat*","array","matrix","pmatrix","bmatrix","Bmatrix","vmatrix","Vmatrix","cases","gather","gather*","gathered","equation","equation*","split","multline","multline*","smallmatrix");
    private static final Map<String,Rendered> CACHE = new LinkedHashMap<>(32,.75f,true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String,Rendered> entry) { return size()>24; }
    };
    public record Rendered(String math, byte[] png, int width, int height) {
        public Rendered { png=png.clone(); }
        @Override public byte[] png() { return png.clone(); }
    }

    public static String decode(byte[] bytes) {
        if (bytes==null || bytes.length>MAX_SOURCE*4) throw new IllegalArgumentException("LaTeX source too large");
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(bytes)).toString();
        } catch (java.nio.charset.CharacterCodingException failure) { throw new IllegalArgumentException("Invalid LaTeX UTF-8",failure); }
    }

    public static String math(String source) {
        if (source==null || source.length()>MAX_SOURCE) throw new IllegalArgumentException("LaTeX source too large");
        String value=source.replace("\uFEFF","").trim();
        String begin="\\begin{document}",end="\\end{document}";
        int body=value.indexOf(begin);
        if (body>=0) {
            int stop=value.indexOf(end,body+begin.length());
            if (stop<0) throw new IllegalArgumentException("Incomplete LaTeX document");
            value=value.substring(body+begin.length(),stop).trim();
        }
        if (value.startsWith("$$") && value.endsWith("$$") && value.length()>=4) value=value.substring(2,value.length()-2).trim();
        else if (value.startsWith("$") && value.endsWith("$") && value.length()>=2) value=value.substring(1,value.length()-1).trim();
        else if ((value.startsWith("\\[") && value.endsWith("\\]")) || (value.startsWith("\\(") && value.endsWith("\\)"))) value=value.substring(2,value.length()-2).trim();
        // JLaTeXMath math environments do not use LaTeX's numbering/starred variants.
        for(String environment : new String[]{"align*","align","flalign*","flalign"}) {
            value=value.replace("\\begin{"+environment+"}","\\begin{aligned}")
                    .replace("\\end{"+environment+"}","\\end{aligned}");
        }
        for(String environment : new String[]{"equation*","equation"}) {
            value=value.replace("\\begin{"+environment+"}","").replace("\\end{"+environment+"}","");
        }
        if (value.isEmpty() || UNSAFE.matcher(value).find()) throw new IllegalArgumentException("Unsupported LaTeX command");
        int depth=0,commands=0;
        for (int i=0;i<value.length();i++) {
            char c=value.charAt(i);
            if (c=='\\' && ++commands>2048) throw new IllegalArgumentException("LaTeX command limit");
            if (c=='{' && ++depth>64) throw new IllegalArgumentException("LaTeX nesting limit");
            if (c=='}') depth--;
        }
        var environments=Pattern.compile("\\\\begin\\s*\\{([^}]+)}").matcher(value);
        while(environments.find()) if(!ENVIRONMENTS.contains(environments.group(1))) throw new IllegalArgumentException("Unsupported math environment");
        return value;
    }

    public static synchronized Rendered render(String source) {
        String math=math(source);
        var cached=CACHE.get(math);if(cached!=null)return cached;
        try {
            var icon=icon(math);
            int w=icon.getIconWidth(),h=icon.getIconHeight();
            // The cached PNG is capped at 4 MiB of RGBA pixels; never allocate from unchecked TeX dimensions.
            if(w<1 || h<1 || w>2048 || h>1024 || (long)w*h>262144) throw new IllegalArgumentException("LaTeX drawing too large");
            var image=new BufferedImage(w*2,h*2,BufferedImage.TYPE_INT_ARGB);
            var graphics=image.createGraphics();
            try { graphics.scale(2,2);graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);icon.paintIcon(null,graphics,0,0); }
            finally { graphics.dispose(); }
            var output=new ByteArrayOutputStream();ImageIO.write(image,"png",output);
            var result=new Rendered(math,output.toByteArray(),w,h);if(output.size()<=128*1024) CACHE.put(math,result);return result;
        } catch (Exception failure) { throw new IllegalArgumentException("LaTeX cannot be rendered",failure); }
    }
    /** Draws glyph outlines into a caller-owned PDF Graphics2D context. */
    public static synchronized void paint(String math,Graphics2D graphics) {
        var icon=icon(math(math));icon.paintIcon(null,graphics,0,0);
    }
    private static TeXIcon icon(String math) {
        var icon=new TeXFormula(math).createTeXIcon(TeXConstants.STYLE_DISPLAY,20);
        icon.setInsets(new Insets(2,2,2,2));icon.setForeground(Color.BLACK);return icon;
    }
}
