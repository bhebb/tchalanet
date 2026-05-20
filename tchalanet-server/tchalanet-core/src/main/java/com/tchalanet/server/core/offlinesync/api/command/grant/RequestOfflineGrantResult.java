package com.tchalanet.server.core.offlinesync.api.command.grant;

import com.tchalanet.server.common.types.id.OfflineCodeBatchId;
import com.tchalanet.server.common.types.id.OfflineGrantId;

import java.time.Instant;
import java.util.List;

/**
 * Server response carrying the signed grant metadata + the freshly allocated code batch
 * + the list of draws the cashier is allowed to sell offline against.
 *
 * @param grantSignature Base64 Ed25519 signature of the canonical grant payload — POS
 *                       verifies it offline with the server public key before producing sales.
 * @param upcomingDraws  Snapshot of {@code SCHEDULED}/{@code OPEN} draws within the configured
 *                       lookahead window (default 3 days). The device caches this list and
 *                       pins one {@code drawId} on every offline sale, so the server doesn't
 *                       have to guess the draw at sync time.
 */
public record RequestOfflineGrantResult(
    OfflineGrantId grantId,
    OfflineCodeBatchId codeBatchId,
    Instant validFrom,
    Instant validUntil,
    Instant syncAcceptedUntil,
    int maxTicketCount,
    String currency,
    List<String> offlineCodes,
    String grantSignature,
    String serverPublicKey,
    List<OfflineUpcomingDrawSnapshot> upcomingDraws
) {}
