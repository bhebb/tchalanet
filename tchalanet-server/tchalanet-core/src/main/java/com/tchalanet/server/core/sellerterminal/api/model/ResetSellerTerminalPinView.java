package com.tchalanet.server.core.sellerterminal.api.model;

import java.time.Instant;
import java.util.UUID;

public record ResetSellerTerminalPinView(
    UUID sellerTerminalId,
    String terminalCode,
    String temporaryPin,
    boolean mustChangePin,
    Instant pinResetAt
) {}
