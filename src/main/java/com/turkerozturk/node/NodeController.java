/*
 * This file is part of the SweetCherry project.
 * Please refer to the project's README.md file for additional details.
 * https://github.com/turkerozturk/SweetCherry
 *
 * Copyright (c) 2024 Turker Ozturk
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.en.html>.
 */
package com.turkerozturk.node;

import com.turkerozturk.children.Children;
import com.turkerozturk.children.ChildrenService;
import com.turkerozturk.children.NaviNode;
import com.turkerozturk.helpers.highlighter.pygments.CodeHighLighter;
import com.turkerozturk.helpers.highlighter.pygments.LexerEnum;
import com.turkerozturk.node.filter.FormSearch;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.python.core.PyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@Controller
public class NodeController {

    private static final Logger logger = LoggerFactory.getLogger(NodeController.class);

    @Autowired
    private MessageSource messageSource;
    @Autowired
    private NodeService nodeService;

    @Autowired
    private ChildrenService childrenService;

    @Autowired
    private NodeContentParserService nodeContentParserService;

    @Autowired
    Filtered filtered;

    @Value("${myapp.debug}")
    private Boolean myappIsDebugEnabled;

    @GetMapping("/nodes")
    public String getAllNodesAsHtml(Model model, @RequestParam(required = false) String filter) {
        List<Node> nodes;
        if (filter != null && !filter.isEmpty()) {
            nodes = nodeService.getNodesByTagsContaining(filter);
        } else {
            nodes = nodeService.getNodes();
        }
        model.addAttribute("nodes", nodes);
        model.addAttribute("filter", filter); // Filtre degerini modelde sakla
        return "nodes"; // Bu, bir Thymeleaf sablonu olmalidir: src/main/resources/templates/nodes.html
    }

    public static final String NODES_ADVANCED = "nodesadvanced";
    private final int[] PAGE_SIZE_OPTIONS = {1,5,10,30,50,100,500,1000,10000,100000};

    public static final int DEFAULT_PAGE_SIZE = 30;

    /**
     * @deprecated
     * This method is long coding version. Use  {@link #postForm(FormSearch, BindingResult, Model, String)} instead.
     * Sayfa ilk cagirildiginda get metodu karsilar ve once bu metoda gelinir.
     * Bu metodun asagida post metod olani var. Ama artik o da deprecated.
     * @param model
     */
    @Deprecated(since = "1", forRemoval = false)
    @GetMapping("/nodesadvanced")
    public void getAllNodesAdvancedAsHtml(Model model) {

        getAllNodesAdvancedAsHtml(model,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    false,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null);

    }

    /*
    //@ModelAttribute("allNodeIcons")
    public LinkedHashSet<NodeIcon> populateNodeIcons() {
        return new LinkedHashSet<NodeIcon>(Arrays.asList(NodeIcon.values()));
    }
    */

    // bilgi: asagida commented olan getmappingli metod, veri getirmeden sadece formu gostermek icin idi.
    // bilgi: artik hem get hem post icin tek metod kullaniyoruz. GET ile gelindiginde FormSearch sinifinda tanimli
    // bilgi: default degerleri sql native querye gonderdigi icin ilk sayfa verisi geliyor. Onceki get metodunda yoktu.
    /*
    @GetMapping("nodesadvancedwithbinding")
    public String getForm() {

        return "/nodesadvancedwithbinding";
    }
    */

