package com.tchalanet.server.platform.audit.api.model;

public enum AuditAction {
    // ── Generic lifecycle ─────────────────────────────────────────────────
    CREATE,
    UPDATE,
    DELETE,
    SOFT_DELETE,
    RESTORE,
    STATE_CHANGE,

    // ── Sales (canonical: TICKET_SELL / TICKET_VOID) ──────────────────────
    TICKET_SELL,
    TICKET_VOID,
    SELL_TICKET,      // legacy alias — prefer TICKET_SELL
    CANCEL_TICKET,
    OVERRIDE_RESULT,
    PRINT_TICKET,

    // ── Payout (canonical: PAYOUT_PAID) ───────────────────────────────────
    PAYOUT_REQUEST,
    PAYOUT_APPROVE,
    PAYOUT_REJECT,
    PAYOUT_PAID,
    PAYOUT_EXECUTE,   // legacy alias — prefer PAYOUT_PAID
    PAY,              // legacy alias — prefer PAYOUT_PAID

    // ── Kill switches ────────────────────────────────────────────────────
    RESULT_SLOT_DISABLE,
    RESULT_SLOT_GAME_DISABLE,
    DRAW_CHANNEL_DISABLE,

    // ── Draw ─────────────────────────────────────────────────────────────
    DRAW_RESULT_PROPOSE,
    DRAW_RESULT_CONFIRM,
    DRAW_GENERATE,
    DRAW_OPEN,
    DRAW_CLOSE,
    DRAW_RESULT_FETCH,
    DRAW_RESULT_APPLY,
    DRAW_RESULT_OVERRIDE,
    DRAW_RESULT_MANUAL,
    DRAW_RESULT_REFRESH,
    DRAW_CORRECT_APPLIED_RESULT,
    DRAW_CANCEL,
    DRAW_RESCHEDULE,
    DRAW_LOCK,
    DRAW_UNLOCK,
    DRAW_ARCHIVE,
    DRAW_OVERRIDE,
    DRAW_SETTLE,
    DRAW_GENERATE_BACKFILL,
    SETTLE,

    // ── Outlet (canonical: OUTLET_LOCK / OUTLET_UNLOCK) ───────────────────
    OUTLET_CREATE,
    OUTLET_UPDATE,
    OUTLET_DELETE,
    OUTLET_LOCK,
    OUTLET_UNLOCK,
    OUTLET_BLOCK,     // legacy alias — prefer OUTLET_LOCK
    OUTLET_UNBLOCK,   // legacy alias — prefer OUTLET_UNLOCK
    OUTLET_BLOCK_SALES,
    OUTLET_UNBLOCK_SALES,
    OUTLET_USER_ASSIGN,
    OUTLET_USER_REMOVE,
    OUTLET_DAY_CLOSE,
    OUTLET_DAY_REOPEN,

    // ── Terminal ──────────────────────────────────────────────────────────
    TERMINAL_CREATE,
    TERMINAL_UPDATE,
    TERMINAL_DELETE,
    TERMINAL_REGISTER,
    TERMINAL_UNREGISTER,
    TERMINAL_LOCK,
    TERMINAL_UNLOCK,
    TERMINAL_ASSIGN_OUTLET,
    TERMINAL_ASSIGN_USER,
    TERMINAL_ACTIVATE_FOR_USER,
    TERMINAL_CHALLENGE_CREATE,
    TERMINAL_BINDING_CREATE,
    TERMINAL_BINDING_REVOKE,
    TERMINAL_SYNC_STATE_UPDATE,
    TERMINAL_METADATA_UPDATE,
    TERMINAL_HEARTBEAT,
    TERMINAL_OPERATIONAL_CONTROL_SET,

    // ── SellerTerminal ────────────────────────────────────────────────────
    SELLER_TERMINAL_CREATE,
    SELLER_TERMINAL_UPDATE,
    SELLER_TERMINAL_BLOCK,
    SELLER_TERMINAL_UNBLOCK,
    SELLER_TERMINAL_DISABLE,
    SELLER_TERMINAL_RESET_ACCESS,
    SELLER_TERMINAL_PIN_RESET,
    SELLER_TERMINAL_PIN_CHANGE,
    SELLER_TERMINAL_COMMISSION_CHANGE,

    // ── Identity/access (canonical: ROLE_ASSIGN) ─────────────────────────
    ROLE_ASSIGN,
    USER_CREATE,
    USER_UPDATE,
    USER_ROLE_CHANGE,  // legacy alias — prefer ROLE_ASSIGN
    APP_USER_EXTERNAL_IDENTITY_LINKED,
    APP_USER_BOOTSTRAP_DENIED,
    APP_USER_BOOTSTRAP_CREATED,
    APP_USER_BOOTSTRAP_INVITE_CONSUMED,

    // ── Limit / commission / config ───────────────────────────────────────
    LIMIT_UPDATE,
    COMMISSION_UPDATE,
    TENANT_THEME_UPDATE,
    FEATURE_FLAG_UPDATE,

    // ── Promotion ─────────────────────────────────────────────────────────
    PROMOTION_ACTIVATE,
    PROMOTION_PAUSE,

    // ── Tenant / platform (canonical: TENANT_OVERRIDE) ───────────────────
    TENANT_CREATE,
    TENANT_UPDATE,
    TENANT_DISABLE,
    TENANT_OVERRIDE,
    SUPER_ADMIN_OVERRIDE,  // legacy alias — prefer TENANT_OVERRIDE
    OVERRIDE_TENANT,       // legacy alias — prefer TENANT_OVERRIDE

    // ── Ops / scheduler (canonical: OPS_FORCE_JOB) ───────────────────────
    OPS_FORCE_JOB,
    FORCE_OPERATION,   // legacy alias — prefer OPS_FORCE_JOB
    BATCH_JOB_START,

    // ── Auth ─────────────────────────────────────────────────────────────
    LOGIN,
    LOGOUT,

    // ── Platform ops ─────────────────────────────────────────────────────
    CACHE_CLEAR,
    AUDIT_PURGE,

    // ── Archive (canonical: ARCHIVE_RUN / ARCHIVE_RESTORE) ───────────────
    ARCHIVE_RUN,
    ARCHIVE_RESTORE,
    ARCHIVE,           // legacy alias — prefer ARCHIVE_RUN or ARCHIVE_RESTORE

    // ── Misc ─────────────────────────────────────────────────────────────
    LIST,  // audit read-many operations
    OTHER
}
