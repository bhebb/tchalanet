package com.tchalanet.server.catalog.plan.internal.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.catalog.plan.api.PlanView;
import com.tchalanet.server.catalog.plan.internal.persistence.PlanJpaEntity;
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
  protected ObjectMapper objectMapper;

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

    JsonNode limitsNode = parseJson(entity.getLimitsJson());
    JsonNode featuresNode = parseJson(entity.getFeaturesJson());

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
    if (json == null || json.isBlank()) {
      return objectMapper.createObjectNode();
    }
    try {
      return objectMapper.readTree(json);
    } catch (Exception e) {
      // Log warning and return empty node
      return objectMapper.createObjectNode();
    }
  }
}
