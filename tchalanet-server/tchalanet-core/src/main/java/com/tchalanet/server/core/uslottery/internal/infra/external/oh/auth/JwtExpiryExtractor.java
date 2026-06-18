package com.tchalanet.server.core.uslottery.internal.infra.external.oh.auth;

import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

public final class JwtExpiryExtractor {

    private JwtExpiryExtractor() {
    }

    public static Optional<Instant> extractExp(String jwt) {
        try {
            var parts = jwt.split("\\.");
            if (parts.length < 2) {
                return Optional.empty();
            }

            var payloadJson = new String(
                Base64.getUrlDecoder().decode(parts[1]),
                StandardCharsets.UTF_8
            );

            var mapper = new JsonMapper();
            var node = mapper.readTree(payloadJson);
            var exp = node.get("exp");

            if (exp == null || !exp.canConvertToLong()) {
                return Optional.empty();
            }

            return Optional.of(Instant.ofEpochSecond(exp.asLong()));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }
}
