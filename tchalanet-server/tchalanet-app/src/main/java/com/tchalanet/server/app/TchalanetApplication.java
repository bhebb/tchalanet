package com.tchalanet.server.app;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.tchalanet.server")
@ConfigurationPropertiesScan(basePackages = "com.tchalanet.server")
@EnableScheduling
@EnableAsync
@EnableAspectJAutoProxy
@EnableAdminServer
public class TchalanetApplication {

    static void main(String[] args) {
        SpringApplication.run(TchalanetApplication.class, args);
    }
}
