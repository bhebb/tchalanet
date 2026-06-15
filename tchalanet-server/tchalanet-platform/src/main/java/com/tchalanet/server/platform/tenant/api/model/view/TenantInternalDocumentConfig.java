package com.tchalanet.server.platform.tenant.api.model.view;

public record TenantInternalDocumentConfig(ReceiptConfig receipt) {

    /**
     * {@code enabled=true} means tenant receipt preferences are applied as overrides/fallbacks.
     * It does not disable ticket printing. Print blocking must rely on outlet-level policy.
     */
    public record ReceiptConfig(
        Boolean enabled,
        String displayName,
        String headerMessage,
        String footerMessage,
        String defaultPaperSize,
        Boolean showQrCode,
        Boolean showSellerName,
        Boolean showOutletName,
        Boolean showPotentialPayout,
        String defaultTemplateKey
    ) {}
}

