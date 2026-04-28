package org.example.project_stage_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AnalysePhosphateDTO {
    private LocalDateTime date;
    @JsonProperty("p2o5Phosphate")
    private Double p2o5Phosphate;

    @JsonProperty("caoPhosphate")
    private Double caOPhosphate;

    @JsonProperty("qPhosphate")
    private Double qPhosphate;
}
