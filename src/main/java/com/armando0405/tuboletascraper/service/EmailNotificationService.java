package com.armando0405.tuboletascraper.service;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
@ConditionalOnProperty(name = "notifications.email.enabled", havingValue = "true", matchIfMissing = false)
public class EmailNotificationService {

    @Value("${SENDGRID_API_KEY}")
    private String sendGridApiKey;

    @Value("${notifications.email.from}")
    private String fromEmail;

    @Value("${notifications.email.to}")
    private String toEmail;

    @Value("${notifications.email.subject-prefix}")
    private String subjectPrefix;

    @Value("${notifications.email.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${notifications.email.retry.delay-seconds:30}")
    private int retryDelaySeconds;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * 🏁 ENVIAR NOTIFICACIÓN DE PRIMERA EJECUCIÓN
     */
    public void enviarNotificacionPrimeraEjecucion(int totalShows) {
        try {
            log.info("📧 Enviando notificación de primera ejecución...");

            String subject = String.format("%s Sistema iniciado - %d shows encontrados",
                    subjectPrefix, totalShows);

            String htmlContent = generarHTMLPrimeraEjecucion(totalShows);

            enviarCorreoConReintentos(subject, htmlContent);

            log.info("✅ Notificación de primera ejecución enviada exitosamente");

        } catch (Exception e) {
            log.error("❌ Error enviando notificación de primera ejecución", e);
        }
    }

    /**
     * 🚨 ENVIAR NOTIFICACIÓN DE CAMBIOS DETECTADOS
     */
    public void enviarNotificacionCambios(List<String> cambios, int totalShows) {
        if (cambios == null || cambios.isEmpty()) {
            log.debug("📧 No hay cambios para notificar por correo");
            return;
        }

        try {
            log.info("📧 Enviando notificación de cambios detectados...");

            String subject = String.format("%s %d cambios detectados",
                    subjectPrefix, cambios.size());

            String htmlContent = generarHTMLCambios(cambios, totalShows);

            enviarCorreoConReintentos(subject, htmlContent);

            log.info("✅ Notificación de cambios enviada exitosamente");

        } catch (Exception e) {
            log.error("❌ Error enviando notificación de cambios", e);
        }
    }

    /**
     * 📧 MÉTODO CON REINTENTOS USANDO SENDGRID API
     */
    private void enviarCorreoConReintentos(String subject, String htmlContent) throws Exception {
        for (int intento = 1; intento <= maxRetryAttempts; intento++) {
            try {
                // Extraer nombre y email del formato "Nombre <email@example.com>"
                String fromEmailClean = extraerEmail(fromEmail);
                String fromName = extraerNombre(fromEmail);

                Email from = new Email(fromEmailClean, fromName);
                Email to = new Email(toEmail);
                Content content = new Content("text/html", htmlContent);
                Mail mail = new Mail(from, subject, to, content);

                SendGrid sg = new SendGrid(sendGridApiKey);
                Request request = new Request();

                request.setMethod(Method.POST);
                request.setEndpoint("mail/send");
                request.setBody(mail.build());

                Response response = sg.api(request);

                if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                    log.info("✅ [INTENTO {}] Correo enviado exitosamente via SendGrid API", intento);
                    log.info("✅ [STATUS] {}", response.getStatusCode());
                    return; // Éxito
                } else {
                    throw new IOException("SendGrid API error: " + response.getStatusCode() + " - " + response.getBody());
                }

            } catch (IOException e) {
                log.warn("⚠️ [INTENTO {}/{}] Error enviando correo: {}",
                        intento, maxRetryAttempts, e.getMessage());

                if (intento == maxRetryAttempts) {
                    throw e; // Último intento fallido
                }

                // Esperar antes del siguiente intento
                try {
                    Thread.sleep(retryDelaySeconds * 1000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupción durante reintento", ie);
                }
            }
        }
    }

    /**
     * 🔧 EXTRAER EMAIL LIMPIO
     */
    private String extraerEmail(String fullEmail) {
        if (fullEmail.contains("<") && fullEmail.contains(">")) {
            int start = fullEmail.indexOf("<") + 1;
            int end = fullEmail.indexOf(">");
            return fullEmail.substring(start, end).trim();
        }
        return fullEmail.trim();
    }

    /**
     * 🔧 EXTRAER NOMBRE
     */
    private String extraerNombre(String fullEmail) {
        if (fullEmail.contains("<")) {
            return fullEmail.substring(0, fullEmail.indexOf("<")).trim();
        }
        return "";
    }

