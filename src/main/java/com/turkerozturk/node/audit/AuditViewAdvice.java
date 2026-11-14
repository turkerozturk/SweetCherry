package com.turkerozturk.node.audit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
@ControllerAdvice
public class AuditViewAdvice {
 private final boolean enabled;public AuditViewAdvice(@Value("${myapp.audit.enabled:false}") boolean enabled){this.enabled=enabled;}
 @ModelAttribute("auditEnabled") public boolean enabled(){return enabled;}
}