    /**
     * This is the main advnced find form page.
     * @param formSearch
     * @param bindingResult
     * @param model
     * @param viewMode
     * @return
     */
    @RequestMapping(value = "/nodesadvancedwithbinding", method = {RequestMethod.GET, RequestMethod.POST})
    public String postForm(@Valid FormSearch formSearch, BindingResult bindingResult, Model model,
                           @CookieValue(value = "viewMode", defaultValue = "mobile") String viewMode) {

        // bilgi: GET ve POST metodlari arasinda fark olursa, ayirdetmek icin asagidaki iki satir kullanilabilir.
        // , HttpServletRequest request
        // String method = request.getMethod();

        Page<Node> nodes = nodeService.getFilteredFormNodes(formSearch, bindingResult);



        int[] pages = new int[nodes.getTotalPages()];
        for (int i = 0; i < pages.length; i++) {
            pages[i] = i;
        }

        //--------------------------------------------------------------------------------------
        model.addAttribute("pages", pages);
        model.addAttribute("pageNumber", formSearch.getPageNumber());
        model.addAttribute("nodes", nodes);
        // -------------------------------------------------------------------------------------

        model.addAttribute("viewMode", viewMode);
        if ("mobile".equals(viewMode)) {
            return "nodesadvancedwithbindingMobile";
        } else {
            return "nodesadvancedwithbinding"; // bilgi bu formsearch sablonunu iceriyor.
            // bilgi bu calisiyor ama sadece mavi form kismi icin: return "formsearch";
        }

    }

    /**
     * @deprecated
     * This method is long coding version. Use  {@link #postForm(FormSearch, BindingResult, Model, String)} instead.
     * @param model
     * @param pageNumber
     * @param pageSize
     * @param oldPageSizeAsString
     * @param nodeIcons
     * @param tagsFilter
     * @param nameStartsWith
     * @param nameFilter
     * @param textFilter
     * @param tsCreationStartFilter
     * @param tsCreationEndFilter
     * @param tsModificationStartFilter
     * @param tsModificationEndFilter
     * @param nodeIdStartFilter
     * @param nodeIdEndFilter
     * @param syntaxFilter
     * @return
     */
    @Deprecated(since = "1", forRemoval = false)
    @PostMapping("/nodesadvanced")
    public String getAllNodesAdvancedAsHtml(Model model,
                                            @RequestParam(required = false) Integer pageNumber,
                                            @RequestParam(required = false) Integer pageSize,
                                            @RequestParam(required = false) String oldPageSizeAsString,
                                            @RequestParam(required = false) Long nodeIcons,
                                            @RequestParam(required = false) String tagsFilter,
                                            @RequestParam(required = false) boolean nameStartsWith,
                                            @RequestParam(required = false) String nameFilter,
                                            @RequestParam(required = false) String textFilter,
                                            @RequestParam(required = false) String tsCreationStartFilter,
                                            @RequestParam(required = false) String tsCreationEndFilter,
                                            @RequestParam(required = false) String tsModificationStartFilter,
                                            @RequestParam(required = false) String tsModificationEndFilter,
                                            @RequestParam(required = false) String nodeIdStartFilter,
                                            @RequestParam(required = false) String nodeIdEndFilter,
                                            @RequestParam(required = false) String syntaxFilter
                                            ) {



        filtered.process( pageNumber,
                pageSize,
                oldPageSizeAsString,
                nodeIcons,
                tagsFilter,
                nameStartsWith,
                nameFilter,
                textFilter,
                tsCreationStartFilter,
                tsCreationEndFilter,
                tsModificationStartFilter,
                tsModificationEndFilter,
                nodeIdStartFilter,
                nodeIdEndFilter,
                syntaxFilter);

        logger.info(filtered.toString());





        model.addAttribute("selectedIcon", filtered.getNodeIcons());
        model.addAttribute("nodeIdStartFilter", filtered.getNodeIdStartFilter());
        model.addAttribute("nodeIdEndFilter", filtered.getNodeIdEndFilter());
        model.addAttribute("tsCreationStartFilter", filtered.getTsCreationStartFilter());
        model.addAttribute("tsCreationEndFilter", filtered.getTsCreationEndFilter());
        model.addAttribute("tsModificationStartFilter", filtered.getTsModificationStartFilter());
        model.addAttribute("tsModificationEndFilter", filtered.getTsModificationEndFilter());

        model.addAttribute("nameFilter", filtered.getNameFilter());
        model.addAttribute("textFilter", filtered.getTextFilter());
        model.addAttribute("tagsFilter", filtered.getTagsFilter());
        model.addAttribute("syntaxFilter", filtered.getSyntaxFilter());


        model.addAttribute("nodes", filtered.getNodes());
        model.addAttribute("oldPageSize", filtered.getOldPageSize());
        model.addAttribute("pages", filtered.getPages());
        model.addAttribute("nodeIcons", filtered.getSortedIcons());


        model.addAttribute("pageSizeOptions", PAGE_SIZE_OPTIONS);
        model.addAttribute("pageNumber", filtered.getPageNumber());
        model.addAttribute("pageSize", filtered.getPageSize());

        model.addAttribute("pageTitle", "Düğümler");

        return NODES_ADVANCED;
    }


// TODO model.addAttribute("icons", nodeIconService.getAllIcons());

