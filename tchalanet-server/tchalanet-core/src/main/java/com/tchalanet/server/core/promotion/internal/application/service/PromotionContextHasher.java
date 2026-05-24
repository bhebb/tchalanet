package com.tchalanet.server.core.promotion.internal.application.service;

import com.tchalanet.server.core.promotion.api.model.PromotionEvaluationContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class PromotionContextHasher {
    public String hash(PromotionEvaluationContext c) {
        var raw = String.join("|",
            String.valueOf(c.tenantId()),
            String.valueOf(c.phase()),
            String.valueOf(c.outletId()),
            String.valueOf(c.terminalId()),
            String.valueOf(c.sellerUserId()),
            String.valueOf(c.paidTotal()),
            String.valueOf(c.currency()),
            String.join(",", c.paidGameCodes()),
            String.valueOf(c.offline())
        );
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("promotionDecision.context_hash_failed", e);
        }
    }
}
