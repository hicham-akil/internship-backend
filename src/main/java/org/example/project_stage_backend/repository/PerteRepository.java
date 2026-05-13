package org.example.project_stage_backend.repository;

import org.example.project_stage_backend.entity.Perte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PerteRepository extends JpaRepository<Perte, Long> {

    @Query("SELECT p FROM Perte p WHERE p.date BETWEEN :debut AND :fin " +
            "ORDER BY ABS(FUNCTION('TIMESTAMPDIFF', SECOND, p.date, :dateRef)) ASC LIMIT 1")
    Optional<Perte> findClosestToDate(
            @Param("dateRef") LocalDateTime dateRef,
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin);

    Optional<Perte> findTopByOrderByDateDesc();

    List<Perte> findTop100ByOrderByDateDesc();

    List<Perte> findByDateBetweenOrderByDateAsc(LocalDateTime debut, LocalDateTime fin);
}
