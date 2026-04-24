package com.tchalanet.server.core.limitpolicy.application.query.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.limitpolicy.application.port.out.LimitAssignmentReaderPort;
import com.tchalanet.server.core.limitpolicy.application.port.out.LimitDefinitionReaderPort;
import com.tchalanet.server.core.limitpolicy.application.query.model.GetLimitsOverviewQuery;
import com.tchalanet.server.core.limitpolicy.application.query.model.LimitsOverviewView;
import com.tchalanet.server.common.types.id.LimitDefinitionId;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitAssignment;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;

@UseCase
@RequiredArgsConstructor
public class GetLimitsOverviewQueryHandler
    implements QueryHandler<GetLimitsOverviewQuery, LimitsOverviewView> {

  private final LimitDefinitionReaderPort defReader;
  private final LimitAssignmentReaderPort asgReader;

  @Override
  public LimitsOverviewView handle(GetLimitsOverviewQuery q) {
    var defs = defReader.listActive().stream()
        .filter(d -> !d.isDeleted())
        .toList();

    var asgs = asgReader.listByTarget(q.target()).stream()
        .filter(a -> !a.isDeleted())
        .toList();

    // index assignments by definitionId
    var byDef = new HashMap<LimitDefinitionId, LimitAssignment>(asgs.size());
    for (var a : asgs) byDef.put(a.limitDefinitionId(), a);

    var defViews = defs.stream()
        .map(d -> new LimitsOverviewView.Definition(
            d.id(), d.ruleKey(), d.enabled(), d.onBreach(), d.params(), d.appliesTo()
        ))
        .toList();

    var asgViews = asgs.stream()
        .map(a -> new LimitsOverviewView.Assignment(
            a.id(), a.limitDefinitionId(), a.enabled(), a.startsAt(), a.endsAt(),
            a.paramsOverride(), a.appliesToOverride()
        ))
        .toList();

    var effViews = defs.stream().map(d -> {
      var a = byDef.get(d.id());
      boolean enabled = d.enabled() && (a == null || a.enabled());

      JsonNode params = mergeObject(d.params(), a == null ? null : a.paramsOverride());
      JsonNode appliesTo = mergeObject(d.appliesTo(), a == null ? null : a.appliesToOverride());

      return new LimitsOverviewView.Effective(
          d.ruleKey(),
          enabled,
          d.onBreach(),
          params,
          appliesTo,
          d.id(),
          a == null ? null : a.id()
      );
    }).toList();

    return new LimitsOverviewView(q.target(), defViews, asgViews, effViews);
  }

  private JsonNode mergeObject(JsonNode base, JsonNode override) {
    if (override == null || override.isNull()) return base;
    if (base == null || base.isNull()) return override;
    if (!(base instanceof ObjectNode) || !(override instanceof ObjectNode)) return override;

    ObjectNode merged = ((ObjectNode) base).deepCopy();
    merged.setAll((ObjectNode) override); // override wins (shallow merge)
    return merged;
  }
}
