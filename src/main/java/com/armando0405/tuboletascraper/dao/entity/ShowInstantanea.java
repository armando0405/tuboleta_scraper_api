package com.armando0405.tuboletascraper.dao.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "show_instantanea")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowInstantanea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consulta_id", nullable = false)
    private ConsultaInstantanea consultaInstantanea;

    @Column(name = "show_id_unico", nullable = false, length = 200)
    private String showIdUnico;

    @Column(nullable = false, length = 300)
    private String titulo;

    @Column(length = 200)
    private String venue;

    @Column(length = 100)
    private String ciudad;

    @Column(name = "fecha_show")
    private LocalDate fechaShow;

    @Column(name = "hora_show")
    private LocalTime horaShow;

    @Column(name = "url_fuente", length = 500)
    private String urlFuente;

    @CreationTimestamp
    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;
}