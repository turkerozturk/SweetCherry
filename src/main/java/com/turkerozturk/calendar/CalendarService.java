package com.turkerozturk.calendar;

import com.turkerozturk.node.Node;
import com.turkerozturk.node.properties.NodePropertiesService;
import com.turkerozturk.node.tags.NodeTags;
import com.turkerozturk.richtext.experimental.RichTextRenderingService;
import jakarta.persistence.*;
import java.sql.*;
import java.time.*;
import java.time.format.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import org.hibernate.Session;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly=true)
public class CalendarService {
    @PersistenceContext private EntityManager entityManager;
    private final CalendarSettings settings;private final NodePropertiesService properties;private final RichTextRenderingService rendering;
    public CalendarService(CalendarSettings settings,NodePropertiesService properties,RichTextRenderingService rendering){this.settings=settings;this.properties=properties;this.rendering=rendering;}
    public record Entry(String id,String contentId,String name,String icon,boolean bold,String color,boolean shared,boolean readOnly,
            long created,long modified,String local,String time,List<String> tags) { }
    public record Day(LocalDate date,String label,String weekday,boolean currentMonth,boolean today,List<Entry> entries) { }
    public record Hour(String label,List<List<Entry>> cells) { }
    public record View(String mode,LocalDate anchor,LocalDate previous,LocalDate next,boolean canPrevious,boolean canNext,String title,
            String zone,List<Day> days,List<Hour> hours,int count,boolean truncated,List<String> tags,String match) { }
    private record Place(long id,long content,boolean shared) { }
    public View view(String mode,LocalDate anchor,List<String> selected,String match,Locale locale) {
        if(!Set.of("month","week").contains(mode)||selected.size()>100||!Set.of("any","all").contains(match))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"invalidFilter");
        for(String tag:selected)if(tag==null||tag.isEmpty()||tag.length()>NodeTags.MAX_INPUT_CHARACTERS||tag.indexOf(' ')>=0||tag.indexOf('\0')>=0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"invalidFilter");
        LocalDate start=(mode.equals("month")?anchor.withDayOfMonth(1):anchor).with(TemporalAdjusters.previousOrSame(settings.firstDay()));
        int length=mode.equals("month")?42:7;
        long from=start.atStartOfDay(settings.zone()).toEpochSecond(),to=start.plusDays(length).atStartOfDay(settings.zone()).toEpochSecond();
        var rows=entityManager.unwrap(Session.class).doReturningWork(c->read(c,from,to,selected,match,settings.maxEntries()+1));
        boolean truncated=rows.size()>settings.maxEntries();if(truncated)rows=rows.subList(0,settings.maxEntries());
        var days=new ArrayList<Day>();LocalDate today=LocalDate.now(settings.zone());
        var label=DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale);var weekday=DateTimeFormatter.ofPattern("EEE",locale);
        for(int i=0;i<length;i++){LocalDate date=start.plusDays(i);var items=new ArrayList<Entry>();for(var row:rows)if(Instant.ofEpochSecond(row.created()).atZone(settings.zone()).toLocalDate().equals(date))items.add(row);
            days.add(new Day(date,date.format(label),date.format(weekday),date.getMonth()==anchor.getMonth()&&date.getYear()==anchor.getYear(),date.equals(today),List.copyOf(items)));}
        var hours=new ArrayList<Hour>();
        if(mode.equals("week"))for(int hour=0;hour<24;hour++){var cells=new ArrayList<List<Entry>>();final int h=hour;for(var day:days)cells.add(day.entries().stream().filter(e->Instant.ofEpochSecond(e.created()).atZone(settings.zone()).getHour()==h).toList());hours.add(new Hour(String.format(Locale.ROOT,"%02d:00",hour),cells));}
        LocalDate previous=mode.equals("month")?anchor.withDayOfMonth(1).minusMonths(1):anchor.minusWeeks(1),next=mode.equals("month")?anchor.withDayOfMonth(1).plusMonths(1):anchor.plusWeeks(1);
        String title=mode.equals("month")?anchor.format(DateTimeFormatter.ofPattern("LLLL yyyy",locale)):days.get(0).label()+" – "+days.get(6).label();
        return new View(mode,anchor,previous,next,previous.getYear()>=1,next.getYear()<=9999,title,settings.zone().getId(),List.copyOf(days),List.copyOf(hours),rows.size(),truncated,List.copyOf(selected),match);
    }
    List<Entry> read(Connection c,long from,long to,List<String> selected,String match,int limit)throws SQLException {
        var places=new HashMap<Long,List<Place>>();
        try(var query=c.createStatement();var rows=query.executeQuery("SELECT node_id,COALESCE(master_id,0) FROM children")) {
            while(rows.next()){long id=rows.getLong(1),master=rows.getLong(2);if(id>0&&master>=0)places.computeIfAbsent(master==0?id:master,key->new ArrayList<>()).add(new Place(id,master==0?id:master,master!=0));}
        }
        places.values().forEach(list->list.sort(Comparator.comparingLong(Place::id)));
        var result=new ArrayList<Entry>();
        try(var query=c.prepareStatement("SELECT node_id,name,tags,is_ro,is_richtxt,syntax,ts_creation,ts_lastsave FROM node WHERE ts_creation>=? AND ts_creation<? ORDER BY ts_creation,node_id")) {
            query.setLong(1,from);query.setLong(2,to);
            try(var rows=query.executeQuery()){while(rows.next()){
                var tags=NodeTags.split(rows.getString(3));if(!selected.isEmpty()&&!(match.equals("all")?tags.containsAll(selected):selected.stream().anyMatch(tags::contains)))continue;
                for(var place:places.getOrDefault(rows.getLong(1),List.of())){result.add(entry(rows,place,tags));if(result.size()>=limit)return result;}
            }}
        }
        return result;
    }
    private Entry entry(ResultSet rows,Place place,List<String> tags)throws SQLException {
        var display=new Node();display.setIsReadOnly16bit(rows.getLong(4));display.setIsRichText(rows.getLong(5));display.setSyntax(Objects.requireNonNullElse(rows.getString(6),"plain-text"));
        display.parseNodeIconAndReadOnlyFlag();display.parseNodeTitleColorAndBoldnessAndTextType();
        var local=Instant.ofEpochSecond(rows.getLong(7)).atZone(settings.zone());
        return new Entry(Long.toString(place.id()),Long.toString(place.content()),Objects.requireNonNullElse(rows.getString(2),""),display.getNodeIcon().getIconName(),display.isBoldnessBit(),String.format(Locale.ROOT,"#%06X",display.getTitleColor()),place.shared(),(rows.getLong(4)&1)!=0,rows.getLong(7),rows.getLong(8),local.format(DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm")),local.format(DateTimeFormatter.ofPattern("HH:mm")),tags);
    }
    private Place place(Connection c,long id)throws SQLException {
        if(id<=0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"invalidNode");
        try(var query=c.prepareStatement("SELECT COALESCE(master_id,0) FROM children WHERE node_id=?")){query.setLong(1,id);try(var rows=query.executeQuery()){
            if(!rows.next())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"missingNode");long master=rows.getLong(1);if(master<0)throw new ResponseStatusException(HttpStatus.CONFLICT,"conflict");return new Place(id,master==0?id:master,master!=0);
        }}
    }
    @Transactional
    public void changeCreated(long id,long expectedCreated,long expectedModified,String local) {
        if(!properties.writable())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"readOnly");long created=settings.timestamp(local);
        entityManager.unwrap(Session.class).doWork(c->{
            Place place=place(c,id);long before,modified,flags;
            try(var query=c.prepareStatement("SELECT ts_creation,ts_lastsave,is_ro FROM node WHERE node_id=?")){query.setLong(1,place.content());try(var rows=query.executeQuery()){
                if(!rows.next())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"missingNode");before=rows.getLong(1);modified=rows.getLong(2);flags=rows.getLong(3);
            }}
            if((flags&1)!=0)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"readOnly");
            if(before!=expectedCreated||modified!=expectedModified)throw new ResponseStatusException(HttpStatus.CONFLICT,"conflict");
            if(before==created || Instant.ofEpochSecond(before).atZone(settings.zone()).format(DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm")).equals(local))return;
            try(var query=c.prepareStatement("UPDATE node SET ts_creation=?,ts_lastsave=? WHERE node_id=? AND ts_creation=? AND ts_lastsave=? AND (is_ro & 1)=0 AND EXISTS (SELECT 1 FROM children WHERE node_id=? AND COALESCE(master_id,0)=?)")){
                query.setLong(1,created);query.setLong(2,Instant.now().getEpochSecond());query.setLong(3,place.content());query.setLong(4,before);query.setLong(5,modified);query.setLong(6,id);query.setLong(7,place.shared()?place.content():0);
                if(query.executeUpdate()!=1)throw new ResponseStatusException(HttpStatus.CONFLICT,"conflict");
            }
        });
    }
    public String content(long id,String token){
        long content=entityManager.unwrap(Session.class).doReturningWork(c->place(c,id).content());Node node=entityManager.find(Node.class,content);
        if(node==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"missingNode");
        if(node.getTxt()!=null&&node.getTxt().length()>2000000)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"largeContent");
        if("custom-colors".equals(node.getSyntax()))return rendering.renderLive(node,token);
        return new org.jsoup.nodes.Element("pre").text(Objects.requireNonNullElse(node.getTxt(),"")).outerHtml();
    }
}
