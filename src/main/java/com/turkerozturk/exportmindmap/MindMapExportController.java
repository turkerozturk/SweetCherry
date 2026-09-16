package com.turkerozturk.exportmindmap;


import com.turkerozturk.children.Children;
import com.turkerozturk.children.ChildrenRepository;
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

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
public class MindMapExportController {

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
            ) Integer maximumLevel
    ) throws XMLStreamException {

        if (maximumLevel != null && maximumLevel < 0) {
            throw new IllegalArgumentException(
                    "Level değeri negatif olamaz."
            );
        }

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
                maximumLevel
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
            Integer maximumLevel
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

            Set<Long> visitedNodeIds = new HashSet<>();

            writeNodeRecursively(
                    writer,
                    root,
                    0,
                    maximumLevel,
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
            Set<Long> visitedNodeIds
    ) throws XMLStreamException {

        long treeNodeId = treeNode.getNodeId();

        /*
         * Bozuk bir veritabanı bağlantısının aynı node'a
         * yeniden dönmesi durumunda sonsuz recursion'ı önler.
         */
        if (!visitedNodeIds.add(treeNodeId)) {
            return;
        }

        Node displayNode = findDisplayNode(treeNode);

        writer.writeStartElement("node");

        writer.writeAttribute(
                "TEXT",
                nullToEmpty(displayNode.getName())
        );

        /*
         * Freeplane ID'lerinin dosya içinde benzersiz
         * olması yeterlidir. CherryTree nodeId değerini
         * kullanmak bu gereksinimi karşılar.
         */
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
                        visitedNodeIds
                );
            }
        }

        writer.writeEndElement();
    }

    /**
     * Normal düğüm:
     *     children.node_id -> node.node_id
     *
     * Alias/shared düğüm:
     *     children.master_id -> node.node_id
     *
     * XML içindeki ID ise alias'ın ağaçtaki gerçek
     * children.node_id değeri olarak kalır.
     */
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

    /**
     * CherryTree timestamp değerleri bazı sürümlerde
     * saniye, Freeplane'de ise milisaniye olabilir.
     */
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
                .replaceAll(
                        "[\\\\/:*?\"<>|]",
                        "_"
                )
                .replaceAll("\\s+", " ")
                .trim();

        if (sanitized.isBlank()) {
            return "cherrytree-mindmap";
        }

        return sanitized;
    }
}