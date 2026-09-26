package com.turkerozturk.quickmindmap;

import com.turkerozturk.children.Children;
import com.turkerozturk.children.ChildrenRepository;
import com.turkerozturk.helpers.NodeIcon;
import com.turkerozturk.node.Node;
import com.turkerozturk.node.NodeRepository;
import com.turkerozturk.multipledatabases.RequiresTenant;
import com.turkerozturk.multipledatabases.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Controller
@RequiresTenant
public class QuickMindMapController {

    private static final int DEFAULT_LEVEL = 3;
    private static final int INDENT_SIZE = 2;

    private final ChildrenRepository childrenRepository;
    private final NodeRepository nodeRepository;

    public QuickMindMapController(
            ChildrenRepository childrenRepository,
            NodeRepository nodeRepository
    ) {
        this.childrenRepository = childrenRepository;
        this.nodeRepository = nodeRepository;
    }

    @GetMapping("/quick-mind-map/all")
    public String showAllRoots(
            @RequestParam(defaultValue = "3") int level,
            @RequestParam(defaultValue = "true") boolean icons,
            @RequestParam(defaultValue = "true") boolean colors,
            @RequestParam(defaultValue = "true") boolean foldChat,
            Model model
    ) {
        if (level < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "level değeri negatif olamaz.");
        }

        String tenantName = TenantContext.getCurrentTenant();
        Map<String, String> colorClasses = new LinkedHashMap<>();
        List<Map<String, String>> iconNodes = new ArrayList<>();
        List<Map<String, String>> navigationNodes = new ArrayList<>();
        StringBuilder definition = new StringBuilder("mindmap\n  ctb_0((\"")
                .append(escapeMermaidLabel(tenantName))
                .append("\"))\n  :::ct-node-0\n");
        addNavigationNode(navigationNodes, "ct-node-0", 0, 0);

        if (level > 0) {
            for (Children root : childrenRepository.findByFatherIdOrderBySequenceAsc(0)) {
                writeNodeRecursively(definition, root, 1, level, icons, colors, foldChat,
                        new HashSet<>(), colorClasses, iconNodes, navigationNodes, 0);
            }
        }

