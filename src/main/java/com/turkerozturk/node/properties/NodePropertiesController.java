package com.turkerozturk.node.properties;

import com.turkerozturk.IconIdAndIsReadOnly;
import com.turkerozturk.helpers.BitOperation;
import com.turkerozturk.helpers.NodeIcon;
import com.turkerozturk.helpers.highlighter.CodeHighLighter;
import com.turkerozturk.multipledatabases.RequiresTenant;
import com.turkerozturk.node.Node;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiresTenant
@PreAuthorize("hasRole('ADMIN')")
public class NodePropertiesController {
    private final NodePropertiesService service;
    public NodePropertiesController(NodePropertiesService service) { this.service = service; }

    @GetMapping("/nodes/properties/{nodeId}")
    public String edit(@PathVariable long nodeId, Model model) {
        if (!service.writable()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The selected CTB is read-only.");
        }
        Node node = service.realNode(nodeId);
        model.addAttribute("node", node);
        model.addAttribute("colors", TitleColor.values());
        TitleColor selected = TitleColor.fromBits(node.getIsRichText());
        model.addAttribute("selectedColor", selected == null ? "KEEP" : selected.name());
        model.addAttribute("originalColor", selected == null ? String.format("#%06X", (node.getIsRichText() >>> 3) & 0xFFFFFF) : null);
        model.addAttribute("bold", (node.getIsRichText() & 2L) != 0);

        model.addAttribute("icons", NodeIcon.values());
        IconIdAndIsReadOnly iconIdAndIsReadOnly = BitOperation.processSixteenBitData((int) node.getIsReadOnly16bit());
        model.addAttribute("selectedIconId", (int) (node.getIsReadOnly16bit() & 0x7FFE));
        boolean g = (node.getIsReadOnly16bit() & 1L) != 0;
        model.addAttribute("contentReadOnly", g);
        boolean richText = "custom-colors".equals(node.getSyntax()) || (node.getIsRichText() & 1L) != 0;
        model.addAttribute("nodeType", richText ? NodeType.RICH_TEXT
                : "plain-text".equals(node.getSyntax()) ? NodeType.PLAIN_TEXT : NodeType.CODE);
        model.addAttribute("syntaxLocked", richText || g);
        List<String> syntaxOptions = new ArrayList<>(CodeHighLighter.supportedSyntaxes());
        if (!richText && node.getSyntax() != null && !"plain-text".equals(node.getSyntax())
                && !syntaxOptions.contains(node.getSyntax())) {
            syntaxOptions.add(0, node.getSyntax()); // Preserve an unsupported legacy syntax on other edits.
        }
        model.addAttribute("syntaxOptions", syntaxOptions);
        //model.addAttribute("contentReadOnly", (node.getIsReadOnly16bit() & 0x8000L) != 0);
        return "node/nodeProperties";
    }

    @PostMapping("/nodes/properties/{nodeId}")
    public String save(@PathVariable long nodeId, @RequestParam String name,
                       @RequestParam(defaultValue = "false") boolean bold,
                       @RequestParam String color, @RequestParam NodeIcon icon,
                       @RequestParam(defaultValue = "false") boolean contentReadOnly,
                       @RequestParam(required = false) NodeType nodeType,
                       @RequestParam(required = false) String codeSyntax) {
        TitleColor selected;
        try {
            selected = "KEEP".equals(color) ? null : TitleColor.valueOf(color);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid title color");
        }
        service.update(nodeId, name, bold, selected, icon, contentReadOnly, nodeType, codeSyntax);
        return "redirect:/nodes/" + nodeId;
    }
}