    @GetMapping({"/", "/rootnode", "/nodes/0"})
    public String getRootNodesAsHtml(Model model, HttpServletRequest request, Locale locale,
                                     @RequestParam(value = "lang", required = false) String lang
            , @CookieValue(value = "viewMode", defaultValue = "mobile") String viewMode) {

        // bilgi dil degisikligi durumunda bu metoda yonleniyor. Geldigi sayfayi ogrenip geri yonlendiriyoruz oraya.
        // BASLA dil degisikligi yonlendirmesi
        String referrer = request.getHeader("referer");
        if (lang != null) {
          //  return "Language parameter is present: " + lang;
          //  logger.info("REFERER: " + referrer);
            return "redirect:" + referrer;
        } else {
          //  return "Language parameter is not present";
        }
        // BITTI dil degisikligi yonlendirmesi

        model.addAttribute("isRootNode", true);

        /* bilgi: this code is moved to UploadController.java
        String exampleDataSource="name=My Database\n" +
                "security-role=USER\n" +
                "datasource.url=jdbc:sqlite:C:/Users/username/Documents/CTB/myDatabase.ctb\n" +
                "datasource.driver-class-name=org.sqlite.JDBC\n" +
                "# datasource.username=admin\n" +
                "# datasource.password=admin\n" +
                "datasource.init-mode=always\n" +
                "# https://www.baeldung.com/multitenancy-with-spring-data-jpa";

        model.addAttribute("exampleDataSource", exampleDataSource);

        model.addAttribute("codeHighLighter", new CodeHighLighter());

        */
        
        String dataSourceMessage = null;

        try {
            final long rootNodeId = 0;
            List<NaviNode> childNodes = childrenService.getNaviNodesByFatherId(rootNodeId);
            model.addAttribute("childNodes", childNodes);

            // yeni satira gecis islemindeki bir problemle alakali, daha onceki commit ve notlarda aciklamasi var
            model.addAttribute ( "newlinechar" , '\n' ) ;


        } catch(InvalidDataAccessApiUsageException e) {

            dataSourceMessage = messageSource.getMessage("selectdatasource.please_select_a_datasource",
                    null, locale);
            //dataSourceMessage = "No Datasource: Please Select A Data Source.";
            logger.warn(dataSourceMessage + " InvalidDataAccessApiUsageException: " + e.getMessage());

            

            model.addAttribute("dataSourceMessage", dataSourceMessage);


            
            return "selectdatasource";

        } catch (JpaSystemException e) {

            dataSourceMessage = messageSource.getMessage("selectdatasource.invalid_or_different_CTB_database",
                    null, locale);

            
            logger.error(dataSourceMessage + " JpaSystemException: " + e.getMessage());

            model.addAttribute("dataSourceMessage", dataSourceMessage);



            

            return "selectdatasource";
        }

        List<NaviNode> siblingNaviNodes = childrenService.getNaviNodesByFatherId(0);
        for (NaviNode siblingNode : siblingNaviNodes) {
            logger.info(
                    siblingNode.sequence() + ", sibling: " +
                            siblingNode.nodeId() + ", " +
                            siblingNode.name() + ", prev: " +
                            siblingNode.prevSiblingNodeId() + ", next: " +
                            siblingNode.nextSiblingNodeId()
            );
        }
        model.addAttribute("siblingNaviNodes", siblingNaviNodes);



        model.addAttribute("viewMode", viewMode);

        if ("mobile".equals(viewMode)) {
            return "node/nodeMobile";
        } else {
            return "node/node";
        }

    }


    


