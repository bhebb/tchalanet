package com.tchalanet.server.core.terminal.internal.infra.web.tenant;

import com.tchalanet.server.core.terminal.application.query.model.TerminalView;
import com.tchalanet.server.core.terminal.infra.web.tenant.model.TerminalResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TerminalTenantWebMapper {
    TerminalResponse toResponse(TerminalView view);
}
