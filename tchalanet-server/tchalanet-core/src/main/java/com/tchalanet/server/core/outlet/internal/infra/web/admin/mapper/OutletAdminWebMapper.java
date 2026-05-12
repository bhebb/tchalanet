package com.tchalanet.server.core.outlet.internal.infra.web.admin.mapper;

import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.core.outlet.api.query.OutletOperationalContextView;
import com.tchalanet.server.core.outlet.api.query.OutletSummaryView;
import com.tchalanet.server.core.outlet.api.query.OutletTerminalView;
import com.tchalanet.server.core.outlet.api.query.OutletUserView;
import com.tchalanet.server.core.outlet.api.query.OutletView;
import com.tchalanet.server.core.outlet.internal.domain.model.SalesCapability;
import com.tchalanet.server.core.outlet.internal.infra.web.admin.model.OutletOperationalContextResponse;
import com.tchalanet.server.core.outlet.internal.infra.web.admin.model.OutletResponse;
import com.tchalanet.server.core.outlet.internal.infra.web.admin.model.OutletSummaryResponse;
import com.tchalanet.server.core.outlet.internal.infra.web.admin.model.OutletTerminalResponse;
import com.tchalanet.server.core.outlet.internal.infra.web.admin.model.OutletUserResponse;
import com.tchalanet.server.core.outlet.internal.infra.web.admin.model.SalesCapabilityResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OutletAdminWebMapper {

    OutletSummaryResponse toResponse(OutletSummaryView view);

    OutletResponse toResponse(OutletView view);

    OutletOperationalContextResponse toResponse(OutletOperationalContextView view);

    OutletUserResponse toResponse(OutletUserView view);

    List<OutletUserResponse> toResponses(List<OutletUserView> view);

    OutletTerminalResponse toResponse(OutletTerminalView view);

    SalesCapabilityResponse toResponse(SalesCapability view);

    List<OutletTerminalResponse> toListOutletTerminalResponse(List<OutletTerminalView> view);

    default TchPage<OutletSummaryResponse> toSummaryResponsePage(TchPage<OutletSummaryView> page) {
        return TchPageMapper.map(page, this::toResponse);
    }
}
