package org.example.project_stage_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AnalyseGypseDTO {
    private LocalDateTime date;
    private Double p2o5GypseA;
    private Double p2o5GypseB;

    @JsonProperty("caOGypseA")
    private Double caOGypseA;

    @JsonProperty("caOGypseB")
    private Double caOGypseB;
}