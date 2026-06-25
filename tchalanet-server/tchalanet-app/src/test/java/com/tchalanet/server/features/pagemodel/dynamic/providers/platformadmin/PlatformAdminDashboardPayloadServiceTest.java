package com.tchalanet.server.features.pagemodel.dynamic.providers.platformadmin;

import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelDynamicProviderException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlatformAdminDashboardPayloadServiceTest {

    private final PlatformAdminDashboardPayloadAssembler assembler =
        mock(PlatformAdminDashboardPayloadAssembler.class);
    private final PlatformAdminOpsDashboardPayloadAssembler opsAssembler =
        mock(PlatformAdminOpsDashboardPayloadAssembler.class);
    private final PlatformAdminDashboardPayloadService service =
        new PlatformAdminDashboardPayloadService(assembler, opsAssembler);

    @Test
    @DisplayName("routes the current superadmin logicalId to the commercial assembler")
    void routesCommercialLogicalId() {
        PlatformAdminDashboardPayloadAssembler.Payload payload =
            new PlatformAdminDashboardPayloadAssembler.Payload(null, null, null, null, null, null, null);
        when(assembler.assemble(null)).thenReturn(payload);

        var resolved = service.assemble("private.dashboard.superadmin", null);

        assertThat(resolved).isSameAs(payload);
        verify(assembler).assemble(null);
    }

    @Test
    @DisplayName("routes the ops logicalId to the ops assembler")
    void routesOpsLogicalId() {
        PlatformAdminOpsDashboardPayloadAssembler.Payload payload =
            new PlatformAdminOpsDashboardPayloadAssembler.Payload(null, null, null, null, null, null);
        when(opsAssembler.assemble(null)).thenReturn(payload);

        var resolved = service.assemble("private.dashboard.superadmin.ops", null);

        assertThat(resolved).isSameAs(payload);
        verify(opsAssembler).assemble(null);
    }

    @Test
    @DisplayName("requires a logicalId")
    void requiresLogicalId() {
        assertThatThrownBy(() -> service.assemble(" ", null))
            .isInstanceOf(PageModelDynamicProviderException.class)
            .extracting("code")
            .isEqualTo("PLATFORM_ADMIN_DASHBOARD_LOGICAL_ID_REQUIRED");
    }

    @Test
    @DisplayName("rejects unsupported logicalIds")
    void rejectsUnsupportedLogicalId() {
        assertThatThrownBy(() -> service.assemble("private.dashboard.superadmin.unknown", null))
            .isInstanceOf(PageModelDynamicProviderException.class)
            .extracting("code")
            .isEqualTo("PLATFORM_ADMIN_DASHBOARD_UNSUPPORTED_LOGICAL_ID");
    }
}
