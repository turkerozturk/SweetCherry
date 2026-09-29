package com.turkerozturk.node.properties;

import com.turkerozturk.IconIdAndIsReadOnly;
import com.turkerozturk.children.Children;
import com.turkerozturk.children.ChildrenRepository;
import com.turkerozturk.helpers.BitOperation;
import com.turkerozturk.helpers.NodeIcon;
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
        if (!writable()) {
            throw new AccessDeniedException("The selected CTB is read-only.");
        }
        if (name == null || name.isBlank() || name.length() > 255 || icon == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid node properties");
        }
        Node node = realNode(nodeId);
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
        entityManager.createNativeQuery("UPDATE node SET name = :name, is_richtxt = :rich, is_ro = :icon WHERE node_id = :id")
                .setParameter("name", name)
                .setParameter("rich", richText)
                .setParameter("icon", concatIconIdAndIsReadOnly)
                .setParameter("id", nodeId)
                .executeUpdate();
    }
}
