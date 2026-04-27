package org.example.project_stage_backend.repository;

import org.example.project_stage_backend.entity.AnalyseGypse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnalyseGypseRepository extends JpaRepository<AnalyseGypse, Long> {

    List<AnalyseGypse> findByDateBetweenOrderByDateAsc(LocalDateTime from, LocalDateTime to);

    // Cherche la donnée la plus proche d'une date, dans une fenêtre de ±1 minute
    @Query("""
        SELECT a FROM AnalyseGypse a
        WHERE a.date BETWEEN :debut AND :fin
        ORDER BY ABS(TIMESTAMPDIFF(SECOND, a.date, :reference)) ASC
        LIMIT 1
        """)
    Optional<AnalyseGypse> findClosestToDate(
            @Param("reference") LocalDateTime reference,
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin
    );

    Optional<AnalyseGypse> findTopByOrderByDateDesc();
}
