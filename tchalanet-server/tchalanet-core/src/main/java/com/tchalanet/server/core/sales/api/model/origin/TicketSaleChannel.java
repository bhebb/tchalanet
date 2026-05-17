package com.tchalanet.server.core.sales.api.model.origin;

public enum TicketSaleChannel {
    POS_ONLINE          (ApprovalPolicy.ALLOWED),
    POS_OFFLINE_SYNCED  (ApprovalPolicy.FORBIDDEN),  // décidé en amont par offlinesync
    WEB                 (ApprovalPolicy.ALLOWED),
    PARTNER_API         (ApprovalPolicy.ALLOWED),
    PARTNER_BATCH       (ApprovalPolicy.FORBIDDEN),  // batches arrivent déjà tranchés
    ADMIN_ADJUSTMENT    (ApprovalPolicy.FORBIDDEN);  // l'admin assume

    private final ApprovalPolicy approvalPolicy;

    TicketSaleChannel(ApprovalPolicy policy) { this.approvalPolicy = policy; }

    public boolean allowsPendingApproval() {
        return approvalPolicy == ApprovalPolicy.ALLOWED;
    }

    private enum ApprovalPolicy { ALLOWED, FORBIDDEN }
}
