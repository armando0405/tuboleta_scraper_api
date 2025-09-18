package com.armando0405.tuboletascraper.service;

import com.armando0405.tuboletascraper.dao.entity.ConsultaInstantanea;
import com.armando0405.tuboletascraper.dao.entity.RegistroCambios;
import com.armando0405.tuboletascraper.dao.entity.ShowInstantanea;
import com.armando0405.tuboletascraper.dao.repository.ConsultaInstantaneaRepository;
import com.armando0405.tuboletascraper.dao.repository.RegistroCambiosRepository;
import com.armando0405.tuboletascraper.dao.repository.ShowInstantaneaRepository;
import com.armando0405.tuboletascraper.exception.ScrapingException;
import com.armando0405.tuboletascraper.dao.entity.ApiResponse;
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

    // ✅ MÉTODO QUE LLAMA EL CONTROLLER
    @Transactional
    public Map<String, Object> createNewSnapshot() {
        log.info("Iniciando creación de nuevo snapshot");

        try {
            ApiResponse result = procesarScraping();

            Map<String, Object> response = new HashMap<>();
            response.put("hasChanges", result.isHasChanges());
            response.put("message", result.getMessage());
            response.put("totalShows", result.getTotalShows());
            response.put("totalChanges", result.getTotalChanges());
            response.put("changes", result.getChanges());
            response.put("lastChecked", result.getLastChecked());
            response.put("shows", result.getShows());

            return response;

        } catch (Exception e) {
            log.error("Error creando snapshot", e);
            throw new ScrapingException("Error creando snapshot", e);
        }
    }

    // ✅ MÉTODO QUE LLAMA EL CONTROLLER
    public Map<String, Object> getLatestSnapshot() {
        log.info("Obteniendo último snapshot");

        try {
            Optional<ConsultaInstantanea> ultimoSnapshot = consultaRepository.findTopByOrderByFechaHoraDesc();

            if (!ultimoSnapshot.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "No hay snapshots disponibles");
                response.put("totalShows", 0);
                response.put("shows", new ArrayList<>());
                return response;
            }

            ConsultaInstantanea snapshot = ultimoSnapshot.get();
            List<ShowInstantanea> showsEntities = showRepository
                    .findByConsultaInstantaneaIdOrderByTitulo(snapshot.getId());

            List<Show> shows = showsEntities.stream()
                    .map(this::convertirAShow)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("id", snapshot.getId());
            response.put("fechaHora", snapshot.getFechaHora());
            response.put("totalShows", snapshot.getTotalShows());
            response.put("hashContenido", snapshot.getHashContenido());
            response.put("tiempoEjecucionMs", snapshot.getTiempoEjecucionMs());
            response.put("shows", shows);

            return response;

        } catch (Exception e) {
            log.error("Error obteniendo último snapshot", e);
            throw new ScrapingException("Error obteniendo último snapshot", e);
        }
    }

    // ✅ MÉTODO QUE LLAMA EL CONTROLLER
    public Map<String, Object> compareWithPreviousSnapshot() {
        log.info("Comparando con snapshot anterior");

        try {
            List<ConsultaInstantanea> snapshots = consultaRepository
                    .findTopNByOrderByFechaHoraDesc(2);

            if (snapshots.size() < 2) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "No hay suficientes snapshots para comparar");
                response.put("totalChanges", 0);
                response.put("changes", new ArrayList<>());
                return response;
            }

            ConsultaInstantanea snaphotNuevo = snapshots.get(0);
            ConsultaInstantanea snapshotAnterior = snapshots.get(1);

            List<String> cambios = detectarCambios(snapshotAnterior, snaphotNuevo);

            Map<String, Object> response = new HashMap<>();
            response.put("message", cambios.isEmpty() ? "No hay cambios" : "Cambios detectados");
            response.put("totalChanges", cambios.size());
            response.put("changes", cambios);
            response.put("snapshotAnterior", snapshotAnterior.getFechaHora());
            response.put("snapshotNuevo", snaphotNuevo.getFechaHora());

            return response;

        } catch (Exception e) {
            log.error("Error comparando snapshots", e);
            throw new ScrapingException("Error comparando snapshots", e);
        }
    }

    // 🔧 MÉTODO INTERNO PRINCIPAL
    @Transactional
    public ApiResponse procesarScraping() {
        long startTime = System.currentTimeMillis();
        log.info("Iniciando proceso de scraping con validación de cambios");

        try {
            // 1. Hacer scraping actual
            List<Show> showsActuales = scrapingService.scrapeShows();
            String hashActual = generarHash(showsActuales);

            log.debug("Hash generado para shows actuales: {}", hashActual);

            // 2. Obtener último snapshot de la BD
            Optional<ConsultaInstantanea> ultimoSnapshot =
                    consultaRepository.findTopByOrderByFechaHoraDesc();

            // 3. Comparar si hay cambios
            if (ultimoSnapshot.isPresent() &&
                    hashActual.equals(ultimoSnapshot.get().getHashContenido())) {

                log.info("No se detectaron cambios desde {}", ultimoSnapshot.get().getFechaHora());
                return crearRespuestaSinCambios(ultimoSnapshot.get(), startTime);
            }

            // 4. HAY CAMBIOS - Guardar nuevo snapshot
            log.info("Cambios detectados! Guardando nuevo snapshot...");
            ConsultaInstantanea nuevoSnapshot = guardarNuevoSnapshot(showsActuales, hashActual, startTime);

            // 5. Detectar cambios específicos (si no es la primera vez)
            List<String> cambiosDetectados = new ArrayList<>();
            if (ultimoSnapshot.isPresent()) {
                cambiosDetectados = detectarCambios(ultimoSnapshot.get(), nuevoSnapshot);
                guardarRegistroCambios(ultimoSnapshot.get(), nuevoSnapshot, cambiosDetectados);
                log.info("Detectados {} cambios específicos", cambiosDetectados.size());
            } else {
                log.info("Primera ejecución - no hay cambios anteriores para comparar");
            }

            // 6. Crear respuesta con cambios
            return crearRespuestaConCambios(nuevoSnapshot, cambiosDetectados, startTime);

        } catch (Exception e) {
            log.error("Error procesando scraping", e);
            throw new ScrapingException("Error procesando scraping", e);
        }
    }

    private String generarHash(List<Show> shows) {
        if (shows.isEmpty()) {
            return DigestUtils.md5Hex("empty");
        }

        // Ordenar shows para hash consistente
        String contenido = shows.stream()
                .sorted(Comparator.comparing(Show::getShowUniqueId))
                .map(show -> String.format("%s|%s|%s|%s",
                        show.getShowUniqueId(),
                        show.getTitulo(),
                        show.getVenue(),
                        show.getFechaShow()))
                .collect(Collectors.joining(","));

        String hash = DigestUtils.md5Hex(contenido);
        log.debug("Contenido para hash: {}", contenido.substring(0, Math.min(100, contenido.length())) + "...");

        return hash;
    }

    @Transactional
    private ConsultaInstantanea guardarNuevoSnapshot(List<Show> shows, String hash, long startTime) {
        // Guardar consulta principal
        ConsultaInstantanea consulta = ConsultaInstantanea.builder()
                .fechaHora(LocalDateTime.now())
                .totalShows(shows.size())
                .hashContenido(hash)
                .urlConsulta(searchUrl)
                .tiempoEjecucionMs(System.currentTimeMillis() - startTime)
                .build();

        ConsultaInstantanea savedConsulta = consultaRepository.save(consulta);
        log.debug("Consulta guardada con ID: {}", savedConsulta.getId());

        // Guardar shows individuales
        List<ShowInstantanea> showsEntities = shows.stream()
                .map(show -> convertirAEntity(show, savedConsulta))
                .collect(Collectors.toList());

        List<ShowInstantanea> savedShows = showRepository.saveAll(showsEntities);
        log.debug("Guardados {} shows en BD", savedShows.size());

        // Cargar shows en la consulta para uso posterior
        savedConsulta.setShows(savedShows);

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

        // Cargar shows si no están cargados
        List<ShowInstantanea> showsAnteriores = anterior.getShows();
        if (showsAnteriores == null || showsAnteriores.isEmpty()) {
            showsAnteriores = showRepository.findByConsultaInstantaneaIdOrderByTitulo(anterior.getId());
        }

        List<ShowInstantanea> showsNuevos = nueva.getShows();
        if (showsNuevos == null || showsNuevos.isEmpty()) {
            showsNuevos = showRepository.findByConsultaInstantaneaIdOrderByTitulo(nueva.getId());
        }

        // Convertir a Maps para comparación eficiente
        Map<String, ShowInstantanea> mapaAnterior = showsAnteriores.stream()
                .collect(Collectors.toMap(ShowInstantanea::getShowIdUnico, s -> s));

        Map<String, ShowInstantanea> mapaNuevo = showsNuevos.stream()
                .collect(Collectors.toMap(ShowInstantanea::getShowIdUnico, s -> s));

        // Detectar shows agregados
        for (String showId : mapaNuevo.keySet()) {
            if (!mapaAnterior.containsKey(showId)) {
                ShowInstantanea show = mapaNuevo.get(showId);
                cambios.add(String.format("Agregado: %s en %s - %s",
                        show.getTitulo(), show.getCiudad(), show.getFechaShow()));
            }
        }

        // Detectar shows eliminados
        for (String showId : mapaAnterior.keySet()) {
            if (!mapaNuevo.containsKey(showId)) {
                ShowInstantanea show = mapaAnterior.get(showId);
                cambios.add(String.format("Eliminado: %s en %s",
                        show.getTitulo(), show.getCiudad()));
            }
        }

        // Detectar shows modificados
        for (String showId : mapaNuevo.keySet()) {
            if (mapaAnterior.containsKey(showId)) {
                ShowInstantanea anteriorShow = mapaAnterior.get(showId);
                ShowInstantanea nuevoShow = mapaNuevo.get(showId);

                // Comparar fecha
                if (!Objects.equals(anteriorShow.getFechaShow(), nuevoShow.getFechaShow())) {
                    cambios.add(String.format("Modificado: %s en %s - fecha cambió de %s a %s",
                            nuevoShow.getTitulo(), nuevoShow.getCiudad(),
                            anteriorShow.getFechaShow(), nuevoShow.getFechaShow()));
                }

                // Comparar venue
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
        if (cambios.isEmpty()) {
            return;
        }

        RegistroCambios registro = RegistroCambios.builder()
                .consultaAnterior(anterior)
                .consultaNueva(nueva)
                .resumenCambios(String.join("; ", cambios))
                .totalCambios(cambios.size())
                .build();

        cambiosRepository.save(registro);
        log.debug("Registro de cambios guardado con {} modificaciones", cambios.size());
    }

    private ApiResponse crearRespuestaSinCambios(ConsultaInstantanea ultimoSnapshot, long startTime) {
        // Cargar shows del último snapshot
        List<ShowInstantanea> showsEntities = showRepository
                .findByConsultaInstantaneaIdOrderByTitulo(ultimoSnapshot.getId());

        List<Show> shows = showsEntities.stream()
                .map(this::convertirAShow)
                .collect(Collectors.toList());

        return ApiResponse.builder()
                .hasChanges(false)
                .message(String.format("No hay cambios desde %s", ultimoSnapshot.getFechaHora()))
                .totalShows(shows.size())
                .lastChecked(ultimoSnapshot.getFechaHora())
                .executionTimeMs(System.currentTimeMillis() - startTime)
                .shows(shows)
                .build();
    }

    private ApiResponse crearRespuestaConCambios(ConsultaInstantanea nuevoSnapshot, List<String> cambios, long startTime) {
        List<Show> shows = nuevoSnapshot.getShows().stream()
                .map(this::convertirAShow)
                .collect(Collectors.toList());

        return ApiResponse.builder()
                .hasChanges(true)
                .message("¡Cambios detectados!")
                .totalShows(shows.size())
                .totalChanges(cambios.size())
                .changes(cambios)
                .lastChecked(nuevoSnapshot.getFechaHora())
                .executionTimeMs(nuevoSnapshot.getTiempoEjecucionMs())
                .shows(shows)
                .build();
    }

    private Show convertirAShow(ShowInstantanea entity) {
        return Show.builder()
                .showUniqueId(entity.getShowIdUnico())
                .titulo(entity.getTitulo())
                .venue(entity.getVenue())
                .ciudad(entity.getCiudad())
                .fechaShow(entity.getFechaShow())
                .horaShow(entity.getHoraShow())
                .urlFuente(entity.getUrlFuente())
                .build();
    }

    // Métodos utilitarios para estadísticas
    public long getTotalSnapshots() {
        return consultaRepository.count();
    }

    public long getTotalCambiosDetectados() {
        return cambiosRepository.count();
    }

    public Optional<ConsultaInstantanea> getUltimoSnapshot() {
        return consultaRepository.findTopByOrderByFechaHoraDesc();
    }
}