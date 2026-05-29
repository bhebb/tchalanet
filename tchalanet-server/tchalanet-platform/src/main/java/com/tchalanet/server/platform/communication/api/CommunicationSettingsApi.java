package com.tchalanet.server.platform.communication.api;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.communication.api.model.value.TenantCommunicationSettingsView;

public interface CommunicationSettingsApi {

    TenantCommunicationSettingsView getTenantSettings(TenantId tenantId);
}
