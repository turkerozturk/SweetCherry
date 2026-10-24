package com.turkerozturk.importing.exportednodes;

import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CtbImportDiagnosticsTest {
    @TempDir Path directory;
    @Test void allDanglingXmlAndImageLinksIncludeSourceTitlesAndIds()throws Exception {
        var fixture=new CtbImportTest();fixture.directory=directory;var path=fixture.source();
        try(var source=fixture.open(path)) {
            fixture.sql(source,"UPDATE node SET txt='<node><rich_text link=\"node 999\">A</rich_text><rich_text link=\"node 998 anchor\">B</rich_text></node>' WHERE node_id=2");
            fixture.sql(source,"UPDATE image SET link='node 888'");
        }
        assertThatThrownBy(()->CtbImportData.read(path)).isInstanceOfSatisfying(CtbImportProblem.class,error->{
            assertThat(error.getLinks()).hasSize(3);
            assertThat(error.getLinks()).extracting(CtbImportProblem.LinkIssue::targetId).containsExactly("999","998","888");
            assertThat(error.getLinks()).allMatch(issue->issue.sourceId()==2&&issue.sourceTitle().equals("Master"));
        });
    }
    @Test void preflightMeasuresEntireSourceBeforeAllocatingPayloadsAndControllerExposesIt()throws Exception {
        var fixture=new CtbImportTest();fixture.directory=directory;var path=fixture.source();byte[] before=Files.readAllBytes(path);
        CtbImportProblem problem;
        try{CtbImportData.read(path,Files.size(path)-1,1);throw new AssertionError();}catch(CtbImportProblem e){problem=e;}
        assertThat(problem.getLimits().fileBytes()).isEqualTo(Files.size(path));
        assertThat(problem.getLimits().dataBytes()).isPositive();assertThat(problem.getLimits().records()).isEqualTo(10);
        var service=mock(CtbImportService.class);when(service.preview("source.ctb")).thenThrow(problem);
        var model=new ExtendedModelMap();var session=new MockHttpSession();
        assertThat(new CtbImportController(service).preview("source.ctb",session,model)).isEqualTo("ctbImportPreview");
        assertThat(model.get("importProblem")).isSameAs(problem);assertThat(session.getAttribute(CtbImportController.SESSION)).isNull();
        assertThat(Files.readAllBytes(path)).isEqualTo(before);
    }
}
