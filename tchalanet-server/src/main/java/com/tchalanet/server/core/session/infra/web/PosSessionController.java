package com.tchalanet.server.core.session.infra.web;
import com.tchalanet.server.common.types.id.SessionId;
import com.tchalanet.server.common.types.id.UserId;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContextHolder;
import com.tchalanet.server.core.session.application.command.model.CloseSessionCommand;
import com.tchalanet.server.core.session.application.command.model.OpenSessionCommand;
import com.tchalanet.server.core.session.application.query.model.GetCurrentSessionQuery;
import com.tchalanet.server.core.session.domain.model.PosSession;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.OutletId;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class PosSessionController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;
    private final TchRequestContextHolder contextHolder;

    @PostMapping("/open")
    public ResponseEntity<PosSession> open(@jakarta.validation.Valid @RequestBody OpenSessionRequest body) {
        var ctx = contextHolder.get();
        var tenantId = TenantId.of(ctx.tenantUuid());
        var userId = com.tchalanet.server.common.types.id.UserId.of(ctx.userUuid()); // source of truth

        var session = commandBus.send(
            new OpenSessionCommand(
                tenantId,
                body.outletId(),     // V1 ok; handler must validate terminal->outlet
                body.terminalId(),
                userId,
                body.openingFloat()
            )
        );

        return ResponseEntity.status(201).body(session);
    }

    @PostMapping("/{sessionId}/close")
    public ResponseEntity<PosSession> close(
        @PathVariable SessionId sessionId,
        @jakarta.validation.Valid @RequestBody CloseSessionRequest body
    ) {
        var tenantId = TenantId.of(contextHolder.get().tenantUuid());

        var session = commandBus.send(
            new CloseSessionCommand(tenantId, sessionId, body.closingAmount())
        );

        return ResponseEntity.ok(session);
    }

    @GetMapping("/current")
    public ResponseEntity<PosSession> current(@RequestParam TerminalId terminalId) {
        var tenantId = TenantId.of(contextHolder.get().tenantUuid());

        var result = queryBus.send(new GetCurrentSessionQuery(tenantId, terminalId));

        return result.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    public record OpenSessionRequest(
        @NotNull OutletId outletId,
        @NotNull TerminalId terminalId,
        @DecimalMin("0.00") BigDecimal openingFloat
    ) {}

    public record CloseSessionRequest(
        @DecimalMin("0.00") BigDecimal closingAmount
    ) {}
}
