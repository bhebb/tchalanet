package com.tchalanet.server.core.billing.infra.persistence.mapper;

import com.tchalanet.server.core.billing.domain.model.Subscription;
import com.tchalanet.server.core.billing.infra.persistence.SubscriptionJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SubscriptionPersistenceMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "planId", source = "plan.id")
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
}
