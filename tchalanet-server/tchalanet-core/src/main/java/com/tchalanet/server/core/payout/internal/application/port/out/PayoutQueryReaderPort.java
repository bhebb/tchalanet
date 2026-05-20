package com.tchalanet.server.core.payout.internal.application.port.out;

import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.payout.api.query.ListPayoutsQuery;
import com.tchalanet.server.core.payout.api.query.PayoutDetails;
import com.tchalanet.server.core.payout.api.query.PayoutReceiptView;
import com.tchalanet.server.core.payout.api.query.PayoutRow;

public interface PayoutQueryReaderPort {

    TchPage<PayoutRow> list(ListPayoutsQuery query);

    PayoutDetails getDetailsById(PayoutId payoutId);

    PayoutReceiptView getReceiptViewById(PayoutId payoutId);
}
