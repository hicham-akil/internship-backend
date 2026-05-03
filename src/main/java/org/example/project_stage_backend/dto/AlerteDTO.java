package org.example.project_stage_backend.dto;

import lombok.Data;
import org.example.project_stage_backend.entity.Alerte;
import java.time.LocalDateTime;

@Data
public class AlerteDTO {
    private Long id;
    private LocalDateTime date;
    private String typeIndicateur;
    private Double valeur;
    private Double seuil;
    private String severite;
    private boolean acquittee;

    public static AlerteDTO from(Alerte a) {
        AlerteDTO dto = new AlerteDTO();
        dto.setId(a.getId());
        dto.setDate(a.getDate());
        dto.setTypeIndicateur(a.getTypeIndicateur());
        dto.setValeur(a.getValeur());
        dto.setSeuil(a.getSeuil());
        dto.setSeverite(a.getSeverite().name());
        dto.setAcquittee(a.isAcquittee());
        return dto;
    }
}