package org.example.project_stage_backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class IndicateursDTO {
    private Long id;
    private LocalDateTime date;



    // Rendements
    private Double rc;
    private Double ri;

    // Capacité
    private Double cap;

    // Consommations spécifiques
    private Double consoH2so4;
    private Double consoEauBrute;
    private Double consoPhosphates;
    private Double consoVapeur;
}
