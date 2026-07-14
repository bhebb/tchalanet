package com.tchalanet.server.common.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Last-resort bridge for framework-created objects that cannot use constructor injection.
 *
 * <p>Allowed usages:
 *
 * <ul>
 *   <li>JPA AttributeConverter instantiated by Hibernate
 *   <li>Hibernate Envers RevisionListener
 * </ul>
 *
 * <p>Forbidden usages:
 *
 * <ul>
 *   <li>controllers
 *   <li>handlers
 *   <li>application services
 *   <li>domain services
 *   <li>regular Spring beans
 * </ul>
 */
@Component
public final class SpringBeans implements ApplicationContextAware {

  private static ApplicationContext context;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) {
    SpringBeans.context = applicationContext;
  }

  public static <T> T getBean(Class<T> type) {
    if (context == null) {
      throw new IllegalStateException("Spring application context is not initialized");
    }
    return context.getBean(type);
  }
}
