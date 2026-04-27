package com.tchalanet.server.core.ledger.infra.persistence.mapper;

import com.tchalanet.server.core.ledger.domain.model.LedgerEntry;
import com.tchalanet.server.core.ledger.infra.persistence.LedgerEntryJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LedgerEntryPersistenceMapper {

  @Mapping(target = "id", source = "id")
  @Mapping(
      target = "tenantId",
      expression = "java(domain.tenantId() == null ? null : domain.tenantId().uuid())")
  @Mapping(target = "refType", source = "refType")
  @Mapping(target = "refId", source = "refId")
  @Mapping(target = "amount", source = "amount")
  @Mapping(target = "direction", source = "direction")
  @Mapping(target = "occurredAt", source = "occurredAt")
  LedgerEntryJpaEntity toEntity(LedgerEntry domain);

  @Mapping(target = "id", source = "id")
  @Mapping(
      target = "tenantId",
      expression =
          "java(entity.getTenantId() == null ? null : com.tchalanet.server.common.types.id.TenantId.of(entity.getTenantId()))")
  @Mapping(target = "refType", source = "refType")
  @Mapping(target = "refId", source = "refId")
  @Mapping(target = "amount", source = "amount")
  @Mapping(target = "direction", source = "direction")
  @Mapping(target = "occurredAt", source = "occurredAt")
  LedgerEntry toDomain(LedgerEntryJpaEntity entity);
}
