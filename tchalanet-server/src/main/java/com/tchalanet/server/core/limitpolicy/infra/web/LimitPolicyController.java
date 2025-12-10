package com.tchalanet.server.core.limitpolicy.infra.web;

import com.tchalanet.server.core.accesscontrol.application.annotation.RequiresPermission;
import com.tchalanet.server.core.limitpolicy.domain.ports.in.UpsertLimitPolicyUseCase;
import com.tchalanet.server.core.limitpolicy.web.dto.LimitPolicyRequest;
import com.tchalanet.server.core.limitpolicy.web.dto.LimitPolicyResponse;
import com.tchalanet.server.core.limitpolicy.web.mapper.LimitPolicyWebMapper;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/limit-policies")
@RequiredArgsConstructor
public class LimitPolicyController {

  private final UpsertLimitPolicyUseCase upsertLimitPolicyUseCase;
  private final LimitPolicyWebMapper mapper;

  // private final AccessCheckerPort accessChecker; // No longer directly injected here, handled by
  // annotation

  @PostMapping
  @RequiresPermission("limits.manage") // Apply the annotation
  public ResponseEntity<LimitPolicyResponse> upsertLimitPolicy(
      @PathVariable UUID tenantId,
      @RequestHeader("X-User-Id")
          UUID userId, // Assuming userId is passed in header for admin actions
      @Valid @RequestBody LimitPolicyRequest request) {
    var command = mapper.toUpsertCommand(tenantId, request);
    var policy = upsertLimitPolicyUseCase.upsert(command);
    return new ResponseEntity<>(mapper.toLimitPolicyResponse(policy), HttpStatus.CREATED);
  }

  @GetMapping("/{policyId}")
  @RequiresPermission("limits.view") // Apply the annotation
  public ResponseEntity<LimitPolicyResponse> getLimitPolicy(
      @PathVariable UUID tenantId,
      @RequestHeader("X-User-Id") UUID userId,
      @PathVariable UUID policyId) {
    var policy =
        upsertLimitPolicyUseCase
            .getLimitPolicy(policyId)
            .filter(p -> p.getTenantId().equals(tenantId)) // Security check
            .map(mapper::toLimitPolicyResponse)
            .orElseThrow(
                () -> new LimitPolicyNotFoundException("Limit Policy not found: " + policyId));
    return ResponseEntity.ok(policy);
  }

  @ResponseStatus(HttpStatus.NOT_FOUND)
  public static class LimitPolicyNotFoundException extends RuntimeException {
    public LimitPolicyNotFoundException(String message) {
      super(message);
    }
  }
}
