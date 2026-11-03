package com.turkerozturk.node;

import java.util.List;

/** One ordered report cell shared by HTML, PDF and XLSX. Ambiguity never selects a winner. */
public record TemplateField(String key,String label,String status,DataNode data,List<DataNode> matches) {
    public static TemplateField of(ProjectNode project,String key,String label){
        var duplicates=project.getDuplicateDataNodes();
        if(duplicates!=null && duplicates.containsKey(key))return new TemplateField(key,label,"ambiguous",null,duplicates.get(key));
        var values=project.getDataNodes();var data=values==null?null:values.get(key);
        return new TemplateField(key,label,data==null?"missing":data.isEmpty()?"empty":"value",data,data==null?List.of():List.of(data));
    }
}
