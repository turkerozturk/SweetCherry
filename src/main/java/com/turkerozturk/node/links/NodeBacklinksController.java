package com.turkerozturk.node.links;

import com.turkerozturk.multipledatabases.RequiresTenant;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequiresTenant
public class NodeBacklinksController {
    private final InternalLinkReportService links;
    public NodeBacklinksController(InternalLinkReportService links){this.links=links;}
    public record Place(String id,String name,String icon,boolean bold,String color,boolean shared,boolean available,String breadcrumb) { }
    public record Item(Place source,String label,String targetId,String anchor,String status,String kind) { }
    public record Panel(List<Item> items,long totalLinks,long totalSources,int page,boolean more,long unreadable,boolean truncated) { }
    @GetMapping("/nodes/backlinks/{id}")
    public ResponseEntity<?> show(@PathVariable String id,@RequestParam String _tenantView,
            @RequestParam(defaultValue="false") boolean group,@RequestParam(defaultValue="1") String page){
        final long nodeId;final int number;
        try {
            if(!id.matches("[1-9][0-9]{0,18}"))throw new IllegalArgumentException();
            nodeId=Long.parseLong(id);number=Integer.parseInt(page);
            if(number<1||number>1_000_000)throw new IllegalArgumentException();
        }catch(IllegalArgumentException invalid){return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).build();}
        try {
            var report=links.inspect(new InternalLinkReportService.Options("","all",20,number,nodeId,group));
            if(report==null)return ResponseEntity.notFound().cacheControl(CacheControl.noStore()).build();
            var items=report.rows().stream().map(row->new Item(place(row.source()),row.label(),row.targetId(),row.anchor(),row.status(),row.kind())).toList();
            boolean truncated=report.issues().stream().anyMatch(issue->issue.reason().equals("scanLimit"));
            return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                    .body(new Panel(items,report.matched(),report.sourcePlaces(),number,number<report.pages(),report.unreadable(),truncated));
        }catch(RuntimeException failure){
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Backlink scan unavailable: {}",failure.getClass().getSimpleName());
            return ResponseEntity.status(503).cacheControl(CacheControl.noStore()).body(Map.of("error","unavailable"));
        }
    }
    private Place place(InternalLinkReportService.Place value){
        return new Place(Long.toString(value.id()),value.name(),value.icon(),value.bold(),value.color(),value.shared(),value.available(),value.breadcrumb());
    }
}
