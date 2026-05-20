package com.tchalanet.server.core.offlinesync.internal.application.service.sync;

import com.tchalanet.server.core.offlinesync.api.command.sync.SyncOfflineSalesCommand;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Comparator;
import java.util.HexFormat;

/**
 * Canonical, deterministic SHA-256 of an offline submission payload. The format is
 * versioned so that a future change to the canonical representation can coexist with
 * older device payloads.
 *
 * <p>Format v1:
 * <pre>
 *   v1
 *   |clientSubmissionId
 *   |offlineCode
 *   |clientSoldAt.epochMilli
 *   |totalStakeAmount.value:currency
 *   |lineCount
 *   ||line[0]|line[1]|...
 * </pre>
 * Each line: {@code lineNo:gameCode:betType:betOption:selectionKey:stake.value:currency:potentialPayout.valueOrEMPTY:currencyOrEMPTY}.
 * Lines are sorted by {@code lineNo} for determinism.
 */
@Component
public class OfflineSubmissionPayloadHasher {

    public static final String VERSION = "v1";
    private static final String SEP = "|";

    public String hash(SyncOfflineSalesCommand.Submission sub) {
        StringBuilder sb = new StringBuilder(256);
        sb.append(VERSION)
            .append(SEP).append(nullSafe(sub.clientSubmissionId()))
            .append(SEP).append(nullSafe(sub.offlineCode()))
            .append(SEP).append(sub.clientSoldAt().toEpochMilli())
            .append(SEP).append(sub.totalStakeAmount().amount().toPlainString())
            .append(':').append(sub.totalStakeAmount().currency().value())
            .append(SEP).append(sub.lineCount());

        sub.lines().stream()
            .sorted(Comparator.comparingInt(SyncOfflineSalesCommand.Line::lineNo))
            .forEach(l -> sb.append(SEP).append(SEP)
                .append(l.lineNo())
                .append(':').append(nullSafe(l.gameCode()))
                .append(':').append(nullSafe(l.betType()))
                .append(':').append(nullSafe(l.betOption()))
                .append(':').append(nullSafe(l.selectionKey()))
                .append(':').append(l.stakeAmount().amount().toPlainString())
                .append(':').append(l.stakeAmount().currency().value())
                .append(':').append(l.potentialPayout() != null
                    ? l.potentialPayout().amount().toPlainString() : "")
                .append(':').append(l.potentialPayout() != null
                    ? l.potentialPayout().currency().value() : ""));

        return sha256Hex(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    /** Convenience for tests: also expose a base64 variant if anyone prefers. */
    public String hashBase64(SyncOfflineSalesCommand.Submission sub) {
        return Base64.getEncoder().encodeToString(hash(sub).getBytes(StandardCharsets.UTF_8));
    }
}
