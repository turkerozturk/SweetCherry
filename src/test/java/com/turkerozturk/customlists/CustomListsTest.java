package com.turkerozturk.customlists;

import com.turkerozturk.node.NodeNames;
import com.turkerozturk.node.creation.ChildNodeService;
import com.turkerozturk.node.properties.NodePropertiesService;
import com.turkerozturk.multipledatabases.*;
import jakarta.persistence.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomListsTest {
    @AfterEach void clear(){TenantContext.clear();}
    @Test void namesTrimInvisibleBoundariesWithoutRewritingInternalText(){
        assertThat(NodeNames.clean("\uFEFF\u200B\u00a0 \tName  İçerik\u200Binside \u00a0\r\n")).isEqualTo("Name  İçerik\u200Binside");
        assertThat(NodeNames.clean("\u200B\u00a0")).isEmpty();assertThat(NodeNames.clean(null)).isNull();
        assertThat(NodeNames.clean(" 🍒 Text ")).isEqualTo("🍒 Text");
    }
    @Test void tenantOverridesGlobalAndInvalidOrAbsentValuesFallBack(){
        var settings=new CustomListsSettings();settings.setMainNodeName(" Global ");settings.setTemplatePrefix("global-");
        var custom=new CustomPropertiesHolder();ReflectionTestUtils.setField(settings,"custom",custom);
        assertThat(settings.effective().main()).isEqualTo("Global");
        custom.addCustomProperties("A",Map.of("custom.customListsNodeName","\u200BTenant ","custom.templatePrefix","mine"));TenantContext.setCurrentTenant("A");
        assertThat(settings.effective()).isEqualTo(new CustomListsSettings.Names("Tenant","mine"));assertThat(settings.effective().template("projects")).isEqualTo("mine-projects");
        custom.addCustomProperties("A",Map.of("custom.customListsNodeName"," ","custom.templatePrefix","x".repeat(256)));
        assertThat(settings.effective().template("projects")).isEqualTo("global-projects");assertThat(settings.effective().main()).isEqualTo("Global");
        assertThatIllegalArgumentException().isThrownBy(()->settings.setMainNodeName("\u200B"));
    }
    private final EntityManager manager=mock(EntityManager.class);
    private final NodePropertiesService properties=mock(NodePropertiesService.class);
    private final ChildNodeService creation=mock(ChildNodeService.class);
    private CustomListsService service(String raw,Map<String,List<Object[]>> rows){
        when(manager.createNativeQuery(anyString())).thenAnswer(call->{
            String sql=call.getArgument(0);var query=mock(Query.class);var parameters=new HashMap<String,Object>();
            when(query.setParameter(anyString(),any())).thenAnswer(p->{parameters.put(p.getArgument(0),p.getArgument(1));return query;});
            when(query.getResultList()).thenAnswer(q->rows.getOrDefault(parameters.get("name"),List.of()));
            when(query.getSingleResult()).thenReturn(raw);when(query.executeUpdate()).thenReturn(1);return query;
        });
        when(properties.writable()).thenReturn(true);
        var service=new CustomListsService(new CustomListsSettings(),properties,creation);ReflectionTestUtils.setField(service,"manager",manager);return service;
    }
    private List<Object[]> match(long id,String name,String syntax){return Collections.singletonList(new Object[]{id,name,syntax,0});}
    @Test void tabLabelsCommentsDuplicateKeysAndWrongTypesAreExplicit(){
        var rows=new HashMap<String,List<Object[]>>();rows.put("cherrytemplatetasks",match(1,"cherrytemplatetasks","plain-text"));
        rows.put("cherrytemplate-projects",match(2,"cherrytemplate-projects","plain-text"));rows.put("cherrytemplate-code",match(3,"cherrytemplate-code","python"));
        var view=service("projects\tMy projects\r\n  #software\tSoftware\nmissing\ncode\nrepeat\nrepeat\n\tEmpty key",rows).inspect();
        assertThat(view.status()).isEqualTo("ready");assertThat(view.lines()).extracting(CustomListsService.Line::status).containsExactly("ready","commented","missing","type","duplicate","duplicate","invalid");
        assertThat(view.lines().get(0).description()).isEqualTo("My projects");verifyNoInteractions(creation);
    }
    @Test void multipleMainNodesAreNeverSilentlySelectedAndCollationDoesNotMergeNames(){
        var rows=new HashMap<String,List<Object[]>>();rows.put("cherrytemplatetasks",List.of(new Object[]{1L,"cherrytemplatetasks","plain-text",0},new Object[]{2L,"cherrytemplatetasks","plain-text",0}));
        var service=service("",rows);assertThat(service.inspect().status()).isEqualTo("multiple");
        rows.put("cherrytemplatetasks",match(3,"CHERRYTEMPLATETASKS","plain-text"));assertThat(service.inspect().status()).isEqualTo("absent");
        rows.put("cherrytemplatetasks",match(3,"cherrytemplatetasks","markdown"));assertThat(service.inspect().status()).isEqualTo("type");
    }
    @Test void creationRechecksDefinitionsAndWritableState(){
        var rows=new HashMap<String,List<Object[]>>();rows.put("cherrytemplatetasks",match(1,"cherrytemplatetasks","plain-text"));
        var service=service("projects\tMy projects\n#software",rows);when(creation.createTopLevel()).thenReturn(9L);
        assertThat(service.createTemplate("projects")).isEqualTo(9);verify(creation).createTopLevel();
        rows.put("cherrytemplate-projects",match(9,"cherrytemplate-projects","plain-text"));
        assertThatThrownBy(()->service.createTemplate("projects")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatThrownBy(()->service.createTemplate("software")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        when(properties.writable()).thenReturn(false);assertThatThrownBy(service::createMain).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);verifyNoMoreInteractions(creation);
    }
}
