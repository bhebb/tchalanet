package com.tchalanet.server.core.billing.infra.persistence.mapper;

import com.tchalanet.server.core.billing.domain.model.Plan;
import com.tchalanet.server.core.billing.infra.persistence.PlanJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PlanPersistenceMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "priceAmount", source = "priceAmount")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "billingFrequency", source = "billingFrequency")
    @Mapping(target = "publicPlan", source = "publicPlan")
    @Mapping(target = "features", source = "features")
    Plan toDomain(PlanJpaEntity entity);

    List<Plan> toDomains(List<PlanJpaEntity> all);
}
