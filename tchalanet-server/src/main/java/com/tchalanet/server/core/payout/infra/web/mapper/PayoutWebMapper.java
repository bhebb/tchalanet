package com.tchalanet.server.core.payout.infra.web.mapper;

import com.tchalanet.server.core.payout.application.command.model.PayoutWorkflowResult;
import com.tchalanet.server.core.payout.application.command.model.RegisterPayoutResult;
import com.tchalanet.server.core.payout.application.query.model.PayoutDetails;
import com.tchalanet.server.core.payout.infra.web.model.PayoutDetailsResponse;
import com.tchalanet.server.core.payout.application.query.model.PayoutRow;
import com.tchalanet.server.core.payout.infra.web.model.PayoutRowResponse;
import com.tchalanet.server.core.payout.infra.web.model.PayoutWorkflowResponse;
import com.tchalanet.server.core.payout.infra.web.model.RegisterPayoutResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PayoutWebMapper {


    RegisterPayoutResponse toResponse(RegisterPayoutResult result);

    PayoutWorkflowResponse toResponse(PayoutWorkflowResult result);

    PayoutRowResponse toResponse(PayoutRow row);

    PayoutDetailsResponse toResponse(PayoutDetails details);
}
