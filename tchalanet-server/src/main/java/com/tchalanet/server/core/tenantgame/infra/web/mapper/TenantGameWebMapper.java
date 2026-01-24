package com.tchalanet.server.core.tenantgame.infra.web.mapper;

import com.tchalanet.server.common.mapper.CommonIdMapper;
import com.tchalanet.server.core.tenantgame.domain.TenantGame;
import com.tchalanet.server.core.tenantgame.infra.web.model.TenantGameView;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = CommonIdMapper.class)
public interface TenantGameWebMapper {
    TenantGameView toView(TenantGame domain);
}
