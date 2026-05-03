package org.example.project_stage_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "analyse_phosphate")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnalysePhosphate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime date;

    @Column(name = "p2o5_phosphate") private Double p2o5Phosphate;
    @Column(name = "cao_phosphate")  private Double caOPhosphate;
    @Column(name = "q_phosphate")    private Double qPhosphate;   // Quantité (T)
}
