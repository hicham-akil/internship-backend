package org.example.project_stage_backend.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import lombok.RequiredArgsConstructor;
import org.example.project_stage_backend.dto.IndicateursDTO;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RapportService {

    private final IndicateurService indicateurService;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Colors
    private static final BaseColor DARK_BG    = new BaseColor(6, 13, 26);
    private static final BaseColor EMERALD    = new BaseColor(0, 232, 122);
    private static final BaseColor AMBER      = new BaseColor(251, 191, 36);
    private static final BaseColor SLATE_400  = new BaseColor(148, 163, 184);
    private static final BaseColor SLATE_700  = new BaseColor(51, 65, 85);
    private static final BaseColor SLATE_900  = new BaseColor(15, 23, 42);

    public byte[] genererRapport24h() throws DocumentException {
        LocalDateTime fin   = LocalDateTime.now();
        LocalDateTime debut = fin.minusHours(24);

        List<IndicateursDTO> donnees =
                indicateurService.getIndicateursSurPeriode(debut, fin);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 60, 40);
        PdfWriter writer = PdfWriter.getInstance(doc, out);

        // Header/Footer
        writer.setPageEvent(new HeaderFooter());

        doc.open();

        // ── HEADER ────────────────────────────────────────────
        ajouterHeader(doc, debut, fin, donnees.size());

        // ── RÉSUMÉ STATISTIQUES ────────────────────────────────
        ajouterResume(doc, donnees);

        // ── TABLEAU DÉTAILLÉ ───────────────────────────────────
        ajouterTableau(doc, donnees);

        // ── ALERTES SEUILS ─────────────────────────────────────
        ajouterAnalyseAlertes(doc, donnees);

        doc.close();
        return out.toByteArray();
    }

    private void ajouterHeader(Document doc, LocalDateTime debut,
                               LocalDateTime fin, int nbPoints)
            throws DocumentException {

        // Titre principal
        Font titreFont = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD, EMERALD);
        Paragraph titre = new Paragraph("RAPPORT JOURNALIER — JFC3", titreFont);
        titre.setAlignment(Element.ALIGN_CENTER);
        titre.setSpacingAfter(4);
        doc.add(titre);

        Font subFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, SLATE_400);
        Paragraph sub = new Paragraph("Acide Phosphorique — Unité JFC1", subFont);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(16);
        doc.add(sub);

        // Ligne séparatrice
        LineSeparator sep = new LineSeparator(1f, 100f, EMERALD, Element.ALIGN_CENTER, -2);
        doc.add(new Chunk(sep));

        // Infos période
        PdfPTable infoTable = new PdfPTable(3);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingBefore(12);
        infoTable.setSpacingAfter(20);

        ajouterInfoCell(infoTable, "PÉRIODE DÉBUT", debut.format(FMT));
        ajouterInfoCell(infoTable, "PÉRIODE FIN",   fin.format(FMT));
        ajouterInfoCell(infoTable, "RELEVÉS",        String.valueOf(nbPoints));

        doc.add(infoTable);
    }

    private void ajouterInfoCell(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(SLATE_900);
        cell.setBorderColor(SLATE_700);
        cell.setPadding(10);

        Font lFont = new Font(Font.FontFamily.HELVETICA, 7, Font.BOLD, SLATE_400);
        Font vFont = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, EMERALD);

        Paragraph p = new Paragraph();
        p.add(new Chunk(label + "\n", lFont));
        p.add(new Chunk(value, vFont));
        cell.addElement(p);
        table.addCell(cell);
    }

    private void ajouterResume(Document doc, List<IndicateursDTO> donnees)
            throws DocumentException {

        if (donnees.isEmpty()) return;

        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, EMERALD);
        Paragraph section = new Paragraph("◉  RÉSUMÉ STATISTIQUES", sectionFont);
        section.setSpacingAfter(10);
        doc.add(section);

        // Calcul stats
        StatBlock se   = calcStat(donnees, d -> d.getSe());
        StatBlock syn  = calcStat(donnees, d -> d.getSyn());
        StatBlock intv = calcStat(donnees, d -> d.getIntVal());
        StatBlock rc   = calcStat(donnees, d -> d.getRc());
        StatBlock ri   = calcStat(donnees, d -> d.getRi());
        StatBlock cap  = calcStat(donnees, d -> d.getCap());

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2.5f, 1.5f, 1.5f, 1.5f, 1.5f});
        table.setSpacingAfter(20);

        // En-tête
        String[] headers = {"INDICATEUR", "MIN", "MAX", "MOYENNE", "SEUIL"};
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h,
                    new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, EMERALD)));
            c.setBackgroundColor(SLATE_900);
            c.setBorderColor(SLATE_700);
            c.setPadding(8);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(c);
        }

        // Lignes
        ajouterLigneStats(table, "SE — Perte Séchage",       se,   "≤ 1.5",  se.avg > 1.5);
        ajouterLigneStats(table, "SYN — Perte Synthèse",     syn,  "≤ 1.8",  syn.avg > 1.8);
        ajouterLigneStats(table, "INT — Perte Intermédiaire",intv, "≤ 1.2",  intv.avg > 1.2);
        ajouterLigneStats(table, "RC — Rendement Conc.",     rc,   "≥ 0.90", rc.avg < 0.90);
        ajouterLigneStats(table, "RI — Rendement Incorp.",   ri,   "≥ 0.85", ri.avg < 0.85);
        ajouterLigneStats(table, "CAP — Capacité Prod.",     cap,  "≥ 1.0",  cap.avg < 1.0);

        doc.add(table);
    }

    private void ajouterLigneStats(PdfPTable table, String nom,
                                   StatBlock s, String seuil, boolean alerte) {
        BaseColor rowBg = alerte ? new BaseColor(45, 25, 0) : DARK_BG;
        BaseColor valColor = alerte ? AMBER : SLATE_400;

        Font nomFont = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD,
                alerte ? AMBER : SLATE_400);
        Font valFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, valColor);

        String[] vals = {
                nom,
                String.format("%.4f", s.min),
                String.format("%.4f", s.max),
                String.format("%.4f", s.avg),
                seuil + (alerte ? " ⚠" : " ✓")
        };

        for (int i = 0; i < vals.length; i++) {
            PdfPCell c = new PdfPCell(new Phrase(vals[i], i == 0 ? nomFont : valFont));
            c.setBackgroundColor(rowBg);
            c.setBorderColor(SLATE_700);
            c.setPadding(7);
            c.setHorizontalAlignment(i == 0 ? Element.ALIGN_LEFT : Element.ALIGN_CENTER);
            table.addCell(c);
        }
    }

    private void ajouterTableau(Document doc, List<IndicateursDTO> donnees)
            throws DocumentException {

        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, EMERALD);
        Paragraph section = new Paragraph("◈  HISTORIQUE DÉTAILLÉ", sectionFont);
        section.setSpacingAfter(10);
        doc.add(section);

        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2f, 1f, 1f, 1f, 1f, 1f, 1f, 1f});
        table.setSpacingAfter(20);

        String[] cols = {"DATE/HEURE", "SE", "SYN", "INT", "RC", "RI", "CAP", "H₂SO₄"};
        for (String col : cols) {
            PdfPCell c = new PdfPCell(new Phrase(col,
                    new Font(Font.FontFamily.HELVETICA, 7, Font.BOLD, EMERALD)));
            c.setBackgroundColor(SLATE_900);
            c.setBorderColor(SLATE_700);
            c.setPadding(6);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(c);
        }

        // Max 50 lignes pour ne pas surcharger
        List<IndicateursDTO> sample = donnees.size() > 50
                ? donnees.subList(donnees.size() - 50, donnees.size())
                : donnees;

        boolean pair = false;
        for (IndicateursDTO d : sample) {
            BaseColor bg = pair ? SLATE_900 : DARK_BG;
            pair = !pair;

            ajouterCellTableau(table, d.getDate() != null
                    ? d.getDate().format(FMT) : "—", bg, false, false);
            ajouterCellTableau(table, fmt(d.getSe()),       bg, d.getSe()  != null && d.getSe()  > 1.5,  false);
            ajouterCellTableau(table, fmt(d.getSyn()),      bg, d.getSyn() != null && d.getSyn() > 1.8,  false);
            ajouterCellTableau(table, fmt(d.getIntVal()),   bg, d.getIntVal() != null && d.getIntVal() > 1.2, false);
            ajouterCellTableau(table, fmt(d.getRc()),       bg, d.getRc()  != null && d.getRc()  < 0.90, false);
            ajouterCellTableau(table, fmt(d.getRi()),       bg, d.getRi()  != null && d.getRi()  < 0.85, false);
            ajouterCellTableau(table, fmt(d.getCap()),      bg, d.getCap() != null && d.getCap() < 1.0,  false);
            ajouterCellTableau(table, fmt(d.getConsoH2so4()), bg, d.getConsoH2so4() != null && d.getConsoH2so4() > 3.2, false);
        }

        doc.add(table);
    }

    private void ajouterCellTableau(PdfPTable table, String val,
                                    BaseColor bg, boolean alerte, boolean header) {
        Font f = new Font(Font.FontFamily.HELVETICA, 7,
                alerte ? Font.BOLD : Font.NORMAL,
                alerte ? AMBER : SLATE_400);
        PdfPCell c = new PdfPCell(new Phrase(val, f));
        c.setBackgroundColor(alerte ? new BaseColor(45, 25, 0) : bg);
        c.setBorderColor(SLATE_700);
        c.setPadding(5);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(c);
    }

    private void ajouterAnalyseAlertes(Document doc, List<IndicateursDTO> donnees)
            throws DocumentException {

        long nbSE  = donnees.stream().filter(d -> d.getSe()  != null && d.getSe()  > 1.5).count();
        long nbSYN = donnees.stream().filter(d -> d.getSyn() != null && d.getSyn() > 1.8).count();
        long nbINT = donnees.stream().filter(d -> d.getIntVal() != null && d.getIntVal() > 1.2).count();
        long nbRC  = donnees.stream().filter(d -> d.getRc()  != null && d.getRc()  < 0.90).count();
        long nbRI  = donnees.stream().filter(d -> d.getRi()  != null && d.getRi()  < 0.85).count();

        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, EMERALD);
        Paragraph section = new Paragraph("⚠  ANALYSE DES DÉPASSEMENTS", sectionFont);
        section.setSpacingAfter(10);
        doc.add(section);

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setSpacingAfter(20);

        String[] hdr = {"INDICATEUR", "NB DÉPASSEMENTS / " + donnees.size(), "TAUX"};
        for (String h : hdr) {
            PdfPCell c = new PdfPCell(new Phrase(h,
                    new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, EMERALD)));
            c.setBackgroundColor(SLATE_900);
            c.setBorderColor(SLATE_700);
            c.setPadding(8);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(c);
        }

        ajouterLigneAlerte(table, "SE  (seuil ≤ 1.5)", nbSE,  donnees.size());
        ajouterLigneAlerte(table, "SYN (seuil ≤ 1.8)", nbSYN, donnees.size());
        ajouterLigneAlerte(table, "INT (seuil ≤ 1.2)", nbINT, donnees.size());
        ajouterLigneAlerte(table, "RC  (seuil ≥ 0.90)", nbRC, donnees.size());
        ajouterLigneAlerte(table, "RI  (seuil ≥ 0.85)", nbRI, donnees.size());

        doc.add(table);

        // Note de bas de rapport
        Font noteFont = new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, SLATE_400);
        Paragraph note = new Paragraph(
                "Rapport généré automatiquement le " +
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm:ss")) +
                        " — Système JFC1 Dashboard", noteFont);
        note.setAlignment(Element.ALIGN_CENTER);
        doc.add(note);
    }

    private void ajouterLigneAlerte(PdfPTable table, String nom,
                                    long nb, int total) {
        double taux = total > 0 ? (nb * 100.0 / total) : 0;
        boolean critique = taux > 20;

        Font f = new Font(Font.FontFamily.HELVETICA, 9,
                critique ? Font.BOLD : Font.NORMAL,
                critique ? AMBER : SLATE_400);

        String[] vals = { nom, nb + " fois", String.format("%.1f%%", taux) };
        for (String v : vals) {
            PdfPCell c = new PdfPCell(new Phrase(v, f));
            c.setBackgroundColor(critique ? new BaseColor(45, 25, 0) : DARK_BG);
            c.setBorderColor(SLATE_700);
            c.setPadding(7);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(c);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────
    private String fmt(Double v) {
        return v != null ? String.format("%.4f", v) : "—";
    }

    private StatBlock calcStat(List<IndicateursDTO> list,
                               java.util.function.Function<IndicateursDTO, Double> getter) {
        double[] vals = list.stream()
                .map(getter)
                .filter(v -> v != null)
                .mapToDouble(Double::doubleValue)
                .toArray();
        if (vals.length == 0) return new StatBlock(0, 0, 0);
        double min = Double.MAX_VALUE, max = Double.MIN_VALUE, sum = 0;
        for (double v : vals) { min = Math.min(min, v); max = Math.max(max, v); sum += v; }
        return new StatBlock(min, max, sum / vals.length);
    }

    record StatBlock(double min, double max, double avg) {}

    // ── Header / Footer sur chaque page ──────────────────────────
    static class HeaderFooter extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            BaseFont bf;
            try { bf = BaseFont.createFont(); } catch (Exception e) { return; }

            // Footer
            cb.saveState();
            cb.setColorFill(new BaseColor(148, 163, 184));
            cb.setFontAndSize(bf, 7);
            cb.beginText();
            cb.showTextAligned(Element.ALIGN_CENTER,
                    "JFC1 — Acide Phosphorique | Page " + writer.getPageNumber(),
                    document.getPageSize().getWidth() / 2, 25, 0);
            cb.endText();
            cb.restoreState();
        }
    }
}