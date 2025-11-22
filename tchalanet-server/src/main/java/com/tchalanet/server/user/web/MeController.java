package com.tchalanet.server.user.web;

import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.usecase.GetTenantContextUseCase;
import com.tchalanet.server.common.web.dto.ContextDto;
import com.tchalanet.server.user.web.dto.UserContextResponse;
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

/** REST API pour récupérer le contexte utilisateur courant. */
@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class MeController {

  private final GetTenantContextUseCase getTenantContextUseCase;

  @GetMapping
  public ResponseEntity<UserContextResponse> me(@AuthenticationPrincipal Jwt jwt) {
    // Extraction des rôles depuis realm_access
    List<String> roles =
        Optional.ofNullable(jwt.getClaimAsMap("realm_access"))
            .map(m -> (List<String>) m.getOrDefault("roles", List.of()))
            .orElse(List.of());

    String activeEnterpriseId = jwt.getClaimAsString("active_enterprise_id");
    boolean isSuperAdmin = roles.contains("SUPER_ADMIN");

    UserContextResponse response =
        new UserContextResponse(
            jwt.getSubject(), roles, activeEnterpriseId, isSuperAdmin, Map.of());

    return ResponseEntity.ok(response);
  }

  @GetMapping(value = "/context", produces = "application/json")
  public ResponseEntity<ContextDto> context(
      @AuthenticationPrincipal Jwt jwt,
      @CurrentContext TchRequestContext context,
      @RequestParam(required = false) String featureSetId) {

    Map<String, Object> tch =
        Optional.ofNullable(jwt.getClaim("tch"))
            .filter(Map.class::isInstance)
            .map(Map.class::cast)
            .orElse(Map.of());

    String tenant = (String) tch.getOrDefault("tenantId", "default");
    String feature =
        featureSetId != null ? featureSetId : (String) tch.getOrDefault("featureSetId", "core");

    return ResponseEntity.ok(getTenantContextUseCase.execute(tenant, feature));
  }
}
