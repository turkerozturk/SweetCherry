package com.turkerozturk.backup;

import jakarta.servlet.http.HttpSession;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;

/** A frozen, short-lived, single-use selection. Confirmation never reruns a date/retention filter. */
@Service
public class CtbBackupDeletionService {
    static final String SESSION="CTB_BACKUP_DELETE_PLAN";
    public record Item(String id,CtbBackupStore.Backup backup,String reason) {}
    public record Preview(String token,List<Item> items,int eligible,long bytes) {}
    public record Outcome(String filename,boolean removed,String reason) {}
    public record Result(int removed,int skipped,List<Outcome> items) {}
    private record Target(String id,String revision,String reason) {}
    private record Plan(String token,Instant expires,List<Target> targets) {}
    private final CtbBackupStore store;
    private final CtbBackupSources sources;
    private final CtbBackupService jobs;
    private final Clock clock;
    @org.springframework.beans.factory.annotation.Autowired
    public CtbBackupDeletionService(CtbBackupStore store,CtbBackupSources sources,CtbBackupService jobs) {this(store,sources,jobs,Clock.systemUTC());}
    CtbBackupDeletionService(CtbBackupStore store,CtbBackupSources sources,CtbBackupService jobs,Clock clock) {this.store=store;this.sources=sources;this.jobs=jobs;this.clock=clock;}
    private boolean running(String id) {var status=jobs.current();return status!=null && id.equals(status.id()) && "RUNNING".equals(status.state());}
    private static String reason(Exception exception) {return exception instanceof CtbBackupProblem p?p.key():"ctbBackup.error.storage";}
    public Preview preview(List<String> ids,HttpSession session) throws java.io.IOException {
        if(ids==null || ids.isEmpty() || ids.size()>200)throw new CtbBackupProblem("selection");
        var selected=new LinkedHashSet<>(ids);
        for(String id:selected)CtbBackupStore.id(id);
        var protectedFiles=sources.protectedFiles();var targets=new ArrayList<Target>();var items=new ArrayList<Item>();
        long bytes=0;int eligible=0;
        for(String id:selected) {
            CtbBackupStore.Backup backup=null;String revision=null,blocked=null;
            try {
                backup=store.get(id);blocked=running(id)?"ctbBackup.error.protected":store.protection(id,protectedFiles);
                if(blocked==null){revision=store.revision(id);bytes=Math.addExact(bytes,backup.bytes());eligible++;}
            }catch(Exception failure){blocked=reason(failure);revision=null;}
            targets.add(new Target(id,revision,blocked));items.add(new Item(id,backup,blocked));
        }
        String token=UUID.randomUUID().toString();
        synchronized(session){session.setAttribute(SESSION,new Plan(token,clock.instant().plusSeconds(600),List.copyOf(targets)));}
        return new Preview(token,List.copyOf(items),eligible,bytes);
    }
    public Result confirm(String token,HttpSession session) {
        Plan plan;
        synchronized(session) {
            Object stored=session.getAttribute(SESSION);
            if(!(stored instanceof Plan p) || token==null || !p.token().equals(token) || !p.expires().isAfter(clock.instant()))throw new CtbBackupProblem("expired");
            plan=p;session.removeAttribute(SESSION); // Consume before touching files; double submits cannot repeat deletion.
        }
        var results=new ArrayList<Outcome>();int removed=0,skipped=0;
        for(Target target:plan.targets()) {
            String blocked=target.reason();
            if(blocked==null)try {
                if(running(target.id()))throw new CtbBackupProblem("protected");
                // Refresh configured-file protection for every item, including registrations made since preview.
                store.deleteReviewed(target.id(),target.revision(),sources::protectedFiles);
            }catch(Exception failure){blocked=reason(failure);}
            if(blocked==null)removed++;else skipped++;
            results.add(new Outcome("backup-"+target.id()+".ctb",blocked==null,blocked));
        }
        return new Result(removed,skipped,List.copyOf(results));
    }
}
