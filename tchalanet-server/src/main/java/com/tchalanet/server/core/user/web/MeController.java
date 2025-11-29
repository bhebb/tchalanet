package com.tchalanet.server.core.user.web;

import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.accesscontrol.domain.model.TchRole;
import com.tchalanet.server.core.accesscontrol.infra.security.RoleUtils;
import com.tchalanet.server.core.accesscontrol.infra.security.RoleUtils.RoleSplit;
import com.tchalanet.server.core.tenant.application.ports.in.GetTenantContextUseCase;
import com.tchalanet.server.core.user.web.dto.UserContextResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    Set<String> roles = RoleUtils.collectRoles(jwt);
    RoleSplit split = RoleUtils.splitRoles(roles);
    boolean isSuperAdmin = split.system.contains(TchRole.SUPER_ADMIN);

    UserContextResponse response =
        new UserContextResponse(jwt.getSubject(), List.copyOf(roles), null, isSuperAdmin, Map.of());

    return ResponseEntity.ok(response);
  }

  @GetMapping(value = "/context", produces = "application/json")
  public ResponseEntity<UserContextResponse> context(
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
