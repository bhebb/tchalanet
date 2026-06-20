package com.tchalanet.server.platform.identity.internal.web.me;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.platform.identity.internal.service.TenantUserAdministrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/identity/me")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
@Tag(name = "Identity • Activation")
public class IdentityActivationController {

  private final TenantUserAdministrationService users;

  @PostMapping("/complete-first-login")
  @Operation(summary = "Complete first login activation for the current user")
  public ApiResponse<CompleteFirstLoginResponse> completeFirstLogin(
      @CurrentContext TchRequestContext ctx,
      @Valid @RequestBody CompleteFirstLoginRequest request) {
    if (ctx.userId() == null) {
      throw ProblemRest.notFound("User not found for current principal");
    }
    var result = users.completeFirstLogin(
        ctx.userId(),
        request.firstName(),
        request.lastName(),
        request.phoneNumber());
    return ApiResponse.success(new CompleteFirstLoginResponse(
        result.userId(),
        result.mustChangePassword(),
        result.mustCompleteProfile(),
        result.entryRoute()));
  }
}
