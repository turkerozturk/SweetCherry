package com.turkerozturk.node.audit;
import com.turkerozturk.multipledatabases.RequiresTenant;
import org.springframework.stereotype.Controller;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletResponse;
import java.time.*;
import java.util.*;
@Controller @RequiresTenant @PreAuthorize("hasRole('ADMIN')") @RequestMapping("/nodes/audit")
public class AuditController {
 private final AuditReadService service;private final boolean enabled;
 public AuditController(AuditReadService service,@Value("${myapp.audit.enabled:false}") boolean enabled){this.service=service;this.enabled=enabled;}
 public record SourceView(AuditSql.TextState source,String plain,String raw,boolean longPlain,boolean longRaw,String objects,boolean longObjects){}
 @GetMapping public String list(@RequestParam(defaultValue="") String day,@RequestParam(defaultValue="0") long nodeId,@RequestParam(defaultValue="0") int page,Model model,HttpServletResponse response){
  response.setHeader("Cache-Control","no-store");if(day.isBlank())day=LocalDate.now(ZoneOffset.UTC).toString();try{LocalDate.parse(day);}catch(RuntimeException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"audit.invalid");}if(nodeId<0||page<0||page>1000000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"audit.invalid");
  model.addAttribute("history",service.history(day,nodeId,page));model.addAttribute("auditDay",day);model.addAttribute("auditNode",nodeId);model.addAttribute("auditPage",page);model.addAttribute("auditEnabled",enabled);return "node/audit";
 }
 @GetMapping("/{id}") public String detail(@PathVariable String id,@RequestParam String _tenantView,Model model,HttpServletResponse response){
  response.setHeader("Cache-Control","no-store");var event=service.event(id);var sources=new ArrayList<SourceView>();for(var source:event.sources()){
   String plain=service.source(event,source.id(),source.stage(),false),raw=service.source(event,source.id(),source.stage(),true);String objects=AuditSql.JSON.writerWithDefaultPrettyPrinter().writeValueAsString(source.objects());sources.add(new SourceView(source,preview(plain),preview(raw),plain.length()>20000,raw.length()>20000,preview(objects),objects.length()>20000));
  }model.addAttribute("event",event);model.addAttribute("nodeChanges",event.nodes().stream().limit(200).toList());model.addAttribute("treeChanges",event.tree().stream().limit(200).toList());model.addAttribute("bookmarkChanges",event.bookmarks().stream().limit(200).toList());model.addAttribute("metadataLimited",event.nodes().size()>200||event.tree().size()>200||event.bookmarks().size()>200);model.addAttribute("sources",sources);return "node/auditEvent";
 }
 @GetMapping("/{id}/download") @ResponseBody public ResponseEntity<byte[]> metadata(@PathVariable String id,@RequestParam String _tenantView){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"ctblogs-event.json\"").contentType(MediaType.APPLICATION_JSON).body(AuditSql.JSON.writerWithDefaultPrettyPrinter().writeValueAsBytes(service.event(id)));}
 @GetMapping("/{id}/source") @ResponseBody public ResponseEntity<byte[]> download(@PathVariable String id,@RequestParam long nodeId,@RequestParam String stage,@RequestParam(defaultValue="false") boolean raw,@RequestParam String _tenantView){
  if(!Set.of("before","after").contains(stage))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"audit.invalid");String text=service.source(service.event(id),nodeId,stage,raw);
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"ctblogs-"+nodeId+"-"+stage+(raw?"-source":"-text")+".txt\"").contentType(new MediaType("text","plain",java.nio.charset.StandardCharsets.UTF_8)).body(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
 }
 private static String preview(String text){if(text.length()<=20000)return text;int end=20000;if(Character.isHighSurrogate(text.charAt(end-1)))end--;return text.substring(0,end);}
 @ExceptionHandler(ResponseStatusException.class) public String problem(ResponseStatusException error,Model model,HttpServletResponse response){response.setStatus(error.getStatusCode().value());response.setHeader("Cache-Control","no-store");String reason=error.getReason();model.addAttribute("auditError",reason!=null&&reason.startsWith("audit.")?reason:"audit.invalid");return "node/auditError";}
}
