package org.example.project_stage_backend.repository;

import org.example.project_stage_backend.entity.AnalysePhosphate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnalysePhosphateRepository extends JpaRepository<AnalysePhosphate, Long> {

    List<AnalysePhosphate> findByDateBetweenOrderByDateAsc(LocalDateTime from, LocalDateTime to);

    @Query("""
        SELECT a FROM AnalysePhosphate a
        WHERE a.date BETWEEN :debut AND :fin
        ORDER BY ABS(TIMESTAMPDIFF(SECOND, a.date, :reference)) ASC
        LIMIT 1
        """)
    Optional<AnalysePhosphate> findClosestToDate(
            @Param("reference") LocalDateTime reference,
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin
    );

    Optional<AnalysePhosphate> findTopByOrderByDateDesc();
}
