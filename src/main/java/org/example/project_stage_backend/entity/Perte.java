package org.example.project_stage_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "perte")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Perte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime date;

    // Perte Séchage
    @Column(name = "se") private Double se;

    // Perte Synthèse
    @Column(name = "syn") private Double syn;

    // Perte Intermédiaire
    @Column(name = "int_val") private Double intVal;
}
