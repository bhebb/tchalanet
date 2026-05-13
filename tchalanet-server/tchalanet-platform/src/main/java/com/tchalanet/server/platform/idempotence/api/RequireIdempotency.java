package com.tchalanet.server.platform.idempotence.api;

import com.tchalanet.server.common.types.enums.IdempotencyScope;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireIdempotency {
    IdempotencyScope scope();

    long ttlSeconds() default 300;
}

