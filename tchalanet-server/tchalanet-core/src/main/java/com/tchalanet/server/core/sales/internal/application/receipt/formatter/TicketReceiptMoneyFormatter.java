package com.tchalanet.server.core.sales.internal.application.receipt.formatter;

import com.tchalanet.server.common.types.money.Money;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class TicketReceiptMoneyFormatter {

    public String format(Money money) {
        if (money == null) {
            return null;
        }
        return money.amount().setScale(2, RoundingMode.HALF_UP) + " " + money.currency().code();
    }
}
