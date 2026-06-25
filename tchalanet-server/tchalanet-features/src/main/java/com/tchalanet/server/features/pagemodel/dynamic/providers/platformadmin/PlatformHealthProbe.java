package com.tchalanet.server.features.pagemodel.dynamic.providers.platformadmin;

import java.util.Map;

/**
 * Port: platform health snapshot for platform Ops surfaces.
 *
 * The adapter (typically wrapping Spring Boot Actuator's {@code HealthEndpoint})
 * lives in the app layer so the features module stays free of actuator dependencies.
 */
public interface PlatformHealthProbe {

  /**
   * @return {@code {"global": <status>, "components": {<name>: <status>, ...}}}.
   *         Implementations must never throw — callers fall back to UNKNOWN
   *         when no probe is available.
   */
  Map<String, Object> snapshot();
}
