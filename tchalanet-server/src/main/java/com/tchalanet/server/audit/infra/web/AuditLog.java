package com.tchalanet.server.audit.infra.web;

import com.tchalanet.server.audit.domain.model.AuditAction;
import com.tchalanet.server.audit.domain.model.AuditEntityType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {
  AuditEntityType entity();

  AuditAction action();

  String idExpression() default "";

  String detailsExpression() default "";
}
