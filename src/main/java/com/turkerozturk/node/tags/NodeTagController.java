package com.turkerozturk.node.tags;

import com.turkerozturk.multipledatabases.RequiresTenant;
import com.turkerozturk.node.properties.NodePropertiesService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiresTenant
public class NodeTagController {
    private final NodeTagService tags;private final NodePropertiesService properties;
    public NodeTagController(NodeTagService tags,NodePropertiesService properties){this.tags=tags;this.properties=properties;}
    @GetMapping("/nodes/tags/catalog") @ResponseBody
    public ResponseEntity<List<String>> catalogue(@RequestParam String _tenantView){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(tags.catalogue());}
    @GetMapping("/nodes/tags")
    public String show(@RequestParam(defaultValue="any")String mode,@RequestParam(defaultValue="50")String limit,
            @RequestParam(defaultValue="1")String page,HttpServletRequest request,Model model) {
        // Read repeated parameters literally: Spring's List converter would split a tag containing a comma.
        String[] selected=request.getParameterValues("tag");
        return view(selected==null?List.of():java.util.Arrays.asList(selected),mode,limit,page,request,model);
    }
    public String view(List<String> selected,String mode,String limit,String page,HttpServletRequest request,Model model) {
        NodeTagService.Options options;
        try{options=new NodeTagService.Options(selected==null?List.of():selected,mode,Integer.parseInt(limit),Integer.parseInt(page));}
        catch(IllegalArgumentException invalid){options=new NodeTagService.Options(List.of(),"any",50,1);model.addAttribute("invalidOptions",true);}
        var view=tags.inspect(options);model.addAttribute("tagView",view);
        var choices=new java.util.TreeSet<String>(NodeTags.ORDER);choices.addAll(view.catalogue());choices.addAll(options.tags());
        model.addAttribute("tagChoices",List.copyOf(choices));model.addAttribute("canEditTags",request.isUserInRole("ADMIN")&&properties.writable());
        return "node/tags";
    }
}
