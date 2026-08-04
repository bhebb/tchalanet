package com.tchalanet.server.app.config;

import com.tchalanet.server.platform.ops.api.PlatformHealthProbe;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.health.actuate.endpoint.HealthDescriptor;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.actuate.endpoint.IndicatedHealthDescriptor;
import org.springframework.boot.health.actuate.endpoint.SystemHealthDescriptor;
import org.springframework.stereotype.Component;

/**
 * Adapter: implements the {@link} port by reading Spring Boot Actuator's {@link HealthEndpoint}.
 * Lives in the app layer so the features module stays free of actuator dependencies.
 *
 * <p>In-process snapshot — no SQL, no external call. Counts as a single "grouped read" for the
 * platform_admin_dashboard budget (§12, ≤4).
 */
@Component
public class PlatformHealthProbeActuatorAdapter implements PlatformHealthProbe {

  private final HealthEndpoint healthEndpoint;
  private final AppProperties appProperties;

  public PlatformHealthProbeActuatorAdapter(
      HealthEndpoint healthEndpoint, AppProperties appProperties) {
    this.healthEndpoint = healthEndpoint;
    this.appProperties = appProperties;
  }

  @Override
  public Map<String, Object> snapshot() {
    HealthDescriptor root = healthEndpoint.health();
    String global =
        root != null && root.getStatus() != null ? root.getStatus().getCode() : "UNKNOWN";
    Map<String, Object> components = new LinkedHashMap<>();
    if (root instanceof SystemHealthDescriptor system) {
      var healthComponents = system.getComponents();
      if (healthComponents != null) {
        healthComponents.forEach(
            (name, desc) -> {
              var status = desc == null ? null : desc.getStatus();
              components.put(name, status == null ? "UNKNOWN" : status.getCode());
            });
      }
    } else if (root instanceof IndicatedHealthDescriptor indicated) {
      var status = indicated.getStatus();
      components.put("global", status == null ? "UNKNOWN" : status.getCode());
    }
    return Map.of(
        "global", global,
        "components", components,
        "appVersion", appProperties.version() != null ? appProperties.version() : "unknown");
  }
}
