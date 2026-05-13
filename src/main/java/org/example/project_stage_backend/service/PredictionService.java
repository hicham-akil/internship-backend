package org.example.project_stage_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PredictionService {

    private final RestTemplate restTemplate;

    @Value("${ml.service.url:http://localhost:8001}")
    private String mlServiceUrl;

    public Map<String, Object> getAllPredictions(int horizon) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    mlServiceUrl + "/predict/all?horizon=" + horizon,
                    Map.class
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("ML service unreachable: {}", e.getMessage());
            return Map.of("error", "ML service unavailable");
        }
    }

    // Called after each ingest — triggers async retrain
    public void notifyNewData() {
        try {
            restTemplate.postForEntity(
                    mlServiceUrl + "/retrain",
                    null, String.class
            );
        } catch (Exception e) {
            log.warn("Could not notify ML service: {}", e.getMessage());
        }
    }
}