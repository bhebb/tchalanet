package com.tchalanet.server.common.command.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark commands that should be audited when the 'force' flag is true.
 * Used by {@code AuditedForceCommandAspect}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditedForceCommand {
}
