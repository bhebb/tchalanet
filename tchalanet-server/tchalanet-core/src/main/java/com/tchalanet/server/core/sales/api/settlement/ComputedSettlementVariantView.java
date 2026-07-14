package com.tchalanet.server.core.sales.api.settlement;

/**
 * Computed settlement metadata for a sale line, for admin/support/debug consumers.
 *
 * <ul>
 *   <li>{@code variant} — technical settlement variant code (e.g. {@code LOTTO4_BOX_24_WAY}).
 *   <li>{@code commercialLabel} — sale-friendly label shown to seller/client (e.g. {@code
 *       Permuté}).
 *   <li>{@code adminLabel} — admin/support label that may expose the technical variant (e.g. {@code
 *       Permuté · 24-way}).
 * </ul>
 *
 * The seller terminal and customer receipt use {@code commercialLabel} only.
 */
public record ComputedSettlementVariantView(
    String variant, String commercialLabel, String adminLabel) {}
