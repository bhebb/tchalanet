package com.tchalanet.server.core.billing.infra.persistence.mapper;

import com.tchalanet.server.common.types.id.PlanId;
import com.tchalanet.server.common.types.id.SubscriptionId;
import com.tchalanet.server.core.billing.domain.model.Subscription;
import com.tchalanet.server.core.billing.infra.persistence.SubscriptionJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Optional;
import java.util.UUID;

import com.tchalanet.server.common.mapper.CommonIdMapper;

@Mapper(componentModel = "spring", uses = CommonIdMapper.class)
public interface SubscriptionPersistenceMapper {

    @Mapping(target = "id", expression = "java(s.id() == null ? null : s.id().uuid())")
    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "status", expression = "java(mapStatus(s.status()))")
    @Mapping(target = "currentPeriodStart", source = "currentPeriodStart")
    @Mapping(target = "currentPeriodEnd", source = "currentPeriodEnd")
    @Mapping(target = "cancelAtPeriodEnd", source = "cancelAtPeriodEnd")
    @Mapping(target = "billingProvider", source = "billingProvider")
    @Mapping(target = "billingExternalId", source = "billingExternalId")
    @Mapping(target = "meta", source = "meta")
    @Mapping(target = "version", source = "version")
    SubscriptionJpaEntity toEntity(Subscription s);

    @Mapping(target = "id", expression = "java(mapSubscrptionId(e.getId()))")
    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "planId", expression = "java(mapPlanId(e.getPlan() == null ? null : e.getPlan().getId()))")
    @Mapping(target = "status", expression = "java(mapStatus(e.getStatus()))")
    @Mapping(target = "currentPeriodStart", source = "currentPeriodStart")
    @Mapping(target = "currentPeriodEnd", source = "currentPeriodEnd")
    @Mapping(target = "cancelAtPeriodEnd", source = "cancelAtPeriodEnd")
    @Mapping(target = "billingProvider", source = "billingProvider")
    @Mapping(target = "billingExternalId", source = "billingExternalId")
    @Mapping(target = "meta", source = "meta")
    @Mapping(target = "version", source = "version")
    Subscription toDomain(SubscriptionJpaEntity e);

    default com.tchalanet.server.core.billing.domain.model.SubscriptionStatus mapStatus(Object s) {
        if (s == null) return null;
        try {
            return com.tchalanet.server.core.billing.domain.model.SubscriptionStatus.valueOf(s.toString());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    default SubscriptionId mapSubscrptionId(UUID id) {
        return id == null ? null : SubscriptionId.of(id);
    }

    default PlanId mapPlanId(UUID id) {
        return id == null ? null : PlanId.of(id);
    }
}
