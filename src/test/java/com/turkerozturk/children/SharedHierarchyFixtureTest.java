package com.turkerozturk.children;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turkerozturk.node.*;
import com.turkerozturk.exportmindmap.MindMapExportController;
import com.turkerozturk.quickmindmap.QuickMindMapController;
import com.turkerozturk.quickmindmapmarkmap.MarkmapQuickMindMapController;
import com.turkerozturk.pdf.*;
import com.turkerozturk.richtext.experimental.RichTextRenderingService;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SharedHierarchyFixtureTest {
    private final ChildrenRepository children = mock(ChildrenRepository.class);
    private final NodeRepository nodes = mock(NodeRepository.class);
    private final ChildrenService childService = new ChildrenService();
    private final NodeService nodeService = new NodeService();
    private final Map<Long, Children> occurrences = new LinkedHashMap<>();
    private final Map<Long, Node> content = new HashMap<>();

    @BeforeEach void loadSqlFixture() throws Exception {
        try (var db = DriverManager.getConnection("jdbc:sqlite::memory:");
             var script = getClass().getResourceAsStream("/fixtures/shared-node-tree.sql")) {
            assertThat(script).isNotNull();
            String sql = new String(script.readAllBytes(), StandardCharsets.UTF_8)
                    .replaceAll("(?m)^--.*$", "");
            try (var statement = db.createStatement()) {
                for (String part : sql.split(";")) if (!part.isBlank()) statement.execute(part);
                try (var rows = statement.executeQuery("SELECT * FROM children ORDER BY sequence,node_id")) {
                    while (rows.next()) {
                        var row = new Children().setNodeId(rows.getLong("node_id"))
                                .setFatherId(rows.getLong("father_id")).setSequence(rows.getLong("sequence"));
                        row.setMasterId(rows.getLong("master_id")); occurrences.put(row.getNodeId(), row);
                    }
                }
                try (var rows = statement.executeQuery("SELECT * FROM node")) {
                    while (rows.next()) {
                        var node = new Node(); node.setNodeId(rows.getLong("node_id"));
                        node.setName(rows.getString("name")); node.setTxt(rows.getString("txt"));
                        node.setSyntax(rows.getString("syntax")); content.put(node.getNodeId(), node);
                    }
                }
            }
        }
        when(children.findAll()).thenReturn(new ArrayList<>(occurrences.values()));
        when(children.findById(anyLong())).thenAnswer(c -> Optional.ofNullable(occurrences.get(c.<Long>getArgument(0))));
        when(children.findByNodeId(anyLong())).thenAnswer(c -> occurrences.get(c.<Long>getArgument(0)));
        when(children.findByFatherId(anyLong())).thenAnswer(c -> childrenOf(c.getArgument(0)));
        when(children.findByFatherIdOrderBySequenceAsc(anyLong())).thenAnswer(c -> childrenOf(c.getArgument(0)));
        when(nodes.findById(anyLong())).thenAnswer(c -> content.get(c.<Long>getArgument(0)));
        when(nodes.findByNodeIdIn(anyList())).thenAnswer(c -> c.<List<Long>>getArgument(0).stream().map(content::get).toList());
        for (Object service : List.of(childService, nodeService)) {
            ReflectionTestUtils.setField(service, "childrenRepository", children);
            ReflectionTestUtils.setField(service, "nodeRepository", nodes);
        }
    }
    private List<Children> childrenOf(long parent) {
        return occurrences.values().stream().filter(row -> row.getFatherId() == parent)
                .sorted(Comparator.comparingLong(Children::getSequence)).toList();
    }
    @Test void navigationAndBreadcrumbsUseSharedParentsAndOwnChildren() {
        assertThat(childService.getNaviNodesByFatherId(11)).extracting(NaviNode::nodeId).containsExactly(12L,13L,18L);
        assertThat(childService.getNaviNodesByFatherId(2)).extracting(NaviNode::nodeId).containsExactly(3L);
        assertThat(childService.getNaviNodesByFatherId(0).stream().filter(row -> row.nodeId()==11).findFirst().orElseThrow().hasChildren()).isTrue();
        assertThat(nodeService.addBreadcrumbs(19L)).containsExactly(
                entry(11L,"Node 2"), entry(18L,"Node 1"), entry(19L,"Node 19"));
        assertThat(nodeService.findDisplayNode(17).getNodeId()).isEqualTo(8);
    }
    @Test void mermaidIncludesOnlyTheSelectedOccurrencesDescendants() {
        var model = new ExtendedModelMap();
        new QuickMindMapController(children,nodes).showQuickMindMap(11,10,false,false,false,model);
        assertThat((String)model.get("mermaidDefinition")).contains("ctb_11","ctb_12","ctb_13","ctb_14","ctb_15","ctb_18","ctb_19")
                .doesNotContain("ctb_3(","ctb_3[", "ctb_16");
        verify(children,never()).findByFatherIdOrderBySequenceAsc(2L);
    }
    @Test void markmapIncludesNestedSharedChildren() throws Exception {
        var mapper = new ObjectMapper(); var model = new ExtendedModelMap();
        new MarkmapQuickMindMapController(children,nodes,mapper).showMarkmapQuickMindMap(11,10,false,false,false,model);
        var root = mapper.readTree((String)model.get("markmapDataJson"));
        assertThat(root.get("children").size()).isEqualTo(3);
        assertThat(root.get("children").get(2).get("payload").get("nodeId").asLong()).isEqualTo(18);
        assertThat(root.get("children").get(2).get("children").get(0).get("payload").get("nodeId").asLong()).isEqualTo(19);
        verify(children,never()).findByFatherIdOrderBySequenceAsc(2L);
    }
    @Test void freeplaneKeepsOccurrenceIdsAndNestedSharedChildren() throws Exception {
        var response = new MindMapExportController(children,nodes).exportMindMap(11,null,"all-unfolded",false,true);
        var doc = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new java.io.ByteArrayInputStream(response.getBody()));
        assertThat(doc.getElementsByTagName("node").getLength()).isEqualTo(7);
        String xml = new String(response.getBody(),StandardCharsets.UTF_8);
        assertThat(xml).contains("ID_11","ID_18","ID_19").doesNotContain("ID_3\"");
        verify(children,never()).findByFatherIdOrderBySequenceAsc(2L);
    }
    @Test void subtreePdfUsesOccurrenceOrderAndMasterContent() {
        var renderer = mock(NodePdfRenderer.class);
        when(renderer.render(any())).thenAnswer(call -> {
            org.w3c.dom.Document doc = call.getArgument(0);
            var titles = doc.getElementsByTagName("h1");
            List<String> ids = new ArrayList<>();
            for (int i=0;i<titles.getLength();i++) ids.add(((org.w3c.dom.Element)titles.item(i)).getAttribute("id"));
            assertThat(ids).containsExactly("sc-node-11","sc-node-12","sc-node-13","sc-node-14","sc-node-15","sc-node-18","sc-node-19");
            assertThat(doc.getDocumentElement().getTextContent()).contains("Content of master 2.","Content of master 1.");
            return new byte[]{1};
        });
        var service = new NodePdfExportService(childService,nodeService,mock(RichTextRenderingService.class),renderer);
        assertThat(service.exportSubtree(11,PdfExportOptions.defaults(),null,false,"Contents").bytes()).containsExactly((byte)1);
    }
}
