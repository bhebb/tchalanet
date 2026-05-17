package com.tchalanet.server.features.cashier.print;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.platform.tenantconfig.api.TenantConfigApi;
import com.tchalanet.server.platform.tenantconfig.api.model.request.GetTenantByIdRequest;
import com.tchalanet.server.platform.tenantconfig.api.model.view.TenantInternalDocumentConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TenantDocumentConfigAdapter {

    private final TenantConfigApi tenantConfigApi;

    public TenantInternalDocumentConfig.ReceiptConfig resolveReceiptConfig(TchRequestContext ctx) {
        if (ctx == null || ctx.effectiveTenantIdOrNull() == null) {
            return null;
        }

        var tenantDocument = tenantConfigApi.getTenantDocumentConfig(
            new GetTenantByIdRequest(ctx.effectiveTenantIdRequired())
        );

        return tenantDocument == null ? null : tenantDocument.receipt();
    }
}

