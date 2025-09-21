package com.armando0405.tuboletascraper.controller;

import com.armando0405.tuboletascraper.dao.entity.Show;
import com.armando0405.tuboletascraper.service.ScrapingService;
import com.armando0405.tuboletascraper.service.SnapshotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ShowController {

    @Autowired
    private SnapshotService snapshotService;

    @Autowired
    private ScrapingService scrapingService;

    // extraccion informacion de shows
    @GetMapping("/scraping")
    public ResponseEntity<Map<String, Object>> extraerShows() {
        try {
            long startTime = System.currentTimeMillis();

            // Hacer scraping
            List<Show> shows = scrapingService.scrapeShows();

            long executionTime = System.currentTimeMillis() - startTime;

            // Preparar respuesta
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Scraping realizado exitosamente");
            response.put("totalShows", shows.size());
            response.put("executionTimeMs", executionTime);
            response.put("shows", shows);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error en scraping: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    //  UN SOLO ENDPOINT QUE HACE TODO EN SECUENCIA
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