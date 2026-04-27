package com.tchalanet.server.common.web.jackson;

import com.tchalanet.server.common.types.id.TypedIdRegistry;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.module.SimpleModule;

public final class TypedIdsJacksonModule {

  private TypedIdsJacksonModule() {}

  public static JacksonModule create() {
    SimpleModule m = new SimpleModule("tch-typed-ids");

    for (Class<?> raw : TypedIdRegistry.ALL) {
      @SuppressWarnings("unchecked")
      Class<Object> idClass = (Class<Object>) raw;
      m.addDeserializer(idClass, new GenericTypedIdDeserializer<>(idClass));
      m.addSerializer(idClass, new GenericTypedIdSerializer<>(idClass));
    }

    return m;
  }
}
