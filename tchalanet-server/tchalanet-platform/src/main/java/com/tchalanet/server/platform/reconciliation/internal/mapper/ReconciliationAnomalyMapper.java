package com.tchalanet.server.platform.reconciliation.internal.mapper;

import com.tchalanet.server.common.mapper.CommonIdMapper;
import com.tchalanet.server.common.mapper.CommonAuditMapper;
import com.tchalanet.server.platform.reconciliation.internal.domain.model.ReconciliationAnomaly;
import com.tchalanet.server.platform.reconciliation.internal.persistence.ReconciliationAnomalyJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", uses = {CommonIdMapper.class, CommonAuditMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReconciliationAnomalyMapper {

    ReconciliationAnomaly toDomain(ReconciliationAnomalyJpaEntity e);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "runId", source = "runId")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    ReconciliationAnomalyJpaEntity toEntity(ReconciliationAnomaly d);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "runId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntity(ReconciliationAnomaly d, @MappingTarget ReconciliationAnomalyJpaEntity e);
}

