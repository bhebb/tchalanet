// common.audit.web.AuditLog
package com.tchalanet.server.common.audit.web;

import com.tchalanet.server.common.audit.domain.model.AuditAction;
import com.tchalanet.server.common.audit.domain.model.AuditEntityType;
import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

  AuditEntityType entity();

  AuditAction action();

  /**
   * SpEL pour trouver l'id de l'entité : - "#result.id" => id du retour - "#command.id" => param
   * "command" - "#ticketId" => param nommé "ticketId"
   */
  String idExpression() default "";

  /**
   * SpEL pour des détails supplémentaires optionnels (JSON). Exemple: - "{'amount':
   * #command.amount, 'lines': #command.lines.size()}"
   */
  String detailsExpression() default "";
}
