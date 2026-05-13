/**
 * Stable HTTP contract for API responses.
 *
 * <p>This package contains the core API response types that define the contract between the backend
 * and frontend. These are pure POJOs/records with no Spring dependencies, ensuring stability and
 * reusability.
 *
 * <p>Used by: - Controllers (explicit return types) - BFF public/private endpoints - Frontend
 * (stable contract consumption)
 */
@org.springframework.modulith.NamedInterface
package com.tchalanet.server.common.web.api;
