package org.example.project_stage_backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AnalyseGypseDTO {
    private LocalDateTime date;
    private Double seA;
    private Double seB;
    private Double synA;
    private Double synB;
    private Double intA;
    private Double intB;
    private Double p2o5GypseA;
    private Double p2o5GypseB;
    private Double caOGypseA;
    private Double caOGypseB;
}
