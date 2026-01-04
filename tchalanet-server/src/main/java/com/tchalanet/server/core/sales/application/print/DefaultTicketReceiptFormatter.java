package com.tchalanet.server.core.sales.application.print;

import com.tchalanet.server.common.print.receipt.ReceiptLine;
import com.tchalanet.server.common.print.receipt.ReceiptModel;
import com.tchalanet.server.core.sales.application.port.out.TicketPrintView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class DefaultTicketReceiptFormatter implements TicketReceiptFormatter {

    private static final String TITLE = "TCHALANET - TICKET";
    private static final String SEP = "--------------------------------";

    // 80mm (monospace) – bon compromis
    // JEU / NUMEROS / MISE / GAIN
    private static final int W_GAME = 10;
    private static final int W_SEL = 10;
    private static final int W_AMT = 6;
    private static final int W_WIN = 6;

    private static final DateTimeFormatter DT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    @Override
    public ReceiptModel formatModel(TicketPrintView t, String verifyUrl) {
        var lines = new ArrayList<ReceiptLine>();

        // Header (labels en gras)
        lines.add(kv("Ticket", safe(t.ticketCode())));
        lines.add(kv("Verifier", safe(t.publicCode())));
        lines.add(kv("Terminal", maskUuid(t.terminalId())));

        // OUTLET / PDV (ajout)
        // -> nécessite TicketPrintView.outletName()
        if (notBlank(t.outletName())) {
            lines.add(kv("Outlet", safe(t.outletName())));
        }

        // Tirage (mono-draw)
        lines.add(kv("Tirage", safe(t.drawChannelLabel())));
        lines.add(kv("Tirage le", safe(t.drawWhenLabel())));
        lines.add(kv("Achat le", formatInstant(t.createdAt())));

        lines.add(ReceiptLine.text(SEP));
        lines.add(ReceiptLine.text(row4("JEU", "NUMEROS", "MISE", "GAIN")));
        lines.add(ReceiptLine.text(SEP));

        BigDecimal sumWin = BigDecimal.ZERO;
        boolean anyWin = false;

        for (var l : t.lines()) {
            String game = humanGame(l.gameCode()); // pas le channel label (mono-draw)
            String sel = humanSelection(l.selection());
            String stake = money(l.stake());

            String win;
            if (l.potentialPayout() == null) {
                win = "—";
            } else {
                win = money(l.potentialPayout());
                sumWin = sumWin.add(l.potentialPayout());
                anyWin = true;
            }

            lines.add(ReceiptLine.text(row4(game, sel, stake, win)));
        }

        lines.add(ReceiptLine.text(SEP));

        // Totaux (label en gras, montant à droite)
        lines.add(total("TOTAL MISE", money(t.totalAmount())));
        if (anyWin) {
            lines.add(total("TOTAL GAIN POT.", money(sumWin)));
        }

        // URL publique uniquement (PAS /api/..)
        lines.add(ReceiptLine.text("Scanner pour verifier"));

        return new ReceiptModel(TITLE, lines);
    }

    // ----- helpers “bold labels” -----


    private ReceiptLine kv(String label, String value) {
        return ReceiptLine.builder().bold(padLabel(label)).normal(": ").normal(value).build();
    }

    private ReceiptLine total(String label, String amount) {
        // largeur logique: SEP ~32 chars
        int totalWidth = 32;
        String left = label;
        String right = amount == null ? "" : amount;

        int spaces = Math.max(1, totalWidth - left.length() - right.length());
        return ReceiptLine.builder().bold(left).normal(" ".repeat(spaces)).normal(right).build();
    }

    // ----- table helpers -----

    private String row4(String c1, String c2, String c3, String c4) {
        // IMPORTANT: format string correct (ton ancien avait un bug)
        return String.format(
            "%-" + W_GAME + "s %-" + W_SEL + "s %" + W_AMT + "s %" + W_WIN + "s",
            trunc(c1, W_GAME),
            trunc(c2, W_SEL),
            c3 == null ? "" : c3,
            c4 == null ? "" : c4);
    }

    private String trunc(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return max <= 1 ? s.substring(0, max) : s.substring(0, max - 1) + "…";
    }

    private String padLabel(String s) {
        // alignement visuel des labels du header
        int width = 9; // ok pour "Terminal"
        if (s == null) s = "";
        if (s.length() >= width) return s.substring(0, width);
        return s + " ".repeat(width - s.length());
    }

    private String humanSelection(String raw) {
        if (raw == null) return "";
        return raw.replace(",", "-").replace(" ", "");
    }

    private String humanGame(String code) {
        if (code == null) return "";
        // MVP mapping simple (à remplacer plus tard par catalogue tenant_game/game.name)
        if (code.contains("TAKE5")) return "Take 5";
        if (code.contains("PICK3")) return "Pick 3";
        if (code.contains("PICK4")) return "Pick 4";
        if (code.contains("LOTTO")) return "Lotto";
        return code;
    }

    private String formatInstant(Instant i) {
        if (i == null) return "—";
        return DT.format(i);
    }

    private String money(BigDecimal v) {
        if (v == null) return "0.00";
        return v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String maskUuid(UUID id) {
        if (id == null) return "—";
        String s = id.toString();
        return s.substring(0, 8) + "…" + s.substring(s.length() - 4);
    }

    private String safe(String s) {
        return s == null ? "—" : s;
    }

    private boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
