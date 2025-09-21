package com.armando0405.tuboletascraper.controller;

import com.armando0405.tuboletascraper.dao.entity.Show;
import com.armando0405.tuboletascraper.service.EmailNotificationService;
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

    @Autowired(required = false)
    private EmailNotificationService emailNotificationService;

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

    @GetMapping("/test-email")
    public ResponseEntity<Map<String, Object>> testEmail() {
        if (emailNotificationService == null) {
            Map<String, Object> response = Map.of(
                    "success", false,
                    "message", "Servicio de correo no habilitado",
                    "tip", "Habilitar en application.yml: notifications.email.enabled=true"
            );
            return ResponseEntity.badRequest().body(response);
        }

        try {
            emailNotificationService.enviarCorreoDePrueba();

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Correo de prueba enviado exitosamente",
                    "destinatario", "iu443805@gmail.com",
                    "timestamp", java.time.LocalDateTime.now().toString()
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                    "success", false,
                    "message", "Error enviando correo de prueba: " + e.getMessage(),
                    "error", e.getClass().getSimpleName()
            );
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/test-primera-ejecucion")
    public ResponseEntity<Map<String, Object>> testPrimeraEjecucion() {
        if (emailNotificationService == null) {
            Map<String, Object> response = Map.of(
                    "success", false,
                    "message", "Servicio de correo no habilitado"
            );
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // Simular cambios para probar el correo
            List<String> cambiosFicticios = List.of(
                    "Agregado: FUCKS NEWS en Teatro Metropolitan - Bogotá - 2024-03-15",
                    "Modificado: FUCKS NEWS en Royal Center - Bogotá - fecha cambió de 2024-03-10 a 2024-03-12",
                    "Eliminado: FUCKS NEWS en Movistar Arena - Bogotá"
            );

            emailNotificationService.enviarNotificacionPrimeraEjecucion(5);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Correos de cambios y primera ejecución enviados exitosamente",
                    "cambiosSimulados", cambiosFicticios,
                    "totalShows", 7,
                    "simulacion", "5 shows encontrados",
                    "timestamp", java.time.LocalDateTime.now().toString()
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                    "success", false,
                    "message", "Error enviando correo: " + e.getMessage()
            );
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/test-cambios-email")
    public ResponseEntity<Map<String, Object>> testCambiosEmail() {
        if (emailNotificationService == null) {
            Map<String, Object> response = Map.of(
                    "success", false,
                    "message", "Servicio de correo no habilitado"
            );
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // Simular cambios para probar el correo
            List<String> cambiosFicticios = List.of(
                    "Agregado: FUCKS NEWS en Teatro Metropolitan - Bogotá - 2024-03-15",
                    "Modificado: FUCKS NEWS en Royal Center - Bogotá - fecha cambió de 2024-03-10 a 2024-03-12",
                    "Eliminado: FUCKS NEWS en Movistar Arena - Bogotá"
            );

            emailNotificationService.enviarNotificacionCambios(cambiosFicticios, 7);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Correo de cambios enviado exitosamente",
                    "cambiosSimulados", cambiosFicticios,
                    "totalShows", 7,
                    "timestamp", java.time.LocalDateTime.now().toString()
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                    "success", false,
                    "message", "Error enviando correo: " + e.getMessage(),
                    "error", e.getClass().getSimpleName()
            );
            return ResponseEntity.status(500).body(response);
        }
    }

}