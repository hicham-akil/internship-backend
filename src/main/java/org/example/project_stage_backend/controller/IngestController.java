package org.example.project_stage_backend.controller;

import org.example.project_stage_backend.dto.*;
import org.example.project_stage_backend.service.IndicateurService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ingest")
@RequiredArgsConstructor
@Slf4j
public class IngestController {

    private final IndicateurService indicateurService;

    @PostMapping("/complet")
    public ResponseEntity<IndicateursDTO> ingestComplet(@RequestBody DonneesCompleteDTO dto) {
        log.info("POST /ingest/complet reçu — date={}", dto.getAnalyseGypse().getDate());
        return ResponseEntity.ok(indicateurService.ingestDonneesCompletes(dto));
    }

    @PostMapping("/gypse")
    public ResponseEntity<IndicateursDTO> ingestGypse(@RequestBody AnalyseGypseDTO dto) {
        log.info("POST /ingest/gypse reçu — date={}", dto.getDate());
        IndicateursDTO result = indicateurService.ingestGypse(dto);
        return result != null ? ResponseEntity.ok(result) : ResponseEntity.accepted().build();
    }

    @PostMapping("/phosphate")
    public ResponseEntity<IndicateursDTO> ingestPhosphate(@RequestBody AnalysePhosphateDTO dto) {
        log.info("POST /ingest/phosphate reçu — date={}", dto.getDate());
        IndicateursDTO result = indicateurService.ingestPhosphate(dto);
        return result != null ? ResponseEntity.ok(result) : ResponseEntity.accepted().build();
    }

    @PostMapping("/production")
    public ResponseEntity<IndicateursDTO> ingestProduction(@RequestBody ProductionDTO dto) {
        log.info("POST /ingest/production reçu — date={}", dto.getDate());
        IndicateursDTO result = indicateurService.ingestProduction(dto);
        return result != null ? ResponseEntity.ok(result) : ResponseEntity.accepted().build();
    }

    @PostMapping("/consommation")
    public ResponseEntity<IndicateursDTO> ingestConsommation(@RequestBody ConsommationDTO dto) {
        log.info("POST /ingest/consommation reçu — date={}", dto.getDate());
        IndicateursDTO result = indicateurService.ingestConsommation(dto);
        return result != null ? ResponseEntity.ok(result) : ResponseEntity.accepted().build();
    }
}