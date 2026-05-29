package com.tchalanet.server.features.pagemodel.contract;

/**
 * Typed contract for a single alert entry in AlertsWidget payloads.
 */
public record AlertItem(
    String id,
    String messageKey,
    String severity,
    String path) {}
