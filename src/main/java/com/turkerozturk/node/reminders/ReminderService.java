package com.turkerozturk.node.reminders;

import com.turkerozturk.calendar.CalendarSettings;
import com.turkerozturk.node.properties.NodePropertiesService;
import jakarta.persistence.*;
import java.sql.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.hibernate.Session;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReminderService {
    @PersistenceContext private EntityManager manager;
    private final NodePropertiesService properties;
    private final CalendarSettings calendar;
    private final Clock clock;
    @org.springframework.beans.factory.annotation.Autowired
    public ReminderService(NodePropertiesService properties,CalendarSettings calendar){this(properties,calendar,Clock.systemUTC());}
    ReminderService(NodePropertiesService properties,CalendarSettings calendar,Clock clock){this.properties=properties;this.calendar=calendar;this.clock=clock;}
    public record Options(String q,String status,int limit,int page){
        public Options {if(q==null||q.length()>500||!Set.of("pending","upcoming","overdue","done","cancelled","all").contains(status)||limit<1||limit>200||page<1||page>1_000_000)throw new IllegalArgumentException();}
    }
    public record Row(String id,long contentId,long nodeId,String name,boolean available,String message,String due,String zone,String status){
        public String dueText(){return ReminderTime.display(due,zone);}
        public boolean pending(){return status.equals("upcoming")||status.equals("overdue");}
    }
    public record View(List<Row> rows,long upcoming,long overdue,long done,long cancelled,long matched,String revision,String asOf,Options options){
        public long pages(){return Math.max(1,(matched+options.limit()-1)/options.limit());}
        public long first(){return rows.isEmpty()?0:(long)(options.page()-1)*options.limit()+1;}
        public long last(){return rows.isEmpty()?0:first()+rows.size()-1;}
    }
    public record Editor(String id,long nodeId,long contentId,String name,boolean available,String localTime,String zone,String message,String status,String revision){
        public Editor input(String local,String zone,String message,String revision){return new Editor(id,nodeId,contentId,name,available,local,zone,message,status,revision);}
    }
    @Transactional(readOnly=true)
    public View inspect(Options options){return manager.unwrap(Session.class).doReturningWork(c->inspect(c,options,clock.instant()));}
    static View inspect(Connection c,Options options,Instant now)throws SQLException{
        var store=ReminderStore.read(c,false);var targets=ReminderTargets.read(c);var rows=new ArrayList<Row>();
        long upcoming=0,overdue=0,done=0,cancelled=0;String query=fold(options.q());
        for(var item:store.data().items()){
            String state=item.status().equals("pending")?(Instant.parse(item.due()).isAfter(now)?"upcoming":"overdue"):item.status();
            switch(state){case "upcoming"->upcoming++;case "overdue"->overdue++;case "done"->done++;case "cancelled"->cancelled++;default->throw ReminderStore.problem("storeInvalid");}
            boolean pending=state.equals("upcoming")||state.equals("overdue");
            if(!options.status().equals("all")&&!(options.status().equals("pending")?pending:options.status().equals(state)))continue;
            var target=targets.content(item.contentId(),item.occurrence());String name=target==null?item.targetName():target.name();
            if(!fold(name+" "+item.message()+" "+item.contentId()+" "+item.id()).contains(query))continue;
            rows.add(new Row(item.id(),item.contentId(),target==null?0:target.id(),name,target!=null,item.message(),item.due(),item.zone(),state));
        }
        rows.sort(Comparator.comparing((Row row)->Instant.parse(row.due())).thenComparing(Row::id));
        long matched=rows.size(),start=(long)(options.page()-1)*options.limit();
        var page=start>=matched?List.<Row>of():List.copyOf(rows.subList((int)start,(int)Math.min(matched,start+options.limit())));
        return new View(page,upcoming,overdue,done,cancelled,matched,store.revision(),now.toString(),options);
    }
    @Transactional(readOnly=true)
    public Editor editor(String id,long nodeId){
        writable();return manager.unwrap(Session.class).doReturningWork(c->{
            var store=ReminderStore.read(c,false);var targets=ReminderTargets.read(c);
            if(id.isEmpty()){
                var target=targets.occurrence(nodeId);if(target==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"targetMissing");
                var date=clock.instant().plus(1,ChronoUnit.HOURS).truncatedTo(ChronoUnit.MINUTES);String zone=calendar.zone().getId();
                return new Editor("",target.id(),target.contentId(),target.name(),true,ReminderTime.local(date.toString(),zone),zone,"","pending",store.revision());
            }
            var item=find(store,id);var target=targets.content(item.contentId(),item.occurrence());
            return new Editor(item.id(),target==null?item.occurrence():target.id(),item.contentId(),target==null?item.targetName():target.name(),target!=null,
                    ReminderTime.local(item.due(),item.zone()),item.zone(),item.message(),item.status(),store.revision());
        });
    }
    @Transactional
    public String save(String id,long nodeId,long contentId,String localTime,String zone,String message,String revision){
        writable();if(id==null||!id.isEmpty()&&!ReminderStore.uuid(id)||message==null||message.length()>2000||message.indexOf('\0')>=0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"invalidInput");
        final Instant due;final String zoneId;
        try{due=ReminderTime.resolve(localTime,zone);zoneId=ZoneId.of(zone).getId();}catch(IllegalArgumentException invalid){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"invalidTime");}
        String saved=write(c->{
            var allocation=ReminderStore.lockForWrite(c);var store=ReminderStore.read(c,true);ReminderStore.expected(store,revision);
            var targets=ReminderTargets.read(c);var target=targets.occurrence(nodeId);
            if(target==null||target.contentId()!=contentId)throw ReminderStore.problem("targetMissing");
            ReminderStore.Item old=id.isEmpty()?null:find(store,id);
            if(old!=null&&old.contentId()!=contentId)throw ReminderStore.problem("targetMissing");
            String uuid=old==null?UUID.randomUUID().toString():old.id();Instant now=clock.instant();String name=target.name().substring(0,Math.min(1000,target.name().length()));
            if(old!=null&&old.occurrence()==target.id()&&old.targetName().equals(name)&&old.message().equals(message)&&old.due().equals(due.toString())&&old.zone().equals(zoneId))return uuid;
            var item=new ReminderStore.Item(uuid,contentId,target.id(),name,message,due.toString(),zoneId,old==null?"pending":old.status(),old==null?now.toString():old.created(),now.toString());
            var items=new ArrayList<>(store.data().items());if(old!=null)items.remove(old);items.add(item);
            ReminderStore.write(c,store,items,allocation,now);return uuid;
        });manager.clear();return saved;
    }
    @Transactional
    public void changeState(String id,String state,String revision){
        writable();if(!Set.of("pending","done","cancelled").contains(state))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"invalidInput");
        write(c->{
            var allocation=ReminderStore.lockForWrite(c);var store=ReminderStore.read(c,true);ReminderStore.expected(store,revision);var old=find(store,id);
            if(state.equals("pending")&&ReminderTargets.read(c).content(old.contentId(),old.occurrence())==null)throw ReminderStore.problem("targetMissing");
            if(old.status().equals(state))return null;Instant now=clock.instant();
            var item=new ReminderStore.Item(old.id(),old.contentId(),old.occurrence(),old.targetName(),old.message(),old.due(),old.zone(),state,old.created(),now.toString());
            var items=new ArrayList<>(store.data().items());items.set(items.indexOf(old),item);ReminderStore.write(c,store,items,allocation,now);return null;
        });manager.clear();
    }
    private static ReminderStore.Item find(ReminderStore.Snapshot store,String id){
        if(!ReminderStore.uuid(id))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"invalidInput");
        return store.data().items().stream().filter(item->item.id().equals(id)).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"missing"));
    }
    private void writable(){if(!properties.writable())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"readonly");}
    @FunctionalInterface private interface Write<T>{T apply(Connection c)throws SQLException;}
    private <T>T write(Write<T> work){return manager.unwrap(Session.class).doReturningWork(c->{
        try{return work.apply(c);}catch(SQLException failure){
            if("23505".equals(failure.getSQLState())||failure.getErrorCode()==19||failure.getErrorCode()==1062)throw ReminderStore.problem("changed");throw failure;
        }
    });}
    private static String fold(String value){return java.text.Normalizer.normalize(value.toLowerCase(Locale.ROOT),java.text.Normalizer.Form.NFD).replaceAll("\\p{M}+","").replace('ı','i');}
}
