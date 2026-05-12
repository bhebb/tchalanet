package com.tchalanet.server.core.payout.internal.infra.web.mapper;

import com.tchalanet.server.core.payout.api.command.PayoutWorkflowResult;
import com.tchalanet.server.core.payout.api.command.RegisterPayoutResult;
import com.tchalanet.server.core.payout.api.query.PayoutDetails;
import com.tchalanet.server.core.payout.internal.infra.web.model.PayoutDetailsResponse;
import com.tchalanet.server.core.payout.api.query.PayoutRow;
import com.tchalanet.server.core.payout.internal.infra.web.model.PayoutRowResponse;
import com.tchalanet.server.core.payout.internal.infra.web.model.PayoutWorkflowResponse;
import com.tchalanet.server.core.payout.internal.infra.web.model.RegisterPayoutResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PayoutWebMapper {


    RegisterPayoutResponse toResponse(RegisterPayoutResult result);

    PayoutWorkflowResponse toResponse(PayoutWorkflowResult result);

    PayoutRowResponse toResponse(PayoutRow row);

    PayoutDetailsResponse toResponse(PayoutDetails details);
}
