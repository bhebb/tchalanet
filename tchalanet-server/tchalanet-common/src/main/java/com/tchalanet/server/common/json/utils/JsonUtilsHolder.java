package com.tchalanet.server.common.json.utils;

import com.tchalanet.server.common.spring.SpringBeans;

/**
 * Bridge used by JPA converters that are instantiated by Hibernate and cannot use
 * regular Spring constructor injection.
 *
 * <p>Do not use this holder in application services, handlers, controllers, or domain code.
 */
public final class JsonUtilsHolder {

    private static volatile JsonUtils instance;

    private JsonUtilsHolder() {}

    public static JsonUtils get() {
        var current = instance;
        if (current != null) {
            return current;
        }

        synchronized (JsonUtilsHolder.class) {
            if (instance == null) {
                instance = SpringBeans.getBean(JsonUtils.class);
            }
            return instance;
        }
    }
}
