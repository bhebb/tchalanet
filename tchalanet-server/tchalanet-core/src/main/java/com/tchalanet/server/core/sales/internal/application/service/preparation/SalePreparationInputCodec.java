package com.tchalanet.server.core.sales.internal.application.service.preparation;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.core.sales.api.command.sell.PromotionChoiceInput;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketCommand;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketLineInput;
import com.tchalanet.server.core.sales.api.model.communication.SaleCommunicationOptions;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Snapshots the sale input on prepare and rebuilds the exact
 * {@link SellTicketCommand} at confirm — with the stored generated promotion
 * selections pinned via {@code promotionChoices} so the confirmed ticket
 * carries exactly the previewed numbers.
 */
@Component
public class SalePreparationInputCodec {

    public Map<String, Object> toMap(SellTicketCommand command) {
        var out = new LinkedHashMap<String, Object>();
        out.put("drawId", command.drawId().value().toString());
        out.put("drawChannelId", command.drawChannelId() == null
            ? null : command.drawChannelId().value().toString());
        out.put("currency", command.currency().value());
        var comm = command.communicationOptions();
        if (comm != null && !comm.isEmpty()) {
            var c = new LinkedHashMap<String, Object>();
            c.put("sendSms", comm.sendSms());
            c.put("sendWhatsapp", comm.sendWhatsapp());
            c.put("sendEmail", comm.sendEmail());
            c.put("buyerPhoneNumber", comm.buyerPhoneNumber());
            c.put("buyerEmail", comm.buyerEmail());
            c.put("buyerLocale", comm.buyerLocale() == null ? null : comm.buyerLocale().toLanguageTag());
            out.put("communication", c);
        }
        var lines = new ArrayList<Map<String, Object>>();
        for (var line : command.lines()) {
            var l = new LinkedHashMap<String, Object>();
            l.put("lineNumber", line.lineNumber());
            l.put("gameCode", line.gameCode().name());
            l.put("betType", line.betType().name());
            l.put("rawSelection", line.rawSelection());
            l.put("betOption", line.betOption() == null ? null : line.betOption().intValue());
            l.put("stakeAmount", line.stakeAmount().toPlainString());
            lines.add(l);
        }
        out.put("lines", lines);
        return out;
    }

    @SuppressWarnings("unchecked")
    public SellTicketCommand fromMap(Map<String, Object> input, List<PromotionChoiceInput> promotionChoices) {
        var linesRaw = (List<Map<String, Object>>) input.get("lines");
        var lines = linesRaw.stream().map(l -> new SellTicketLineInput(
            ((Number) l.get("lineNumber")).intValue(),
            GameCode.valueOf((String) l.get("gameCode")),
            BetType.valueOf((String) l.get("betType")),
            (String) l.get("rawSelection"),
            l.get("betOption") == null ? null : ((Number) l.get("betOption")).shortValue(),
            new BigDecimal((String) l.get("stakeAmount"))
        )).toList();

        SaleCommunicationOptions comm = SaleCommunicationOptions.none();
        if (input.get("communication") instanceof Map<?, ?> raw) {
            var c = (Map<String, Object>) raw;
            comm = new SaleCommunicationOptions(
                Boolean.TRUE.equals(c.get("sendSms")),
                Boolean.TRUE.equals(c.get("sendWhatsapp")),
                Boolean.TRUE.equals(c.get("sendEmail")),
                (String) c.get("buyerPhoneNumber"),
                (String) c.get("buyerEmail"),
                c.get("buyerLocale") == null ? null : Locale.forLanguageTag((String) c.get("buyerLocale"))
            );
        }

        return new SellTicketCommand(
            DrawId.of(UUID.fromString((String) input.get("drawId"))),
            input.get("drawChannelId") == null
                ? null : DrawChannelId.of(UUID.fromString((String) input.get("drawChannelId"))),
            CurrencyCode.of((String) input.get("currency")),
            lines,
            comm,
            promotionChoices
        );
    }

    /** Deterministic hash of the paid input (drift detection / deduplication). */
    public String hash(SellTicketCommand command) {
        var sb = new StringBuilder();
        sb.append(command.drawId().value()).append('|').append(command.currency().value());
        command.lines().stream()
            .sorted(Comparator.comparingInt(SellTicketLineInput::lineNumber))
            .forEach(l -> sb.append('|')
                .append(l.lineNumber()).append(':')
                .append(l.gameCode()).append(':')
                .append(l.betType()).append(':')
                .append(l.rawSelection()).append(':')
                .append(l.betOption()).append(':')
                .append(l.stakeAmount().stripTrailingZeros().toPlainString()));
        return sha256(sb.toString());
    }

    private static String sha256(String value) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
