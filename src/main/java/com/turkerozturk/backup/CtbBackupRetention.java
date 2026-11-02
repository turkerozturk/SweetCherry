package com.turkerozturk.backup;

import java.nio.file.*;
import java.util.*;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Manual/legacy backups and working copies are never candidates for automatic cleanup. */
@Component
public class CtbBackupRetention {
    public record Result(int removed,int skipped) {static final Result NONE=new Result(0,0);}
    private final CtbBackupSettings settings;
    private final CtbBackupStore store;
    private final CtbBackupSources sources;
    public CtbBackupRetention(CtbBackupSettings settings,CtbBackupStore store,CtbBackupSources sources){this.settings=settings;this.store=store;this.sources=sources;}
    Result afterSuccess(CtbBackupStore.Backup latest) {
        int keep=settings.getAutomatic().getKeepLast();
        if(!latest.automatic() || latest.sourceKey()==null || keep==0)return Result.NONE;
        int removed=0,skipped=0;
        try {
            store.checked(latest.id()); // A verified, readable replacement must exist before any deletion.
            var protectedFiles=sources.protectedFiles();
            var candidates=store.catalog().backups().stream().filter(b->b.automatic() && latest.sourceKey().equals(b.sourceKey()) && !latest.id().equals(b.id())).toList();
            for(int i=keep-1;i<candidates.size();i++) {
                try {if(store.deleteAutomatic(candidates.get(i).id(),latest.sourceKey(),protectedFiles))removed++;else skipped++;}
                catch(Exception failure){skipped++;}
            }
        }catch(Exception failure){skipped++;}
        if(skipped>0)LoggerFactory.getLogger(getClass()).warn("Automatic CTB backup cleanup skipped files: job={}, skipped={}",latest.id(),skipped);
        return new Result(removed,skipped);
    }
}
