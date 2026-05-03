package org.example.project_stage_backend.controller;

import lombok.RequiredArgsConstructor;
import org.example.project_stage_backend.dto.AlerteDTO;
import org.example.project_stage_backend.service.AlerteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/alertes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AlerteController {

    private final AlerteService alerteService;

    // Dashboard temps réel — alertes non acquittées
    @GetMapping("/actives")
    public ResponseEntity<List<AlerteDTO>> getActives() {
        return ResponseEntity.ok(alerteService.getAlertesNonAcquittees());
    }

    // Historique complet
    @GetMapping("/historique")
    public ResponseEntity<List<AlerteDTO>> getHistorique() {
        return ResponseEntity.ok(alerteService.getHistoriqueAlertes());
    }

    // Opérateur acquitte une alerte
    @PatchMapping("/{id}/acquitter")
    public ResponseEntity<AlerteDTO> acquitter(@PathVariable Long id) {
        return ResponseEntity.ok(alerteService.acquitter(id));
    }
}