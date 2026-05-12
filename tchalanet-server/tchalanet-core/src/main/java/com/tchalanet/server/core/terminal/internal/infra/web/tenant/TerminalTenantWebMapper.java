package com.tchalanet.server.core.terminal.internal.infra.web.tenant;

import com.tchalanet.server.core.terminal.api.query.TerminalView;
import com.tchalanet.server.core.terminal.internal.infra.web.tenant.model.TerminalResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TerminalTenantWebMapper {
    TerminalResponse toResponse(TerminalView view);
}
