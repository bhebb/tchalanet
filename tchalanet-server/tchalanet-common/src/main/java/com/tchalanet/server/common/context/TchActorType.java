package com.tchalanet.server.common.context;

/**
 * Runtime actor type for the canonical request context.
 *
 * <p>APP_USER  — human or admin actor mapped from provider identity to AppUser.
 * <p>SELLER_TERMINAL — operational selling actor mapped from provider identity to SellerTerminal.
 * <p>SYSTEM — batch/scheduler context; never produced by HTTP identity bootstrap.
 */
public enum TchActorType {
    APP_USER,
    SELLER_TERMINAL,
    SYSTEM
}
