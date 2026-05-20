package com.tchalanet.server.core.offlinesync.internal.application.service.grant;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates short user-visible offline codes. Cheap collision resistance is acceptable
 * (DB unique constraint on {@code (tenant_id, code)} guarantees integrity); we just need
 * a code dense enough to avoid frequent retries on insertion.
 *
 * <p>10 characters from a 32-symbol Crockford-style alphabet (no 0/O/1/I confusion):
 * ~50 bits of entropy, fits a printed ticket and a barcode scanner.
 */
@Component
public class OfflineCodeGenerator {

    private static final char[] ALPHABET =
        "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final int CODE_LENGTH = 10;

    private final SecureRandom random = new SecureRandom();

    public String next() {
        char[] buf = new char[CODE_LENGTH];
        for (int i = 0; i < CODE_LENGTH; i++) {
            buf[i] = ALPHABET[random.nextInt(ALPHABET.length)];
        }
        return new String(buf);
    }
}
