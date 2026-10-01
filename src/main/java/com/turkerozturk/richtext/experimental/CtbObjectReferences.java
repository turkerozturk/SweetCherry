package com.turkerozturk.richtext.experimental;

import com.turkerozturk.image.Image;
import com.turkerozturk.anchor.Anchor;
import com.turkerozturk.codebox.CodeBox;
import com.turkerozturk.grid.Grid;
import com.turkerozturk.richtext.experimental.RichTextLayout.*;

/** Adapts existing entities without changing their services, payloads or independent browse pages. */
public final class CtbObjectReferences {
    /** Image-table rows represent anchors, attached files or inline images, depending on their fields. */
    public EmbeddedObject fromImage(Image image) {
        ObjectKind kind = image.getAnchor() != null && !image.getAnchor().isEmpty() ? ObjectKind.ANCHOR
                : image.getFileName() != null && !image.getFileName().isEmpty() ? ObjectKind.ATTACHMENT
                : ObjectKind.IMAGE;
        return new EmbeddedObject(kind, image.getNodeId(), image.getOffset());
    }

    public EmbeddedObject fromAnchor(Anchor anchor) {
        return new EmbeddedObject(ObjectKind.ANCHOR, anchor.getNodeId(), anchor.getOffset());
    }

    public EmbeddedObject fromCodeBox(CodeBox box) {
        return new EmbeddedObject(ObjectKind.CODEBOX, box.getId().getNodeId(), box.getId().getOffset());
    }

    public EmbeddedObject fromTable(Grid table) {
        return new EmbeddedObject(ObjectKind.TABLE, table.getId().getNodeId(), table.getId().getOffset());
    }
}
