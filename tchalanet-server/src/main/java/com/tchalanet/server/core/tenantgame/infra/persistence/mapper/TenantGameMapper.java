package com.tchalanet.server.core.tenantgame.infra.persistence.mapper;

import com.tchalanet.server.common.mapper.CommonIdMapper;
import com.tchalanet.server.common.types.id.GameId;
import com.tchalanet.server.core.tenantgame.domain.TenantGame;
import com.tchalanet.server.core.tenantgame.infra.persistence.TenantGameJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for TenantGame (JPA entity <-> domain model).
 * Maps gameId (UUID) to GameId typed wrapper (per typed_ids.md).
 * Note: game metadata (name, category, etc.) is stored in domain model,
 * NOT fetched from GameJpaEntity (per inter_domain_calls.md boundaries).
 * Game validation/lookup happens at application layer via GameCatalog API.
 */
@Mapper(componentModel = "spring", uses = {CommonIdMapper.class})
public interface TenantGameMapper {

  @Mapping(source = "gameId", target = "gameId")
  @Mapping(target = "tenantGameId", ignore = true) // TODO: map from ID if available
  @Mapping(target = "code", ignore = true) // TODO: requires GameCatalog lookup (domain logic)
  @Mapping(target = "name", ignore = true)
  @Mapping(target = "category", ignore = true)
  @Mapping(target = "minDigits", ignore = true)
  @Mapping(target = "maxDigits", ignore = true)
  @Mapping(target = "combination", ignore = true)
  TenantGame toDomain(TenantGameJpaEntity entity);

  @Mapping(target = "gameId", source = "gameId")
  @Mapping(target = "id", ignore = true) // Generated
  @Mapping(target = "version", ignore = true) // Not in domain
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  TenantGameJpaEntity toEntity(TenantGame domain);

  @Mapping(target = "gameId", source = "gameId")
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "tenantId", ignore = true) // Should not change
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  void updateEntityFromDomain(TenantGame domain, @MappingTarget TenantGameJpaEntity entity);
}
