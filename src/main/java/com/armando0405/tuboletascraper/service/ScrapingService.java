package com.armando0405.tuboletascraper.service;


import com.armando0405.tuboletascraper.dao.entity.Show;
import com.armando0405.tuboletascraper.exception.ScrapingException;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ScrapingService {

    @Value("${scraping.tuboleta.base-url}")
    private String baseUrl;

    @Value("${scraping.tuboleta.search-url}")
    private String searchUrl;

    @Value("${scraping.tuboleta.user-agent}")
    private String userAgent;

    @Value("${scraping.tuboleta.timeout}")
    private int timeout;

    // Mapeo de meses en español
    private static final Map<String, String> MESES = Map.ofEntries(
            Map.entry("Ene", "01"), Map.entry("Feb", "02"), Map.entry("Mar", "03"),
            Map.entry("Abr", "04"), Map.entry("May", "05"), Map.entry("Jun", "06"),
            Map.entry("Jul", "07"), Map.entry("Ago", "08"), Map.entry("Sep", "09"),
            Map.entry("Oct", "10"), Map.entry("Nov", "11"), Map.entry("Dic", "12")
    );

    public List<Show> scrapeShows() {
        log.info("Iniciando scraping de shows de Fucks News");

        try {
            // 1. Conectar a TuBoleta
            Document doc = conectarTuBoleta();

            // 2. Extraer shows
            Elements showContainers = doc.select("article.bg-grey-light");
            log.info("Encontrados {} contenedores de shows", showContainers.size());

            // 3. Procesar cada show
            List<Show> shows = showContainers.stream()
                    .map(this::extraerShow)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            log.info("Shows extraídos exitosamente: {}", shows.size());
            return shows;

        } catch (Exception e) {
            log.error("Error durante el scraping", e);
            throw new ScrapingException("Error al hacer scraping de TuBoleta", e);
        }
    }

    private Document conectarTuBoleta() throws Exception {
        String fullUrl = baseUrl + searchUrl;
        log.debug("Conectando a: {}", fullUrl);

        Document doc = Jsoup.connect(fullUrl)
                .userAgent(userAgent)
                .timeout(timeout)
                .get();

        log.debug("Conexión exitosa. Título de la página: {}", doc.title());
        return doc;
    }

    private Show extraerShow(Element showElement) {
        try {
            // Extraer título
            String titulo = extraerTitulo(showElement);
            if (titulo == null || !titulo.toLowerCase().contains("fucks news")) {
                log.debug("Show descartado, no es de Fucks News: {}", titulo);
                return null;
            }

            // Extraer venue
            String venue = extraerVenue(showElement);

            // Extraer ciudad
            String ciudad = extraerCiudad(showElement);

            // Extraer fecha
            LocalDate fechaShow = extraerFecha(showElement);

            // Extraer URL
            String urlFuente = extraerUrl(showElement);

            // Crear show
            Show show = Show.builder()
                    .titulo(titulo.trim())
                    .venue(venue != null ? venue.trim() : "")
                    .ciudad(ciudad != null ? ciudad.trim() : "")
                    .fechaShow(fechaShow)
                    .urlFuente(urlFuente)
                    //.rawHtml(showElement.outerHtml()) //habilitar unicamente para debug guarda todo el html del show
                    .build();

            // Generar ID único
            show.generateUniqueId();

            log.debug("Show extraído: {} - {} - {}", show.getTitulo(), show.getVenue(), show.getFechaShow());
            return show;

        } catch (Exception e) {
            log.warn("Error extrayendo show: {}", e.getMessage());
            return null;
        }
    }

    private String extraerTitulo(Element showElement) {
        // Selector principal para el título
        Element tituloElement = showElement.selectFirst(".content-info .fs-8.fw-bold.mb-1 span");
        if (tituloElement != null) {
            return tituloElement.text();
        }

        // Fallback: buscar cualquier elemento con texto que contenga "FUCKS NEWS"
        Elements posiblesTitulos = showElement.select("span:contains(FUCKS NEWS)");
        if (!posiblesTitulos.isEmpty()) {
            return posiblesTitulos.first().text();
        }

        return null;
    }

    private String extraerVenue(Element showElement) {
        // Los venues están en los elementos con class "text-grey"
        Elements elementosGrises = showElement.select(".content-info .text-grey span");

        // El primer elemento gris es el venue
        if (!elementosGrises.isEmpty()) {
            return elementosGrises.get(0).text();
        }

        return null;
    }

    private String extraerCiudad(Element showElement) {
        // Los venues están en los elementos con class "text-grey"
        Elements elementosGrises = showElement.select(".content-info .text-grey span");

        // El segundo elemento gris es la ciudad
        if (elementosGrises.size() >= 2) {
            return elementosGrises.get(1).text();
        }

        return null;
    }

    private LocalDate extraerFecha(Element showElement) {
        try {
            // Extraer día
            Element diaElement = showElement.selectFirst(".content-date .fs-5.fw-bold.lh-1");
            String dia = diaElement != null ? diaElement.text() : "";

            // Extraer mes
            Element mesElement = showElement.selectFirst(".content-date .fs-8.fw-bold");
            String mes = mesElement != null ? mesElement.text() : "";

            // Si no encontramos día y mes por separado, buscar formato conjunto
            if (dia.isEmpty() || mes.isEmpty()) {
                Element fechaCompacta = showElement.selectFirst(".content-date .fs-7.fw-bold");
                if (fechaCompacta != null) {
                    String fechaTexto = fechaCompacta.text(); // "23 Sep"
                    String[] partes = fechaTexto.split("\\s+");
                    if (partes.length >= 2) {
                        dia = partes[0];
                        mes = partes[1];
                    }
                }
            }

            return parsearFecha(dia, mes);

        } catch (Exception e) {
            log.warn("Error extrayendo fecha: {}", e.getMessage());
            return null;
        }
    }

    private LocalDate parsearFecha(String dia, String mes) {
        if (dia.isEmpty() || mes.isEmpty()) {
            return null;
        }

        // Año actual (asumimos que todos los eventos son del año actual o siguiente)
        int año = LocalDate.now().getYear();

        // Convertir mes de texto a número
        String mesNumero = MESES.get(mes);
        if (mesNumero == null) {
            log.warn("Mes no reconocido: {}", mes);
            return null;
        }

        // Parsear fecha
        String fechaString = String.format("%04d-%s-%02d", año, mesNumero, Integer.parseInt(dia));
        LocalDate fecha = LocalDate.parse(fechaString, DateTimeFormatter.ISO_LOCAL_DATE);

        // Si la fecha ya pasó, probablemente es del año siguiente
        if (fecha.isBefore(LocalDate.now())) {
            fecha = fecha.plusYears(1);
        }

        return fecha;
    }

    private String extraerUrl(Element showElement) {
        Element linkElement = showElement.selectFirst("a.content-link-container");
        if (linkElement != null) {
            String href = linkElement.attr("href");
            if (href.startsWith("/")) {
                return baseUrl + href;
            }
            return href;
        }
        return null;
    }
}