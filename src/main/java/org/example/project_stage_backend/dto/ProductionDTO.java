package org.example.project_stage_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProductionDTO {

    private LocalDateTime date;

    @JsonProperty("qP2o529")
    private Double qP2o529;

    @JsonProperty("qP2o554")
    private Double qP2o554;
}