        model.addAttribute("rootNodeId", 0L);
        model.addAttribute("rootNodeName", tenantName);
        model.addAttribute("rootParentNodeId", 0L);
        model.addAttribute("maximumLevel", level);
        model.addAttribute("icons", icons);
        model.addAttribute("colors", colors);
        model.addAttribute("foldChat", foldChat);
        model.addAttribute("mermaidDefinition", definition.toString());
        model.addAttribute("mermaidColorCss", createColorCss(colorClasses));
        model.addAttribute("iconNodes", iconNodes);
        model.addAttribute("navigationNodes", navigationNodes);
        return "quick-mind-map";
    }

    /**
     * Örnek:
     * /quick-mind-map/2573?level=3&icons=false&colors=true&foldChat=true
     *
     * level=0 yalnızca kökü, level=1 kökü ve doğrudan çocuklarını gösterir.
     */
    @GetMapping("/quick-mind-map/{nodeId}")
    public String showQuickMindMap(
            @PathVariable long nodeId,
            @RequestParam(defaultValue = "3") int level,
            @RequestParam(defaultValue = "true") boolean icons,
            @RequestParam(defaultValue = "true") boolean colors,
            @RequestParam(defaultValue = "true") boolean foldChat,
            Model model
    ) {
        if (level < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "level değeri negatif olamaz."
            );
        }

        Children root = childrenRepository.findByNodeId(nodeId);
        if (root == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Children tablosunda nodeId bulunamadı: " + nodeId
            );
        }

        Node rootDisplayNode = findDisplayNode(root);
        long rootParentNodeId = findNavigableParentNodeId(root);
        Map<String, String> colorClasses = new LinkedHashMap<>();
        List<Map<String, String>> iconNodes = new ArrayList<>();
        List<Map<String, String>> navigationNodes = new ArrayList<>();
        StringBuilder definition = new StringBuilder("mindmap\n");

        writeNodeRecursively(
                definition,
                root,
                0,
                level,
                icons,
                colors,
                foldChat,
                new HashSet<>(),
                colorClasses,
                iconNodes,
                navigationNodes,
                rootParentNodeId
        );

        model.addAttribute("rootNodeId", nodeId);
        model.addAttribute("rootNodeName", nullToEmpty(rootDisplayNode.getName()));
        model.addAttribute("rootParentNodeId", rootParentNodeId);
        model.addAttribute("maximumLevel", level);
        model.addAttribute("icons", icons);
        model.addAttribute("colors", colors);
        model.addAttribute("foldChat", foldChat);
        model.addAttribute("mermaidDefinition", definition.toString());
        model.addAttribute("mermaidColorCss", createColorCss(colorClasses));
        model.addAttribute("iconNodes", iconNodes);
        model.addAttribute("navigationNodes", navigationNodes);

        return "quick-mind-map";
    }

    private void writeNodeRecursively(
            StringBuilder definition,
            Children treeNode,
            int currentLevel,
            int maximumLevel,
            boolean icons,
            boolean colors,
            boolean foldChat,
            Set<Long> currentPath,
            Map<String, String> colorClasses,
            List<Map<String, String>> iconNodes,
            List<Map<String, String>> navigationNodes,
            long rootParentNodeId
    ) {
        long treeNodeId = treeNode.getNodeId();

        // Yalnız mevcut recursion yolunu denetler; bozuk bir ilişki sonsuz döngüye girmez.
        if (!currentPath.add(treeNodeId)) {
            return;
        }

        try {
            Node displayNode = findDisplayNode(treeNode);
            String nodeClass = "ct-node-" + treeNodeId;

            addNavigationNode(
                    navigationNodes,
                    nodeClass,
                    currentLevel == 0 ? rootParentNodeId : treeNodeId,
                    treeNodeId
            );

            definition.append(" ".repeat((currentLevel + 1) * INDENT_SIZE));
            definition.append("ctb_").append(treeNodeId);

            if (currentLevel == 0) {
                definition.append("((\"")
                        .append(escapeMermaidLabel(displayNode.getName()))
                        .append("\"))");
            } else {
                definition.append("[\"")
                        .append(escapeMermaidLabel(displayNode.getName()))
                        .append("\"]");
            }

            definition.append('\n');

            // Mermaid'in mindmap sözdiziminde class ve icon direktifleri,
            // ait oldukları düğümün hemen ardından aynı girinti seviyesinde yazılır.
            definition.append(" ".repeat((currentLevel + 1) * INDENT_SIZE));
            definition.append(":::").append(nodeClass);

            if (displayNode.isBoldnessBit()) {
                definition.append(" ct-bold");
            }

            definition.append('\n');

            if (colors && (displayNode.getTitleColorAsHtmlHex().length() == 7)) {
                String color = normalizeHtmlColor(
                        displayNode.getTitleColorAsHtmlHex()
                );
                if (color != null) {
                    colorClasses.put(nodeClass, color);
                }
            }

            if (icons) {
                addIconNode(iconNodes, nodeClass, displayNode);
            }

            boolean isFoldedChatNode =
                    foldChat
                            && "chat".equalsIgnoreCase(
                                    nullToEmpty(displayNode.getName()).trim()
                            );

            if (currentLevel >= maximumLevel || isFoldedChatNode) {
                return;
            }

            List<Children> children =
                    childrenRepository.findByFatherIdOrderBySequenceAsc(
                            treeNodeId
                    );

            for (Children child : children) {
                writeNodeRecursively(
                        definition,
                        child,
                        currentLevel + 1,
                        maximumLevel,
                        icons,
                        colors,
                        foldChat,
                        currentPath,
                        colorClasses,
                        iconNodes,
                        navigationNodes,
                        rootParentNodeId
                );
            }
        } finally {
            currentPath.remove(treeNodeId);
        }
    }

    private void addIconNode(
            List<Map<String, String>> iconNodes,
            String nodeClass,
            Node displayNode
    ) {
        NodeIcon icon = displayNode.getNodeIcon();
        if (icon == null || icon.getIconName() == null) {
            return;
        }

        String iconName = icon.getIconName().trim();
        if (iconName.isEmpty() || "zero".equalsIgnoreCase(iconName)) {
            return;
        }

        // Dosya adı dışında bir yol yazılmasını engeller.
        if (!iconName.matches("[A-Za-z0-9_-]+")) {
            return;
        }

        Map<String, String> iconNode = new LinkedHashMap<>();
        iconNode.put("nodeClass", nodeClass);
        iconNode.put("iconName", iconName);
        iconNodes.add(iconNode);
    }

    private void addNavigationNode(
            List<Map<String, String>> navigationNodes,
            String nodeClass,
            long targetNodeId,
            long contentNodeId
    ) {
        Map<String, String> navigationNode = new LinkedHashMap<>();
        navigationNode.put("nodeClass", nodeClass);
        navigationNode.put("targetNodeId", Long.toString(targetNodeId));
        navigationNode.put("contentNodeId", Long.toString(contentNodeId));
        navigationNodes.add(navigationNode);
    }

    /**
     * Root düğüm alias olsa bile parent ilişkisi children tablosundaki
     * alias kaydının father_id değerinden okunur. master_id yalnızca
     * gösterilecek Node içeriğini belirler.
     */
    private long findNavigableParentNodeId(Children root) {
        long rootNodeId = root.getNodeId();
        long fatherId = root.getFatherId();

        if (fatherId == 0) {
            return 0;
        }
        if (fatherId < 0 || fatherId == rootNodeId) {
            return rootNodeId;
        }

        return childrenRepository.findByNodeId(fatherId) != null ? fatherId : rootNodeId;
    }

    private Node findDisplayNode(Children children) {
        Long masterId = children.getMasterId();
        long contentNodeId =
                masterId != null && masterId != 0
                        ? masterId
                        : children.getNodeId();

        // SweetCherry'deki repository metodu Optional değil, doğrudan Node döndürüyor.
        Node node = nodeRepository.findById(contentNodeId);

        if (node == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Node tablosunda kayıt bulunamadı: " + contentNodeId
            );
        }

        return node;
    }

    private String createColorCss(Map<String, String> colorClasses) {
        StringBuilder css = new StringBuilder();

        for (Map.Entry<String, String> entry : colorClasses.entrySet()) {
            String selector = "." + entry.getKey();
            String color = entry.getValue();

            css.append(selector)
                    .append(" .nodeLabel,")
                    .append(selector)
                    .append(" span,")
                    .append(selector)
                    .append(" p {")
                    .append("color:").append(color).append(" !important;")
                    .append("fill:").append(color).append(" !important;")
                    .append("}\n");
        }

        return css.toString();
    }

    private String normalizeHtmlColor(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.matches("#[0-9A-F]{6}") ? normalized : null;
    }

    private String escapeMermaidLabel(String value) {
        return nullToEmpty(value)
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\r", " ")
                .replace("\n", " ");
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
