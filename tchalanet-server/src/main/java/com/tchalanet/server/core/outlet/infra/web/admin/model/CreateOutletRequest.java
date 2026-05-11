package com.tchalanet.server.core.outlet.infra.web.admin.model;

import com.tchalanet.server.core.address.application.model.AddressInput;

public record CreateOutletRequest(String name, String slug, AddressInput address) {
}
