package org.example.project_stage_backend.dto;

import lombok.Data;

/**
 * Payload unique envoyé par le système Python.
 * Python envoie tout d'un coup — Spring Boot désagrège et calcule.
 *
 * Alternative : Python peut appeler les 4 endpoints séparément
 * si les données viennent de sources différentes.
 */
@Data
public class DonneesCompleteDTO {
    private AnalyseGypseDTO      analyseGypse;
    private AnalysePhosphateDTO  analysePhosphate;
    private ProductionDTO        production;
    private ConsommationDTO      consommation;
}
