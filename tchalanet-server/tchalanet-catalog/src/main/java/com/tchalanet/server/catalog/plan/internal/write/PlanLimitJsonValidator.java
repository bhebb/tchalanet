package com.tchalanet.server.catalog.plan.internal.write;

import com.tchalanet.server.catalog.plan.api.PlanLimitKeys;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.web.error.ProblemRest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class PlanLimitJsonValidator {

    private final JsonUtils jsonUtils;

    public Map<String, Integer> parseAndValidate(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return Map.of();
        }

        Map<String, Integer> limits;
        try {
            limits = jsonUtils.readValue(
                rawJson,
                new TypeReference<Map<String, Integer>>() {
                }
            );
        } catch (Exception e) {
            throw ProblemRest.badRequest("plan.limits_json_invalid");
        }

        for (var entry : limits.entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue();

            if (key == null || key.isBlank()) {
                throw ProblemRest.badRequest("plan.limit_key_blank");
            }

            if (!PlanLimitKeys.ALL.contains(key)) { // Using LimitKeys.ALL
                throw ProblemRest.badRequest("plan.limit_key_unknown");
            }

            if (value == null) {
                throw ProblemRest.badRequest("plan.limit_value_required");
            }

            if (value < 0) {
                throw ProblemRest.badRequest("plan.limit_value_negative");
            }
        }

        return limits;
    }
}
