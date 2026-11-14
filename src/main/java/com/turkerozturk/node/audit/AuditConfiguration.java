package com.turkerozturk.node.audit;
import java.lang.reflect.Method;
import java.util.*;
import org.springframework.aop.*;
import org.springframework.aop.support.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.*;
/** Infrastructure advisor runs after authorization and outside the existing REQUIRED service transaction. */
@Configuration(proxyBeanMethods=false) @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class AuditConfiguration {
 static String action(Class<?> type,Method method,Object[] args){
  String name=type.getName().split("\\$")[0],m=method.getName();
  if(name.equals("com.turkerozturk.richtext.editing.RichTextEditingService")&&m.equals("save"))return "content.edit";
  if(name.equals("com.turkerozturk.node.contentediting.NodeContentEditingService")&&m.equals("update"))return "content.edit";
  if(name.equals("com.turkerozturk.node.properties.NodePropertiesService")&&m.equals("update"))return "properties.update";
  if(name.equals("com.turkerozturk.node.tags.QuickTagService")&&m.equals("save"))return "tags.update";
  if(name.equals("com.turkerozturk.node.tags.TagManagementService")&&m.equals("apply"))return "tags.manage";
  if(name.equals("com.turkerozturk.node.moving.NodeMoveService")&&Set.of("move","moveTo").contains(m))return "tree.move";
  if(name.equals("com.turkerozturk.node.moving.BatchNodeService")&&m.equals("apply"))return args==null?"batch": "batch."+args[2];
  if(name.equals("com.turkerozturk.bookmark.BookmarkWriteService")&&Set.of("add","remove","reorder","reorderAll").contains(m))return "bookmark."+m;
  return null;
 }
 @Bean @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
 public static Advisor ctblogsAdvisor(ObjectProvider<AuditInterceptor> interceptor){
  var pointcut=new StaticMethodMatcherPointcut(){@Override public boolean matches(Method method,Class<?> targetClass){return java.lang.reflect.Modifier.isPublic(method.getModifiers())&&action(targetClass,method,null)!=null;}};
  var advisor=new DefaultPointcutAdvisor(pointcut,(org.aopalliance.intercept.MethodInterceptor)call->interceptor.getObject().invoke(call));advisor.setOrder(1000);return advisor;
 }
}
