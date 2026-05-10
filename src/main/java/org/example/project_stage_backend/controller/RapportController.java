package org.example.project_stage_backend.controller;

import lombok.RequiredArgsConstructor;
import org.example.project_stage_backend.service.RapportService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/rapport")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RapportController {

    private final RapportService rapportService;

    @GetMapping("/journalier")
    public ResponseEntity<byte[]> telechargerRapport() {
        try {
            byte[] pdf = rapportService.genererRapport24h();

            String filename = "rapport_jfc3_" +
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".pdf";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                    ContentDisposition.attachment().filename(filename).build());

            return ResponseEntity.ok().headers(headers).body(pdf);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}