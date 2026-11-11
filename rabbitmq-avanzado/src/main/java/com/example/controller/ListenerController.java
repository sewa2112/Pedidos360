package com.example.controller;

import com.example.service.ListenerManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/listeners")
@CrossOrigin(originPatterns = "*")
public class ListenerController {

    private final ListenerManagementService listenerService;

    public ListenerController(ListenerManagementService listenerService) {
        this.listenerService = listenerService;
    }

    @GetMapping
    public ResponseEntity<Object> getAllListeners() {
        return ResponseEntity.ok(listenerService.getAllListenerIds());
    }

    @PostMapping("/{listenerId}/pause")
    public ResponseEntity<?> pauseListener(@PathVariable String listenerId) {
        if (listenerId == null || listenerId.isBlank()) {
            throw new IllegalArgumentException("El listenerId no puede estar vacío");
        }
        boolean found = listenerService.pauseListener(listenerId.trim());
        if (!found) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("message", "Listener pausado: " + listenerId.trim(), "listenerId", listenerId.trim()));
    }

    @PostMapping("/{listenerId}/resume")
    public ResponseEntity<?> resumeListener(@PathVariable String listenerId) {
        if (listenerId == null || listenerId.isBlank()) {
            throw new IllegalArgumentException("El listenerId no puede estar vacío");
        }
        boolean found = listenerService.resumeListener(listenerId.trim());
        if (!found) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("message", "Listener reanudado: " + listenerId.trim(), "listenerId", listenerId.trim()));
    }
}
