package org.example.project_stage_backend.repository;

import org.example.project_stage_backend.entity.Alerte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlerteRepository extends JpaRepository<Alerte, Long> {

    // 50 dernières alertes non acquittées — pour le dashboard
    List<Alerte> findTop50ByAcquitteeOrderByDateDesc(boolean acquittee);

    // Toutes les alertes sur une période
    List<Alerte> findByDateBetweenOrderByDateDesc(LocalDateTime debut, LocalDateTime fin);

    // Alertes récentes (toutes, acquittées ou non)
    List<Alerte> findTop100ByOrderByDateDesc();
}