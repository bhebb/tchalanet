package com.tchalanet.server.core.terminal.internal.infra.web.tenant;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.TerminalActivationChallengeId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.terminal.api.command.CreateTerminalActivationChallengeCommand;
import com.tchalanet.server.core.terminal.api.command.CreateTerminalActivationChallengeResult;
import com.tchalanet.server.core.terminal.api.command.VerifyTerminalActivationChallengeCommand;
import com.tchalanet.server.core.terminal.api.command.VerifyTerminalActivationChallengeResult;
import com.tchalanet.server.core.terminal.internal.application.service.binding.TerminalBindingCredentialHasher;
import com.tchalanet.server.core.terminal.internal.domain.model.binding.TerminalBindingType;
import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalChallengeDeliveryMode;
import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalChallengeType;
import com.tchalanet.server.core.terminal.internal.infra.web.tenant.model.CreateTerminalActivationChallengeRequest;
import com.tchalanet.server.core.terminal.internal.infra.web.tenant.model.TerminalActivationBindingResponse;
import com.tchalanet.server.core.terminal.internal.infra.web.tenant.model.TerminalActivationChallengeResponse;
import com.tchalanet.server.core.terminal.internal.infra.web.tenant.model.VerifyTerminalActivationChallengeRequest;
import com.tchalanet.server.platform.accesscontrol.api.RequiresPermission;
import com.tchalanet.server.platform.audit.api.AuditLog;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/terminals/{terminalId}/activation-challenges")
@PreAuthorize("hasAnyAuthority('CASHIER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Terminal • POS Activation")
@RequiredArgsConstructor
public class TerminalActivationController {

    private final CommandBus commandBus;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a POS pairing challenge for the authenticated user")
    @RequiresPermission("terminal.challenge.create")
    @AuditLog(
        entity = AuditEntityType.TERMINAL,
        action = AuditAction.TERMINAL_CHALLENGE_CREATE,
        idExpression = "#terminalId.value().toString()")
    public ApiResponse<TerminalActivationChallengeResponse> createChallenge(
        @CurrentContext TchRequestContext ctx,
        @PathVariable TerminalId terminalId,
        @Valid @RequestBody(required = false) CreateTerminalActivationChallengeRequest request
    ) {
        var result = commandBus.execute(new CreateTerminalActivationChallengeCommand(
            ctx.effectiveTenantIdRequired(),
            terminalId,
            ctx.currentUserIdRequired(),
            TerminalChallengeType.POS_PAIRING,
            deliveryModeOrE2e(request),
            ctx.currentUserIdRequired()));

        return ApiResponse.created(toResponse(result));
    }

    @PostMapping("/{challengeId}/verify")
    @Operation(summary = "Verify a POS pairing challenge and bind the device")
    @RequiresPermission("terminal.binding.create")
    @AuditLog(
        entity = AuditEntityType.TERMINAL,
        action = AuditAction.TERMINAL_BINDING_CREATE,
        idExpression = "#terminalId.value().toString()")
    public ApiResponse<TerminalActivationBindingResponse> verifyChallenge(
        @CurrentContext TchRequestContext ctx,
        @PathVariable TerminalId terminalId,
        @PathVariable TerminalActivationChallengeId challengeId,
        @Valid @RequestBody VerifyTerminalActivationChallengeRequest request
    ) {
        var result = commandBus.execute(new VerifyTerminalActivationChallengeCommand(
            ctx.effectiveTenantIdRequired(),
            challengeId,
            ctx.currentUserIdRequired(),
            request.clearCode(),
            TerminalBindingType.POS_DEVICE,
            request.bindingPublicKey(),
            TerminalBindingCredentialHasher.hash(
                ctx.effectiveTenantIdRequired(),
                terminalId,
                request.bindingCredential()),
            request.deviceFingerprintHash(),
            ctx.currentUserIdRequired()));

        return ApiResponse.success(toResponse(result));
    }

    private static TerminalChallengeDeliveryMode deliveryModeOrE2e(CreateTerminalActivationChallengeRequest request) {
        return request == null || request.deliveryMode() == null
            ? TerminalChallengeDeliveryMode.E2E
            : request.deliveryMode();
    }

    private static TerminalActivationChallengeResponse toResponse(CreateTerminalActivationChallengeResult result) {
        return new TerminalActivationChallengeResponse(
            result.challengeId(),
            result.challengeType(),
            result.channel(),
            result.expiresAt(),
            result.deliveryRef());
    }

    private static TerminalActivationBindingResponse toResponse(VerifyTerminalActivationChallengeResult result) {
        return new TerminalActivationBindingResponse(
            result.terminalId(),
            result.bindingId(),
            result.bindingType());
    }
}
