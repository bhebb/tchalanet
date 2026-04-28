package com.tchalanet.server.features.tenantadmin.outlets;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.outlet.application.command.model.CreateOutletCommand;
import com.tchalanet.server.core.outlet.application.command.model.UpdateOutletConfigCommand;
import com.tchalanet.server.core.outlet.application.query.model.GetOutletByIdQuery;
import com.tchalanet.server.core.outlet.application.query.model.OutletView;
import com.tchalanet.server.core.outlet.application.query.model.ListOutletsByTenantQuery;
import com.tchalanet.server.features.tenantadmin.outlets.model.OutletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TenantAdminOutletsOrchestrator {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    public List<OutletResponse> list(TchRequestContext ctx) {
        TenantId tenantId = ctx.tenantIdSafe();
        var q = new ListOutletsByTenantQuery(tenantId);
        List<OutletView> list = queryBus.send(q);
        return list.stream().map(OutletWebMapper::toResponse).collect(Collectors.toList());
    }

    public OutletResponse get(TchRequestContext ctx, OutletId id) {
        TenantId tenantId = ctx.tenantIdSafe();
        OutletView dto = queryBus.send(new GetOutletByIdQuery(tenantId, id));
        return OutletWebMapper.toResponse(dto);
    }

    public OutletId create(TchRequestContext ctx, String name, String slug, com.tchalanet.server.core.address.application.model.AddressInput address) {
        TenantId tenantId = ctx.tenantIdSafe();
        CreateOutletCommand cmd = new CreateOutletCommand(tenantId, name, slug, null, address);
        return commandBus.send(cmd);
    }

    public void updateConfig(TchRequestContext ctx, OutletId id, com.tchalanet.server.core.outlet.application.command.model.OutletConfigPatch patch) {
        TenantId tenantId = ctx.tenantIdSafe();
        commandBus.send(new UpdateOutletConfigCommand(tenantId, id, patch));
    }
}
