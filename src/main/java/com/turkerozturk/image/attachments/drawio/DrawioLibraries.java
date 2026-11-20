package com.turkerozturk.image.attachments.drawio;

import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Fixed, versioned vendor resource names. Configuration never supplies a script path or URL. */
@Component
public final class DrawioLibraries {
    public static final String PREFIX="myapp.drawio-preview.shape-libraries.";
    public static final List<String> GROUPS=groups();
    private static final Set<String> ICON_GROUPS=Set.of("active_directory","allied_telesis","atlassian","azure2","clip_art","cumulus","dynamics365","ibm","mscae","sap");
    private final List<String> enabled;
    public DrawioLibraries(Environment environment){enabled=GROUPS.stream().filter(g->environment.getProperty(key(g),Boolean.class,true)).toList();}
    public static String key(String group){return PREFIX+group.toLowerCase(Locale.ROOT).replace('_','-');}
    public List<String> enabled(){return enabled;}
    public List<String> iconScripts(){return enabled.stream().filter(ICON_GROUPS::contains).map(g->"/js/drawio/icons/"+g+".js").toList();}
    private static List<String> groups(){
        try(var in=DrawioLibraries.class.getResourceAsStream("/static/js/drawio/groups.txt")){
            if(in==null)throw new IllegalStateException("Missing Draw.io library manifest");
            var names=new String(in.readAllBytes(),StandardCharsets.UTF_8).lines().toList();
            if(names.isEmpty()||names.stream().anyMatch(s->!s.matches("[A-Za-z][A-Za-z0-9_]{0,40}")))throw new IllegalStateException("Invalid Draw.io library manifest");
            return names;
        }catch(java.io.IOException ex){throw new IllegalStateException("Cannot read Draw.io library manifest",ex);}
    }
    static String iconPath(String value){
        String path=value;
        if(path.startsWith("https://")){
            try{var uri=java.net.URI.create(path);if(!Set.of("app.diagrams.net","www.draw.io","www.diagrams.net").contains(uri.getHost())||uri.getUserInfo()!=null||uri.getPort()!=-1||uri.getQuery()!=null||uri.getFragment()!=null)return null;path=uri.getPath();}catch(IllegalArgumentException ex){return null;}
        }
        if(path.startsWith("/"))path=path.substring(1);
        if(!path.startsWith("img/lib/"))return null;path=path.substring(8);
        return path.length()<=240&&path.matches("[A-Za-z0-9_, /().+\\-]+\\.(svg|png)")&&!path.contains("..")?path:null;
    }
}
