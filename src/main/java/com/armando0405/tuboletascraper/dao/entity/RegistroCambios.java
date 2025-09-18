package com.armando0405.tuboletascraper.dao.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "registro_cambios")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistroCambios {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "consulta_anterior_id")
    private ConsultaInstantanea consultaAnterior;

    @ManyToOne
    @JoinColumn(name = "consulta_nueva_id", nullable = false)
    private ConsultaInstantanea consultaNueva;

    @Lob
    @Column(name = "resumen_cambios")
    private String resumenCambios;

    @Column(name = "total_cambios")
    private Integer totalCambios;

    @CreationTimestamp
    @Column(name = "fecha_deteccion")
    private LocalDateTime fechaDeteccion;
}