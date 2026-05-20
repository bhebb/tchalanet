package com.tchalanet.server.features.pagemodel.dynamic.providers;

import org.springframework.stereotype.Component;

@Component
public class PublicTestimonialsProvider extends StubPageModelDynamicProvider {

  public PublicTestimonialsProvider() {
    super("public_testimonials");
  }
}
