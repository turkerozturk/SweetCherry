package com.turkerozturk.node.audit;
import jakarta.persistence.*;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.aopalliance.intercept.*;
import java.time.*;
import java.util.*;
@Component
public class AuditInterceptor implements MethodInterceptor {
 @PersistenceContext private EntityManager manager;
 private final boolean enabled;private final int limit;private final TransactionTemplate transaction;
 private final ThreadLocal<Boolean> active=new ThreadLocal<>();
 public AuditInterceptor(PlatformTransactionManager transactions,@Value("${myapp.audit.enabled:false}") boolean enabled,@Value("${myapp.audit.max-event-characters:16777216}") int limit){
  if(limit<1000000||limit>67108864)throw new IllegalArgumentException("myapp.audit.max-event-characters must be 1000000..67108864");this.enabled=enabled;this.limit=limit;this.transaction=new TransactionTemplate(transactions);
 }
 private static final class Failure extends RuntimeException{final Throwable original;Failure(Throwable t){super(t);original=t;}}
 @Override public Object invoke(MethodInvocation call)throws Throwable{
  if(!enabled||Boolean.TRUE.equals(active.get()))return call.proceed();
  String action=AuditConfiguration.action(org.springframework.aop.support.AopUtils.getTargetClass(call.getThis()),call.getMethod(),call.getArguments());
  if(action==null)return call.proceed();var subjects=new ArrayList<Long>();Object first=call.getArguments().length==0?null:call.getArguments()[0];
  if(first instanceof Number n)subjects.add(n.longValue());else if(first instanceof Collection<?> list)for(Object id:list)if(id instanceof Number n)subjects.add(n.longValue());
  subjects.removeIf(id->id<=0);var ids=subjects.stream().distinct().sorted().toList();boolean content=action.equals("content.edit")||action.equals("properties.update");
  active.set(true);
  try{return transaction.execute(status->{
   var before=manager.unwrap(Session.class).doReturningWork(c->AuditSnapshot.read(c,ids,content,limit,true));
   final Object result;try{result=call.proceed();}catch(Throwable failure){throw new Failure(failure);}
   manager.flush();var after=manager.unwrap(Session.class).doReturningWork(c->AuditSnapshot.read(c,ids,content,limit,false));
   var auth=SecurityContextHolder.getContext().getAuthentication();String actor=auth==null?"unknown":auth.getName();var deleted=new ArrayList<String>();
   if(call.getThis() instanceof com.turkerozturk.richtext.editing.RichTextEditingService&&call.getArguments().length>6&&call.getArguments()[6] instanceof String json){
    try{var tree=AuditSql.JSON.readTree(json);if(tree.isArray())for(var value:tree)if(value.isTextual())deleted.add(value.textValue());}catch(RuntimeException ignored){/* The service already validated successful deletion requests. */}
   }
   manager.unwrap(Session.class).doWork(c->AuditSql.append(c,before,after,ids,action,deleted,actor,Instant.now(),limit));manager.clear();return result;
  });}catch(Failure failure){throw failure.original;}finally{active.remove();}
 }
}
