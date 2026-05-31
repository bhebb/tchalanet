package com.tchalanet.server.core.terminal.internal.infra.web.admin;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.core.terminal.api.command.UpdateTerminalMetadataCommand;
import com.tchalanet.server.core.terminal.internal.infra.web.admin.model.UpdateMetadataRequest;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/terminals")
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','SUPER_ADMIN')")
@Tag(name = "Terminal • Admin Metadata")
@RequiredArgsConstructor
public class TerminalAdminMetadataController {

    private final CommandBus commandBus;

    @PatchMapping("/{terminalId}/metadata")
    @ResponseStatus(HttpStatus.OK)
    @RequiresPermission("terminal.metadata.update")
    @Operation(summary = "Update metadata fields on a terminal")
    @AuditLog(
        entity = AuditEntityType.TERMINAL,
        action = AuditAction.TERMINAL_METADATA_UPDATE,
        idExpression = "#terminalId.value().toString()")
    public void updateMetadata(
        @CurrentContext TchRequestContext ctx,
        @PathVariable TerminalId terminalId,
        @Valid @RequestBody UpdateMetadataRequest req) {
        commandBus.execute(
            new UpdateTerminalMetadataCommand(
                ctx.tenantIdSafe(), terminalId, req.metadataPatch(), ctx.currentUserIdRequired()));
    }
}
