package com.tchalanet.server.core.accesscontrol.application.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declarative permission check marker.
 *
 * <p>Usage : @RequiresPermission("ticket.create") @RequiresPermission({"ticket.create",
 * "ticket.pay"})
 *
 * <p>Sémantique : l'utilisateur doit posséder TOUTES les permissions listées.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequiresPermission {

  /**
   * One or more permission keys, e.g. "ticket.create", "roles.manage".
   *
   * @return array of permission codes that must all be granted
   */
  String[] value();
}
