package org.example.project_stage_backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ConsommationDTO {
    private LocalDateTime date;
    private Double qteH2so4;
    private Double qteEauBrute;
    private Double qtePhosphates;
    private Double qteVapeur;
}
