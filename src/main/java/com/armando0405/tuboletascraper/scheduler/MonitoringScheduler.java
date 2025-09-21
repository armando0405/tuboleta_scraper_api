package com.armando0405.tuboletascraper.scheduler;

import com.armando0405.tuboletascraper.service.SnapshotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class MonitoringScheduler {

    @Autowired
    private SnapshotService snapshotService;

    @Value("${scheduler.monitoring.enabled:true}")
    private boolean schedulingEnabled;

    @Value("${scheduler.monitoring.interval-minutes:30}")
    private int intervalMinutes;

    @Value("${scheduler.monitoring.initial-delay-minutes:2}")
    private int initialDelayMinutes;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 🚀 EVENTO: Se ejecuta cuando la aplicación está completamente lista
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (schedulingEnabled) {
            log.info("🤖 ==========================================");
            log.info("🤖 SCHEDULER AUTOMÁTICO CONFIGURADO");
            log.info("🤖 ==========================================");
            log.info("🤖 Estado: ACTIVADO ✅");
            log.info("🤖 Intervalo: {} minutos", intervalMinutes);
            log.info("🤖 Delay inicial: {} minutos", initialDelayMinutes);
            log.info("🤖 Próxima ejecución en: {} minutos", initialDelayMinutes);
            log.info("🤖 ==========================================");
        } else {
            log.info("⏸️ SCHEDULER AUTOMÁTICO DESACTIVADO");
        }
    }

    /**
     *  MÉTODO PRINCIPAL: Se ejecuta automáticamente cada X minutos
     */
    @Scheduled(
            initialDelayString = "#{${scheduler.monitoring.initial-delay-minutes:2} * 60 * 1000}",  // Delay inicial
            fixedRateString = "#{${scheduler.monitoring.interval-minutes:30} * 60 * 1000}"          // Intervalo
    )
    public void ejecutarMonitoreoAutomatico() {
        if (!schedulingEnabled) {
            log.debug("⏸️ [AUTO] Scheduling deshabilitado via configuración");
            return;
        }

        String timestamp = LocalDateTime.now().format(formatter);

        log.info("🤖 ==========================================");
        log.info("🤖 [AUTO] MONITOREO AUTOMÁTICO INICIADO");
        log.info("🤖 [AUTO] Timestamp: {}", timestamp);
        log.info("🤖 ==========================================");

        try {
            long startTime = System.currentTimeMillis();

            // ✅ EJECUTAR EL MONITOREO COMPLETO
            Map<String, Object> resultado = snapshotService.ejecutarMonitoreoCompleto();

            long totalTime = System.currentTimeMillis() - startTime;

            // ✅ ANALIZAR RESULTADOS
            analizarResultados(resultado, totalTime);

        } catch (Exception e) {
            log.error("❌ [AUTO] ERROR CRÍTICO en monitoreo automático", e);
            log.error("❌ [AUTO] Tipo error: {}", e.getClass().getSimpleName());
            log.error("❌ [AUTO] Mensaje: {}", e.getMessage());
        }

        // ✅ INFORMACIÓN DE PRÓXIMA EJECUCIÓN
        LocalDateTime proximaEjecucion = LocalDateTime.now().plusMinutes(intervalMinutes);
        log.info("🕐 [AUTO] Próximo monitoreo: {}", proximaEjecucion.format(formatter));
        log.info("🤖 [AUTO] ==========================================");
    }

    /**
     *  ANALIZAR Y MOSTRAR RESULTADOS
     */
    private void analizarResultados(Map<String, Object> resultado, long totalTime) {
        Boolean hayCambios = (Boolean) resultado.getOrDefault("hayCambios", false);
        Boolean esPrimeraEjecucion = (Boolean) resultado.getOrDefault("esPrimeraEjecucion", false);

        if (esPrimeraEjecucion) {
            log.info("🏁 [AUTO] PRIMERA EJECUCIÓN COMPLETADA");
            log.info("📊 [AUTO] Total shows guardados: {}", resultado.get("totalShows"));

        } else if (hayCambios) {
            log.info("🚨 [AUTO] ¡¡¡ CAMBIOS DETECTADOS AUTOMÁTICAMENTE !!!");
            log.info("🔢 [AUTO] Total cambios: {}", resultado.get("totalCambios"));

            @SuppressWarnings("unchecked")
            List<String> cambios = (List<String>) resultado.get("cambios");

            if (cambios != null && !cambios.isEmpty()) {
                log.info("📋 [AUTO] DETALLE DE CAMBIOS:");
                for (int i = 0; i < cambios.size(); i++) {
                    log.info("   🔸 [AUTO] {}. {}", i + 1, cambios.get(i));
                }
            }

            // 🔔 AQUÍ PUEDES AGREGAR NOTIFICACIONES FUTURAS
            // enviarNotificacionCambios(cambios);

        } else {
            log.info("✅ [AUTO] Sin cambios detectados en monitoreo automático");
            Object ultimaActualizacion = resultado.get("ultimaActualizacion");
            if (ultimaActualizacion != null) {
                log.info("📅 [AUTO] Última actualización: {}", ultimaActualizacion);
            }
        }

        log.info("⏱️ [AUTO] Tiempo total ejecución: {} ms", totalTime);
        log.info("🎯 [AUTO] Estado: COMPLETADO EXITOSAMENTE");
    }

    /**
     * 🔔 MÉTODO PARA FUTURAS NOTIFICACIONES (PLACEHOLDER)
     */
    private void enviarNotificacionCambios(List<String> cambios) {
        // TODO: Implementar notificaciones (Email, Slack, Telegram, etc.)
        log.info("📧 [AUTO] Enviando notificaciones de cambios... (No implementado aún)");
    }

    /**
     * 📊 MÉTODO MANUAL PARA TESTING (OPCIONAL)
     */
    public void ejecutarMonitoreoManual() {
        log.info("🔧 [MANUAL] Ejecutando monitoreo manual...");

        try {
            Map<String, Object> resultado = snapshotService.ejecutarMonitoreoCompleto();
            analizarResultados(resultado, 0);
            log.info("✅ [MANUAL] Monitoreo manual completado");

        } catch (Exception e) {
            log.error("❌ [MANUAL] Error en monitoreo manual", e);
        }
    }

    /**
     * 🛠️ MÉTODOS DE CONTROL DEL SCHEDULER
     */
    public void habilitarScheduler() {
        this.schedulingEnabled = true;
        log.info("✅ [CONTROL] Scheduler HABILITADO");
    }

    public void deshabilitarScheduler() {
        this.schedulingEnabled = false;
        log.info("⏸️ [CONTROL] Scheduler DESHABILITADO");
    }

    public boolean isSchedulerHabilitado() {
        return schedulingEnabled;
    }

    public int getIntervaloMinutos() {
        return intervalMinutes;
    }
}