    @GetMapping("/nodes/{nodeId}")
    public String getNodeAsHtml(@PathVariable long nodeId, Model model, HttpServletRequest request,
                                @CookieValue(value = "viewMode", defaultValue = "mobile") String viewMode) {


        Children nodeInChildrenTable = childrenService.findById(nodeId);


        boolean isRealNode = nodeInChildrenTable.getMasterId() == null
                || nodeInChildrenTable.getMasterId() == 0; // bilgi eski dblerde null var, yenilerde 0 var.
        if(isRealNode) {



            Node node = nodeService.findById(nodeId);
            node.setMasterNode(true);
            nodeService.prepareFatherNode(node);
           // System.out.println("FATHERR: " + node.getFatherNode().getName());
        /*
        System.out.println("MASTER: " + node.getChildren().getMasterId());
        Long masterId = node.getChildren().getMasterId();
        boolean isSharedNode = false;
        if(masterId == null || masterId == 0) {

        } else {
            isSharedNode = true;
        }
        */

            // model.addAttribute("isSharedNode", isSharedNode);

            // breadcrumbs set etme hesaplamisini ayri metod yapma sebebim, gerektiginde kullanip, sql ile yormamak.
            node.setBreadcrumbs(nodeService.addBreadcrumbs(nodeId));


            node = nodeContentParserService.parseNodeContent(node, request);


            List<Long> sharedNodeIds = childrenService.getSharedNodeIdsByMasterId(nodeId);
            model.addAttribute("sharedNodeIds", sharedNodeIds);


            long parentNodeId = node.getChildren().getFatherId(); // childrenService.findById(nodeId).getFatherId();
            model.addAttribute("parentNodeId", parentNodeId);

            // List<Long> sharedNodeIds = node.getChildren().

            //logger.info("*** parentNodeId: " + parentNodeId + ", node.name: " + node.getName());

            model.addAttribute("node", node);



            // yeni satira gecis islemindeki bir problemle alakali, daha onceki commit ve notlarda aciklamasi var
            model.addAttribute ( "newlinechar" , '\n' ) ;

            // for debugging content, colorize node.txt to xml if it is not plain-text
            if(!node.getSyntax().equals("plain-text")) {
                String debugTxt = CodeHighLighter.highlightLanguage(LexerEnum.XML_LEXER.getLanguageString(), node.getTxt());
                model.addAttribute ( "debugTxt" , debugTxt ) ;
            }

            model.addAttribute("myappIsDebugEnabled", myappIsDebugEnabled);




            List<NaviNode> childNodes = childrenService.getNaviNodesByFatherId(nodeId);
            if(!childNodes.isEmpty()) {
                model.addAttribute("childNodes", childNodes);
            }


            if(node.getTxtAsHtml() != null && NodeContentParserService.isHtmlContentEmpty(node.getTxtAsHtml())) {
                model.addAttribute("hasEmptyContent", true);
            } else if (node.getTxt().trim().length() == 0) {
                model.addAttribute("hasEmptyContent", true);

            }



            List<NaviNode> siblingNaviNodes = childrenService.getNaviNodesByFatherId(parentNodeId);
            model.addAttribute("siblingNaviNodes", siblingNaviNodes);

            NaviNode prevSiblingNaviNode = null;
            NaviNode nextSiblingNaviNode = null;

            for (int i = 0; i < siblingNaviNodes.size(); i++) {
                NaviNode siblingNode = siblingNaviNodes.get(i);

                if (siblingNode.nodeId() == node.getNodeId()) {
                    // Önceki sibling node'u al (eğer varsa)
                    if (i > 0) {
                        prevSiblingNaviNode = siblingNaviNodes.get(i - 1);
                    }

                    // Sonraki sibling node'u al (eğer varsa)
                    if (i < siblingNaviNodes.size() - 1) {
                        nextSiblingNaviNode = siblingNaviNodes.get(i + 1);
                    }

                    // Bulduktan sonra döngüden çık
                    break;
                }
            }

            model.addAttribute("prevSiblingNaviNode", prevSiblingNaviNode);
            model.addAttribute("nextSiblingNaviNode", nextSiblingNaviNode);

            /*
            for (NaviNode siblingNode : siblingNaviNodes) {
                logger.info(
                        siblingNode.sequence() + ", sibling: " +
                                siblingNode.nodeId() + ", " +
                                siblingNode.name() + ", prev: " +
                                siblingNode.prevSiblingNodeId() + ", next: " +
                                siblingNode.nextSiblingNodeId()
                );
            }
            */

        } else { // bilgi Gercek dugum degil, shared node.
            Node node = nodeService.findById(nodeInChildrenTable.getMasterId());
            node.setMasterNode(false);

            node.setBreadcrumbs(nodeService.addBreadcrumbs(nodeInChildrenTable.getMasterId())); // shared node nin yolu.

            node.setTxtAsHtml("<a href=\"/nodes/" + nodeInChildrenTable.getMasterId() + "\">Gerçek node için tıklayınız: " + nodeInChildrenTable.getMasterId() + "</a>");

            model.addAttribute("node", node);



            long parentNodeId = nodeInChildrenTable.getFatherId();
            model.addAttribute("parentNodeId", parentNodeId);





        }





        model.addAttribute("viewMode", viewMode);

        if ("mobile".equals(viewMode)) {
            return "node/nodeMobile";
        } else {
            return "node/node";
        }




    }


