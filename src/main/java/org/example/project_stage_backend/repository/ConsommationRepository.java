package org.example.project_stage_backend.repository;

import org.example.project_stage_backend.entity.Consommation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConsommationRepository extends JpaRepository<Consommation, Long> {

    List<Consommation> findByDateBetweenOrderByDateAsc(LocalDateTime from, LocalDateTime to);

    @Query("""
        SELECT c FROM Consommation c
        WHERE c.date BETWEEN :debut AND :fin
        ORDER BY ABS(TIMESTAMPDIFF(SECOND, c.date, :reference)) ASC
        LIMIT 1
        """)
    Optional<Consommation> findClosestToDate(
            @Param("reference") LocalDateTime reference,
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin
    );

    Optional<Consommation> findTopByOrderByDateDesc();
}
