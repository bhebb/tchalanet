package com.tchalanet.server.api;

import com.tchalanet.server.config.context.CurrentContext;
import com.tchalanet.server.config.context.RequestContext;
import com.tchalanet.server.dto.ContextDto;
import com.tchalanet.server.services.ContextService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class MeController {

  private final ContextService service;

  @GetMapping
  public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
    // roles depuis realm_access
    List<String> roles =
        Optional.ofNullable(jwt.getClaimAsMap("realm_access"))
            .map(m -> (List<String>) m.getOrDefault("roles", List.of()))
            .orElse(List.of());

    String activeEnterpriseId = jwt.getClaimAsString("active_enterprise_id");
    // todo set tenant
    //        Object tenants = jwt.getClaim("tenants"); // si JSON, spring la mappe déjà en type
    // Map/List

    return Map.of(
        "sub",
        jwt.getSubject(),
        "roles",
        roles,
        "active_enterprise_id",
        activeEnterpriseId,
        //                "tenants", tenants,
        "is_super_admin",
        roles.contains("SUPER_ADMIN"));
  }

  @GetMapping(value = "/context", produces = "application/json")
  public ResponseEntity<ContextDto> context(
      @AuthenticationPrincipal Jwt jwt,
      @CurrentContext RequestContext context,
      @RequestParam(required = false) String featureSetId) {

    Map<String, Object> tch =
        Optional.ofNullable(jwt.getClaim("tch"))
            .filter(Map.class::isInstance)
            .map(Map.class::cast)
            .orElse(Map.of());

    String tenant = (String) tch.getOrDefault("tenantId", "default");
    String feature =
        featureSetId != null ? featureSetId : (String) tch.getOrDefault("featureSetId", "core");

    return ResponseEntity.ok(service.getContext(tenant, feature));
  }
}
