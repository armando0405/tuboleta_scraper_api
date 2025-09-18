package com.armando0405.tuboletascraper.dao.repository;

import com.armando0405.tuboletascraper.dao.entity.ConsultaInstantanea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConsultaInstantaneaRepository extends JpaRepository<ConsultaInstantanea, Long> {

    // Obtener la última consulta (más reciente)
    Optional<ConsultaInstantanea> findTopByOrderByFechaHoraDesc();

    // Buscar por hash específico
    Optional<ConsultaInstantanea> findByHashContenido(String hashContenido);

    // Obtener consultas en un rango de fechas
    List<ConsultaInstantanea> findByFechaHoraBetweenOrderByFechaHoraDesc(
            LocalDateTime fechaInicio, LocalDateTime fechaFin);

    //  MÉTODO QUE FALTABA - Obtener últimas N consultas
    @Query(value = "SELECT c FROM ConsultaInstantanea c ORDER BY c.fechaHora DESC")
    List<ConsultaInstantanea> findTopNByOrderByFechaHoraDesc(@Param("limit") int limit);

    // Contar total de consultas
    long count();
}