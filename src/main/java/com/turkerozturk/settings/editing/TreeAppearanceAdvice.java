package com.turkerozturk.settings.editing;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
/** Bounded numeric values only; never interpolate arbitrary configuration into CSS. */
@ControllerAdvice
public class TreeAppearanceAdvice {
 private final int font,padding;
 public TreeAppearanceAdvice(@Value("${myapp.tree.font-size:16}") String font,@Value("${myapp.tree.vertical-padding:1}") String padding){this.font=bounded(font,16,10,32);this.padding=bounded(padding,1,0,12);}
 static int bounded(String text,int fallback,int min,int max){try{int n=Integer.parseInt(text);return n>=min&&n<=max?n:fallback;}catch(RuntimeException e){return fallback;}}
 @ModelAttribute("treeFontSize") public int fontSize(){return font;}
 @ModelAttribute("treeVerticalPadding") public int verticalPadding(){return padding;}
}
