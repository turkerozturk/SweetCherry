package com.turkerozturk.exportmindmap;

import com.turkerozturk.children.Children;
import com.turkerozturk.children.ChildrenRepository;
import com.turkerozturk.helpers.NodeIcon;
import com.turkerozturk.node.Node;
import com.turkerozturk.node.NodeRepository;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
public class MindMapExportController {

    private static final String DEFAULT_FOLD_MODE = "chat";

    private final ChildrenRepository childrenRepository;
    private final NodeRepository nodeRepository;

    public MindMapExportController(
            ChildrenRepository childrenRepository,
            NodeRepository nodeRepository
    ) {
        this.childrenRepository = childrenRepository;
        this.nodeRepository = nodeRepository;
    }

    @GetMapping("/mindmap-export")
    public String showExportPage() {
        return "mindmap-export";
    }

    @PostMapping("/mindmap-export")
    public ResponseEntity<byte[]> exportMindMap(
            @RequestParam("nodeId") long nodeId,
            @RequestParam(
                    name = "level",
                    required = false
            ) Integer maximumLevel,
            @RequestParam(
                    name = "foldMode",
                    defaultValue = DEFAULT_FOLD_MODE
            ) String foldModeValue,
            @RequestParam(
                    name = "includeIcons",
                    defaultValue = "false"
            ) boolean includeIcons,
            @RequestParam(
                    name = "includeColors",
                    defaultValue = "true"
            ) boolean includeColors
    ) throws XMLStreamException {

        if (maximumLevel != null && maximumLevel < 0) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Level değeri negatif olamaz."
            );
        }

        FoldMode foldMode = FoldMode.fromRequestValue(
                foldModeValue
        );

        Children rootChildren =
                childrenRepository.findByNodeId(nodeId);

        if (rootChildren == null) {
            throw new ResponseStatusException(
                    NOT_FOUND,
                    "Children tablosunda nodeId bulunamadı: "
                            + nodeId
            );
        }

        Node rootDisplayNode = findDisplayNode(rootChildren);

        byte[] mindMapContent = createMindMap(
                rootChildren,
                maximumLevel,
                foldMode,
                includeIcons,
                includeColors
        );

        String rootName = rootDisplayNode.getName();

        String filename =
                sanitizeFilename(rootName)
                        + "-"
                        + nodeId
                        + ".mm";

        ContentDisposition disposition =
                ContentDisposition.attachment()
                        .filename(
                                filename,
                                StandardCharsets.UTF_8
                        )
                        .build();

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition.toString()
                )
                .contentType(
                        MediaType.parseMediaType(
                                "application/x-freemind"
                        )
                )
                .contentLength(mindMapContent.length)
                .body(mindMapContent);
    }

    private byte[] createMindMap(
            Children root,
            Integer maximumLevel,
            FoldMode foldMode,
            boolean includeIcons,
            boolean includeColors
    ) throws XMLStreamException {

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        XMLOutputFactory factory =
                XMLOutputFactory.newFactory();

        XMLStreamWriter writer =
                factory.createXMLStreamWriter(
                        output,
                        StandardCharsets.UTF_8.name()
                );

        try {
            writer.writeStartDocument(
                    StandardCharsets.UTF_8.name(),
                    "1.0"
            );
            writer.writeCharacters("\n");

            writer.writeStartElement("map");
            writer.writeAttribute(
                    "version",
                    "freeplane 1.12.15"
            );
            writer.writeCharacters("\n");

            writer.writeComment(
                    "To view this file, download free mind mapping software "
                            + "Freeplane from https://www.freeplane.org"
            );
            writer.writeCharacters("\n");

            writer.writeEmptyElement("bookmarks");
            writer.writeCharacters("\n");

            Set<Long> visitedNodeIds = new HashSet<>();

            writeNodeRecursively(
                    writer,
                    root,
                    0,
                    maximumLevel,
                    foldMode,
                    includeIcons,
                    includeColors,
                    visitedNodeIds
            );

            writer.writeCharacters("\n");
            writer.writeEndElement();
            writer.writeCharacters("\n");
            writer.writeEndDocument();
            writer.flush();

            return output.toByteArray();
        } finally {
            writer.close();
        }
    }

    private void writeNodeRecursively(
            XMLStreamWriter writer,
            Children treeNode,
            int currentLevel,
            Integer maximumLevel,
            FoldMode foldMode,
            boolean includeIcons,
            boolean includeColors,
            Set<Long> visitedNodeIds
    ) throws XMLStreamException {

        long treeNodeId = treeNode.getNodeId();

        if (!visitedNodeIds.add(treeNodeId)) {
            return;
        }

        Node displayNode = findDisplayNode(treeNode);
        boolean isRootNode = currentLevel == 0;

        writer.writeStartElement("node");
        writer.writeAttribute(
                "TEXT",
                nullToEmpty(displayNode.getName())
        );
        writer.writeAttribute(
                "FOLDED",
                Boolean.toString(
                        shouldFold(
                                displayNode,
                                isRootNode,
                                foldMode
                        )
                )
        );
        writer.writeAttribute(
                "ID",
                "ID_" + treeNodeId
        );
        writer.writeAttribute(
                "CREATED",
                Long.toString(
                        toFreeplaneTimestamp(
                                displayNode.getCreationTimestamp()
                        )
                )
        );
        writer.writeAttribute(
                "MODIFIED",
                Long.toString(
                        toFreeplaneTimestamp(
                                displayNode.getLastSaveTimestamp()
                        )
                )
        );

        if (isRootNode) {
            writer.writeAttribute("STYLE", "oval");
        }

        String color = displayNode.getTitleColorAsHtmlHex();
        if (includeColors && isValidHtmlColor(color)) {
            writer.writeAttribute("COLOR", color);
        }

        writeFontElement(
                writer,
                isRootNode,
                displayNode.isBoldnessBit()
        );

        if (isRootNode) {
            writeMapStyleHook(writer);
        }

        if (includeIcons) {
            writeIconHook(writer, displayNode);
        }

        boolean mayWriteChildren =
                maximumLevel == null
                        || currentLevel < maximumLevel;

        if (mayWriteChildren) {
            List<Children> children =
                    childrenRepository
                            .findByFatherIdOrderBySequenceAsc(
                                    treeNodeId
                            );

            for (Children child : children) {
                writer.writeCharacters("\n");

                writeNodeRecursively(
                        writer,
                        child,
                        currentLevel + 1,
                        maximumLevel,
                        foldMode,
                        includeIcons,
                        includeColors,
                        visitedNodeIds
                );
            }
        }

        writer.writeEndElement();
    }

    private boolean shouldFold(
            Node displayNode,
            boolean isRootNode,
            FoldMode foldMode
    ) {
        // Freeplane ana düğümü açık kalmalıdır.
        if (isRootNode) {
            return false;
        }

        return switch (foldMode) {
            case ALL_FOLDED -> true;
            case ALL_UNFOLDED -> false;
            case CHAT -> "chat".equals(displayNode.getName());
        };
    }

    private void writeFontElement(
            XMLStreamWriter writer,
            boolean isRootNode,
            boolean isBold
    ) throws XMLStreamException {

        if (!isRootNode && !isBold) {
            return;
        }

        writer.writeCharacters("\n");
        writer.writeStartElement("font");

        if (isRootNode) {
            writer.writeAttribute("SIZE", "22");
        }

        if (isBold) {
            writer.writeAttribute("BOLD", "true");
        }

        writer.writeEndElement();
    }

    private void writeMapStyleHook(
            XMLStreamWriter writer
    ) throws XMLStreamException {
        writer.writeCharacters("\n");
        writer.writeStartElement("hook");
        writer.writeAttribute("NAME", "MapStyle");
        writer.writeAttribute("background", "#ffffccff");
        writer.writeEndElement();
    }

    private void writeIconHook(
            XMLStreamWriter writer,
            Node displayNode
    ) throws XMLStreamException {

        NodeIcon icon = displayNode.getNodeIcon();

        if (icon == null
                || icon.getIconName() == null
                || "zero".equals(icon.getIconName())) {
            return;
        }

        writer.writeCharacters("\n");
        writer.writeStartElement("hook");
        writer.writeAttribute(
                "URI",
                "ctbicons/" + icon.getIconName() + ".png"
        );
        writer.writeAttribute("SIZE", "1.0");
        writer.writeAttribute("NAME", "ExternalObject");
        writer.writeEndElement();
    }

    private boolean isValidHtmlColor(String color) {
        return color != null
                && color.matches("#[0-9A-Fa-f]{6}");
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
                    NOT_FOUND,
                    "Node tablosunda kayıt bulunamadı: "
                            + contentNodeId
            );
        }

        return node;
    }

    private long toFreeplaneTimestamp(long timestamp) {
        if (timestamp <= 0) {
            return Instant.now().toEpochMilli();
        }

        if (timestamp < 100_000_000_000L) {
            return timestamp * 1000L;
        }

        return timestamp;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) {
            return "cherrytree-mindmap";
        }

        String sanitized = name
                .replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\s+", " ")
                .trim();

        if (sanitized.isBlank()) {
            return "cherrytree-mindmap";
        }

        return sanitized;
    }

    private enum FoldMode {
        CHAT,
        ALL_FOLDED,
        ALL_UNFOLDED;

        private static FoldMode fromRequestValue(String value) {
            if (value == null) {
                return CHAT;
            }

            return switch (value) {
                case "chat" -> CHAT;
                case "all-folded" -> ALL_FOLDED;
                case "all-unfolded" -> ALL_UNFOLDED;
                default -> throw new ResponseStatusException(
                        BAD_REQUEST,
                        "Geçersiz foldMode değeri: " + value
                );
            };
        }
    }
}
