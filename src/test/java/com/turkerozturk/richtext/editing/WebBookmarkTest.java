package com.turkerozturk.richtext.editing;

import com.turkerozturk.node.*;
import java.util.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebBookmarkTest {
    Node node(){var n=new Node();n.setName("https://example.org/page#part");n.setSyntax("plain-text");n.setTxt("");n.setReadOnly(true);return n;}
    @Test void onlyExplicitReadOnlyEmptyUrlNodesQualifyIncludingEmptyRichMarkup(){
        var n=node();assertThat(WebBookmarkService.destination(n)).isEqualTo(n.getName());
        n.setReadOnly(false);assertThat(WebBookmarkService.destination(n)).isNull();n.setReadOnly(true);
        n.setTxt("Keep this content");assertThat(WebBookmarkService.destination(n)).isNull();n.setTxt("");
        for(String value:List.of("Normal name","javascript:alert(1)","https://user:pass@example.org/","https://example.org:8080/","https://example.org/a b")){n.setName(value);assertThat(WebBookmarkService.destination(n)).isNull();}
        n=node();n.setSyntax("custom-colors");n.setTxt("<node><rich_text>\n</rich_text></node>");assertThat(WebBookmarkService.destination(n)).isNotNull();
        n.setHasImage(true);assertThat(WebBookmarkService.destination(n)).isNull();n.setHasImage(false);n.setHasTable(true);assertThat(WebBookmarkService.destination(n)).isNull();n.setHasTable(false);n.setHasCodeBox(true);assertThat(WebBookmarkService.destination(n)).isNull();
    }
    @Test void previewIsFetchedAgainForEveryLoadAndSharedOccurrencesResolveThroughNormalNodeService(){
        var nodes=mock(NodeService.class);var importer=mock(WebContentImportService.class);var n=node();when(nodes.findDisplayNode(55)).thenReturn(n);
        when(importer.load(n.getName())).thenReturn(new WebContentImportService.Result("Title",n.getName(),"<p>First</p>",Map.of(),0),new WebContentImportService.Result("Title",n.getName(),"<p>Updated</p>",Map.of(),0));
        var service=new WebBookmarkService(nodes,importer);assertThat(service.load(55).html()).contains("First");assertThat(service.load(55).html()).contains("Updated");verify(importer,times(2)).load(n.getName());
        n.setTxt("Now nonempty");assertThatThrownBy(()->service.load(55)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);verifyNoMoreInteractions(importer);
    }
    @Test void onlyInertFormattingAndValidatedEmbeddedImageReferencesReachTheSandbox(){
        String raw="<base href='https://evil.test'><meta http-equiv='refresh' content='0;url=https://evil.test'><style>@import 'https://evil.test';</style><script>alert(1)</script>"
                +"<div id='workspace-content' style='background:url(https://evil.test)' onclick='alert(1)'><a href='https://evil.test' target='_top'>Link text</a><b>Bold</b><table><tr><td>Cell</td></tr></table>"
                +"<img src='https://evil.test/pixel' data-import-image='image0' onerror='alert(1)'><img src='https://evil.test/pixel' alt='Removed image'><iframe src='https://evil.test'></iframe><svg onload='alert(1)'></svg><form action='https://evil.test'><input></form></div>";
        String html=WebBookmarkService.document(new WebContentImportService.Result("Remote title","https://example.org",raw,Map.of("image0","data:image/png;base64,aGVsbG8="),0));
        var doc=Jsoup.parse(html);assertThat(doc.body().select("a,script,style,iframe,svg,form,input,[onclick],[onerror],[style],[id],[data-import-image]")).isEmpty();
        assertThat(doc.body().text()).contains("Link text","Bold","Cell","Removed image");assertThat(doc.body().select("img").eachAttr("src")).containsExactly("data:image/png;base64,aGVsbG8=");
        assertThat(html).doesNotContain("evil.test");assertThat(doc.selectFirst("meta[http-equiv=Content-Security-Policy]").attr("content")).contains("default-src 'none'","img-src data:","base-uri 'none'","form-action 'none'");
    }
    @Test void onlyPreparedNodeControllerViewsReceiveBookmarkMetadata()throws Exception {
        var handler=new org.springframework.web.method.HandlerMethod(new NodeController(),NodeController.class.getMethod("treeWorkspace",org.springframework.ui.Model.class,long.class));
        var view=new org.springframework.web.servlet.ModelAndView("node/treeContentFragment");view.addObject("node",node());
        new WebBookmarkViewConfiguration(true).interceptor().postHandle(null,null,handler,view);assertThat(view.getModel().get("webBookmarkUrl")).isEqualTo("https://example.org/page#part");
        new WebBookmarkViewConfiguration(false).interceptor().postHandle(null,null,handler,view);assertThat(view.getModel().get("webBookmarkUrl")).isNull();
    }
}
