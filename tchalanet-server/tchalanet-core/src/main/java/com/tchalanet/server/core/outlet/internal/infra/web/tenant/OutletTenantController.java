package com.tchalanet.server.core.outlet.internal.infra.web.tenant;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.apiresponse.ApiResponse;
import com.tchalanet.server.core.outlet.application.query.model.GetOutletOperationalContextQuery;
import com.tchalanet.server.core.outlet.application.query.model.GetOutletSalesCapabilityQuery;
import com.tchalanet.server.core.outlet.infra.web.admin.mapper.OutletAdminWebMapper;
import com.tchalanet.server.core.outlet.infra.web.admin.model.OutletOperationalContextResponse;
import com.tchalanet.server.core.outlet.infra.web.admin.model.SalesCapabilityResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/outlets")
@PreAuthorize("hasAuthority('TENANT_USER')")
@RequiredArgsConstructor
@Tag(name = "Outlet • Tenant Admin")
public class OutletTenantController {

    private final QueryBus queryBus;
    private final OutletAdminWebMapper mapper;

    @GetMapping("/{id}/operational-context")
    public ApiResponse<OutletOperationalContextResponse> operationalContext(@PathVariable OutletId id) {
        var outlet = queryBus.ask(new GetOutletOperationalContextQuery(id));
        return ApiResponse.success(mapper.toResponse(outlet));
    }


    @GetMapping("/{id}/sales-capability")
    public ApiResponse<SalesCapabilityResponse> salesCapability(@PathVariable OutletId id) {
        var salesCapability = queryBus.ask(new GetOutletSalesCapabilityQuery(id));
        return ApiResponse.success(mapper.toResponse(salesCapability));
    }
}
