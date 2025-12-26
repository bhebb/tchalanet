package com.tchalanet.server.core.billing.infra.persistence.mapper;

import com.tchalanet.server.common.types.id.PlanId;
import com.tchalanet.server.core.billing.domain.model.Plan;
import com.tchalanet.server.core.billing.infra.persistence.PlanJpaEntity;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PlanPersistenceMapper {

  @Mapping(target = "id", expression = "java(mapPlanId(entity.getId()))")
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

  default PlanId mapPlanId(UUID id) {
    return id == null ? null : PlanId.of(id);
  }
}
