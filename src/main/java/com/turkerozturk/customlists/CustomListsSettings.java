package com.turkerozturk.customlists;

import com.turkerozturk.node.NodeNames;
import com.turkerozturk.multipledatabases.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component @ConfigurationProperties(prefix="myapp.custom-lists")
public class CustomListsSettings {
    private String mainNodeName="cherrytemplatetasks",templatePrefix="cherrytemplate";
    @org.springframework.beans.factory.annotation.Autowired(required=false) private CustomPropertiesHolder custom;
    public record Names(String main,String prefix){public String template(String key){return prefix+(prefix.endsWith("-")?"":"-")+key;}}
    public Names effective(){
        var values=custom==null?null:custom.getCustomProperties(TenantContext.getCurrentTenant());
        return new Names(choose(values==null?null:values.get("custom.customListsNodeName"),mainNodeName),choose(values==null?null:values.get("custom.templatePrefix"),templatePrefix));
    }
    private static String choose(String value,String fallback){String clean=NodeNames.clean(value);return clean==null||clean.isEmpty()||clean.length()>255?fallback:clean;}
    public String getMainNodeName(){return mainNodeName;}public void setMainNodeName(String value){mainNodeName=valid(value);}
    public String getTemplatePrefix(){return templatePrefix;}public void setTemplatePrefix(String value){templatePrefix=valid(value);}
    private String valid(String value){String clean=NodeNames.clean(value);if(clean==null||clean.isEmpty()||clean.length()>255)throw new IllegalArgumentException("Invalid custom-list name");return clean;}
}
