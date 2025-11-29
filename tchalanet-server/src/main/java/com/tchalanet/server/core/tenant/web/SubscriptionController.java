package com.tchalanet.server.core.tenant.web;

import com.tchalanet.server.core.tenant.domain.usecase.subscription.CancelSubscriptionUseCase;
import com.tchalanet.server.core.tenant.domain.usecase.subscription.ChangePlanUseCase;
import com.tchalanet.server.core.tenant.domain.usecase.subscription.GetCurrentSubscriptionUseCase;
import com.tchalanet.server.core.tenant.domain.usecase.subscription.ResumeSubscriptionUseCase;
import com.tchalanet.server.core.tenant.web.dto.ChangePlanRequest;
import com.tchalanet.server.core.tenant.web.dto.SubscriptionDTO;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/** REST API pour la gestion des subscriptions et plans tenant. */
@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

  private static final String TENANT_ID_CLAIM = "tenant_id";

  private final GetCurrentSubscriptionUseCase getCurrentSubscriptionUseCase;
  private final ChangePlanUseCase changePlanUseCase;
  private final CancelSubscriptionUseCase cancelSubscriptionUseCase;
  private final ResumeSubscriptionUseCase resumeSubscriptionUseCase;

  @GetMapping("/me")
  @PreAuthorize("hasRole('TENANT_ADMIN')")
  public ResponseEntity<SubscriptionDTO> getCurrentSubscription(@AuthenticationPrincipal Jwt jwt) {
    String tenantId = jwt.getClaim(TENANT_ID_CLAIM);
    SubscriptionDTO subscription = getCurrentSubscriptionUseCase.execute(UUID.fromString(tenantId));
    return ResponseEntity.ok(subscription);
  }

  @PostMapping("/me/change")
  @PreAuthorize("hasRole('TENANT_ADMIN')")
  public ResponseEntity<SubscriptionDTO> changePlan(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePlanRequest request) {

    String tenantId = jwt.getClaim(TENANT_ID_CLAIM);
    SubscriptionDTO subscription = changePlanUseCase.execute(UUID.fromString(tenantId), request);
    return ResponseEntity.ok(subscription);
  }

  @PostMapping("/me/cancel")
  @PreAuthorize("hasRole('TENANT_ADMIN')")
  public ResponseEntity<SubscriptionDTO> cancelSubscription(
      @AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "true") boolean atPeriodEnd) {

    String tenantId = jwt.getClaim(TENANT_ID_CLAIM);
    SubscriptionDTO subscription =
        cancelSubscriptionUseCase.execute(UUID.fromString(tenantId), atPeriodEnd);
    return ResponseEntity.ok(subscription);
  }

  @PostMapping("/me/resume")
  @PreAuthorize("hasRole('TENANT_ADMIN')")
  public ResponseEntity<SubscriptionDTO> resumeSubscription(@AuthenticationPrincipal Jwt jwt) {
    String tenantId = jwt.getClaim(TENANT_ID_CLAIM);
    SubscriptionDTO subscription = resumeSubscriptionUseCase.execute(UUID.fromString(tenantId));
    return ResponseEntity.ok(subscription);
  }
}
