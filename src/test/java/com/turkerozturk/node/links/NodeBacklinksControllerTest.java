package com.turkerozturk.node.links;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class NodeBacklinksControllerTest {
 @Test void rejectsInvalidIdsAndPagesBeforeScanning(){var service=mock(InternalLinkReportService.class);var controller=new NodeBacklinksController(service);for(String id:new String[]{"-1","0","m","9223372036854775808"})assertThat(controller.show(id,"current",false,"1").getStatusCode().value()).isEqualTo(400);assertThat(controller.show("1","current",false,"0").getStatusCode().value()).isEqualTo(400);verifyNoInteractions(service);}
 @Test void missingOccurrenceReturnsNoStore404(){var service=mock(InternalLinkReportService.class);var response=new NodeBacklinksController(service).show("11","current",true,"2");assertThat(response.getStatusCode().value()).isEqualTo(404);assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");verify(service).inspect(new InternalLinkReportService.Options("","all",20,2,11L,true));}
}
