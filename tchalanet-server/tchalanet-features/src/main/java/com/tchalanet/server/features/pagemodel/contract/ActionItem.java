package com.tchalanet.server.features.pagemodel.contract;

/**
 * Typed contract for a quick-action entry in QuickActionsWidget payloads.
 * Uses {@code path} (not {@code route}) and {@code labelKey} (not {@code label})
 * per harden-pagemodel-security-v2 normalization (D4).
 */
public record ActionItem(
    String id,
    String labelKey,
    String icon,
    String path) {}
