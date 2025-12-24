package com.tchalanet.server.core.ledger.infra.persistence.mapper;

import com.tchalanet.server.core.ledger.domain.model.LedgerEntry;
import com.tchalanet.server.core.ledger.infra.persistence.LedgerEntryJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LedgerEntryPersistenceMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "refType", source = "refType")
    @Mapping(target = "refId", source = "refId")
    @Mapping(target = "amount", source = "amount")
    @Mapping(target = "direction", source = "direction")
    @Mapping(target = "occurredAt", source = "occurredAt")
    LedgerEntryJpaEntity toEntity(LedgerEntry domain);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "refType", source = "refType")
    @Mapping(target = "refId", source = "refId")
    @Mapping(target = "amount", source = "amount")
    @Mapping(target = "direction", source = "direction")
    @Mapping(target = "occurredAt", source = "occurredAt")
    LedgerEntry toDomain(LedgerEntryJpaEntity entity);
}
