package com.armando0405.tuboletascraper.service;

import com.armando0405.tuboletascraper.dao.entity.ConsultaInstantanea;
import com.armando0405.tuboletascraper.dao.entity.RegistroCambios;
import com.armando0405.tuboletascraper.dao.entity.ShowInstantanea;
import com.armando0405.tuboletascraper.dao.repository.ConsultaInstantaneaRepository;
import com.armando0405.tuboletascraper.dao.repository.RegistroCambiosRepository;
import com.armando0405.tuboletascraper.dao.repository.ShowInstantaneaRepository;
import com.armando0405.tuboletascraper.exception.ScrapingException;
import com.armando0405.tuboletascraper.dao.entity.Show;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SnapshotService {

    @Autowired
    private ConsultaInstantaneaRepository consultaRepository;

    @Autowired
    private ShowInstantaneaRepository showRepository;

    @Autowired
    private RegistroCambiosRepository cambiosRepository;

    @Autowired
    private ScrapingService scrapingService;

    @Value("${scraping.tuboleta.search-url}")
    private String searchUrl;

    // 🎯 MÉTODO PRINCIPAL - EJECUTA TODO EN SECUENCIA
    @Transactional
    public Map<String, Object> ejecutarMonitoreoCompleto() {
        long startTime = System.currentTimeMillis();

        log.info("========================================");
        log.info("🚀 INICIANDO MONITOREO COMPLETO DE SHOWS");
        log.info("========================================");

        try {
            // ✅ PASO 1: HACER SCRAPING
            log.info("📡 PASO 1: Consultando información de TuBoleta...");
            List<Show> showsActuales = scrapingService.scrapeShows();
            log.info("✅ Scraping completado. Shows encontrados: {}", showsActuales.size());

            // Mostrar detalles de los shows encontrados
            showsActuales.forEach(show ->
                    log.info("   📅 Show: {} en {} - {}", show.getTitulo(), show.getCiudad(), show.getFechaShow())
            );

            // ✅ PASO 2: GENERAR HASH PARA COMPARACIÓN
            log.info("🔍 PASO 2: Generando hash para comparación...");
            String hashActual = generarHash(showsActuales);
            log.info("✅ Hash generado: {}", hashActual);

            // ✅ PASO 3: BUSCAR ÚLTIMO SNAPSHOT EN BD
            log.info("🗄️ PASO 3: Consultando último snapshot en base de datos...");
            Optional<ConsultaInstantanea> ultimoSnapshot = consultaRepository.findTopByOrderByFechaHoraDesc();

            if (!ultimoSnapshot.isPresent()) {
                log.info("ℹ️ No se encontraron snapshots anteriores. Esta es la primera ejecución.");
                return ejecutarPrimeraVez(showsActuales, hashActual, startTime);
            }

            ConsultaInstantanea snapshotAnterior = ultimoSnapshot.get();
            log.info("✅ Último snapshot encontrado:");
            log.info("   📅 Fecha: {}", snapshotAnterior.getFechaHora());
            log.info("   🔢 Total shows: {}", snapshotAnterior.getTotalShows());
            log.info("   🔑 Hash anterior: {}", snapshotAnterior.getHashContenido());

            // ✅ PASO 4: COMPARAR HASHES
            log.info("⚖️ PASO 4: Comparando con snapshot anterior...");
            if (hashActual.equals(snapshotAnterior.getHashContenido())) {
                log.info("✅ RESULTADO: Sin cambios detectados");
                return crearRespuestaSinCambios(snapshotAnterior, startTime);
            }

            // ✅ PASO 5: HAY CAMBIOS - GUARDAR NUEVO SNAPSHOT
            log.info("🆕 PASO 5: ¡Cambios detectados! Guardando nuevo snapshot...");
            ConsultaInstantanea nuevoSnapshot = guardarNuevoSnapshot(showsActuales, hashActual, startTime);
            log.info("✅ Nuevo snapshot guardado con ID: {}", nuevoSnapshot.getId());

            // ✅ PASO 6: DETECTAR CAMBIOS ESPECÍFICOS
            log.info("🔍 PASO 6: Analizando cambios específicos...");
            List<String> cambiosDetectados = detectarCambios(snapshotAnterior, nuevoSnapshot);

            if (!cambiosDetectados.isEmpty()) {
                log.info("✅ Cambios detectados:");
                cambiosDetectados.forEach(cambio -> log.info("   🔸 {}", cambio));

                // Guardar registro de cambios
                guardarRegistroCambios(snapshotAnterior, nuevoSnapshot, cambiosDetectados);
                log.info("✅ Registro de cambios guardado en BD");
            }

            // ✅ PASO 7: CREAR RESPUESTA FINAL
            log.info("📋 PASO 7: Preparando respuesta final...");
            Map<String, Object> respuesta = crearRespuestaConCambios(cambiosDetectados, startTime);

            log.info("🎉 MONITOREO COMPLETADO EXITOSAMENTE");
            log.info("========================================");

            return respuesta;

        } catch (Exception e) {
            log.error("❌ ERROR EN MONITOREO: {}", e.getMessage(), e);
            throw new ScrapingException("Error en monitoreo completo", e);
        }
    }

    private Map<String, Object> ejecutarPrimeraVez(List<Show> shows, String hash, long startTime) {
        log.info("🏁 Primera ejecución: Guardando snapshot inicial...");

        ConsultaInstantanea primerSnapshot = guardarNuevoSnapshot(shows, hash, startTime);
        log.info("✅ Snapshot inicial guardado con ID: {}", primerSnapshot.getId());

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("esPrimeraEjecucion", true);
        respuesta.put("message", "Primera ejecución completada. Snapshot inicial guardado.");
        respuesta.put("totalShows", shows.size());
        respuesta.put("executionTimeMs", System.currentTimeMillis() - startTime);

        log.info("🎉 Primera ejecución completada exitosamente");
        return respuesta;
    }

    private Map<String, Object> crearRespuestaSinCambios(ConsultaInstantanea ultimoSnapshot, long startTime) {
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("hayCambios", false);
        respuesta.put("message", String.format("Sin cambios desde %s", ultimoSnapshot.getFechaHora()));
        respuesta.put("ultimaActualizacion", ultimoSnapshot.getFechaHora());
        respuesta.put("totalShows", ultimoSnapshot.getTotalShows());
        respuesta.put("executionTimeMs", System.currentTimeMillis() - startTime);

        return respuesta;
    }

    private Map<String, Object> crearRespuestaConCambios(List<String> cambios, long startTime) {
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("hayCambios", true);
        respuesta.put("message", "¡Cambios detectados!");
        respuesta.put("totalCambios", cambios.size());
        respuesta.put("cambios", cambios);
        respuesta.put("executionTimeMs", System.currentTimeMillis() - startTime);

        return respuesta;
    }

    private String generarHash(List<Show> shows) {
        if (shows.isEmpty()) {
            return DigestUtils.md5Hex("empty");
        }

        String contenido = shows.stream()
                .sorted(Comparator.comparing(Show::getShowUniqueId))
                .map(show -> String.format("%s|%s|%s|%s",
                        show.getShowUniqueId(),
                        show.getTitulo(),
                        show.getVenue(),
                        show.getFechaShow()))
                .collect(Collectors.joining(","));

        return DigestUtils.md5Hex(contenido);
    }

    @Transactional
    private ConsultaInstantanea guardarNuevoSnapshot(List<Show> shows, String hash, long startTime) {
        ConsultaInstantanea consulta = ConsultaInstantanea.builder()
                .fechaHora(LocalDateTime.now())
                .totalShows(shows.size())
                .hashContenido(hash)
                .urlConsulta(searchUrl)
                .tiempoEjecucionMs(System.currentTimeMillis() - startTime)
                .build();

        ConsultaInstantanea savedConsulta = consultaRepository.save(consulta);

        List<ShowInstantanea> showsEntities = shows.stream()
                .map(show -> convertirAEntity(show, savedConsulta))
                .collect(Collectors.toList());

        showRepository.saveAll(showsEntities);
        savedConsulta.setShows(showsEntities);

        return savedConsulta;
    }

    private ShowInstantanea convertirAEntity(Show show, ConsultaInstantanea consulta) {
        return ShowInstantanea.builder()
                .consultaInstantanea(consulta)
                .showIdUnico(show.getShowUniqueId())
                .titulo(show.getTitulo())
                .venue(show.getVenue())
                .ciudad(show.getCiudad())
                .fechaShow(show.getFechaShow())
                .horaShow(show.getHoraShow())
                .urlFuente(show.getUrlFuente())
                .build();
    }

    private List<String> detectarCambios(ConsultaInstantanea anterior, ConsultaInstantanea nueva) {
        List<String> cambios = new ArrayList<>();

        List<ShowInstantanea> showsAnteriores = showRepository
                .findByConsultaInstantaneaIdOrderByTitulo(anterior.getId());
        List<ShowInstantanea> showsNuevos = nueva.getShows();

        Map<String, ShowInstantanea> mapaAnterior = showsAnteriores.stream()
                .collect(Collectors.toMap(ShowInstantanea::getShowIdUnico, s -> s));

        Map<String, ShowInstantanea> mapaNuevo = showsNuevos.stream()
                .collect(Collectors.toMap(ShowInstantanea::getShowIdUnico, s -> s));

        // Shows agregados
        for (String showId : mapaNuevo.keySet()) {
            if (!mapaAnterior.containsKey(showId)) {
                ShowInstantanea show = mapaNuevo.get(showId);
                cambios.add(String.format("Agregado: %s en %s - %s",
                        show.getTitulo(), show.getCiudad(), show.getFechaShow()));
            }
        }

        // Shows eliminados
        for (String showId : mapaAnterior.keySet()) {
            if (!mapaNuevo.containsKey(showId)) {
                ShowInstantanea show = mapaAnterior.get(showId);
                cambios.add(String.format("Eliminado: %s en %s",
                        show.getTitulo(), show.getCiudad()));
            }
        }

        // Shows modificados
        for (String showId : mapaNuevo.keySet()) {
            if (mapaAnterior.containsKey(showId)) {
                ShowInstantanea anteriorShow = mapaAnterior.get(showId);
                ShowInstantanea nuevoShow = mapaNuevo.get(showId);

                if (!Objects.equals(anteriorShow.getFechaShow(), nuevoShow.getFechaShow())) {
                    cambios.add(String.format("Modificado: %s en %s - fecha cambió de %s a %s",
                            nuevoShow.getTitulo(), nuevoShow.getCiudad(),
                            anteriorShow.getFechaShow(), nuevoShow.getFechaShow()));
                }

                if (!Objects.equals(anteriorShow.getVenue(), nuevoShow.getVenue())) {
                    cambios.add(String.format("Modificado: %s en %s - venue cambió de %s a %s",
                            nuevoShow.getTitulo(), nuevoShow.getCiudad(),
                            anteriorShow.getVenue(), nuevoShow.getVenue()));
                }
            }
        }

        return cambios;
    }

    @Transactional
    private void guardarRegistroCambios(ConsultaInstantanea anterior, ConsultaInstantanea nueva, List<String> cambios) {
        if (cambios.isEmpty()) return;

        RegistroCambios registro = RegistroCambios.builder()
                .consultaAnterior(anterior)
                .consultaNueva(nueva)
                .resumenCambios(String.join("; ", cambios))
                .totalCambios(cambios.size())
                .build();

        cambiosRepository.save(registro);
    }
}