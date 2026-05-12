package com.tchalanet.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync // Enable Spring's asynchronous method execution capability
public class TchalanetApiApplication {

  static void main(String[] args) {
    SpringApplication.run(TchalanetApiApplication.class, args);
  }
}
