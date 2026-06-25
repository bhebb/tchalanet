package com.tchalanet.server.app.config;

import java.util.LinkedHashMap;
import java.util.Map;

import com.tchalanet.server.features.pagemodel.dynamic.providers.platformadmin.PlatformHealthProbe;
import org.springframework.boot.health.actuate.endpoint.HealthDescriptor;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.actuate.endpoint.IndicatedHealthDescriptor;
import org.springframework.boot.health.actuate.endpoint.SystemHealthDescriptor;
import org.springframework.stereotype.Component;

/**
 * Adapter: implements the {@link} port by reading Spring
 * Boot Actuator's {@link HealthEndpoint}. Lives in the app layer so the
 * features module stays free of actuator dependencies.
 *
 * In-process snapshot — no SQL, no external call. Counts as a single
 * "grouped read" for the platform_admin_dashboard budget (§12, ≤4).
 */
@Component
public class PlatformHealthProbeActuatorAdapter implements PlatformHealthProbe {

  private final HealthEndpoint healthEndpoint;
  private final AppProperties appProperties;

  public PlatformHealthProbeActuatorAdapter(HealthEndpoint healthEndpoint, AppProperties appProperties) {
    this.healthEndpoint = healthEndpoint;
    this.appProperties = appProperties;
  }

  @Override
  public Map<String, Object> snapshot() {
    HealthDescriptor root = healthEndpoint.health();
    String global = root != null && root.getStatus() != null
        ? root.getStatus().getCode() : "UNKNOWN";
    Map<String, Object> components = new LinkedHashMap<>();
    if (root instanceof SystemHealthDescriptor system) {
      system.getComponents().forEach((name, desc) ->
          components.put(name, desc.getStatus() != null ? desc.getStatus().getCode() : "UNKNOWN"));
    } else if (root instanceof IndicatedHealthDescriptor indicated) {
      components.put("global", indicated.getStatus().getCode());
    }
    return Map.of(
        "global", global,
        "components", components,
        "appVersion", appProperties.version() != null ? appProperties.version() : "unknown");
  }
}
