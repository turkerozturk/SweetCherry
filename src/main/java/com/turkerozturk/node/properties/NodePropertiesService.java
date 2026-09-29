package com.turkerozturk.node.properties;

import com.turkerozturk.IconIdAndIsReadOnly;
import com.turkerozturk.children.Children;
import com.turkerozturk.children.ChildrenRepository;
import com.turkerozturk.helpers.BitOperation;
import com.turkerozturk.helpers.NodeIcon;
import com.turkerozturk.helpers.highlighter.CodeHighLighter;
import com.turkerozturk.multipledatabases.CustomPropertiesHolder;
import com.turkerozturk.multipledatabases.TenantContext;
import com.turkerozturk.node.Node;
import com.turkerozturk.node.NodeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Objects;

@Service
public class NodePropertiesService {
   // private static final long ICON_MASK = 0x7FFEL;
   // private static final long READ_ONLY_MASK = 0x8000L;

    private final NodeRepository nodes;
    private final ChildrenRepository children;
    private final CustomPropertiesHolder settings;
    @PersistenceContext private EntityManager entityManager;

    public NodePropertiesService(NodeRepository nodes, ChildrenRepository children, CustomPropertiesHolder settings) {
        this.nodes = nodes;
        this.children = children;
        this.settings = settings;
    }

    public boolean writable() {
        String tenant = TenantContext.getCurrentTenant();
        Map<String, String> properties = tenant == null ? null : settings.getCustomProperties(tenant);
        return properties != null && Boolean.parseBoolean(properties.get("custom.isWritable"));
    }

    public Node realNode(long nodeId) {
        Children entry = children.findByNodeId(nodeId);
        if (entry == null || entry.getMasterId() != null && entry.getMasterId() != 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Real node not found");
        }
        Node node = nodes.findById(nodeId);
        if (node == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Real node not found");
        }
        return node;
    }

    /**
     * Updates editable title properties while preserving the node's content type
     * and any title color outside the predefined palette when KEEP is selected.
     */
    @Transactional
    public void update(long nodeId, String name, boolean bold, TitleColor color,
                       NodeIcon icon, boolean contentReadOnly) {
        update(nodeId, name, bold, color, icon, contentReadOnly, null, null);
    }

    /**
     * Changes plain text and code syntax without converting CherryTree rich text.
     * A node locked before this request cannot change type, even if unlocked in the same request.
     */
    @Transactional
    public void update(long nodeId, String name, boolean bold, TitleColor color,
                       NodeIcon icon, boolean contentReadOnly, NodeType nodeType, String codeSyntax) {
        if (!writable()) {
            throw new AccessDeniedException("The selected CTB is read-only.");
        }
        if (name == null || name.isBlank() || name.length() > 255 || icon == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid node properties");
        }
        Node node = realNode(nodeId);
        String oldSyntax = node.getSyntax();
        String newSyntax = requestedSyntax(node, nodeType, codeSyntax);
        boolean syntaxChanged = !Objects.equals(oldSyntax, newSyntax);
        // KEEP retains an existing color outside the predefined palette.
        long titleColor = color == null ? (node.getIsRichText() >>> 3) & 0xFFFFFFL : color.rgb();
        boolean richTextContent = (node.getIsRichText() & 1L) != 0;
        long richText = BitOperation.concatNodeTitleColorAndBoldnessAndTextType(
                titleColor, bold, richTextContent);
//        long iconAndLock = (node.getIsReadOnly16bit() & ~(ICON_MASK | READ_ONLY_MASK))
//                | icon.getIconIdIn16bit() | (contentReadOnly ? READ_ONLY_MASK : 0);

        IconIdAndIsReadOnly iconIdAndIsReadOnly = new IconIdAndIsReadOnly(icon.getIconId(), contentReadOnly);
        long concatIconIdAndIsReadOnly = BitOperation.concatIconIdAndIsReadOnly(iconIdAndIsReadOnly);
        // Explicit columns avoid merging the entity's bookmark/children relationships.
        String sql = "UPDATE node SET name = :name, is_richtxt = :rich, is_ro = :icon"
                + (syntaxChanged ? ", syntax = :syntax" : "")
                + " WHERE node_id = :id"
                + (syntaxChanged ? (oldSyntax == null ? " AND syntax IS NULL" : " AND syntax = :oldSyntax")
                + " AND (is_ro & 1) = 0 AND (is_richtxt & 1) = 0" : "");
        var query = entityManager.createNativeQuery(sql)
                .setParameter("name", name)
                .setParameter("rich", richText)
                .setParameter("icon", concatIconIdAndIsReadOnly)
                .setParameter("id", nodeId);
        if (syntaxChanged) {
            query.setParameter("syntax", newSyntax);
            if (oldSyntax != null) {
                query.setParameter("oldSyntax", oldSyntax);
            }
        }
        int updated = query.executeUpdate();
        if (syntaxChanged && updated != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Node type changed before it could be saved.");
        }
    }

    /** Resolves the requested type without allowing rich text conversions or unsupported new syntax. */
    private String requestedSyntax(Node node, NodeType nodeType, String codeSyntax) {
        String current = node.getSyntax();
        if (nodeType == null) {
            return current; // Disabled radio controls do not submit a type.
        }
        if (nodeType == NodeType.RICH_TEXT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rich text conversion is not supported.");
        }
        String requested = nodeType == NodeType.PLAIN_TEXT ? "plain-text" : codeSyntax;
        if (requested == null || requested.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select a code syntax.");
        }
        if (Objects.equals(current, requested)) {
            return current; // Legacy unsupported syntax can remain unchanged.
        }
        if ("custom-colors".equals(current) || (node.getIsRichText() & 1L) != 0) {
            throw new AccessDeniedException("CherryTree rich text type cannot be changed.");
        }
        if ((node.getIsReadOnly16bit() & 1L) != 0) {
            throw new AccessDeniedException("A read-only node's type cannot be changed.");
        }
        if (nodeType == NodeType.CODE && !CodeHighLighter.supportsCodeSyntax(requested)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported code syntax.");
        }
        return requested;
    }
}
