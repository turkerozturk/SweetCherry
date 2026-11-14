package com.turkerozturk.node.audit;
import jakarta.persistence.*;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @Transactional(readOnly=true)
public class AuditReadService {
 @PersistenceContext private EntityManager manager;
 public AuditSql.History history(String day,long node,int page){return manager.unwrap(Session.class).doReturningWork(c->AuditSql.history(c,day,node,page));}
 public AuditSql.Event event(String id){return manager.unwrap(Session.class).doReturningWork(c->AuditSql.event(c,id));}
 public String source(AuditSql.Event event,long node,String stage,boolean raw){return manager.unwrap(Session.class).doReturningWork(c->AuditSql.source(c,event,node,stage,raw));}
}
