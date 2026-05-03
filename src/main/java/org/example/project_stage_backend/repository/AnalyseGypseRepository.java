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

    @Query("SELECT g FROM AnalyseGypse g WHERE g.date BETWEEN :debut AND :fin " +
            "ORDER BY ABS(FUNCTION('TIMESTAMPDIFF', SECOND, g.date, :dateRef)) ASC LIMIT 1")
    Optional<AnalyseGypse> findClosestToDate(
            @Param("dateRef") LocalDateTime dateRef,
            @Param("debut")   LocalDateTime debut,
            @Param("fin")     LocalDateTime fin);

    // NEW — for REST endpoints
    Optional<AnalyseGypse> findTopByOrderByDateDesc();
    List<AnalyseGypse>     findTop100ByOrderByDateDesc();

}
