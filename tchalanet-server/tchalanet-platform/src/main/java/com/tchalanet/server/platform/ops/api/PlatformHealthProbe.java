package com.tchalanet.server.platform.ops.api;

import java.util.Map;

/**
 * Platform health snapshot for platform Ops surfaces.
 *
 * <p>The adapter usually wraps Spring Boot Actuator's health endpoint in the app layer.
 */
public interface PlatformHealthProbe {

  /**
   * @return {@code {"global": <status>, "components": {<name>: <status>, ...}}}.
   *     Implementations must not throw; callers fall back to UNKNOWN when no probe is available.
   */
  Map<String, Object> snapshot();
}
