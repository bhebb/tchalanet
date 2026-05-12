package com.tchalanet.server.common.util;

import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SchemaRegistryConfig;
import com.networknt.schema.SpecificationVersion;
import com.networknt.schema.regex.JoniRegularExpressionFactory;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class JsonSchemaValidatorUtil {

    private final SchemaRegistry schemaRegistry;
    private final JsonUtils jsonUtils;

    public JsonSchemaValidatorUtil(JsonUtils jsonUtils) {
        this.jsonUtils = jsonUtils;

        var schemaRegistryConfig = SchemaRegistryConfig.builder()
            .regularExpressionFactory(JoniRegularExpressionFactory.getInstance())
            .build();

        this.schemaRegistry = SchemaRegistry.withDefaultDialect(
            SpecificationVersion.DRAFT_2020_12,
            builder -> builder.schemaRegistryConfig(schemaRegistryConfig)
        );
    }

    public List<Error> validate(JsonNode schemaNode, JsonNode instanceNode) {
        if (schemaNode == null || schemaNode.isNull() || schemaNode.isEmpty()) {
            return List.of();
        }

        if (instanceNode == null || instanceNode.isNull()) {
            return List.of();
        }

        Schema schema = schemaRegistry.getSchema(
            SchemaLocation.of("urn:tchalanet:schema:inline"),
            schemaNode
        );

        String input = jsonUtils.toJson(instanceNode);

        return schema.validate(input, InputFormat.JSON, executionContext ->
            executionContext.executionConfig(config ->
                config.formatAssertionsEnabled(true)
            )
        );
    }
}
