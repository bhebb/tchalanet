package com.tchalanet.server.core.tenantgame.infra.persistence.mapper;

import com.tchalanet.server.core.tenantgame.domain.TenantGame;
import com.tchalanet.server.core.tenantgame.infra.persistence.TenantGameJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TenantGameMapper {

  @Mapping(target = "gameCode", source = "game.code")
  TenantGame toDomain(TenantGameJpaEntity entity);

  @Mapping(target = "game", ignore = true) // Handled manually or via helper
  @Mapping(target = "id", ignore = true) // Generated
  TenantGameJpaEntity toEntity(TenantGame domain);

  @Mapping(target = "game", ignore = true)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "tenantId", ignore = true) // Should not change
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  void updateEntityFromDomain(TenantGame domain, @MappingTarget TenantGameJpaEntity entity);
}
