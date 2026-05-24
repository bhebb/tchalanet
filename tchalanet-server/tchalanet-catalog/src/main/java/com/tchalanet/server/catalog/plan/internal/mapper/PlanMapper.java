package com.tchalanet.server.catalog.plan.internal.mapper;

import com.tchalanet.server.catalog.plan.api.PlanView;
import com.tchalanet.server.catalog.plan.internal.persistence.PlanJpaEntity;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.mapper.CommonIdMapper;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

/**
 * MapStruct mapper for Plan (JPA entity -> View).
 * Uses CommonIdMapper for PlanId conversions.
 */
@Mapper(componentModel = "spring", uses = {CommonIdMapper.class})
public abstract class PlanMapper {

    @Autowired
    protected JsonUtils jsonUtils;

    @Autowired
    protected CommonIdMapper idMapper;

    /**
     * Map JPA entity to immutable View.
     * Converts JSON strings to JsonNode objects.
     */
    public PlanView toView(PlanJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        var limitsNode = jsonOrEmptyObject(entity.getLimitsJson());
        var featuresNode = jsonOrEmptyObject(entity.getFeaturesJson());

        return new PlanView(
            idMapper.mapToPlanId(entity.getId()),
            entity.getCode(),
            entity.getName(),
            entity.getDescription(),
            entity.getPriceAmount(),
            entity.getCurrency(),
            entity.getBillingPeriod(),
            limitsNode,
            featuresNode,
            entity.isActive(),
            entity.isDefaultPlan(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    private JsonNode jsonOrEmptyObject(String json) {
        if (json == null || json.isBlank()) {
            return jsonUtils.emptyObject();
        }

        var node = jsonUtils.toJsonNode(json);

        if (node == null || node.isNull()) {
            return jsonUtils.emptyObject();
        }

        return jsonUtils.requireObject(node);
    }
}
