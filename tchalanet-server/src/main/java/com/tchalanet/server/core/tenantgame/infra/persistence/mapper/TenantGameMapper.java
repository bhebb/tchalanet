package com.tchalanet.server.core.tenantgame.infra.persistence.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.common.mapper.CommonIdMapper;
import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.core.tenantgame.domain.TenantGame;
import com.tchalanet.server.core.tenantgame.infra.persistence.TenantGameJpaEntity;
import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring", uses = {CommonIdMapper.class})
public abstract class TenantGameMapper {

  @Autowired
  protected JsonUtils jsonUtils;

  @Mapping(source = "gameId", target = "gameId")
  @Mapping(target = "tenantGameId", ignore = true)
  @Mapping(target = "tenantId", ignore = true)
  @Mapping(target = "code", ignore = true)
  @Mapping(target = "name", ignore = true)
  @Mapping(target = "category", ignore = true)
  @Mapping(target = "minDigits", ignore = true)
  @Mapping(target = "maxDigits", ignore = true)
  @Mapping(target = "combination", ignore = true)
  @Mapping(target = "flags", source = "entity.flags")
  public abstract TenantGame toDomain(TenantGameJpaEntity entity);

  @Mapping(target = "gameId", source = "gameId")
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(target = "flags", source = "domain.flags")
  public abstract TenantGameJpaEntity toEntity(TenantGame domain);

  @Mapping(target = "gameId", source = "gameId")
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "tenantId", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(target = "flags", source = "domain.flags")
  public abstract void updateEntityFromDomain(TenantGame domain, @MappingTarget TenantGameJpaEntity entity);

  protected Map<String, Object> mapJsonToMap(JsonNode node) {
    if (node == null || node.isNull()) return Map.of();
    return jsonUtils.convertValue(node, new TypeReference<Map<String, Object>>() {});
  }

  protected JsonNode mapMapToJson(Map<String, Object> map) {
    if (map == null) return jsonUtils.emptyObjectNode();
    return jsonUtils.valueToTree(map);
  }
}
