package com.tchalanet.server.core.session.infra.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "tch.session")
public class SalesSessionProperties {

    private Auto auto = new Auto();

    @Getter
    @Setter
    public static class Auto {

        private boolean active = true;

        /**
         * Global enable/disable for auto-open scheduler.
         */
        private boolean openEnabled = true;

        /**
         * Global enable/disable for auto-close scheduler.
         */
        private boolean closeEnabled = true;

        /**
         * Cron for auto-open.
         */
        private String openCron = "0 0 5 * * *";

        /**
         * Cron for auto-close.
         */
        private String closeCron = "0 0 20 * * *";
    }
}
