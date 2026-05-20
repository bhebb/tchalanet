package com.tchalanet.server.core.outlet.internal.infra.web.admin.model;

import com.tchalanet.server.platform.address.api.model.AddressInput;

public record CreateOutletRequest(String name, String slug, AddressInput address) {
}
