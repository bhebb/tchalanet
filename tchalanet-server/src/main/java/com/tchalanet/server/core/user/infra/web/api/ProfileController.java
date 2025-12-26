package com.tchalanet.server.core.user.infra.web.api;

import static org.springframework.http.ResponseEntity.ok;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.constant.ContextKeys;
import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.user.application.command.model.EnsureUserExistsForPrincipalCommand;
import com.tchalanet.server.core.user.application.command.model.UpdateUserProfileCommand;
import com.tchalanet.server.core.user.application.query.model.GetCurrentUserQuery;
import com.tchalanet.server.core.user.infra.web.dto.MeResponse;
import com.tchalanet.server.core.user.infra.web.dto.UpdateUserProfileRequest;
import com.tchalanet.server.core.user.infra.web.dto.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

  private final QueryBus queryBus;
  private final CommandBus commandBus;

  @GetMapping("/me")
  public ResponseEntity<MeResponse> me(@CurrentContext TchRequestContext context) {
    var details = queryBus.send(new GetCurrentUserQuery(context.userId()));

    var meResponse =
        new MeResponse(
            details.id,
            details.keycloakId,
            details.tenantId.uuid(),
            details.username,
            details.email,
            details.firstName,
            details.lastName,
            details.displayName,
            false);

    return ok(meResponse);
  }

  @PostMapping("/bootstrap")
  public ResponseEntity<MeResponse> bootstrap(HttpServletRequest request) {
    var ctxObj = request.getAttribute(ContextKeys.REQUEST_CONTEXT);
    if (!(ctxObj instanceof TchRequestContext ctx)) {
      return ResponseEntity.status(500).build();
    }

    String tenantCode =
        ctx.effectiveTenantCode() != null ? ctx.effectiveTenantCode() : ctx.originalTenantCode();

    var cmd =
        new EnsureUserExistsForPrincipalCommand(
            UUID.fromString(ctx.keycloakUserId()),
            tenantCode,
            ctx.keycloakUserId(), // username fallback
            null, // email non disponible ici
            null,
            null,
            null,
            null,
            ctx.locale() != null ? ctx.locale().toLanguageTag() : null,
            null);

    var result = commandBus.send(cmd);
    var details = queryBus.send(new GetCurrentUserQuery(result.userId()));

    var meResponse =
        new MeResponse(
            details.id,
            details.keycloakId,
            details.tenantId.uuid(),
            details.username,
            details.email,
            details.firstName,
            details.lastName,
            details.displayName,
            result.isNew());

    return ok(meResponse);
  }

  @PatchMapping
  public ResponseEntity<UserResponse> updateProfile(
      @CurrentContext TchRequestContext context, @RequestBody UpdateUserProfileRequest req) {
    UserId userId = context.userId();
    if (userId == null) {
      return ResponseEntity.status(409).build();
    }

    var cmd =
        new UpdateUserProfileCommand(
            userId,
            Optional.ofNullable(req.firstName()),
            Optional.ofNullable(req.lastName()),
            Optional.ofNullable(req.email()),
            Optional.ofNullable(req.locale()));

    commandBus.send(cmd);

    var details = queryBus.send(new GetCurrentUserQuery(userId));
    var res =
        new UserResponse(
            UserId.nullableOf(details.id),
            details.keycloakId,
            details.tenantId,
            details.username,
            details.email,
            details.firstName,
            details.lastName,
            details.displayName);

    return ok(res);
  }
}
