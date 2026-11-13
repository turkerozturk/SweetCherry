package com.turkerozturk.node.links;
import com.turkerozturk.multipledatabases.TenantContext;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class LinkGraphServiceTest {
 Connection connection;LinkGraphService service;
 @BeforeEach void setup()throws Exception{
  connection=DriverManager.getConnection("jdbc:sqlite::memory:");
  try(var q=connection.createStatement()){
   q.execute("CREATE TABLE node(node_id INTEGER,name TEXT,is_ro INTEGER,is_richtxt INTEGER,syntax TEXT,txt TEXT)");q.execute("CREATE TABLE children(node_id INTEGER,father_id INTEGER,master_id INTEGER)");q.execute("CREATE TABLE image(node_id INTEGER,offset INTEGER,filename TEXT,anchor TEXT,link TEXT,png BLOB)");
   q.execute("INSERT INTO node VALUES(1,'<Source>',0,1,'custom-colors','<node><rich_text link=\"node 2\">Target</rich_text><rich_text>Gap</rich_text><rich_text link=\"node 2\">Again</rich_text><rich_text link=\"node 999\">Missing</rich_text></node>'),(2,'Target',0,0,'plain-text',''),(3,'Parent',0,0,'plain-text','')");q.execute("INSERT INTO children VALUES(1,3,0),(2,0,0),(3,0,0),(11,3,1)");
  }
  var em=mock(jakarta.persistence.EntityManager.class);var session=mock(org.hibernate.Session.class);when(em.unwrap(org.hibernate.Session.class)).thenReturn(session);
  when(session.doReturningWork(any())).thenAnswer(invocation->((org.hibernate.jdbc.ReturningWork<?>)invocation.getArgument(0)).execute(connection));
  var links=new InternalLinkReportService();ReflectionTestUtils.setField(links,"entityManager",em);service=new LinkGraphService(links);ReflectionTestUtils.setField(service,"entityManager",em);TenantContext.setCurrentTenant("A");
 }
 @AfterEach void close()throws Exception{TenantContext.clear();connection.close();}
 @Test void linksAreDirectedGroupedAndSharedOccurrencesStayDistinct(){
  var graph=service.graph(2,"view",false,40,false);assertThat(graph.nodes()).extracting(LinkGraphService.Vertex::id).containsExactly("2","1","11");
  assertThat(graph.edges()).hasSize(2).allMatch(e->e.count()==2&&e.target().equals("2"));assertThat(graph.broken()).isEqualTo(2);
  assertThat(graph.nodes()).anyMatch(n->n.id().equals("11")&&n.shared());
  var source=service.graph(1,"view",true,40,false);assertThat(source.nodes()).anyMatch(n->n.id().equals("3")&&n.zone().equals("parent"));assertThat(source.edges()).anyMatch(e->e.kind().equals("tree"));
 }
 @Test void cacheIsScopedToTenantAndViewAndExplicitRefreshSeesExternalChanges()throws Exception{
  service.graph(1,"one",false,40,false);try(var q=connection.createStatement()){q.execute("UPDATE node SET txt='<node><rich_text>Removed</rich_text></node>' WHERE node_id=1");}
  assertThat(service.graph(1,"one",false,40,false).edges()).hasSize(1);
  assertThat(service.graph(1,"two",false,40,false).edges()).isEmpty();
  TenantContext.setCurrentTenant("B");assertThat(service.graph(1,"one",false,40,false).edges()).isEmpty();
  TenantContext.setCurrentTenant("A");assertThat(service.graph(1,"one",false,40,true).edges()).isEmpty();
 }
 @Test void missingNodesAndInvalidLimitsFailWithoutWriting()throws Exception{
  assertThatThrownBy(()->service.graph(999,"v",false,40,false)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
  assertThatThrownBy(()->service.graph(1,"v",false,201,false)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
  TenantContext.clear();assertThatThrownBy(()->service.graph(1,"v",false,40,false)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
  try(var q=connection.createStatement();var rows=q.executeQuery("SELECT count(*) FROM node")){rows.next();assertThat(rows.getInt(1)).isEqualTo(3);}
 }
 @Test void neighbourLimitsAreDeterministicAndBothDirectionsRemainVisible(){
  var places=new HashMap<Long,LinkGraphService.Place>();var arcs=new HashMap<LinkGraphService.Arc,Integer>();
  for(long id=1;id<=15;id++){places.put(id,new LinkGraphService.Place(id,0,"N"+id,"ct_notes",false,"#000000",false));if(id>1)arcs.put(new LinkGraphService.Arc(1,id,"link"),1);}
  arcs.put(new LinkGraphService.Arc(2,1,"link"),1);
  var index=new LinkGraphService.Index(places,arcs,false,0,0,1);var graph=LinkGraphService.select(index,1,false,10);
  assertThat(graph.more()).isTrue();assertThat(graph.nodes()).hasSize(11);assertThat(graph.nodes()).anyMatch(n->n.id().equals("2")&&n.zone().equals("both"));
 }
}
