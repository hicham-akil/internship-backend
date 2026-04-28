package org.example.project_stage_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ConsommationDTO {
    private LocalDateTime date;
    @JsonProperty("qteH2so4")
    private Double qteH2so4;

    @JsonProperty("qteEauBrute")
    private Double qteEauBrute;

    @JsonProperty("qtePhosphates")
    private Double qtePhosphates;

    @JsonProperty("qteVapeur")
    private Double qteVapeur;
}
