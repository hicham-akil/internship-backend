package org.example.project_stage_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "analyse_gypse")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnalyseGypse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime date;

    // Sulfate d'eau — lignes A et B
    @Column(name = "se_a") private Double seA;
    @Column(name = "se_b") private Double seB;

    // Synoptique — lignes A et B
    @Column(name = "syn_a") private Double synA;
    @Column(name = "syn_b") private Double synB;

    // Intermédiaire — lignes A et B
    @Column(name = "int_a") private Double intA;
    @Column(name = "int_b") private Double intB;

    // P2O5 dans le gypse — lignes A et B
    @Column(name = "p2o5_gypse_a") private Double p2o5GypseA;
    @Column(name = "p2o5_gypse_b") private Double p2o5GypseB;

    // CaO dans le gypse — lignes A et B
    @Column(name = "cao_gypse_a") private Double caOGypseA;
    @Column(name = "cao_gypse_b") private Double caOGypseB;
}
