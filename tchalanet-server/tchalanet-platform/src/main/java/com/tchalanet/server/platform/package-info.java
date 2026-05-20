/**
 * Platform layer — transversal application service modules.
 *
 * <p>Each sub-package is a Spring Modulith application module with its own api/ and internal/.
 * Modules under platform MUST NOT depend on core or features.
 * Cross-capability communication uses domain events or a documented ADR exception.
 */
@org.springframework.modulith.NamedInterface
package com.tchalanet.server.platform;
