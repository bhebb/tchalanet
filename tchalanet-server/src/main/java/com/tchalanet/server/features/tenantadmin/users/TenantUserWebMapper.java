package com.tchalanet.server.features.tenantadmin.users;

import com.tchalanet.server.features.tenantadmin.users.model.TenantUserDetails;
import com.tchalanet.server.features.tenantadmin.users.model.TenantUserResponse;
import com.tchalanet.server.features.tenantadmin.users.model.TenantUserRow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TenantUserWebMapper {

    @Mapping(source = "details.keycloakSub", target = "keycloakSub")
    TenantUserResponse toResponse(TenantUserDetails details);

    // Map core query row to web row (use fully-qualified name to avoid import clash)
    TenantUserRow toRow(com.tchalanet.server.core.tenantuser.application.query.model.TenantUserRow row);

}
