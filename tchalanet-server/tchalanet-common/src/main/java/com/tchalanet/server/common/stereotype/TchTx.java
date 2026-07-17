package com.tchalanet.server.common.stereotype;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.core.annotation.AliasFor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Transactional
public @interface TchTx {
  @AliasFor(annotation = Transactional.class, attribute = "readOnly")
  boolean readOnly() default false;

  @AliasFor(annotation = Transactional.class, attribute = "propagation")
  Propagation propagation() default Propagation.REQUIRED;
}
