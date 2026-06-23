package com.tchalanet.server.platform.identity.internal.web.me;

import com.tchalanet.server.platform.identity.internal.service.UserPasswordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/identity")
@RequiredArgsConstructor
@Tag(name = "Identity • Password Reset")
public class IdentityPasswordResetController {

  private final UserPasswordService passwords;

  @PostMapping("/reset-password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Reset password for any platform user by email (no authentication required)")
  public void resetPassword(@Valid @RequestBody PasswordResetRequest request) {
    passwords.resetPasswordByEmail(request.email(), request.newPassword());
  }
}
