package com.turkerozturk.shutdown;

import java.util.UUID;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public, source-independent readiness only; a new context gets a new opaque identity. */
@RestController
public class ApplicationReadiness {
    private final String instance = UUID.randomUUID().toString();
    private volatile boolean ready;
    public record Status(String instance, boolean ready) { }
    public String instance() { return instance; }
    @EventListener(ApplicationReadyEvent.class)
    public void ready() { ready = true; }
    @GetMapping("/application-ready")
    public ResponseEntity<Status> status() {
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(new Status(instance, ready));
    }
}