    /**
     * 🎨 GENERAR HTML PARA PRIMERA EJECUCIÓN
     */
    private String generarHTMLPrimeraEjecucion(int totalShows) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<title>Fucks News - Sistema Iniciado</title>");
        agregarEstilosCSS(html);
        html.append("</head>");
        html.append("<body>");

        html.append("<div class='container'>");
        html.append("<div class='header'>");
        html.append("<h1>🚀 SISTEMA INICIADO</h1>");
        html.append("<div class='subtitle'>Monitoreo de Fucks News en TuBoleta</div>");
        html.append("</div>");

        html.append("<div class='content'>");
        html.append("<div class='success-message'>");
        html.append("<h3>✅ Sistema activado exitosamente</h3>");
        html.append("<p>El monitoreo automático de shows de <strong>Fucks News</strong> ha sido iniciado.</p>");
        html.append("</div>");

        html.append("<div class='stats'>");
        html.append("<div class='stat-item'>");
        html.append("<div class='stat-number'>").append(totalShows).append("</div>");
        html.append("<div class='stat-label'>Shows Encontrados</div>");
        html.append("</div>");
        html.append("</div>");

        html.append("<div class='info-box'>");
        html.append("<h4>📋 ¿Qué significa esto?</h4>");
        html.append("<ul>");
        html.append("<li>✅ El sistema está monitoreando TuBoleta automáticamente</li>");
        html.append("<li>🔍 Detectará cambios en shows de Fucks News</li>");
        html.append("<li>📧 Te notificará por correo cuando haya cambios</li>");
        html.append("<li>⏰ Próximo monitoreo programado</li>");
        html.append("</ul>");
        html.append("</div>");

        html.append("</div>");
        agregarFooter(html);
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    /**
     * 🎨 GENERAR HTML PARA CAMBIOS DETECTADOS
     */
    private String generarHTMLCambios(List<String> cambios, int totalShows) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<title>Fucks News - Cambios Detectados</title>");
        agregarEstilosCSS(html);
        html.append("</head>");
        html.append("<body>");

        html.append("<div class='container'>");
        html.append("<div class='header header-alert'>");
        html.append("<h1>🚨 CAMBIOS DETECTADOS</h1>");
        html.append("<div class='subtitle'>Nuevas actualizaciones en shows de Fucks News</div>");
        html.append("</div>");

        html.append("<div class='content'>");

        html.append("<div class='stats'>");
        html.append("<div class='stat-item'>");
        html.append("<div class='stat-number alert'>").append(cambios.size()).append("</div>");
        html.append("<div class='stat-label'>Cambios Detectados</div>");
        html.append("</div>");
        html.append("<div class='stat-item'>");
        html.append("<div class='stat-number'>").append(totalShows).append("</div>");
        html.append("<div class='stat-label'>Total Shows Actuales</div>");
        html.append("</div>");
        html.append("</div>");

        html.append("<div class='changes-section'>");
        html.append("<h3>📋 Detalle de Cambios:</h3>");
        html.append("<div class='changes-list'>");

        for (int i = 0; i < cambios.size(); i++) {
            String cambio = cambios.get(i);
            String tipoClase = determinarTipoCambio(cambio);
            String icono = obtenerIconoCambio(cambio);

            html.append("<div class='change-item ").append(tipoClase).append("'>");
            html.append("<span class='change-icon'>").append(icono).append("</span>");
            html.append("<span class='change-number'>").append(i + 1).append(".</span>");
            html.append("<span class='change-text'>").append(cambio).append("</span>");
            html.append("</div>");
        }

        html.append("</div>");
        html.append("</div>");

