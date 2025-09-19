package com.armando0405.tuboletascraper.controller;

import com.armando0405.tuboletascraper.service.SnapshotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ShowController {

    @Autowired
    private SnapshotService snapshotService;

    // ✅ UN SOLO ENDPOINT QUE HACE TODO EN SECUENCIA
    @GetMapping("/monitor")
    public ResponseEntity<Map<String, Object>> monitorearShows() {
        try {
            // Ejecuta toda la secuencia: scraping → validación → guardado → respuesta
            Map<String, Object> resultado = snapshotService.ejecutarMonitoreoCompleto();
            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "Error en monitoreo: " + e.getMessage(),
                    "error", e.getClass().getSimpleName()
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // ✅ ENDPOINT DE SALUD (OPCIONAL)
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = Map.of(
                "status", "UP",
                "message", "API funcionando correctamente",
                "timestamp", java.time.LocalDateTime.now().toString()
        );
        return ResponseEntity.ok(response);
    }
}