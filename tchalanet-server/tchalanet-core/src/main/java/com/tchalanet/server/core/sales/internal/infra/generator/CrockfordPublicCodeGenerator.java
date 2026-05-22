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

    // 4-4 format (XXXX-XXXX): matches PublicCode pattern, 32^8 ≈ 1.1e12 keyspace.
    private static final int GROUP_LENGTH = 4;

    public String generate() {
        StringBuilder sb = new StringBuilder(GROUP_LENGTH * 2 + 1);
        for (int i = 0; i < GROUP_LENGTH; i++) {
            sb.append(CROCKFORD_BASE32.charAt(RNG.nextInt(CROCKFORD_BASE32.length())));
        }
        sb.append('-');
        for (int i = 0; i < GROUP_LENGTH; i++) {
            sb.append(CROCKFORD_BASE32.charAt(RNG.nextInt(CROCKFORD_BASE32.length())));
        }
        return sb.toString();
    }
}

