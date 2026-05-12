package com.tchalanet.server.common.web.paging;

import org.springframework.data.domain.Sort;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TchPaging {

  int defaultPage() default 0;

  int defaultSize() default 20;

  int maxSize() default 100;

  /** Champs autorisés pour le tri (allowlist). */
  String[] allowedSort() default {};

  /** Tri par défaut. Ex: {"occurredAt,DESC", "slotKey,ASC"} */
  String[] defaultSort() default {};

  Sort.Direction defaultDirection() default Sort.Direction.DESC;
}
