package com.turkerozturk.image.attachments.drawio;

/** A navigation-only subset of Draw.io links. No actions, executable schemes or relative paths. */
final class DrawioLinks {
    static final String PAGE_PREFIX="data:page/id,";
    static String safe(String value){
        if(value==null||value.chars().anyMatch(Character::isISOControl))return null;String link=value.strip();
        if(link.isEmpty()||link.length()>4096||link.chars().anyMatch(Character::isISOControl)||link.indexOf('\\')>=0)return null;
        if(link.startsWith(PAGE_PREFIX))return pageId(link.substring(PAGE_PREFIX.length())).isEmpty()?null:link;
        try{
            var uri=new java.net.URI(link);
            return ("http".equalsIgnoreCase(uri.getScheme())||"https".equalsIgnoreCase(uri.getScheme()))
                &&uri.getHost()!=null&&uri.getRawUserInfo()==null&&!link.matches("(?is).*%(?:0[0-9a-f]|1[0-9a-f]|7f).*")?uri.toASCIIString():null;
        }catch(java.net.URISyntaxException malformed){return null;}
    }
    static String pageId(String value){return value!=null&&!value.isBlank()&&value.length()<=255&&value.chars().noneMatch(Character::isISOControl)?value:"";}
}
