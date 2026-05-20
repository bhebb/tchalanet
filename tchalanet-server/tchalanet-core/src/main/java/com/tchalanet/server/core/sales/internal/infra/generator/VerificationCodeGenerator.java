package com.tchalanet.server.core.sales.internal.infra.generator;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class VerificationCodeGenerator {

    private static final char[] CROCKFORD =
        "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();

    private static final int BODY_LENGTH = 7;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        var body = randomBody(BODY_LENGTH);
        var check = checkChar(body);
        return format(body + check);
    }

    private String randomBody(int length) {
        var chars = new char[length];

        for (int i = 0; i < length; i++) {
            chars[i] = CROCKFORD[random.nextInt(CROCKFORD.length)];
        }

        return new String(chars);
    }

    /**
     * Simple check character for manual input mistakes.
     * This is not a security feature.
     */
    private char checkChar(String body) {
        int sum = 0;

        for (int i = 0; i < body.length(); i++) {
            int value = indexOf(body.charAt(i));
            sum += value * (i + 3);
        }

        return CROCKFORD[Math.floorMod(sum, CROCKFORD.length)];
    }

    private int indexOf(char c) {
        for (int i = 0; i < CROCKFORD.length; i++) {
            if (CROCKFORD[i] == c) {
                return i;
            }
        }
        throw new IllegalArgumentException("Unsupported verification char: " + c);
    }

    private String format(String raw) {
        return raw.substring(0, 4) + "-" + raw.substring(4);
    }
}
