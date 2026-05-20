package com.tchalanet.server.core.offlinesync.internal.domain.service;

import com.tchalanet.server.core.offlinesync.api.model.code.OfflineCodeStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

/**
 * Pure transition rule book for {@link OfflineCodeStatus}.
 *
 * <p>Hard rule: a submitted code ({@code RESERVED}) NEVER returns to {@code AVAILABLE}.
 * Technical rejection leads to {@code CONSUMED_REJECTED}; promotion success to
 * {@code CONSUMED_PROMOTED}. {@code AVAILABLE} codes may expire or be voided.
 */
public final class OfflineCodeTransitionPolicy {

    private static final Map<OfflineCodeStatus, EnumSet<OfflineCodeStatus>> ALLOWED = new EnumMap<>(OfflineCodeStatus.class);

    static {
        ALLOWED.put(OfflineCodeStatus.AVAILABLE, EnumSet.of(
            OfflineCodeStatus.RESERVED,
            OfflineCodeStatus.EXPIRED,
            OfflineCodeStatus.VOIDED
        ));
        ALLOWED.put(OfflineCodeStatus.RESERVED, EnumSet.of(
            OfflineCodeStatus.CONSUMED_PROMOTED,
            OfflineCodeStatus.CONSUMED_REJECTED
        ));
        ALLOWED.put(OfflineCodeStatus.CONSUMED_PROMOTED, EnumSet.noneOf(OfflineCodeStatus.class));
        ALLOWED.put(OfflineCodeStatus.CONSUMED_REJECTED, EnumSet.noneOf(OfflineCodeStatus.class));
        ALLOWED.put(OfflineCodeStatus.EXPIRED, EnumSet.noneOf(OfflineCodeStatus.class));
        ALLOWED.put(OfflineCodeStatus.VOIDED, EnumSet.noneOf(OfflineCodeStatus.class));
    }

    private OfflineCodeTransitionPolicy() {}

    public static boolean canTransition(OfflineCodeStatus from, OfflineCodeStatus to) {
        if (from == to) return false;
        return ALLOWED.get(from).contains(to);
    }

    public static void require(OfflineCodeStatus from, OfflineCodeStatus to) {
        if (!canTransition(from, to)) {
            throw new IllegalStateException(
                "Illegal OfflineCode transition " + from + " -> " + to);
        }
    }
}
