package com.turkerozturk.quickmindmap;

import com.turkerozturk.children.Children;
import com.turkerozturk.children.ChildrenRepository;
import com.turkerozturk.node.Node;
import com.turkerozturk.node.NodeContentParserService;
import com.turkerozturk.node.NodeRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class QuickNodeContentController {

    private final ChildrenRepository childrenRepository;
    private final NodeRepository nodeRepository;
    private final NodeContentParserService nodeContentParserService;

    public QuickNodeContentController(
            ChildrenRepository childrenRepository,
            NodeRepository nodeRepository,
            NodeContentParserService nodeContentParserService
    ) {
        this.childrenRepository = childrenRepository;
        this.nodeRepository = nodeRepository;
        this.nodeContentParserService = nodeContentParserService;
    }

    /**
     * Mind map ve benzeri özet görünümlerden bir düğümün yalnız içeriğini açar.
     * Alias düğümlerde ağaçtaki nodeId korunur, içerik masterId'den alınır.
     */
    @GetMapping("/quick-node-content/{nodeId}")
    public String showQuickNodeContent(
            @PathVariable long nodeId,
            Model model,
            HttpServletRequest request
    ) {
        Children treeNode = childrenRepository.findByNodeId(nodeId);

        if (treeNode == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Children tablosunda nodeId bulunamadı: " + nodeId
            );
        }

        Long masterId = treeNode.getMasterId();
        boolean isAlias = masterId != null && masterId != 0;
        long contentNodeId = isAlias ? masterId : nodeId;

        Node node = nodeRepository.findById(contentNodeId);

        if (node == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Node tablosunda kayıt bulunamadı: " + contentNodeId
            );
        }

        node.setMasterNode(!isAlias);
        node = nodeContentParserService.parseNodeContent(node, request);

        String rawText = node.getTxt();
        String htmlText = node.getTxtAsHtml();

        boolean hasEmptyContent =
                (htmlText != null
                        && NodeContentParserService.isHtmlContentEmpty(htmlText))
                        || rawText == null
                        || rawText.isBlank();

        model.addAttribute("node", node);
        model.addAttribute("requestedNodeId", nodeId);
        model.addAttribute("contentNodeId", contentNodeId);
        model.addAttribute("isAlias", isAlias);
        model.addAttribute("hasEmptyContent", hasEmptyContent);

        return "node/quick-node-content";
    }
}
