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
    private final NodeTypeConversion conversion;

    public NodePropertiesService(NodeRepository nodes, ChildrenRepository children, CustomPropertiesHolder settings) {
        this(nodes,children,settings,null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public NodePropertiesService(NodeRepository nodes, ChildrenRepository children, CustomPropertiesHolder settings, NodeTypeConversion conversion) {
        this.conversion = conversion;
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
     * Changes plain text and code syntax; rich text conversion requires the explicit confirmation overload.
     * A node locked before this request cannot change type, even if unlocked in the same request.
     */
    @Transactional
    public void update(long nodeId, String name, boolean bold, TitleColor color,
                       NodeIcon icon, boolean contentReadOnly, NodeType nodeType, String codeSyntax) {
        update(nodeId,name,bold,color,icon,contentReadOnly,nodeType,codeSyntax,null);
    }

    /** Optional tags preserve legacy callers; submitted values are normalized and saved with the other properties. */
    @Transactional
    public void update(long nodeId,String name,boolean bold,TitleColor color,NodeIcon icon,boolean contentReadOnly,
            NodeType nodeType,String codeSyntax,String tags) {
        update(nodeId,name,bold,color,icon,contentReadOnly,nodeType,codeSyntax,tags,null,null);
    }

    /** Null flags preserve values for older callers; the properties form submits both explicitly. */
    @Transactional
    public void update(long nodeId,String name,boolean bold,TitleColor color,NodeIcon icon,boolean contentReadOnly,
            NodeType nodeType,String codeSyntax,String tags,Boolean excludeThisNode,Boolean excludeSubnodes) {
        update(nodeId,name,bold,color,icon,contentReadOnly,nodeType,codeSyntax,tags,excludeThisNode,excludeSubnodes,false,null);
    }

    @Transactional(readOnly=true)
    public NodeTypeConversion.Preview conversionPreview(long nodeId){
        if(!writable())throw new AccessDeniedException("The selected CTB is read-only.");
        Node node=realNode(nodeId);
        if((node.getIsReadOnly16bit()&1)!=0)throw new AccessDeniedException("The node is read-only.");
        return conversion.preview(nodeId);
    }

    @Transactional
    public void update(long nodeId,String name,boolean bold,TitleColor color,NodeIcon icon,boolean contentReadOnly,
            NodeType nodeType,String codeSyntax,String tags,Boolean excludeThisNode,Boolean excludeSubnodes,
            boolean confirmRichTextConversion,String conversionRevision) {
        if (!writable()) {
            throw new AccessDeniedException("The selected CTB is read-only.");
        }
        name = com.turkerozturk.node.NodeNames.clean(name);
        if (name == null || name.isBlank() || name.length() > 255 || icon == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid node properties");
        }
        String normalizedTags=null;
        if(tags!=null)try{normalizedTags=com.turkerozturk.node.tags.NodeTags.normalize(tags);}
        catch(IllegalArgumentException invalid){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid tags",invalid);}
        Node node = realNode(nodeId);
        entityManager.unwrap(org.hibernate.Session.class).doWork(c->com.turkerozturk.node.TrashSql.guardHelper(c,nodeId));
        String oldSyntax = node.getSyntax();
        String newSyntax = requestedSyntax(node, nodeType, codeSyntax);
        boolean syntaxChanged = !Objects.equals(oldSyntax, newSyntax);
        boolean converting=syntaxChanged&&("custom-colors".equals(oldSyntax)||(node.getIsRichText()&1)!=0);
        NodeTypeConversion.Snapshot converted=null;
        if(converting){
            if(!confirmRichTextConversion)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"confirmationRequired");
            converted=conversion.prepare(nodeId,conversionRevision);
        }
        // KEEP retains an existing color outside the predefined palette.
        long titleColor = color == null ? (node.getIsRichText() >>> 3) & 0xFFFFFFL : color.rgb();
        boolean richTextContent = !converting && (node.getIsRichText() & 1L) != 0;
        long richText = BitOperation.concatNodeTitleColorAndBoldnessAndTextType(
                titleColor, bold, richTextContent);
//        long iconAndLock = (node.getIsReadOnly16bit() & ~(ICON_MASK | READ_ONLY_MASK))
//                | icon.getIconIdIn16bit() | (contentReadOnly ? READ_ONLY_MASK : 0);

        IconIdAndIsReadOnly iconIdAndIsReadOnly = new IconIdAndIsReadOnly(icon.getIconId(), contentReadOnly);
        long concatIconIdAndIsReadOnly = BitOperation.concatIconIdAndIsReadOnly(iconIdAndIsReadOnly);
        // Explicit columns avoid merging the entity's bookmark/children relationships.
        String sql = "UPDATE node SET name = :name, is_richtxt = :rich, is_ro = :icon, ts_lastsave = :timestamp"
                + (tags!=null ? ", tags = :tags" : "")
                + (excludeThisNode!=null && excludeSubnodes!=null ? ", level = (COALESCE(level,0) & -4) | :searchFlags" : "")
                + (syntaxChanged ? ", syntax = :syntax" : "")
                + (converting ? ", txt = :convertedText, has_image = 0, has_table = 0, has_codebox = 0" : "")
                + " WHERE node_id = :id"
                + (syntaxChanged ? (oldSyntax == null ? " AND syntax IS NULL" : " AND syntax = :oldSyntax")
                + " AND (is_ro & 1) = 0" + (converting ? (node.getTxt()==null?" AND txt IS NULL":" AND txt = :oldText")+" AND is_richtxt = :oldRich AND is_ro = :oldIcon AND ts_lastsave = :oldModified" : " AND (is_richtxt & 1) = 0") : "");
        var query = entityManager.createNativeQuery(sql)
                .setParameter("name", name)
                .setParameter("rich", richText)
                .setParameter("icon", concatIconIdAndIsReadOnly)
                .setParameter("timestamp", com.turkerozturk.node.NodeTimestamps.now())
                .setParameter("id", nodeId);
        if(excludeThisNode!=null && excludeSubnodes!=null)query.setParameter("searchFlags",(excludeThisNode?1L:0L)|(excludeSubnodes?2L:0L));
        if(tags!=null)query.setParameter("tags",normalizedTags);
        if (syntaxChanged) {
            query.setParameter("syntax", newSyntax);
            if (oldSyntax != null) {
                query.setParameter("oldSyntax", oldSyntax);
            }
        }
        if(converting)query.setParameter("convertedText",converted.text())
                .setParameter("oldRich",node.getIsRichText()).setParameter("oldIcon",node.getIsReadOnly16bit()).setParameter("oldModified",node.getLastSaveTimestamp());
        if(converting&&node.getTxt()!=null)query.setParameter("oldText",node.getTxt());
        int updated = query.executeUpdate();
        if (syntaxChanged && updated != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Node type changed before it could be saved.");
        }
        if(converting)conversion.removeObjects(nodeId,converted);
    }

    /** Resolves the requested type; the update path separately requires confirmation for rich text loss. */
    private String requestedSyntax(Node node, NodeType nodeType, String codeSyntax) {
        String current = node.getSyntax();
        if (nodeType == null) {
            return current; // Disabled radio controls do not submit a type.
        }
        if (nodeType == NodeType.RICH_TEXT) {
            if("custom-colors".equals(current)||(node.getIsRichText()&1)!=0)return current;
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rich text conversion is not supported.");
        }
        String requested = nodeType == NodeType.PLAIN_TEXT ? "plain-text" : codeSyntax;
        if (requested == null || requested.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select a code syntax.");
        }
        if (Objects.equals(current, requested)) {
            return current; // Legacy unsupported syntax can remain unchanged.
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
