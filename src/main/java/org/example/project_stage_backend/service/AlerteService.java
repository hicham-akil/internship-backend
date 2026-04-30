package org.example.project_stage_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.project_stage_backend.dto.AlerteDTO;
import org.example.project_stage_backend.entity.Alerte;
import org.example.project_stage_backend.entity.Alerte.Severite;
import org.example.project_stage_backend.entity.IndicateursCalcules;
import org.example.project_stage_backend.repository.AlerteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlerteService {

    private final AlerteRepository alerteRepo;
    private final SimpMessagingTemplate messagingTemplate;
    private final RestTemplate restTemplate;
    private final Map<String, LocalDateTime> dernierEnvoiN8n = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MINUTES = 30L;

    @Value("${n8n.webhook.url}")
    private String n8nWebhookUrl;



    private static final Map<String, double[]> SEUILS_MAX = Map.of(
            "SE",               new double[]{1.5,  1.8},   // [warning, critical]
            "SYN",              new double[]{1.8,  2.2},
            "INT",              new double[]{1.2,  1.5},
            "CONSO_H2SO4",      new double[]{3.2,  3.8},
            "CONSO_EAU_BRUTE",  new double[]{15.0, 18.0},
            "CONSO_PHOSPHATES", new double[]{3.5,  4.2},
            "CONSO_VAPEUR",     new double[]{1.2,  1.5}
    );

    private static final Map<String, double[]> SEUILS_MIN = Map.of(
            "RC", new double[]{0.90, 0.84},
            "RI", new double[]{0.85, 0.80}
    );

    // ── Point d'entrée principal ──────────────────────────────────
    // Appelé par IndicateurService après chaque calcul

    public void verifierEtCreerAlertes(IndicateursCalcules ind) {
        List<Alerte> alertes = new ArrayList<>();

        // Vérifications MAX (trop haut = mauvais)
        verifierMax(alertes, "SE",               ind.getSe(),               ind.getDate());
        verifierMax(alertes, "SYN",              ind.getSyn(),              ind.getDate());
        verifierMax(alertes, "INT",              ind.getIntVal(),           ind.getDate());
        verifierMax(alertes, "CONSO_H2SO4",      ind.getConsoH2so4(),      ind.getDate());
        verifierMax(alertes, "CONSO_EAU_BRUTE",  ind.getConsoEauBrute(),   ind.getDate());
        verifierMax(alertes, "CONSO_PHOSPHATES", ind.getConsoPhosphates(), ind.getDate());
        verifierMax(alertes, "CONSO_VAPEUR",     ind.getConsoVapeur(),     ind.getDate());

        // Vérifications MIN (trop bas = mauvais)
        verifierMin(alertes, "RC", ind.getRc(), ind.getDate());
        verifierMin(alertes, "RI", ind.getRi(), ind.getDate());

        if (alertes.isEmpty()) return;

        // Sauvegarder en base
        List<Alerte> saved = alerteRepo.saveAll(alertes);
        log.info("{} alerte(s) créée(s) pour date={}", saved.size(), ind.getDate());

        // Broadcast WebSocket → dashboard React
        List<AlerteDTO> dtos = saved.stream().map(AlerteDTO::from).collect(Collectors.toList());
        messagingTemplate.convertAndSend("/topic/alertes", dtos);

        // Appeler n8n pour chaque alerte CRITICAL
        saved.stream()
                .filter(a -> a.getSeverite() == Severite.CRITICAL)
                .forEach(this::notifierN8n);
    }

    private void verifierMax(List<Alerte> alertes, String type, Double valeur, LocalDateTime date) {
        if (valeur == null) return;
        double[] seuils = SEUILS_MAX.get(type);

        Severite sev = null;
        double seuilRef = 0;

        if (valeur >= seuils[1]) { sev = Severite.CRITICAL; seuilRef = seuils[1]; }
        else if (valeur >= seuils[0]) { sev = Severite.WARNING;  seuilRef = seuils[0]; }

        if (sev != null) alertes.add(creerAlerte(date, type, valeur, seuilRef, sev));
    }

    private void verifierMin(List<Alerte> alertes, String type, Double valeur, LocalDateTime date) {
        if (valeur == null) return;
        double[] seuils = SEUILS_MIN.get(type);

        Severite sev = null;
        double seuilRef = 0;

        if (valeur <= seuils[1]) { sev = Severite.CRITICAL; seuilRef = seuils[1]; }
        else if (valeur <= seuils[0]) { sev = Severite.WARNING;  seuilRef = seuils[0]; }

        if (sev != null) alertes.add(creerAlerte(date, type, valeur, seuilRef, sev));
    }

    private Alerte creerAlerte(LocalDateTime date, String type,
                               Double valeur, double seuil, Severite sev) {
        return Alerte.builder()
                .date(date)
                .typeIndicateur(type)
                .valeur(valeur)
                .seuil(seuil)
                .severite(sev)
                .acquittee(false)
                .build();
    }

    // ── n8n webhook ───────────────────────────────────────────────

    private void notifierN8n(Alerte alerte) {
        String key = alerte.getTypeIndicateur();
        LocalDateTime maintenant = LocalDateTime.now();
        LocalDateTime dernierEnvoi = dernierEnvoiN8n.get(key);

        if (dernierEnvoi != null &&
                dernierEnvoi.plusMinutes(COOLDOWN_MINUTES).isAfter(maintenant)) {
            log.info("Cooldown actif pour {} — email ignoré", key);
            return;
        }

        try {
            Map<String, Object> payload = Map.of(
                    "indicateur", alerte.getTypeIndicateur(),
                    "valeur",     alerte.getValeur(),
                    "seuil",      alerte.getSeuil(),
                    "severite",   alerte.getSeverite().name(),
                    "date",       alerte.getDate().toString(),
                    "unite",      "JFC1 - OCP"
            );
            restTemplate.postForEntity(n8nWebhookUrl, payload, String.class);
            dernierEnvoiN8n.put(key, maintenant);
            log.info("n8n notifié pour {} — prochain dans {} min", key, COOLDOWN_MINUTES);
        } catch (Exception e) {
            log.error("Échec notification n8n : {}", e.getMessage());
        }
    }

    // ── Lecture ───────────────────────────────────────────────────

    public List<AlerteDTO> getAlertesNonAcquittees() {
        return alerteRepo.findTop50ByAcquitteeOrderByDateDesc(false)
                .stream().map(AlerteDTO::from).collect(Collectors.toList());
    }

    public List<AlerteDTO> getHistoriqueAlertes() {
        return alerteRepo.findTop100ByOrderByDateDesc()
                .stream().map(AlerteDTO::from).collect(Collectors.toList());
    }

    public AlerteDTO acquitter(Long id) {
        Alerte alerte = alerteRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alerte introuvable : " + id));
        alerte.setAcquittee(true);
        return AlerteDTO.from(alerteRepo.save(alerte));
    }
}