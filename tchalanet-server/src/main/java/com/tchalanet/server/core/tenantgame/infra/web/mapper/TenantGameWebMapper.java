package com.tchalanet.server.core.tenantgame.infra.web.mapper;

import tools.jackson.databind.JsonNode;
import com.tchalanet.server.common.mapper.CommonIdMapper;
import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.core.tenantgame.domain.TenantGame;
import com.tchalanet.server.core.tenantgame.infra.web.model.TenantGameView;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring", uses = {CommonIdMapper.class})
public abstract class TenantGameWebMapper {

  @Autowired
  protected JsonUtils jsonUtils;

  @Mapping(target = "gameCode", source = "domain.code")
  @Mapping(target = "flags", expression = "java(map(domain.flags()))")
  public abstract TenantGameView toView(TenantGame domain);

  /** Pass-through mapping for JsonNode (Jackson 3). */
  public JsonNode map(JsonNode value) {
    return value;
  }
}
