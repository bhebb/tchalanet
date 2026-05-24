package com.tchalanet.server.core.sales.api.model.promotion;

/**
 * Indicates where the final pricing / odds snapshot came from.
 * <p>
 * STANDARD:
 * Normal catalog/pricing configuration.
 * <p>
 * PROMOTION:
 * Pricing or odds were modified by a promotion decision.
 * <p>
 * Example:
 * - normal boule line: CUSTOMER + STANDARD
 * - free Maryaj line: PROMOTION + PROMOTION
 * - paid boule line with boosted odds: CUSTOMER + PROMOTION
 */
public enum TicketLinePricingSource {
    STANDARD,
    PROMOTION
}
