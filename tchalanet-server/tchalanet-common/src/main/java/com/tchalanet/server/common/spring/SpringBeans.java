package com.tchalanet.server.common.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Last-resort bridge for framework-created objects that cannot use constructor injection.
 *
 * <p>Allowed usages:
 * <ul>
 *   <li>JPA AttributeConverter instantiated by Hibernate</li>
 *   <li>Hibernate Envers RevisionListener</li>
 * </ul>
 *
 * <p>Forbidden usages:
 * <ul>
 *   <li>controllers</li>
 *   <li>handlers</li>
 *   <li>application services</li>
 *   <li>domain services</li>
 *   <li>regular Spring beans</li>
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
