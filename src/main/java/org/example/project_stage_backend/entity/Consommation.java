package org.example.project_stage_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "consommation")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Consommation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime date;

    @Column(name = "qte_h2so4")       private Double qteH2so4;      // Quantité H2SO4 (T)
    @Column(name = "qte_eau_brute")   private Double qteEauBrute;   // Quantité eau brute (m³)
    @Column(name = "qte_phosphates")  private Double qtePhosphates;  // Quantité phosphates (T)
    @Column(name = "qte_vapeur")      private Double qteVapeur;      // Quantité vapeur (T)
}
