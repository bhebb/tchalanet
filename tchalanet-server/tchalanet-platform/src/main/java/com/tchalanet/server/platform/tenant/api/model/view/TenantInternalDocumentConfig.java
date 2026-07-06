package com.tchalanet.server.platform.tenant.api.model.view;

public record TenantInternalDocumentConfig(ReceiptConfig receipt) {

    /**
     * {@code enabled=true} means tenant receipt preferences are applied as overrides/fallbacks.
     * It does not disable ticket printing.
     */
    public record ReceiptConfig(
        Boolean enabled,
        String headerMessage,
        String footerMessage,
        String defaultPaperSize,
        Boolean showQrCode,
        String defaultTemplateKey
    ) {}
}
