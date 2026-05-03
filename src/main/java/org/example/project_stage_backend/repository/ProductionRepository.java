package org.example.project_stage_backend.repository;

import org.example.project_stage_backend.entity.Production;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionRepository extends JpaRepository<Production, Long> {

    List<Production> findByDateBetweenOrderByDateAsc(LocalDateTime from, LocalDateTime to);
    @Query(value = """
    SELECT *
    FROM production p
    WHERE p.date BETWEEN :debut AND :fin
    ORDER BY ABS(TIMESTAMPDIFF(SECOND, p.date, :reference))
    LIMIT 1
    """, nativeQuery = true)
    Optional<Production> findClosestToDate(
            @Param("reference") LocalDateTime reference,
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin
    );
    Optional<Production> findTopByOrderByDateDesc();
    List<Production> findTop100ByOrderByDateDesc();
}

