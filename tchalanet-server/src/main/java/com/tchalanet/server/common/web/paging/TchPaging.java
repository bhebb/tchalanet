package com.tchalanet.server.common.web.paging;

import java.lang.annotation.*;
import org.springframework.data.domain.Sort;

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
