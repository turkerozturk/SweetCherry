package com.turkerozturk.node;

/** Trim boundary whitespace, NBSP and formatting/control characters without rewriting visible/internal text. */
public final class NodeNames {
    private NodeNames() {}
    public static String clean(String value){
        if(value==null)return null;int start=0,end=value.length();
        while(start<end&&boundary(value.codePointAt(start)))start+=Character.charCount(value.codePointAt(start));
        while(end>start&&boundary(value.codePointBefore(end)))end-=Character.charCount(value.codePointBefore(end));
        return value.substring(start,end);
    }
    private static boolean boundary(int point){int type=Character.getType(point);return Character.isWhitespace(point)||Character.isSpaceChar(point)||type==Character.FORMAT||type==Character.CONTROL;}
}
