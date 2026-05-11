package com.tchalanet.server.core.payout.application.port.out;

import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.payout.application.query.model.ListPayoutsQuery;
import com.tchalanet.server.core.payout.application.query.model.PayoutDetails;
import com.tchalanet.server.core.payout.application.query.model.PayoutReceiptView;
import com.tchalanet.server.core.payout.application.query.model.PayoutRow;

public interface PayoutQueryReaderPort {

    TchPage<PayoutRow> list(ListPayoutsQuery query);

    PayoutDetails getDetailsById(PayoutId payoutId);

    PayoutReceiptView getReceiptViewById(PayoutId payoutId);
}
