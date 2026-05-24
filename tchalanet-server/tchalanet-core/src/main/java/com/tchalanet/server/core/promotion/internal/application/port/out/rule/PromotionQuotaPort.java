package com.tchalanet.server.core.promotion.internal.application.port.out.rule;

public interface PromotionQuotaPort {
    /** Atomically consumes one quota slot. Returns false if exhausted. */
    boolean tryConsume(String quotaKey);
}

