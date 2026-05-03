package org.example.project_stage_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.project_stage_backend.dto.*;
import org.example.project_stage_backend.entity.*;
import org.example.project_stage_backend.repository.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndicateurService {

    private static final long TOLERANCE_MINUTES = 1L;

    private final AlerteService                 alerteService;
    private final AnalyseGypseRepository        gypseRepo;
    private final AnalysePhosphateRepository    phosphateRepo;
    private final ProductionRepository          productionRepo;
    private final ConsommationRepository        consommationRepo;
    private final IndicateursCalculesRepository indicateursRepo;
    private final SimpMessagingTemplate         messagingTemplate;

    // ── INGESTION ─────────────────────────────────────────────────

    @Transactional
    public IndicateursDTO ingestDonneesCompletes(DonneesCompleteDTO dto) {
        AnalyseGypse     gypse      = sauvegarderGypse(dto.getAnalyseGypse());
        AnalysePhosphate phosphate  = sauvegarderPhosphate(dto.getAnalysePhosphate());
        Production       production = sauvegarderProduction(dto.getProduction());
        Consommation     conso      = sauvegarderConsommation(dto.getConsommation());

        // broadcast raw gypse A/B fields in real time
        messagingTemplate.convertAndSend("/topic/gypse", dto.getAnalyseGypse());
        log.info("📡 Gypse A/B broadcasted on /topic/gypse for date={}", gypse.getDate());

        LocalDateTime dateRef = gypse.getDate();
        IndicateursCalcules indicateurs = calculer(dateRef, gypse, phosphate, production, conso);
        IndicateursCalcules saved = indicateursRepo.save(indicateurs);
        log.info("✅ Indicateurs calculés et sauvegardés pour date={}", dateRef);

        IndicateursDTO result = toDTO(saved);
        messagingTemplate.convertAndSend("/topic/indicateurs", result);
        alerteService.verifierEtCreerAlertes(saved);

        log.info("PRODUCTION DTO = {}", dto.getProduction());
        log.info("Q29 = {}", dto.getProduction().getQP2o529());
        log.info("Q54 = {}", dto.getProduction().getQP2o554());

        return result;
    }

    @Transactional
    public IndicateursDTO ingestGypse(AnalyseGypseDTO dto) {
        AnalyseGypse gypse = sauvegarderGypse(dto);

        // broadcast raw gypse A/B fields in real time
        messagingTemplate.convertAndSend("/topic/gypse", dto);
        log.info("📡 Gypse A/B broadcasted on /topic/gypse for date={}", gypse.getDate());

        return recalculerDepuisDate(gypse.getDate());
    }

    @Transactional
    public IndicateursDTO ingestPhosphate(AnalysePhosphateDTO dto) {
        AnalysePhosphate phosphate = sauvegarderPhosphate(dto);
        return recalculerDepuisDate(phosphate.getDate());
    }

    @Transactional
    public IndicateursDTO ingestProduction(ProductionDTO dto) {
        Production production = sauvegarderProduction(dto);
        return recalculerDepuisDate(production.getDate());
    }

    @Transactional
    public IndicateursDTO ingestConsommation(ConsommationDTO dto) {
        Consommation conso = sauvegarderConsommation(dto);
        return recalculerDepuisDate(conso.getDate());
    }

    // ── LECTURE ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Optional<IndicateursDTO> getDernierIndicateur() {
        return indicateursRepo.findTopByOrderByDateDesc().map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public List<IndicateursDTO> getIndicateursSurPeriode(LocalDateTime debut, LocalDateTime fin) {
        return indicateursRepo.findByDateBetweenOrderByDateAsc(debut, fin)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IndicateursDTO> getHistoriqueRecent() {
        return indicateursRepo.findTop100ByOrderByDateDesc()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    // ── NEW: fetch latest gypse directly from gypse table ─────────

    @Transactional(readOnly = true)
    public Optional<AnalyseGypseDTO> getDernierGypse() {
        return gypseRepo.findTopByOrderByDateDesc().map(this::toGypseDTO);
    }

    @Transactional(readOnly = true)
    public List<AnalyseGypseDTO> getHistoriqueGypse() {
        return gypseRepo.findTop100ByOrderByDateDesc()
                .stream().map(this::toGypseDTO).collect(Collectors.toList());
    }

    // ── MOTEUR DE CALCUL ──────────────────────────────────────────

    private IndicateursDTO recalculerDepuisDate(LocalDateTime dateRef) {
        LocalDateTime debut = dateRef.minusMinutes(TOLERANCE_MINUTES);
        LocalDateTime fin   = dateRef.plusMinutes(TOLERANCE_MINUTES);

        Optional<AnalyseGypse>     gypse      = gypseRepo.findClosestToDate(dateRef, debut, fin);
        Optional<AnalysePhosphate> phosphate  = phosphateRepo.findClosestToDate(dateRef, debut, fin);
        Optional<Production>       production = productionRepo.findClosestToDate(dateRef, debut, fin);
        Optional<Consommation>     conso      = consommationRepo.findClosestToDate(dateRef, debut, fin);

        if (gypse.isEmpty() || phosphate.isEmpty() || production.isEmpty() || conso.isEmpty()) {
            log.warn("⚠ Données incomplètes pour date={} — indicateurs non calculés", dateRef);
            return null;
        }

        IndicateursCalcules indicateurs = calculer(
                dateRef, gypse.get(), phosphate.get(), production.get(), conso.get());

        IndicateursDTO result = toDTO(indicateursRepo.save(indicateurs));
        messagingTemplate.convertAndSend("/topic/indicateurs", result);
        alerteService.verifierEtCreerAlertes(indicateurs);
        return result;
    }

    private IndicateursCalcules calculer(
            LocalDateTime dateRef,
            AnalyseGypse gypse,
            AnalysePhosphate phosphate,
            Production production,
            Consommation conso) {

        log.info("⚙ CALCULATION START for {}", dateRef);

        // =========================
        // 1. AVERAGES
        // =========================
        Double se     = moyenne(gypse.getSeA(), gypse.getSeB());
        Double syn    = moyenne(gypse.getSynA(), gypse.getSynB());
        Double intVal = moyenne(gypse.getIntA(), gypse.getIntB());

        Double p2o5Gypse = moyenne(gypse.getP2o5GypseA(), gypse.getP2o5GypseB());
        Double caOGypse  = moyenne(gypse.getCaOGypseA(), gypse.getCaOGypseB());

        Double p2o5Phosphate = phosphate.getP2o5Phosphate();
        Double caOPhosphate  = phosphate.getCaOPhosphate();
        Double qPhosphate    = phosphate.getQPhosphate();

        Double q29 = production.getQP2o529();
        Double q54 = production.getQP2o554();

        Double h2so4  = conso.getQteH2so4();
        Double eau    = conso.getQteEauBrute();
        Double phosph = conso.getQtePhosphates();
        Double vapeur = conso.getQteVapeur();

        // =========================
        // 2. RC
        // RC = 1 - (p2o5_gypse * CaO_phosphate) / (p2o5_phosphate * CaO_gypse)
        // =========================
        Double rc = null;
        if (p2o5Gypse != null && caOPhosphate != null &&
                p2o5Phosphate != null && caOGypse != null &&
                p2o5Phosphate != 0 && caOGypse != 0) {
            rc = 1.0 - ((p2o5Gypse * caOPhosphate) / (p2o5Phosphate * caOGypse));
        }

        // =========================
        // 3. RI
        // RI = q29 / ((p2o5_phosphate * Q_phosphate) / 100)
        // =========================
        Double ri = null;
        if (q29 != null && p2o5Phosphate != null && qPhosphate != null) {
            Double denominator = (p2o5Phosphate * qPhosphate) / 100.0;
            if (denominator != 0) {
                ri = q29 / denominator;
            }
        }

        // =========================
        // 4. CAP
        // CAP = q54 / q29
        // =========================
        Double cap = null;
        if (q29 != null && q54 != null && q29 != 0) {
            cap = q54 / q29;
        }

        // =========================
        // 5. CONSUMPTIONS
        // =========================
        Double consoH2so4    = (h2so4 != null && q29 != null && q29 != 0) ? h2so4 / q29 : null;
        Double consoEauBrute = (eau   != null && q29 != null && q29 != 0) ? eau   / q29 : null;
        Double consoPhos     = (phosph != null && q29 != null && q29 != 0) ? phosph / q29 : null;
        Double consoVapeur   = (vapeur != null && q54 != null && q54 != 0) ? vapeur / q54 : null;

        log.info("📊 RESULTS → RC={} | RI={} | CAP={} | CONSO H2SO4={}", rc, ri, cap, consoH2so4);

        return IndicateursCalcules.builder()
                .date(dateRef)
                .se(arrondir(se))
                .syn(arrondir(syn))
                .intVal(arrondir(intVal))
                .rc(arrondir(rc))
                .ri(arrondir(ri))
                .cap(arrondir(cap))
                .consoH2so4(arrondir(consoH2so4))
                .consoEauBrute(arrondir(consoEauBrute))
                .consoPhosphates(arrondir(consoPhos))
                .consoVapeur(arrondir(consoVapeur))
                .build();
    }

    // ── MAPPERS ───────────────────────────────────────────────────

    private AnalyseGypse sauvegarderGypse(AnalyseGypseDTO dto) {
        return gypseRepo.save(AnalyseGypse.builder()
                .date(dto.getDate())
                .seA(dto.getSeA()).seB(dto.getSeB())
                .synA(dto.getSynA()).synB(dto.getSynB())
                .intA(dto.getIntA()).intB(dto.getIntB())
                .p2o5GypseA(dto.getP2o5GypseA()).p2o5GypseB(dto.getP2o5GypseB())
                .caOGypseA(dto.getCaOGypseA()).caOGypseB(dto.getCaOGypseB())
                .build());
    }

    private AnalysePhosphate sauvegarderPhosphate(AnalysePhosphateDTO dto) {
        return phosphateRepo.save(AnalysePhosphate.builder()
                .date(dto.getDate())
                .p2o5Phosphate(dto.getP2o5Phosphate())
                .caOPhosphate(dto.getCaOPhosphate())
                .qPhosphate(dto.getQPhosphate())
                .build());
    }

    private Production sauvegarderProduction(ProductionDTO dto) {
        return productionRepo.save(Production.builder()
                .date(dto.getDate())
                .qP2o529(dto.getQP2o529())
                .qP2o554(dto.getQP2o554())
                .build());
    }

    private Consommation sauvegarderConsommation(ConsommationDTO dto) {
        return consommationRepo.save(Consommation.builder()
                .date(dto.getDate())
                .qteH2so4(dto.getQteH2so4())
                .qteEauBrute(dto.getQteEauBrute())
                .qtePhosphates(dto.getQtePhosphates())
                .qteVapeur(dto.getQteVapeur())
                .build());
    }

    private IndicateursDTO toDTO(IndicateursCalcules e) {
        IndicateursDTO dto = new IndicateursDTO();
        dto.setId(e.getId());
        dto.setDate(e.getDate());
        dto.setSe(e.getSe());   dto.setSyn(e.getSyn());   dto.setIntVal(e.getIntVal());
        dto.setRc(e.getRc());   dto.setRi(e.getRi());     dto.setCap(e.getCap());
        dto.setConsoH2so4(e.getConsoH2so4());
        dto.setConsoEauBrute(e.getConsoEauBrute());
        dto.setConsoPhosphates(e.getConsoPhosphates());
        dto.setConsoVapeur(e.getConsoVapeur());
        return dto;
    }

    // NEW: map AnalyseGypse entity → AnalyseGypseDTO
    private AnalyseGypseDTO toGypseDTO(AnalyseGypse e) {
        AnalyseGypseDTO dto = new AnalyseGypseDTO();
        dto.setDate(e.getDate());
        dto.setSeA(e.getSeA());           dto.setSeB(e.getSeB());
        dto.setSynA(e.getSynA());         dto.setSynB(e.getSynB());
        dto.setIntA(e.getIntA());         dto.setIntB(e.getIntB());
        dto.setP2o5GypseA(e.getP2o5GypseA()); dto.setP2o5GypseB(e.getP2o5GypseB());
        dto.setCaOGypseA(e.getCaOGypseA());   dto.setCaOGypseB(e.getCaOGypseB());
        return dto;
    }

    // ── UTILITAIRES ───────────────────────────────────────────────

    private Double moyenne(Double a, Double b) {
        if (a == null && b == null) return null;
        if (a == null) return b;
        if (b == null) return a;
        return (a + b) / 2.0;
    }

    private Double somme(Double a, Double b) {
        if (a == null && b == null) return null;
        if (a == null) return b;
        if (b == null) return a;
        return a + b;
    }

    private Double diviser(Double n, Double d) {
        if (n == null || d == null || d == 0.0) return null;
        return n / d;
    }

    private Double diviserPourcentage(Double n, Double d) {
        Double r = diviser(n, d);
        return r == null ? null : r * 100.0;
    }

    private Double arrondir(Double v) {
        if (v == null) return null;
        return Math.round(v * 10000.0) / 10000.0;
    }


    @Transactional(readOnly = true)
    public Optional<AnalysePhosphateDTO> getDernierPhosphate() {
        return phosphateRepo.findTopByOrderByDateDesc()
                .map(this::toPhosphateDTO);
    }

    @Transactional(readOnly = true)
    public List<AnalysePhosphateDTO> getHistoriquePhosphate() {
        return phosphateRepo.findTop100ByOrderByDateDesc()
                .stream()
                .map(this::toPhosphateDTO)
                .collect(Collectors.toList());
    }
    // NEW: map AnalysePhosphate entity → AnalysePhosphateDTO
    private AnalysePhosphateDTO toPhosphateDTO(AnalysePhosphate e) {
        AnalysePhosphateDTO dto = new AnalysePhosphateDTO();
        dto.setDate(e.getDate());
        dto.setP2o5Phosphate(e.getP2o5Phosphate());
        dto.setCaOPhosphate(e.getCaOPhosphate());
        dto.setQPhosphate(e.getQPhosphate());
        return dto;
    }
}