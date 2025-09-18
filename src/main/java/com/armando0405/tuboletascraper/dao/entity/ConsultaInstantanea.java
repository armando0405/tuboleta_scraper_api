package com.armando0405.tuboletascraper.dao.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "consulta_instantanea")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultaInstantanea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "total_shows", nullable = false)
    private Integer totalShows;

    @Column(name = "hash_contenido", length = 64, unique = true, nullable = false)
    private String hashContenido;

    @Column(name = "url_consulta", length = 500, nullable = false)
    private String urlConsulta;

    @Column(name = "tiempo_ejecucion_ms")
    private Long tiempoEjecucionMs;

    @OneToMany(mappedBy = "consultaInstantanea", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ShowInstantanea> shows = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;
}