    /**
     *
     * @param request
     * @param pyException
     * @param node
     * @return
     */
    public static String getErrorContent(HttpServletRequest request, PyException pyException, Node node) {
        StringBuilder errorContent = new StringBuilder();
        errorContent.append("ERROR HIGHLIGHTING: ");
        errorContent.append(node.getSyntax());
        errorContent.append(" data structure.\n");
        errorContent.append("\n"+ request.getRequestURL().toString());
        errorContent.append("\nERROR MESSAGE\n");
        errorContent.append(pyException.getMessage());
        errorContent.append("\nCONTENT:\n");
        errorContent.append(node.getTxt());
        return errorContent.toString();
    }

    @GetMapping("/findNodeIdsUntilFatherId")
    public ResponseEntity<List<Long>> findNodeIdsUntilFatherId(@RequestParam long startNodeId) {
        List<Long> nodeIds = nodeService.findAllNodeIdsUntilFatherId(startNodeId);
        return ResponseEntity.ok(nodeIds);
    }

    /**
     * @deprecated
     * This method is no longer necessary.
     * @param model
     * @return
     */
    @Deprecated(since = "1", forRemoval = true)
    @GetMapping("/nodes/timeline/created")
    public String getTimelineCreated(Model model) {
        List<Node> recentlyCreatedNodes = nodeService.findRecentlyCreatedNodes();
        model.addAttribute("nodes", recentlyCreatedNodes);
        model.addAttribute("pageTitle", "Son Oluşturulanlar");
        return NODES_ADVANCED;
    }

    /**
     * @deprecated
     * This method is no longer necessary.
     * @param model
     * @return
     */
    @Deprecated(since = "1", forRemoval = true)
    @GetMapping("/nodes/timeline/modified")
    public String getTimelineModified(Model model) {
        List<Node> recentlyModifiedNodes = nodeService.findRecentlyModifiedNodes();
        model.addAttribute("nodes", recentlyModifiedNodes);
        model.addAttribute("pageTitle", "Son Güncellenenler");
        return NODES_ADVANCED;
    }

    // bilgi yeni 20 nisan







}
