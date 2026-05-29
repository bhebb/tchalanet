package com.tchalanet.server.catalog.plan.internal.write;

import com.tchalanet.server.catalog.plan.api.PlanFeatureKeys;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.web.error.ProblemRest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class PlanFeatureJsonValidator {

    private final JsonUtils jsonUtils;

    public Map<String, Boolean> parseAndValidate(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return Map.of();
        }

        Map<String, Boolean> features;
        try {
            features = jsonUtils.readValue(
                rawJson,
                new TypeReference<Map<String, Boolean>>() {
                }
            );
        } catch (Exception e) {
            throw ProblemRest.badRequest("plan.features_json_invalid");
        }

        for (var entry : features.entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue();

            if (key == null || key.isBlank()) {
                throw ProblemRest.badRequest("plan.feature_key_blank");
            }

            if (!PlanFeatureKeys.ALL.contains(key)) { // Using FeatureKeys.ALL
                throw ProblemRest.badRequest("plan.feature_key_unknown");
            }

            if (value == null) {
                throw ProblemRest.badRequest("plan.feature_value_required");
            }
        }

        return features;
    }
}
