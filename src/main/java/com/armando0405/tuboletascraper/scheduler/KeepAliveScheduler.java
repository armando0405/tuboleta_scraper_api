package com.armando0405.tuboletascraper.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 💓 KEEP-ALIVE SCHEDULER
 *
 * Responsabilidad ÚNICA: Mantener la aplicación "despierta" en servidores
 * que tienen políticas de auto-sleep (como Render.com).
 *
 * Funcionamiento:
 * - Se ejecuta cada X minutos (configurable)
 * - Hace una petición HTTP GET a su propio endpoint /api/health
 * - Esto cuenta como "actividad" para el servidor
 * - El servidor no duerme la aplicación
 *
 * NO hace:
 * - Scraping
 * - Monitoreo de shows
 * - Envío de emails
 * - Lógica de negocio
 */
@Component
@Slf4j
@ConditionalOnProperty(
        name = "keepalive.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class KeepAliveScheduler {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${keepalive.interval-minutes:10}")
    private int intervalMinutes;

    @Value("${keepalive.url:http://localhost:8080}")
    private String baseUrl;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 💓 MÉTODO PRINCIPAL - Se ejecuta automáticamente cada X minutos
     *
     * Intervalo: Configurable via keepalive.interval-minutes (default: 10 min)
     *
     * Proceso:
     * 1. Obtiene la URL base del servidor
     * 2. Construye URL completa: {baseUrl}/api/health
     * 3. Hace GET request
     * 4. Loguea resultado
     */
    @Scheduled(
            initialDelayString = "#{60 * 1000}",  // Esperar 1 minuto después del startup
            fixedRateString = "#{${keepalive.interval-minutes:10} * 60 * 1000}"  // Cada X minutos
    )
    public void mantenerActivo() {
        String timestamp = LocalDateTime.now().format(formatter);

        try {
            // 1. Obtener URL del servidor
            String serverUrl = obtenerUrlServidor();
            String healthUrl = serverUrl + "/api/health";

            log.info("💓 ==========================================");
            log.info("💓 [KEEP-ALIVE] Iniciando ping");
            log.info("💓 [KEEP-ALIVE] Timestamp: {}", timestamp);
            log.info("💓 [KEEP-ALIVE] URL destino: {}", healthUrl);
            log.info("💓 ==========================================");

            // 2. Hacer petición GET
            String response = restTemplate.getForObject(healthUrl, String.class);

            // 3. Log de éxito
            log.info("✅ [KEEP-ALIVE] Ping exitoso");
            log.info("✅ [KEEP-ALIVE] Respuesta recibida: {}",
                    response != null ? response.substring(0, Math.min(100, response.length())) : "null");
            log.info("✅ [KEEP-ALIVE] Servidor manteniéndose activo");
            log.info("⏰ [KEEP-ALIVE] Próximo ping en {} minutos", intervalMinutes);
            log.info("💓 ==========================================");

        } catch (Exception e) {
            // Manejar errores sin interrumpir el flujo
            log.warn("⚠️ [KEEP-ALIVE] Error en ping (normal en desarrollo local)");
            log.warn("⚠️ [KEEP-ALIVE] Tipo error: {}", e.getClass().getSimpleName());
            log.warn("⚠️ [KEEP-ALIVE] Mensaje: {}", e.getMessage());
            log.debug("⚠️ [KEEP-ALIVE] Detalles:", e);
            log.info("⏰ [KEEP-ALIVE] Reintentará en {} minutos", intervalMinutes);
        }
    }

    /**
     * 🌐 OBTENER URL DEL SERVIDOR
     *
     * Lógica:
     * 1. Intenta obtener RENDER_EXTERNAL_URL (variable de entorno de Render.com)
     * 2. Si no existe, usa la URL configurada en application.yml
     * 3. Fallback final: http://localhost:8080
     *
     * @return URL base del servidor
     */
    private String obtenerUrlServidor() {
        // Prioridad 1: Variable de entorno RENDER_EXTERNAL_URL
        String renderUrl = System.getenv("RENDER_EXTERNAL_URL");
        if (renderUrl != null && !renderUrl.isEmpty()) {
            log.debug("💓 [KEEP-ALIVE] Usando RENDER_EXTERNAL_URL: {}", renderUrl);
            return renderUrl;
        }

        // Prioridad 2: Configuración en application.yml
        if (baseUrl != null && !baseUrl.isEmpty()) {
            log.debug("💓 [KEEP-ALIVE] Usando baseUrl configurado: {}", baseUrl);
            return baseUrl;
        }

        // Prioridad 3: Fallback a localhost (desarrollo)
        log.debug("💓 [KEEP-ALIVE] Usando fallback localhost");
        return "http://localhost:8080";
    }

    /**
     * 📊 INFORMACIÓN DEL KEEP-ALIVE
     *
     * Método público para debugging o monitoreo
     */
    public String getEstado() {
        return String.format(
                "Keep-Alive activo - Intervalo: %d minutos - URL: %s",
                intervalMinutes,
                obtenerUrlServidor()
        );
    }
}