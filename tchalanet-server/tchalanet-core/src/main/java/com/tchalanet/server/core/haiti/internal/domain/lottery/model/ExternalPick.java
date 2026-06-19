package com.tchalanet.server.core.haiti.internal.domain.lottery.model;

import com.tchalanet.server.core.haiti.internal.domain.lottery.exception.InvalidExternalPickException;

public record ExternalPick(String pick3, String pick4) {

    public static ExternalPick of(String p3, String p4) {
        String n3 = normalize(p3);
        String n4 = normalize(p4);

        if (n3 == null || n3.length() != 3) {
            throw new InvalidExternalPickException("pick3 must be 3 digits");
        }
        if (n4 == null || n4.length() != 4) {
            throw new InvalidExternalPickException("pick4 must be 4 digits");
        }

        return new ExternalPick(n3, n4);
    }

    public static ExternalPick partial(String p3, String p4) {
        String n3 = normalize(p3);
        String n4 = normalize(p4);

        if (n3 != null && n3.length() != 3) {
            throw new InvalidExternalPickException("pick3 must be 3 digits when present");
        }
        if (n4 != null && n4.length() != 4) {
            throw new InvalidExternalPickException("pick4 must be 4 digits when present");
        }
        if (n3 == null && n4 == null) {
            throw new InvalidExternalPickException("at least one external pick is required");
        }

        return new ExternalPick(n3, n4);
    }

    public boolean hasPick3() {
        return pick3 != null && !pick3.isBlank();
    }

    public boolean hasPick4() {
        return pick4 != null && !pick4.isBlank();
    }

    public boolean complete() {
        return hasPick3() && hasPick4();
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim().replaceAll("\\s+", "");

        if (trimmed.isEmpty()) {
            return null;
        }

        if (!trimmed.chars().allMatch(Character::isDigit)) {
            throw new InvalidExternalPickException("picks must contain digits only");
        }

        return trimmed;
    }
}
