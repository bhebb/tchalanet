package com.tchalanet.server.core.offlinesync.internal.application.service.grant;

import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineCryptoPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/**
 * Builds the canonical byte representation of a grant payload and asks the crypto adapter
 * to sign it. The canonical format is versioned so device-side verifiers stay forward-compatible.
 *
 * <p>Format v1:
 * <pre>
 *   v1
 *   |grantId
 *   |tenantId
 *   |deviceId
 *   |keyId
 *   |validFrom.epochMilli
 *   |validUntil.epochMilli
 *   |syncAcceptedUntil.epochMilli
 *   |maxTicketCount
 *   |maxTotalAmount.value:currency
 * </pre>
 */
@Component
@RequiredArgsConstructor
public class OfflineGrantPayloadSigner {

    public static final String VERSION = "v1";
    private static final String SEP = "|";

    private final OfflineCryptoPort crypto;

    public String sign(
        OfflineGrantId grantId, UUID tenantUuid, UUID deviceId, String keyId,
        Instant validFrom, Instant validUntil, Instant syncAcceptedUntil,
        int maxTicketCount, Money maxTotalAmount
    ) {
        String canonical = VERSION
            + SEP + grantId.value()
            + SEP + tenantUuid
            + SEP + deviceId
            + SEP + keyId
            + SEP + validFrom.toEpochMilli()
            + SEP + validUntil.toEpochMilli()
            + SEP + syncAcceptedUntil.toEpochMilli()
            + SEP + maxTicketCount
            + SEP + maxTotalAmount.amount().toPlainString()
            + ':' + maxTotalAmount.currency().value();
        return crypto.signGrant(canonical.getBytes(StandardCharsets.UTF_8));
    }
}
