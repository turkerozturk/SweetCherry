package com.turkerozturk.quickmindmapmarkmap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.turkerozturk.children.Children;
import com.turkerozturk.children.ChildrenRepository;
import com.turkerozturk.helpers.NodeIcon;
import com.turkerozturk.node.Node;
import com.turkerozturk.node.NodeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Controller
public class MarkmapQuickMindMapController {

    private final ChildrenRepository childrenRepository;
    private final NodeRepository nodeRepository;
    private final ObjectMapper objectMapper;

    public MarkmapQuickMindMapController(
            ChildrenRepository childrenRepository,
            NodeRepository nodeRepository,
            ObjectMapper objectMapper
    ) {
        this.childrenRepository = childrenRepository;
        this.nodeRepository = nodeRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Örnek:
     * /markmap-quick-mind-map/2573?level=3&icons=false&colors=true&foldChat=true
     *
     * level=0 yalnızca kökü, level=1 kökü ve doğrudan çocuklarını gösterir.
     */
    @GetMapping("/markmap-quick-mind-map/{nodeId}")
    public String showMarkmapQuickMindMap(
            @PathVariable long nodeId,
            @RequestParam(defaultValue = "3") int level,
            @RequestParam(defaultValue = "false") boolean icons,
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
        ObjectNode markmapRoot = createNodeRecursively(
                root,
                0,
                level,
                icons,
                colors,
                foldChat,
                new HashSet<>()
        );

        if (markmapRoot == null) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Markmap kök düğümü oluşturulamadı."
            );
        }

        final String markmapDataJson;
        try {
            markmapDataJson = objectMapper.writeValueAsString(markmapRoot);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Markmap verisi JSON biçimine dönüştürülemedi.",
                    exception
            );
        }

        model.addAttribute("rootNodeId", nodeId);
        model.addAttribute("rootNodeName", nullToEmpty(rootDisplayNode.getName()));
        model.addAttribute("maximumLevel", level);
        model.addAttribute("icons", icons);
        model.addAttribute("colors", colors);
        model.addAttribute("foldChat", foldChat);
        model.addAttribute("markmapDataJson", markmapDataJson);

        return "markmap-quick-mind-map";
    }

    private ObjectNode createNodeRecursively(
            Children treeNode,
            int currentLevel,
            int maximumLevel,
            boolean icons,
            boolean colors,
            boolean foldChat,
            Set<Long> currentPath
    ) {
        long treeNodeId = treeNode.getNodeId();

        // Aynı düğüm yalnız mevcut recursion yolunda engellenir. Böylece farklı
        // dallardaki geçerli alias kullanımları korunurken bozuk döngüler durur.
        if (!currentPath.add(treeNodeId)) {
            return null;
        }

        try {
            Node displayNode = findDisplayNode(treeNode);
            String nodeName = nullToEmpty(displayNode.getName());

            ObjectNode result = objectMapper.createObjectNode();
            result.put("content", nodeName);

            ObjectNode payload = result.putObject("payload");
            payload.put("nodeId", treeNodeId);
            payload.put("bold", displayNode.isBoldnessBit());

            if (colors) {
                String color = normalizeHtmlColor(
                        displayNode.getTitleColorAsHtmlHex()
                );
                if (color != null) {
                    payload.put("color", color);
                }
            }

            if (icons) {
                String iconName = findIconName(displayNode);
                if (iconName != null) {
                    // Şablon bu adı /img/ctbicons/<ikon-adi>.png yoluna dönüştürür.
                    payload.put("iconName", iconName);
                }
            }

            boolean isFoldedChatNode =
                    currentLevel > 0
                            && foldChat
                            && "chat".equalsIgnoreCase(nodeName.trim());

            if (isFoldedChatNode) {
                // Markmap payload.fold=1 değerini başlangıçta kapalı düğüm olarak
                // yorumlar. Çocuklar JSON'da kalır ve tıklanınca tekrar açılabilir.
                payload.put("fold", 1);
            }

            ArrayNode childNodes = objectMapper.createArrayNode();

            if (currentLevel < maximumLevel) {
                List<Children> children =
                        childrenRepository.findByFatherIdOrderBySequenceAsc(
                                treeNodeId
                        );

                for (Children child : children) {
                    ObjectNode childNode = createNodeRecursively(
                            child,
                            currentLevel + 1,
                            maximumLevel,
                            icons,
                            colors,
                            foldChat,
                            currentPath
                    );

                    if (childNode != null) {
                        childNodes.add(childNode);
                    }
                }
            }

            result.set("children", childNodes);

            return result;
        } finally {
            currentPath.remove(treeNodeId);
        }
    }

    private String findIconName(Node displayNode) {
        NodeIcon icon = displayNode.getNodeIcon();
        if (icon == null || icon.getIconName() == null) {
            return null;
        }

        String iconName = icon.getIconName().trim();
        if (iconName.isEmpty() || "zero".equalsIgnoreCase(iconName)) {
            return null;
        }

        return iconName;
    }

    private Node findDisplayNode(Children children) {
        Long masterId = children.getMasterId();
        long contentNodeId =
                masterId != null && masterId != 0
                        ? masterId
                        : children.getNodeId();

        Node node = nodeRepository.findById(contentNodeId);

        if (node == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Node tablosunda kayıt bulunamadı: " + contentNodeId
            );
        }

        return node;
    }

    private String normalizeHtmlColor(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.matches("#[0-9A-F]{6}") ? normalized : null;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
