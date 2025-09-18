package com.armando0405.tuboletascraper.controller;

import com.armando0405.tuboletascraper.dao.entity.Show;
import com.armando0405.tuboletascraper.service.ScrapingService;
import com.armando0405.tuboletascraper.service.SnapshotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ShowController {

    @Autowired
    private ScrapingService scrapingService;

    @Autowired
    private SnapshotService snapshotService;

    @GetMapping("/scraping")
    public ResponseEntity<Map<String, Object>> testScraping() {
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

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "API funcionando correctamente");
        response.put("timestamp", java.time.LocalDateTime.now().toString());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createSnapshot() {
        try {
            long startTime = System.currentTimeMillis();

            // Crear nuevo snapshot y obtener cambios
            Map<String, Object> changes = snapshotService.createNewSnapshot();

            long executionTime = System.currentTimeMillis() - startTime;

            // Preparar respuesta
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Snapshot creado exitosamente");
            response.put("executionTimeMs", executionTime);
            response.put("changes", changes);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error al crear snapshot: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/latest")
    public ResponseEntity<Map<String, Object>> getLatestSnapshot() {
        try {
            Map<String, Object> snapshot = snapshotService.getLatestSnapshot();
            return ResponseEntity.ok(snapshot);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error al obtener último snapshot: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/compare")
    public ResponseEntity<Map<String, Object>> compareWithPrevious() {
        try {
            Map<String, Object> changes = snapshotService.compareWithPreviousSnapshot();
            return ResponseEntity.ok(changes);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error al comparar snapshots: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
