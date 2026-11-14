package com.turkerozturk.node;
import com.turkerozturk.multipledatabases.RequiresTenant;
import org.springframework.stereotype.Controller;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import jakarta.servlet.http.HttpServletResponse;
@Controller @RequiresTenant @PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/nodes/trash")
public class TrashController {
 private final TrashService service;
 public TrashController(TrashService service){this.service=service;}
 @GetMapping public String list(Model model,HttpServletResponse response,@RequestParam(defaultValue="0") String page){
  response.setHeader("Cache-Control","no-store");var all=service.listing();int pages=Math.max(1,(all.entries().size()+99)/100),number=0;
  try{number=Math.max(0,Math.min(pages-1,Integer.parseInt(page)));}catch(NumberFormatException ignored){}
  model.addAttribute("trash",new TrashSql.Listing(all.rootId(),all.entries().subList(number*100,Math.min(all.entries().size(),number*100+100))));
  model.addAttribute("trashPage",number);model.addAttribute("trashPages",pages);model.addAttribute("trashTotal",all.entries().size());model.addAttribute("trashEnabled",service.enabled());return "node/trash";
 }
 @GetMapping("/{id}") public String review(@PathVariable long id,@RequestParam String _tenantView,Model model,HttpServletResponse response){response.setHeader("Cache-Control","no-store");model.addAttribute("entry",service.entry(id));return "node/trashEntry";}
 @GetMapping("/{id}/purge") public String confirmPurge(@PathVariable long id,@RequestParam String _tenantView,Model model,HttpServletResponse response){response.setHeader("Cache-Control","no-store");var preview=service.purgePreview(id);model.addAttribute("entry",preview.entry());model.addAttribute("purgeRevision",preview.revision());return "node/trashPurge";}
 @PostMapping("/{id}/restore") public String restore(@PathVariable long id,@RequestParam String revision,@RequestParam String destination,@RequestParam String _tenantView){if(!java.util.Set.of("original","root").contains(destination))throw TrashSql.problem(HttpStatus.BAD_REQUEST,"trash.invalid");long node=service.restore(id,revision,"root".equals(destination));return "redirect:/tree?nodeId="+node;}
 @PostMapping("/{id}/purge") public String purge(@PathVariable long id,@RequestParam String revision,@RequestParam(defaultValue="false") boolean confirmed,@RequestParam String _tenantView){service.purge(id,revision,confirmed);return "redirect:/nodes/trash";}
 @ExceptionHandler(ResponseStatusException.class) public String problem(ResponseStatusException error,Model model,HttpServletResponse response){response.setStatus(error.getStatusCode().value());response.setHeader("Cache-Control","no-store");String key=error.getReason();model.addAttribute("trashError",key!=null&&key.startsWith("trash.")?key:"trash.changed");return "node/trashError";}
}
