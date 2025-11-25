package com.tchalanet.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TchalanetApiApplication {

  public static void main(String[] args) {
    SpringApplication.run(TchalanetApiApplication.class, args);
  }
}
