package com.tchalanet.server.features.pagemodel.dynamic.providers;

import org.springframework.stereotype.Component;

@Component
public class PublicTchalaProvider extends StubPageModelDynamicProvider {

  public PublicTchalaProvider() {
    super("public_tchala");
  }
}
