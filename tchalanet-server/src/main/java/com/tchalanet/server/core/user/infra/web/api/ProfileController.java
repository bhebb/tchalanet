package com.tchalanet.server.core.user.infra.web.api;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.constant.ContextKeys;
import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.user.application.command.model.EnsureUserExistsForPrincipalCommand;
import com.tchalanet.server.core.user.application.command.model.UpdateUserProfileCommand;
import com.tchalanet.server.core.user.application.query.model.GetCurrentUserQuery;
import com.tchalanet.server.core.user.infra.web.dto.MeResponse;
import com.tchalanet.server.core.user.infra.web.dto.UpdateUserProfileRequest;
import com.tchalanet.server.core.user.infra.web.dto.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.mapstruct.control.MappingControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final QueryBus queryBus;
    private final CommandBus commandBus;

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(@CurrentContext TchRequestContext context) {
        var details = queryBus.send(new GetCurrentUserQuery(context.keycloakAsUuid()));

        var meResponse = new MeResponse(
            details.id,
            details.keycloakId,
            details.tenantId,
            details.username,
            details.email,
            details.firstName,
            details.lastName,
            details.displayName,
            false // existing user, not new
        );

        return ok(meResponse);
    }

    @PostMapping("/bootstrap")
    public ResponseEntity<MeResponse> bootstrap(HttpServletRequest request) {
        // Get existing request context filled by RequestUserContextFilter
        var ctxObj = request.getAttribute(ContextKeys.REQUEST_CONTEXT);
        if (!(ctxObj instanceof TchRequestContext ctx)) {
            return ResponseEntity.status(500).build();
        }

        // Prepare tenantIds set from effective tenant
        Set<UUID> tenantIds = new HashSet<>();
        UUID tenant = ctx.tenantUuid();
        if (tenant != null) tenantIds.add(tenant);

        var cmd = new EnsureUserExistsForPrincipalCommand(
            ctx.keycloakUserId(),
            null,
            null,
            null,
            tenantIds
        );

        // Use MeResponse returned by the handler
        var me = commandBus.send(cmd);

        return ok(me);
    }
}
