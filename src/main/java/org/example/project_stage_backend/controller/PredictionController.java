package org.example.project_stage_backend.controller;

import lombok.RequiredArgsConstructor;
import org.example.project_stage_backend.service.PredictionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/predictions")
@RequiredArgsConstructor
public class PredictionController {

    private final PredictionService predictionService;

    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAll(
            @RequestParam(defaultValue = "10") int horizon) {
        return ResponseEntity.ok(predictionService.getAllPredictions(horizon));
    }
}