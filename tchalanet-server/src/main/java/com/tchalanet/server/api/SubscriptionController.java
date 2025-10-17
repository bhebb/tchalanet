package com.tchalanet.server.api;

import com.tchalanet.server.dto.ChangePlanRequest;
import com.tchalanet.server.dto.SubscriptionDTO;
import com.tchalanet.server.services.subscription.ISubscription;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
class SubscriptionController {
  private final ISubscription service;

  @GetMapping("/me")
  @PreAuthorize("hasRole('TENANT_ADMIN')")
  SubscriptionDTO me(@AuthenticationPrincipal Jwt jwt) {
    String tenantId = jwt.getClaim("tenant_id");
    return service.currentForTenant(tenantId);
  }

  @PostMapping("/me/change")
  @PreAuthorize("hasRole('TENANT_ADMIN')")
  SubscriptionDTO change(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePlanRequest req) {
    return service.changePlan(jwt.getClaim("tenant_id"), req);
  }

  @PostMapping("/me/cancel")
  @PreAuthorize("hasRole('TENANT_ADMIN')")
  SubscriptionDTO cancel(
      @AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "true") boolean atPeriodEnd) {
    return service.cancel(jwt.getClaim("tenant_id"), atPeriodEnd);
  }
}
