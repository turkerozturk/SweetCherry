package com.turkerozturk.backup;

import com.turkerozturk.settings.editing.ApplicationSettingsService;
import java.io.StringReader;
import java.util.*;
import org.yaml.snakeyaml.*;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.*;

/** Edits only the declared YAML tenant sequence, preserving all surrounding configuration. */
public final class AutomaticBackupTenantList {
    private AutomaticBackupTenantList() { }
    private static final String KEY="myapp.ctb-backup.automatic.tenants";
    public static String remove(String yaml,String tenant){
        ApplicationSettingsService.validateYaml(yaml);
        var options=new LoaderOptions();options.setMaxAliasesForCollections(0);options.setCodePointLimit(1048576);
        var root=new Yaml(new SafeConstructor(options)).compose(new StringReader(yaml));
        var matches=new ArrayList<Node>();find(root,"",matches);
        if(matches.size()!=1||!(matches.get(0) instanceof SequenceNode sequence))throw new IllegalArgumentException("invalid");
        var values=new ArrayList<String>();boolean removed=false;
        for(var child:sequence.getValue()){
            if(!(child instanceof ScalarNode scalar)||!scalar.getTag().equals(Tag.STR))throw new IllegalArgumentException("invalid");
            if(scalar.getValue().chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("invalid");
            if(scalar.getValue().equals(tenant))removed=true;else values.add("'"+scalar.getValue().replace("'","''")+"'");
        }
        if(!removed)throw new IllegalArgumentException("stale");
        int start=yaml.offsetByCodePoints(0,sequence.getStartMark().getIndex());
        int endMark=sequence.getEndMark().getIndex();
        if(sequence.getFlowStyle()==DumperOptions.FlowStyle.BLOCK&&!sequence.getValue().isEmpty())endMark=sequence.getValue().get(sequence.getValue().size()-1).getEndMark().getIndex();
        int end=yaml.offsetByCodePoints(0,endMark);
        String result=yaml.substring(0,start)+"["+String.join(", ",values)+"]"+yaml.substring(end);
        ApplicationSettingsService.validateYaml(result);return result;
    }
    private static void find(Node node,String prefix,List<Node> result){
        if(!(node instanceof MappingNode mapping))return;
        for(var entry:mapping.getValue()){
            String key=prefix.isEmpty()?((ScalarNode)entry.getKeyNode()).getValue():prefix+"."+((ScalarNode)entry.getKeyNode()).getValue();
            if(key.equals(KEY))result.add(entry.getValueNode());else find(entry.getValueNode(),key,result);
        }
    }
}
