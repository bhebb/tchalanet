package com.tchalanet.server.common.security;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Permissions {

    @UtilityClass
    public static class Platform {
        public static final String TENANT_OVERRIDE = "platform.tenant.override";
    }

    @UtilityClass
    public static class Payout {
        public static final String ADMIN = "payout.admin";

        public static final String READ = "payout.read";
        public static final String REQUEST = "payout.request";
        public static final String EXECUTE = "payout.execute";
        public static final String APPROVE = "payout.approve";
        public static final String REJECT = "payout.reject";
    }

    @UtilityClass
    public static class Ticket {
        public static final String READ = "ticket.read";
        public static final String SELL = "ticket.sell";
        public static final String CANCEL = "ticket.cancel";
    }

    @UtilityClass
    public static class Draw {
        public static final String READ = "draw.read";
        public static final String MANAGE = "draw.manage";
        public static final String RESULT_APPLY = "draw.result.apply";
    }
}
