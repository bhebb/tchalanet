package com.tchalanet.server.common.web.jackson;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.tchalanet.server.common.types.id.TypedIdRegistry;

public final class TypedIdsJacksonModule {

    private TypedIdsJacksonModule() {
    }

    public static Module create() {
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
