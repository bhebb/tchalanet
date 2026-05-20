package com.tchalanet.server.platform.tenantconfig.api.model.view;

public record TenantInternalDocumentConfig(ReceiptConfig receipt) {

    /**
     * {@code enabled=true} means tenant receipt preferences are applied as overrides/fallbacks.
     * It does not disable ticket printing. Print blocking must rely on outlet-level policy.
     */
    public record ReceiptConfig(
        boolean enabled,
        String displayName,
        String headerMessage,
        String footerMessage,
        String defaultPaperSize,
        boolean showQrCode,
        boolean showSellerName,
        boolean showOutletName,
        boolean showPotentialPayout,
        String defaultTemplateKey
    ) {}
}

