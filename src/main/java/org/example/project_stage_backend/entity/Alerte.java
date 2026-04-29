package org.example.project_stage_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alertes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Alerte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime date;

    // Quel indicateur a déclenché l'alerte (ex: "RC", "SE", "RI")
    @Column(name = "type_indicateur", nullable = false)
    private String typeIndicateur;

    // Valeur mesurée
    @Column(nullable = false)
    private Double valeur;

    // Seuil qui a été dépassé
    @Column(nullable = false)
    private Double seuil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severite severite;

    // true = opérateur a pris en compte l'alerte
    @Column(nullable = false)
    private boolean acquittee = false;

    public enum Severite {
        WARNING,   // proche du seuil
        CRITICAL   // très au-delà du seuil
    }
}