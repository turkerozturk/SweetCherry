package com.turkerozturk.export.texttree;

import java.text.Normalizer;
import java.util.*;

/** Portable ZIP components: no traversal, absolute paths or case-insensitive sibling collisions. */
final class TextTreePaths {
    private final Set<String> used=new HashSet<>();
    static String safe(String value,long id){
        String result=Normalizer.normalize(Objects.toString(value,""),Normalizer.Form.NFC)
                .replaceAll("[\\p{Cntrl}\\\\/:*?\"<>|]","_").strip().replaceAll("[. ]+$","");
        var bounded=new StringBuilder();int bytes=0;
        for(int point:result.codePoints().toArray()){
            int length=new String(Character.toChars(point)).getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            if(bytes+length>180)break;bounded.appendCodePoint(point);bytes+=length;
        }
        result=bounded.toString().strip().replaceAll("[. ]+$","");
        if(result.isEmpty()||result.equals(".")||result.equals(".."))result="node-"+id;
        String stem=result.split("\\.",2)[0];
        if(Normalizer.normalize(stem,Normalizer.Form.NFKC).matches("(?i)CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9]"))result="node-"+id+"-"+result;
        return result;
    }
    String allocate(String parent,String name,long id,boolean directory){
        String base=safe(name,id);
        if(!directory&&!base.contains("."))base+=".txt";
        String candidate=parent+base;
        int attempt=0;
        while(!used.add(key(candidate))) {
            String suffix="--"+id+(attempt++==0?"":"-"+attempt);
            int dot=!directory?base.lastIndexOf('.'): -1;
            candidate=parent+(dot>0?base.substring(0,dot)+suffix+base.substring(dot):base+suffix);
        }
        if(directory)used.add(key(candidate+"/_content.txt"));
        return candidate;
    }
    private String key(String path){return Normalizer.normalize(path,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);}
}
