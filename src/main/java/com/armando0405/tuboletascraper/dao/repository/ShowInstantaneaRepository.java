package com.armando0405.tuboletascraper.dao.repository;

import com.armando0405.tuboletascraper.dao.entity.ShowInstantanea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShowInstantaneaRepository extends JpaRepository<ShowInstantanea, Long> {

    // Obtener shows de una consulta específica
    List<ShowInstantanea> findByConsultaInstantaneaIdOrderByTitulo(Long consultaId);

    // Buscar shows por ciudad
    List<ShowInstantanea> findByCiudadContainingIgnoreCase(String ciudad);

    // Buscar shows por venue
    List<ShowInstantanea> findByVenueContainingIgnoreCase(String venue);

    // Buscar shows por ID único
    List<ShowInstantanea> findByShowIdUnico(String showIdUnico);

    // Obtener todas las ciudades únicas
    @Query("SELECT DISTINCT s.ciudad FROM ShowInstantanea s ORDER BY s.ciudad")
    List<String> findDistinctCiudades();

    // Obtener todos los venues únicos
    @Query("SELECT DISTINCT s.venue FROM ShowInstantanea s ORDER BY s.venue")
    List<String> findDistinctVenues();

    // Contar shows por consulta
    long countByConsultaInstantaneaId(Long consultaId);
}