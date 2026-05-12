package com.tchalanet.server.core.sales.internal.application.print;

import com.tchalanet.server.common.document.receipt.ReceiptLine;
import com.tchalanet.server.common.document.receipt.ReceiptModel;
import com.tchalanet.server.common.types.enums.BetType;
import com.tchalanet.server.core.sales.application.port.out.TicketPrintLine;
import com.tchalanet.server.core.sales.application.port.out.TicketPrintView;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public abstract class AbstractTicketReceiptFormatter implements TicketReceiptFormatter {

  protected static final String SEP = "--------------------------------";
  protected static final int W_GAME = 10;
  protected static final int W_SEL = 10;
  protected static final int W_AMT = 6;
  protected static final int W_WIN = 6;

  protected final DateTimeFormatter dt =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("UTC"));

  protected abstract String title();

  protected abstract Labels labels();

  protected abstract String sanitize(String s);

  protected abstract String ellipsis(String s, int max);

  protected record Labels(
      String ticket,
      String verify,
      String terminal,
      String outlet,
      String draw,
      String drawAt,
      String soldAt,
      String scan,
      String gameHdr,
      String selHdr,
      String stakeHdr,
      String winHdr,
      String totalStake,
      String totalWin) {}

  @Override
  public ReceiptModel formatModel(TicketPrintView t, String verifyUrl) {
    Labels L = labels();
    var out = new ArrayList<ReceiptLine>();

    // Header
    out.add(kv(L.ticket, safe(t.ticketCode())));
    out.add(kv(L.verify, safe(t.publicCode())));
    out.add(kv(L.terminal, maskUuid(t.terminalId())));
    if (notBlank(t.outletName())) out.add(kv(L.outlet, safe(t.outletName())));

    out.add(kv(L.draw, safe(t.drawChannelLabel())));
    out.add(kv(L.drawAt, safe(t.drawWhenLabel())));
    out.add(kv(L.soldAt, formatInstant(t.createdAt())));

    // Group by (betType, betOption)
    record Key(BetType betType, Short betOption) {}
    Map<Key, List<TicketPrintLine>> groups = new LinkedHashMap<>();
    for (var l : safeLines(t.lines())) {
      groups.computeIfAbsent(new Key(l.betType(), l.betOption()), k -> new ArrayList<>()).add(l);
    }

    BigDecimal sumPotential = BigDecimal.ZERO;
    boolean anyPotential = false;

    for (var entry : groups.entrySet()) {
      var key = entry.getKey();
      var lines = entry.getValue();

      out.add(ReceiptLine.text(SEP));
      out.add(sectionTitle(sectionLabel(key.betType, key.betOption)));
      out.add(ReceiptLine.text(row4(L.gameHdr, L.selHdr, L.stakeHdr, L.winHdr)));
      out.add(ReceiptLine.text(SEP));

      for (var l : lines) {
        String game = ellipsis(humanGame(l.gameCode()), W_GAME);
        String sel = ellipsis(humanSelection(l.selection()), W_SEL);
        String stake = money(l.stake());

        String win;
        if (l.potentialPayout() == null) {
          win = "-";
        } else {
          win = money(l.potentialPayout());
          sumPotential = sumPotential.add(l.potentialPayout());
          anyPotential = true;
        }

        out.add(ReceiptLine.text(row4(game, sel, stake, win)));
      }
    }

    out.add(ReceiptLine.text(SEP));
    out.add(total(L.totalStake, money(t.totalAmount())));
    if (anyPotential) out.add(total(L.totalWin, money(sumPotential)));
    out.add(ReceiptLine.text(SEP));

    out.add(ReceiptLine.text(sanitize(L.scan)));

    return new ReceiptModel(title(), out);
  }

  // ----- section label -----
  protected String sectionLabel(BetType bt, Short opt) {
    if (bt == null) return "BET";
    String base = humanBetType(bt);
    if (bt.requiresBetOption()) return base + " - OPT " + (opt == null ? "?" : opt);
    return base;
  }

  protected String humanBetType(BetType bt) {
    return switch (bt) {
      case MATCH_1_2D -> "BOLET 1";
      case MATCH_2_2D -> "BOLET 2";
      case MATCH_3_2D -> "BOLET 3";
      case MARRIAGE_2D2D -> "MARRIAGE";
      case LOTTO3_3D -> "LOTO 3";
      case LOTTO4_PATTERN -> "LOTO 4";
      case LOTTO5_PATTERN -> "LOTO 5";
    };
  }

  // ----- receipt line builders -----

  protected ReceiptLine sectionTitle(String label) {
    return ReceiptLine.builder().bold(sanitize(label)).build();
  }

  protected ReceiptLine kv(String label, String value) {
    return ReceiptLine.builder()
        .bold(padLabel(sanitize(label)))
        .normal(": ")
        .normal(sanitize(value))
        .build();
  }

  protected ReceiptLine total(String label, String amount) {
    int totalWidth = 32;
    String left = sanitize(label);
    String right = sanitize(amount);
    int spaces = Math.max(1, totalWidth - left.length() - right.length());
    return ReceiptLine.builder().bold(left).normal(" ".repeat(spaces)).normal(right).build();
  }

  protected String row4(String c1, String c2, String c3, String c4) {
    return String.format(
        "%-" + W_GAME + "s %-" + W_SEL + "s %" + W_AMT + "s %" + W_WIN + "s",
        trunc(c1, W_GAME),
        trunc(c2, W_SEL),
        c3 == null ? "" : c3,
        c4 == null ? "" : c4);
  }

  protected String trunc(String text, int max) {
    if (text == null) return "";
    if (text.length() <= max) return text;
    return ellipsis(text, max);
  }

  protected String padLabel(String text) {
    int width = 9;
    if (text == null) text = "";
    if (text.length() >= width) return text.substring(0, width);
    return text + " ".repeat(width - text.length());
  }

  protected String humanSelection(String raw) {
    if (raw == null) return "";
    return raw.replace(",", "-").replace(" ", "");
  }

  protected String humanGame(String code) {
    if (code == null) return "";
    String upperCode = code.toUpperCase(Locale.ROOT);
    if (upperCode.contains("HT_LOTO3")) return "Loto3";
    if (upperCode.contains("HT_LOTO4")) return "Loto4";
    if (upperCode.contains("HT_LOTO5")) return "Loto5";
    if (upperCode.contains("HT_MARYAJ")) return "Maryaj";
    if (upperCode.contains("HT_BOLET")) return "Bolet";
    if (upperCode.contains("HT_NUMERO")) return "Numero";
    return code;
  }

  protected String formatInstant(Instant instant) {
    if (instant == null) return "-";
    return dt.format(instant);
  }

  protected String money(BigDecimal amount) {
    if (amount == null) return "0.00";
    return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
  }

  protected String maskUuid(UUID id) {
    if (id == null) return "-";
    String idString = id.toString();
    return idString.substring(0, 8) + "..." + idString.substring(idString.length() - 4);
  }

  protected String safe(String value) {
    return value == null ? "-" : value;
  }

  protected boolean notBlank(String s) {
    return s != null && !s.trim().isEmpty();
  }

  protected List<TicketPrintLine> safeLines(List<TicketPrintLine> lines) {
    return lines == null ? List.of() : lines;
  }
}
