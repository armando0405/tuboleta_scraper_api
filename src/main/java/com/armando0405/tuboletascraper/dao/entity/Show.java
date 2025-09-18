package com.armando0405.tuboletascraper.dao.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Show {
    private String showUniqueId;
    private String titulo;
    private String venue;
    private String ciudad;
    private LocalDate fechaShow;
    private LocalTime horaShow;
    private String urlFuente;
    private String rawHtml;

    // Método para generar ID único
    public void generateUniqueId() {
        String base = titulo + "|" + venue + "|" + ciudad;
        this.showUniqueId = base.toLowerCase()
                .replaceAll("[^a-z0-9|\\s]", "") // Solo letras, números, pipes y espacios
                .replaceAll("\\s+", "-")         // Espacios → guiones
                .replaceAll("\\|+", "|")         // Múltiples pipes → uno solo
                .replaceAll("^-+|-+$", "");      // Quitar guiones al inicio/final
    }
}