        html.append("</div>");
        agregarFooter(html);
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    /**
     * 🎨 AGREGAR ESTILOS CSS
     */
    private void agregarEstilosCSS(StringBuilder html) {
        html.append("<style>");
        html.append("body { font-family: 'Arial', sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }");
        html.append(".container { max-width: 600px; margin: 0 auto; background-color: white; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.15); overflow: hidden; }");
        html.append(".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; }");
        html.append(".header.header-alert { background: linear-gradient(135deg, #ff6b6b 0%, #ee5a52 100%); }");
        html.append(".header h1 { margin: 0; font-size: 28px; font-weight: bold; }");
        html.append(".header .subtitle { margin-top: 10px; opacity: 0.9; font-size: 16px; }");
        html.append(".content { padding: 30px; }");
        html.append(".success-message { background-color: #d4edda; border: 1px solid #c3e6cb; border-radius: 8px; padding: 20px; margin-bottom: 25px; text-align: center; }");
        html.append(".success-message h3 { margin-top: 0; color: #155724; }");
        html.append(".success-message p { color: #155724; margin-bottom: 0; }");
        html.append(".stats { display: flex; justify-content: center; margin-bottom: 25px; gap: 30px; }");
        html.append(".stat-item { text-align: center; }");
        html.append(".stat-number { font-size: 36px; font-weight: bold; color: #007bff; }");
        html.append(".stat-number.alert { color: #dc3545; }");
        html.append(".stat-label { font-size: 14px; color: #6c757d; margin-top: 5px; }");
        html.append(".info-box { background-color: #e3f2fd; border: 1px solid #bbdefb; border-radius: 8px; padding: 20px; margin-top: 20px; }");
        html.append(".info-box h4 { margin-top: 0; color: #1565c0; }");
        html.append(".info-box ul { margin-bottom: 0; }");
        html.append(".info-box li { color: #1565c0; margin-bottom: 8px; }");
        html.append(".changes-section h3 { color: #333; margin-bottom: 20px; }");
        html.append(".change-item { background-color: #f8f9fa; border: 1px solid #dee2e6; border-radius: 6px; padding: 15px; margin-bottom: 10px; display: flex; align-items: center; }");
        html.append(".change-item.agregado { background-color: #d4edda; border-color: #c3e6cb; }");
        html.append(".change-item.modificado { background-color: #cce5ff; border-color: #99ccff; }");
        html.append(".change-item.eliminado { background-color: #f8d7da; border-color: #f5c6cb; }");
        html.append(".change-icon { font-size: 18px; margin-right: 10px; }");
        html.append(".change-number { font-weight: bold; margin-right: 10px; color: #666; }");
        html.append(".change-text { flex: 1; }");
        html.append(".footer { background-color: #f8f9fa; padding: 20px; text-align: center; color: #6c757d; font-size: 12px; border-top: 1px solid #dee2e6; }");
        html.append("</style>");
    }

    private void agregarFooter(StringBuilder html) {
        html.append("<div class='footer'>");
        html.append("🤖 Mensaje generado automáticamente por Fucks News Bot<br>");
        html.append("📅 ").append(LocalDateTime.now().format(formatter)).append("<br>");
        html.append("💻 Sistema de Monitoreo de TuBoleta");
        html.append("</div>");
    }

    private String determinarTipoCambio(String cambio) {
        if (cambio.startsWith("Agregado:")) return "agregado";
        if (cambio.startsWith("Modificado:")) return "modificado";
        if (cambio.startsWith("Eliminado:")) return "eliminado";
        return "general";
    }

    private String obtenerIconoCambio(String cambio) {
        if (cambio.startsWith("Agregado:")) return "➕";
        if (cambio.startsWith("Modificado:")) return "✏️";
        if (cambio.startsWith("Eliminado:")) return "➖";
        return "🔸";
    }

    /**
     * 🧪 MÉTODO DE PRUEBA
     */
    public void enviarCorreoDePrueba() {
        try {
            String subject = subjectPrefix + " Prueba de Configuración";
            String content = generarHTMLPrueba();

            enviarCorreoConReintentos(subject, content);
            log.info("✅ Correo de prueba enviado exitosamente");

        } catch (Exception e) {
            log.error("❌ Error enviando correo de prueba", e);
            throw new RuntimeException("Error en configuración de correo", e);
        }
    }

    private String generarHTMLPrueba() {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<title>Fucks News - Prueba</title>");
        agregarEstilosCSS(html);
        html.append("</head>");
        html.append("<body>");
        html.append("<div class='container'>");
        html.append("<div class='header'>");
        html.append("<h1>🧪 PRUEBA DE CONFIGURACIÓN</h1>");
        html.append("<div class='subtitle'>Test del sistema de notificaciones</div>");
        html.append("</div>");
        html.append("<div class='content'>");
        html.append("<div class='success-message'>");
        html.append("<h3>✅ ¡Configuración Correcta!</h3>");
        html.append("<p>Si recibes este correo, el sistema está funcionando perfectamente.</p>");
        html.append("</div>");
        html.append("</div>");
        agregarFooter(html);
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");
        return html.toString();
    }
}