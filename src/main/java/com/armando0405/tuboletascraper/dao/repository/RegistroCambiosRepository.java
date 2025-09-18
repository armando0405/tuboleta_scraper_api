package com.armando0405.tuboletascraper.dao.repository;

import com.armando0405.tuboletascraper.dao.entity.RegistroCambios;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RegistroCambiosRepository extends JpaRepository<RegistroCambios, Long> {

    // Obtener cambios más recientes
    List<RegistroCambios> findTopByOrderByFechaDeteccionDesc();

    // Obtener cambios en un rango de fechas
    List<RegistroCambios> findByFechaDeteccionBetweenOrderByFechaDeteccionDesc(
            LocalDateTime fechaInicio, LocalDateTime fechaFin);

    // Obtener cambios por consulta nueva
    List<RegistroCambios> findByConsultaNuevaId(Long consultaId);

    // Obtener últimos N cambios
    @Query("SELECT r FROM RegistroCambios r ORDER BY r.fechaDeteccion DESC")
    List<RegistroCambios> findTopNByOrderByFechaDeteccionDesc(int limit);

    // Contar total de cambios detectados
    long count();

    // Suma total de cambios individuales
    @Query("SELECT COALESCE(SUM(r.totalCambios), 0) FROM RegistroCambios r")
    Long sumTotalCambios();
}