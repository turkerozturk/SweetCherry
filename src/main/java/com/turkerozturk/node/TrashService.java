package com.turkerozturk.node;
import com.turkerozturk.node.properties.NodePropertiesService;
import jakarta.persistence.*;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
@Service
public class TrashService {
 @PersistenceContext private EntityManager entityManager;
 private final NodePropertiesService properties;private final boolean enabled;
 public TrashService(NodePropertiesService properties,@Value("${myapp.trash.enabled:false}") boolean enabled){this.properties=properties;this.enabled=enabled;}
 public boolean enabled(){return enabled;}
 private void writable(){if(!properties.writable())throw TrashSql.problem(HttpStatus.FORBIDDEN,"trash.readonly");}
 @Transactional(readOnly=true) public TrashSql.Preview preview(long id){writable();if(!enabled)throw TrashSql.problem(HttpStatus.CONFLICT,"trash.disabled");return entityManager.unwrap(Session.class).doReturningWork(c->TrashSql.preview(c,id));}
 @Transactional public long trash(long id,String revision){writable();if(!enabled)throw TrashSql.problem(HttpStatus.CONFLICT,"trash.disabled");var auth=SecurityContextHolder.getContext().getAuthentication();String actor=auth==null?"unknown":auth.getName();long next=entityManager.unwrap(Session.class).doReturningWork(c->TrashSql.trash(c,id,revision,actor));entityManager.clear();return next;}
 @Transactional(readOnly=true) public TrashSql.Listing listing(){writable();return entityManager.unwrap(Session.class).doReturningWork(TrashSql::listing);}
 @Transactional(readOnly=true) public TrashSql.Entry entry(long id){writable();return entityManager.unwrap(Session.class).doReturningWork(c->TrashSql.entry(c,id));}
 @Transactional public long restore(long id,String revision,boolean root){writable();long node=entityManager.unwrap(Session.class).doReturningWork(c->TrashSql.restore(c,id,revision,root));entityManager.clear();return node;}
 @Transactional(readOnly=true) public TrashSql.PurgePreview purgePreview(long id){writable();return entityManager.unwrap(Session.class).doReturningWork(c->TrashSql.purgePreview(c,id));}
 @Transactional public void purge(long id,String revision,boolean confirmed){writable();if(!confirmed)throw TrashSql.problem(HttpStatus.BAD_REQUEST,"trash.confirmRequired");entityManager.unwrap(Session.class).doWork(c->TrashSql.purge(c,id,revision));entityManager.clear();}
}
