package com.tchalanet.server.core.sales.internal.infra.generator;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates a public shareable code for ticket verification (QR/SMS/URL).
 * <p>
 * Requirements:
 * - Short and human-friendly
 * - Non-guessable (random)
 * - Avoid ambiguous characters (Crockford Base32)
 * <p>
 * Note: collisions are extremely unlikely, but still possible.
 * Handle collision by retrying when DB unique constraint fails.
 */
@Component
public class CrockfordPublicCodeGenerator {

    private static final String CROCKFORD_BASE32 = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    private static final SecureRandom RNG = new SecureRandom();

    // 12 gives huge space: 32^12 ≈ 1.15e18
    private static final int LENGTH = 12;

    public String generate() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(CROCKFORD_BASE32.charAt(RNG.nextInt(CROCKFORD_BASE32.length())));
        }
        return sb.toString();
    }
}

