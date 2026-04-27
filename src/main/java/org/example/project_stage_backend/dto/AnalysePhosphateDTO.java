package org.example.project_stage_backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AnalysePhosphateDTO {
    private LocalDateTime date;
    private Double p2o5Phosphate;
    private Double caOPhosphate;
    private Double qPhosphate;
}
