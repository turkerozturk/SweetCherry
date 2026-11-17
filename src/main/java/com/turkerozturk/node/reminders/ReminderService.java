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
    public record Row(String id,long contentId,long nodeId,String name,boolean available,String message,String due,String zone,String status,
            String repeat,String lastCompleted,String acknowledgedAt,String snoozedUntil,String alarmId){
        public Row(String id,long contentId,long nodeId,String name,boolean available,String message,String due,String zone,String status){this(id,contentId,nodeId,name,available,message,due,zone,status,"none","","","","");}
        public String dueText(){return ReminderTime.display(due,zone);}
        public String lastCompletedText(){return lastCompleted.isEmpty()?"":ReminderTime.display(lastCompleted,zone);}
        public String snoozedText(){return snoozedUntil.isEmpty()?"":ReminderTime.display(snoozedUntil,zone);}
        public boolean pending(){return status.equals("upcoming")||status.equals("overdue");}
        public boolean alarmMuted(){return !acknowledgedAt.isEmpty()||!snoozedUntil.isEmpty();}
    }
    public record View(List<Row> rows,long upcoming,long overdue,long done,long cancelled,long matched,String revision,String asOf,Options options){
        public long pages(){return Math.max(1,(matched+options.limit()-1)/options.limit());}
        public long first(){return rows.isEmpty()?0:(long)(options.page()-1)*options.limit()+1;}
        public long last(){return rows.isEmpty()?0:first()+rows.size()-1;}
    }
    public record Editor(String id,long nodeId,long contentId,String name,boolean available,String localTime,String zone,String message,String status,String revision,String repeat,String leapDay,String anchor,String alarmId){
        public Editor(String id,long nodeId,long contentId,String name,boolean available,String localTime,String zone,String message,String status,String revision){this(id,nodeId,contentId,name,available,localTime,zone,message,status,revision,"none","feb28","","");}
        public Editor input(String local,String zone,String message,String revision){return input(local,zone,message,revision,repeat,leapDay);}
        public Editor input(String local,String zone,String message,String revision,String repeat,String leapDay){return input(local,zone,message,revision,repeat,leapDay,alarmId);}
        public Editor input(String local,String zone,String message,String revision,String repeat,String leapDay,String occurrence){return new Editor(id,nodeId,contentId,name,available,local,zone,message,status,revision,repeat,leapDay,anchor,occurrence);}
    }
    public record Alarm(String id,String key,String nodeId,String name,String message,String dueText,String repeat) { }
    public record Alarms(String storeId,List<Alarm> items,long total) { }
    @Transactional(readOnly=true)
    public View inspect(Options options){return manager.unwrap(Session.class).doReturningWork(c->inspect(c,options,clock.instant()));}
    static View inspect(Connection c,Options options,Instant now)throws SQLException{
        var store=ReminderStore.read(c,false);var targets=ReminderTargets.read(c);var rows=new ArrayList<Row>();
        long upcoming=0,overdue=0,done=0,cancelled=0;String query=fold(options.q());
        for(var stored:store.data().items()){
            var item=effective(stored,now);
            String state=item.status().equals("pending")?(Instant.parse(item.due()).isAfter(now)?"upcoming":"overdue"):item.status();
            switch(state){case "upcoming"->upcoming++;case "overdue"->overdue++;case "done"->done++;case "cancelled"->cancelled++;default->throw ReminderStore.problem("storeInvalid");}
            boolean pending=state.equals("upcoming")||state.equals("overdue");
            if(!options.status().equals("all")&&!(options.status().equals("pending")?pending:options.status().equals(state)))continue;
            var target=targets.content(item.contentId(),item.occurrence());String name=target==null?item.targetName():target.name();
            if(!fold(name+" "+item.message()+" "+item.contentId()+" "+item.id()).contains(query))continue;
            rows.add(new Row(item.id(),item.contentId(),target==null?0:target.id(),name,target!=null,item.message(),item.due(),item.zone(),state,item.repeat(),item.lastCompleted(),item.acknowledgedAt(),item.snoozedUntil(),item.alarmId()));
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
            var item=effective(find(store,id),clock.instant());var target=targets.content(item.contentId(),item.occurrence());
            return new Editor(item.id(),target==null?item.occurrence():target.id(),item.contentId(),target==null?item.targetName():target.name(),target!=null,
                    ReminderTime.local(item.due(),item.zone()),item.zone(),item.message(),item.status(),store.revision(),item.repeat(),item.leapDay(),item.anchor(),item.alarmId());
        });
    }
    @Transactional
    public String save(String id,long nodeId,long contentId,String localTime,String zone,String message,String revision){return save(id,nodeId,contentId,localTime,zone,message,"none","feb28",revision);}
    @Transactional
    public String save(String id,long nodeId,long contentId,String localTime,String zone,String message,String repeat,String leapDay,String revision){return save(id,nodeId,contentId,localTime,zone,message,repeat,leapDay,revision,null);}
    @Transactional
    public String save(String id,long nodeId,long contentId,String localTime,String zone,String message,String repeat,String leapDay,String revision,String occurrence){
        writable();if(id==null||!id.isEmpty()&&!ReminderStore.uuid(id)||message==null||message.length()>2000||message.indexOf('\0')>=0
                ||!Set.of("none","yearly").contains(repeat)||!Set.of("feb28","mar1").contains(leapDay))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"invalidInput");
        final Instant due;final String zoneId;
        try{due=ReminderTime.resolve(localTime,zone);zoneId=ZoneId.of(zone).getId();}catch(IllegalArgumentException invalid){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"invalidTime");}
        String saved=write(c->{
            var allocation=ReminderStore.lockForWrite(c);var store=ReminderStore.read(c,true);ReminderStore.expected(store,revision);
            var target=ReminderTargets.read(c).occurrence(nodeId);if(target==null||target.contentId()!=contentId)throw ReminderStore.problem("targetMissing");
            Instant now=clock.instant();ReminderStore.Item old=id.isEmpty()?null:effective(find(store,id),now);
            if(old!=null&&old.contentId()!=contentId)throw ReminderStore.problem("targetMissing");
            if(old!=null&&occurrence!=null&&old.repeat().equals("yearly")&&!old.alarmId().equals(occurrence))throw ReminderStore.problem("changed");
            String uuid=old==null?UUID.randomUUID().toString():old.id();String name=target.name().substring(0,Math.min(1000,target.name().length()));
            boolean dateChanged=old==null||!old.due().equals(due.toString())||!old.zone().equals(zoneId)||!old.repeat().equals(repeat);
            boolean scheduleChanged=dateChanged||old!=null&&!old.leapDay().equals(leapDay);
            boolean anchorChanged=old==null||!ReminderTime.local(old.due(),old.zone()).equals(localTime)||!old.repeat().equals(repeat);
            String anchor=repeat.equals("none")?"":anchorChanged?localTime:old.anchor();
            String actualDue=due.toString();
            if(repeat.equals("yearly")&&!anchorChanged&&scheduleChanged)actualDue=AnnualReminder.occurrence(anchor,zoneId,leapDay,LocalDateTime.parse(localTime).getYear()).toString();
            if(old!=null&&old.occurrence()==target.id()&&old.targetName().equals(name)&&old.message().equals(message)&&!scheduleChanged)return uuid;
            var item=new ReminderStore.Item(uuid,contentId,target.id(),name,message,actualDue,zoneId,old==null?"pending":old.status(),old==null?now.toString():old.created(),now.toString(),repeat,leapDay,anchor,
                    old==null?"":old.lastCompleted(),scheduleChanged?UUID.randomUUID().toString():old.alarmId(),scheduleChanged?"":old.acknowledgedAt(),scheduleChanged?"":old.snoozedUntil());
            ReminderStore.write(c,store,replace(store,item),allocation,now);return uuid;
        });manager.clear();return saved;
    }
    @Transactional
    public void changeState(String id,String state,String revision){changeState(id,state,revision,null);}
    @Transactional
    public void changeState(String id,String state,String revision,String occurrence){
        writable();if(!Set.of("pending","done","cancelled").contains(state))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"invalidInput");
        write(c->{
            var allocation=ReminderStore.lockForWrite(c);var store=ReminderStore.read(c,true);ReminderStore.expected(store,revision);Instant now=clock.instant();var old=effective(find(store,id),now);
            if(occurrence!=null&&old.repeat().equals("yearly")&&!old.alarmId().equals(occurrence))throw ReminderStore.problem("changed");
            if(state.equals("pending")&&ReminderTargets.read(c).content(old.contentId(),old.occurrence())==null)throw ReminderStore.problem("targetMissing");
            if(old.status().equals(state)&&!(state.equals("pending")&&(!old.acknowledgedAt().isEmpty()||!old.snoozedUntil().isEmpty())))return null;
            String due=old.due(),completed=old.lastCompleted();
            if(state.equals("done")){
                completed=old.due();if(old.repeat().equals("yearly")){due=next(old,now).toString();}
            }
            String newState=state.equals("done")&&old.repeat().equals("yearly")?"pending":state;
            var item=new ReminderStore.Item(old.id(),old.contentId(),old.occurrence(),old.targetName(),old.message(),due,old.zone(),newState,old.created(),now.toString(),old.repeat(),old.leapDay(),old.anchor(),completed,UUID.randomUUID().toString(),"","");
            ReminderStore.write(c,store,replace(store,item),allocation,now);return null;
        });manager.clear();
    }
    static ReminderStore.Item effective(ReminderStore.Item old,Instant now){
        if(!old.repeat().equals("yearly")||!old.status().equals("pending"))return old;
        var due=AnnualReminder.current(old.anchor(),old.zone(),old.leapDay(),Instant.parse(old.due()),now);
        if(due.toString().equals(old.due()))return old;
        return new ReminderStore.Item(old.id(),old.contentId(),old.occurrence(),old.targetName(),old.message(),due.toString(),old.zone(),old.status(),old.created(),old.updated(),old.repeat(),old.leapDay(),old.anchor(),old.lastCompleted(),ReminderStore.legacyAlarmId(old.alarmId(),due.toString()),"","");
    }
    private static Instant next(ReminderStore.Item item,Instant now){
        try{var due=Instant.parse(item.due());return AnnualReminder.next(item.anchor(),item.zone(),item.leapDay(),due.isAfter(now)?due:now);}
        catch(RuntimeException invalid){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"invalidTime");}
    }
    private static List<ReminderStore.Item> replace(ReminderStore.Snapshot store,ReminderStore.Item item){var items=new ArrayList<>(store.data().items());items.removeIf(value->value.id().equals(item.id()));items.add(item);return items;}
    static String alarmKey(ReminderStore.Item item){return ReminderStore.legacyAlarmId(item.alarmId(),item.snoozedUntil());}
    @Transactional(readOnly=true)
    public Alarms alarms(){return manager.unwrap(Session.class).doReturningWork(c->{
        var store=ReminderStore.read(c,false);if(store.id()==0)return new Alarms("",List.of(),0);
        var targets=ReminderTargets.read(c);Instant now=clock.instant();var due=new ArrayList<ReminderStore.Item>();
        for(var stored:store.data().items()){
            var item=effective(stored,now);
            if(item.status().equals("pending")&&!Instant.parse(item.due()).isAfter(now)&&item.acknowledgedAt().isEmpty()
                    &&(item.snoozedUntil().isEmpty()||!Instant.parse(item.snoozedUntil()).isAfter(now))&&targets.content(item.contentId(),item.occurrence())!=null)due.add(item);
        }
        due.sort(Comparator.comparing((ReminderStore.Item item)->Instant.parse(item.due())).reversed().thenComparing(ReminderStore.Item::id));
        var rows=new ArrayList<Alarm>();for(var item:due.subList(0,Math.min(20,due.size()))){var target=targets.content(item.contentId(),item.occurrence());rows.add(new Alarm(item.id(),alarmKey(item),Long.toString(target.id()),target.name().substring(0,Math.min(1000,target.name().length())),item.message(),ReminderTime.display(item.due(),item.zone()),item.repeat()));}
        return new Alarms(store.data().storeId(),List.copyOf(rows),due.size());
    });}
    @Transactional
    public void alarmAction(String id,String key,String action){
        writable();if(!Set.of("dismiss","snooze").contains(action))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"invalidInput");
        write(c->{
            var allocation=ReminderStore.lockForWrite(c);var store=ReminderStore.read(c,true);Instant now=clock.instant();var old=effective(find(store,id),now);
            if(!old.status().equals("pending")||!alarmKey(old).equals(key))throw ReminderStore.problem("changed");
            if(action.equals("dismiss")&&!old.acknowledgedAt().isEmpty())return null;
            if(!old.acknowledgedAt().isEmpty())throw ReminderStore.problem("changed");
            if(ReminderTargets.read(c).content(old.contentId(),old.occurrence())==null)throw ReminderStore.problem("targetMissing");
            var item=new ReminderStore.Item(old.id(),old.contentId(),old.occurrence(),old.targetName(),old.message(),old.due(),old.zone(),old.status(),old.created(),now.toString(),old.repeat(),old.leapDay(),old.anchor(),old.lastCompleted(),old.alarmId(),action.equals("dismiss")?now.toString():"",action.equals("snooze")?now.plus(10,ChronoUnit.MINUTES).toString():"");
            ReminderStore.write(c,store,replace(store,item),allocation,now);return null;
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
