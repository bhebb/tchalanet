package com.tchalanet.server.core.uslottery.internal.infra.external.oh.auth;

import com.tchalanet.server.common.json.utils.JsonUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

public final class JwtExpiryExtractor {

    private JwtExpiryExtractor() {
    }

    public static Optional<Instant> extractExp(JsonUtils jsonUtils, String jwt) {
        try {
            var parts = jwt.split("\\.");
            if (parts.length < 2) {
                return Optional.empty();
            }

            var payloadJson = new String(
                Base64.getUrlDecoder().decode(parts[1]),
                StandardCharsets.UTF_8
            );

            var node = jsonUtils.parse(payloadJson);
            var exp = node == null ? null : node.get("exp");

            if (exp == null || !exp.canConvertToLong()) {
                return Optional.empty();
            }

            return Optional.of(Instant.ofEpochSecond(exp.asLong()));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }
}
