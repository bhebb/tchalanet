package com.tchalanet.server.core.outlet.infra.persistence;

import com.tchalanet.server.common.mapper.CommonIdMapper;
import com.tchalanet.server.core.outlet.application.query.model.OutletSummaryView;
import com.tchalanet.server.core.outlet.domain.model.Outlet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = CommonIdMapper.class)
public interface OutletPersistenceMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "addressId", source = "addressId")
    Outlet toDomain(OutletJpaEntity entity);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "tenantId", source = "tenantId")
    OutletSummaryView toSummaryView(OutletJpaEntity entity);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "addressId", source = "addressId")
    OutletJpaEntity toEntity(Outlet outlet);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "addressId", source = "addressId")
    void updateEntity(Outlet outlet, @MappingTarget OutletJpaEntity entity);
}
