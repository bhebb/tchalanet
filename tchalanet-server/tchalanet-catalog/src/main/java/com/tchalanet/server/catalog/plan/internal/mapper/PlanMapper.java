package com.tchalanet.server.catalog.plan.internal.mapper;

import com.tchalanet.server.catalog.plan.api.PlanView;
import com.tchalanet.server.catalog.plan.internal.persistence.PlanJpaEntity;
import com.tchalanet.server.common.json.utils.JsonUtils;
import tools.jackson.databind.JsonNode;

import com.tchalanet.server.common.mapper.CommonIdMapper;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * MapStruct mapper for Plan (JPA entity <-> View).
 * Maps to spec requirement P4 (mapping boundaries).
 * Uses CommonIdMapper for PlanId conversions.
 */
@Mapper(componentModel = "spring", uses = {CommonIdMapper.class})
public abstract class PlanMapper {

  @Autowired
  public JsonUtils jsonUtils;

  @Autowired
  protected CommonIdMapper idMapper;

  /**
   * Map JPA entity to immutable View.
   * Converts JSON strings to JsonNode.
   */
  public PlanView toView(PlanJpaEntity entity) {
    if (entity == null) {
      return null;
    }

    JsonNode limitsNode = jsonUtils.toJsonNode(entity.getLimitsJson());
    JsonNode featuresNode = jsonUtils.toJsonNode(entity.getFeaturesJson());

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

  private JsonNode parseJson(String json) {
      return jsonUtils.valueToTree(json);
  }
}
