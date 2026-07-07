package com.tchalanet.server.features.tenantadmin.pricing;

import com.tchalanet.server.platform.audit.api.AuditLog;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.assertj.core.api.Assertions.assertThat;

class TenantAdminPricingControllerTest {

    @Test
    void tenantPricingControllerRequiresGamePricingUpdatePermission() {
        var annotation = TenantAdminPricingController.class.getAnnotation(PreAuthorize.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("hasPermission(null, 'game-pricing.update')");
    }

    @Test
    void tenantPricingMutationsAreAudited() throws NoSuchMethodException {
        var upsert = TenantAdminPricingController.class
            .getMethod("upsert", TenantAdminPricingController.UpsertTenantOddsRequest.class,
                com.tchalanet.server.common.context.TchRequestContext.class)
            .getAnnotation(AuditLog.class);
        var delete = TenantAdminPricingController.class
            .getMethod("delete", TenantAdminPricingController.DeleteTenantOddsRequestBody.class,
                com.tchalanet.server.common.context.TchRequestContext.class)
            .getAnnotation(AuditLog.class);

        assertThat(upsert.entity()).isEqualTo(AuditEntityType.ODDS);
        assertThat(upsert.action()).isEqualTo(AuditAction.UPDATE);
        assertThat(delete.entity()).isEqualTo(AuditEntityType.ODDS);
        assertThat(delete.action()).isEqualTo(AuditAction.DELETE);
    }
}
