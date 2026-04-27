package org.example.project_stage_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "production")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Production {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime date;

    // Quantité P2O5 produite à 29% (T)
    @Column(name = "q_p2o5_29") private Double qP2o529;

    // Quantité P2O5 produite à 54% (T)
    @Column(name = "q_p2o5_54") private Double qP2o554;
}
