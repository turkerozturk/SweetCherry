package com.turkerozturk.backup;

import jakarta.annotation.PreDestroy;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** A separate minute timer: the unrelated experimental scheduling flag is not required. */
@Component
public class CtbBackupScheduler {
    public record Row(List<String> names,String state,String last,String next,String error) {}
    public record View(boolean enabled,int interval,int keepLast,String checked,String error,List<Row> rows) {}
    private final CtbBackupSettings settings;
    private final CtbBackupSources sources;
    private final CtbBackupService service;
    private final CtbBackupStore store;
    private final Clock clock;
    private final Map<String,Instant> attempts=new HashMap<>();
    private final Map<String,String> pendingJobs=new HashMap<>();
    private final ScheduledExecutorService timer=Executors.newSingleThreadScheduledExecutor(task->{var thread=new Thread(task,"ctb-backup-schedule");thread.setDaemon(true);return thread;});
    private boolean started,closed;
    private Instant checked;
    private String error;
    @org.springframework.beans.factory.annotation.Autowired
    public CtbBackupScheduler(CtbBackupSettings settings,CtbBackupSources sources,CtbBackupService service,CtbBackupStore store) {
        this(settings,sources,service,store,Clock.systemUTC());
    }
    CtbBackupScheduler(CtbBackupSettings settings,CtbBackupSources sources,CtbBackupService service,CtbBackupStore store,Clock clock) {
        this.settings=settings;this.sources=sources;this.service=service;this.store=store;this.clock=clock;
    }
    private boolean enabled(){return settings.isEnabled() && settings.getAutomatic().isEnabled();}
    @EventListener(ApplicationReadyEvent.class)
    public synchronized void start() {
        if(!started && !closed && enabled()){started=true;timer.scheduleWithFixedDelay(this::tick,60,60,TimeUnit.SECONDS);}
    }
    synchronized void tick() {
        if(closed || !enabled())return;
        checked=clock.instant();error=null;
        if(service.isRunning())return;
        for(var pending:pendingJobs.entrySet()) {
            try {
                var status=service.status(pending.getValue());
                if(status==null || !"SUCCEEDED".equals(status.state()))attempts.put(pending.getKey(),clock.instant());
                else attempts.remove(pending.getKey());
            }catch(CtbBackupProblem unavailable){attempts.put(pending.getKey(),clock.instant());}
        }
        pendingJobs.clear();
        try {
            var groups=sources.resolve(settings.getAutomatic().getTenants());
            var backups=store.catalog().backups();
            groups.stream().filter(s->s.error()==null).sorted(Comparator.comparing(s->last(s.key(),backups)))
                .filter(s->!due(s.key(),backups).isAfter(clock.instant())).findFirst().ifPresent(source->{
                    try {String job=service.startAutomatic(source.path(),source.names().get(0));attempts.put(source.key(),clock.instant());if(job!=null)pendingJobs.put(source.key(),job);}
                    catch(CtbBackupProblem failure){if(!"ctbBackup.error.busy".equals(failure.key()))attempts.put(source.key(),clock.instant());error=failure.key();}
                });
            attempts.keySet().retainAll(groups.stream().map(CtbBackupSources.Source::key).filter(Objects::nonNull).toList());
        } catch(Exception failure){error="ctbBackup.error.storage";}
    }
    private Instant last(String key,List<CtbBackupStore.Backup> backups) {
        return backups.stream().filter(b->key.equals(b.sourceKey())).map(b->Instant.parse(b.created())).max(Comparator.naturalOrder()).orElse(Instant.MIN);
    }
    private Instant due(String key,List<CtbBackupStore.Backup> backups) {
        Instant last=last(key,backups);
        Instant next=last.equals(Instant.MIN)?Instant.MIN:last.plus(Duration.ofMinutes(settings.getAutomatic().getIntervalMinutes()));
        Instant attempt=attempts.get(key);
        if(attempt!=null && last.isBefore(attempt)){Instant retry=attempt.plus(Duration.ofMinutes(settings.getAutomatic().getRetryMinutes()));if(retry.isAfter(next))next=retry;}
        return next;
    }
    private String date(Instant instant) {
        if(instant==null || instant.equals(Instant.MIN))return "—";
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault()).format(instant);
    }
    public synchronized View view() {
        List<Row> rows=new ArrayList<>();String displayError=error;
        if(enabled())try {
            var backups=store.catalog().backups();String running=service.runningSourceKey();
            for(var source:sources.resolve(settings.getAutomatic().getTenants())) {
                if(source.error()!=null){rows.add(new Row(source.names(),"ERROR","—","—",source.error()));continue;}
                Instant next=due(source.key(),backups);String state=source.key().equals(running)?"RUNNING":next.isAfter(clock.instant())?"WAITING":"DUE";
                rows.add(new Row(source.names(),state,date(last(source.key(),backups)),date(next),null));
            }
        }catch(Exception failure){displayError="ctbBackup.error.storage";}
        return new View(enabled(),settings.getAutomatic().getIntervalMinutes(),settings.getAutomatic().getKeepLast(),date(checked),displayError,List.copyOf(rows));
    }
    @PreDestroy public synchronized void close(){closed=true;timer.shutdownNow();}
}
