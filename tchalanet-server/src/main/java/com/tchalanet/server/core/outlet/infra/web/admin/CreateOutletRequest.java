package com.tchalanet.server.core.outlet.infra.web.admin;

import com.tchalanet.server.core.address.application.dto.AddressDto;

public record CreateOutletRequest(String name, String slug, AddressDto address) {}
