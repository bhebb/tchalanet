package com.tchalanet.server.core.pricing.api.model;

/** Indicates where the effective odds for a given (game, betType, betOption) came from. */
public enum OddsSource {
    /** A per-seller-terminal override is active. */
    SELLER_TERMINAL_OVERRIDE,
    /** No override — fallback to the tenant-level catalog odds. */
    TENANT_DEFAULT
}
