package com.tchalanet.server.platform.identity.internal.web.me;

import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.platform.identity.internal.service.PublicLoginIdentifierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/auth/login-identifier")
@RequiredArgsConstructor
@Tag(name = "Identity • Login")
public class PublicLoginIdentifierController {

  private final PublicLoginIdentifierService identifiers;
  private final PublicLoginIdentifierRateLimiter rateLimiter;

  @PostMapping("/resolve")
  @Operation(summary = "Resolve a Tchalanet username to a provider sign-in identifier")
  public ResponseEntity<ApiResponse<ResolveLoginIdentifierResponse>> resolve(
      HttpServletRequest httpRequest, @Valid @RequestBody ResolveLoginIdentifierRequest request) {
    rateLimiter.check(httpRequest, request.identifier());
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .header("Pragma", "no-cache")
        .body(
            ApiResponse.success(
                new ResolveLoginIdentifierResponse(
                    identifiers.resolveIdentifier(request.identifier()))));
  }
}
