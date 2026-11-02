package com.turkerozturk.backup;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.*;
import java.sql.Connection;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import javax.sql.DataSource;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Captures the selected connection before dispatch; workers never consult tenant thread/session state. */
@Service
public class CtbBackupService {
    public record Status(String id,String sourceName,String started,String state,String phase,int remaining,int total,String error,boolean automatic,int removed,int skipped) {
        public Status(String id,String sourceName,String started,String state,String phase,int remaining,int total,String error) {
            this(id,sourceName,started,state,phase,remaining,total,error,false,0,0);
        }
    }
    private static final class Job {
        volatile Status status;
        final String sourceKey;
        Job(String source,String sourceKey,boolean automatic) { this.sourceKey=sourceKey;status=new Status(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(java.time.ZoneOffset.UTC).format(Instant.now())+"-"+UUID.randomUUID(),source,Instant.now().toString(),"RUNNING","COPYING",0,0,null,automatic,0,0); }
        void update(String state,String phase,int remaining,int total,String error) {
            Status s=status;status=new Status(s.id(),s.sourceName(),s.started(),state,phase,Math.max(0,remaining),Math.max(0,total),error,s.automatic(),s.removed(),s.skipped());
        }
    }
    private final CtbBackupSettings settings;
    private final CtbBackupEngine engine;
    private final CtbBackupStore store;
    private final CtbBackupRetention retention;
    private final ExecutorService executor=Executors.newSingleThreadExecutor(task->{var thread=new Thread(task,"ctb-backup");thread.setDaemon(true);return thread;});
    private final Map<String,Job> jobs=new LinkedHashMap<>();
    private Job current;
    private boolean closing;
    public CtbBackupService(CtbBackupSettings settings,CtbBackupEngine engine,CtbBackupStore store) {
        this(settings,engine,store,null);
    }
    @org.springframework.beans.factory.annotation.Autowired
    public CtbBackupService(CtbBackupSettings settings,CtbBackupEngine engine,CtbBackupStore store,CtbBackupRetention retention) {
        this.settings=settings;this.engine=engine;this.store=store;this.retention=retention;
    }
    public synchronized Status current() { return current==null?null:current.status; }
    public synchronized Status status(String id) {
        CtbBackupStore.id(id);Job job=jobs.get(id);if(job==null)throw new CtbBackupProblem("missing");return job.status;
    }
    synchronized String runningSourceKey(){return isRunning()?current.sourceKey:null;}
    public synchronized boolean isRunning(){return current!=null && "RUNNING".equals(current.status.state());}
    public String start(DataSource selected,String sourceName) { return start(selected,sourceName,false); }
    String startAutomatic(Path path,String name) {
        if(!settings.getAutomatic().isEnabled())throw new CtbBackupProblem("disabled");
        var source=new org.springframework.jdbc.datasource.AbstractDataSource() {
            @Override public Connection getConnection() throws java.sql.SQLException{return CtbBackupEngine.readOnly(path);}
            @Override public Connection getConnection(String user,String password)throws java.sql.SQLException{return getConnection();}
        };
        return start(source,name,true);
    }
    private synchronized String start(DataSource selected,String sourceName,boolean automatic) {
        if(!settings.isEnabled())throw new CtbBackupProblem("disabled");
        if(closing || current!=null && "RUNNING".equals(current.status.state()))throw new CtbBackupProblem("busy");
        Connection connection=null;
        try {
            if(selected==null)throw new CtbBackupProblem("source");
            connection=selected.getConnection();String sourceKey=CtbBackupSources.key(engine.sourcePath(connection));store.root();
            Job job=new Job(sourceName,sourceKey,automatic);Connection captured=connection;
            current=job;jobs.put(job.status.id(),job);
            while(jobs.size()>20)jobs.remove(jobs.keySet().iterator().next());
            executor.execute(()->run(job,captured));connection=null;
            return job.status.id();
        } catch(CtbBackupProblem problem) {throw problem;}
        catch(Exception failure) {throw new CtbBackupProblem(failure instanceof IOException?"storage":"source");}
        finally {if(connection!=null)try{connection.close();}catch(Exception ignored){}}
    }
    private void run(Job job,Connection captured) {
        Path pending=null,target=null;boolean published=false;String error=null;CtbBackupStore.Backup completed=null;
        try(captured) {
            pending=store.temp(".ctb.partial");
            engine.copy(captured,pending,(remaining,total)->job.update("RUNNING","COPYING",remaining,total,null));
            job.update("RUNNING","CHECKING",0,0,null);
            var counts=engine.verify(pending);
            job.update("RUNNING","HASHING",0,0,null);
            String hash=CtbBackupStore.sha256(pending);long bytes=Files.size(pending);CtbBackupStore.force(pending);
            synchronized(this) {
                if(closing)throw new CtbBackupProblem("interrupted");
                target=store.file(job.status.id());CtbBackupStore.move(pending,target);
                completed=new CtbBackupStore.Backup(job.status.id(),target.getFileName().toString(),job.status.sourceName(),
                        Instant.now().toString(),bytes,hash,counts,job.sourceKey,job.status.automatic());
                store.publish(completed);
                published=true;
            }
        } catch(Exception failure) {
            String key=failure instanceof CtbBackupProblem problem?problem.key():failure instanceof IOException?"ctbBackup.error.storage":"ctbBackup.error.failed";
            error=key;
            LoggerFactory.getLogger(getClass()).warn("CTB backup failed: job={}, category={}",job.status.id(),key);
        } finally {
            if(!published) {cleanup(target);cleanup(pending);}
            if(published && error==null && retention!=null && completed!=null && completed.automatic()) {
                job.update("RUNNING","CLEANING",0,0,null);
                var result=retention.afterSuccess(completed);var status=job.status;
                job.status=new Status(status.id(),status.sourceName(),status.started(),status.state(),status.phase(),0,0,null,true,result.removed(),result.skipped());
            }
            job.update(published && error==null?"SUCCEEDED":"FAILED","FINISHED",0,0,
                    error!=null?error:published?null:"ctbBackup.error.interrupted");
        }
    }
    private static void cleanup(Path path) {
        if(path!=null)try{Files.deleteIfExists(path);}catch(IOException failure){LoggerFactory.getLogger(CtbBackupService.class).warn("An incomplete CTB backup could not be removed.");}
    }
    @PreDestroy public synchronized void close() { closing=true;executor.shutdown(); }
}
