package com.tchalanet.server.platform.address.api;

import com.tchalanet.server.common.types.id.AddressId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.address.api.model.AddressInput;
import com.tchalanet.server.platform.address.api.model.AddressView;
import java.util.Optional;

public interface AddressApi {

    AddressId upsertTenantPrimary(TenantId tenantId, AddressInput input);

    Optional<AddressView> get(TenantId tenantId, AddressId id);
}
