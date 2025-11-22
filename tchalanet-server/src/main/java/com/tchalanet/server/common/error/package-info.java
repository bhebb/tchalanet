package com.tchalanet.server.common.error;

/**
 * Erreurs et gestion d'erreurs transverses à l'application.
 *
 * <p>Contient typiquement :
 *
 * <ul>
 *   <li>Exceptions de base partagées (ex: {@code DomainException}, {@code TechnicalException}).
 *   <li>Mapping d'erreurs global (ex: {@code @RestControllerAdvice} qui renvoie des
 *       ProblemDetails).
 *   <li>Types de payload d'erreur communs à plusieurs domaines.
 * </ul>
 *
 * Si une exception est propre à un seul domaine (ex: {@code TicketNotFoundException}), elle peut
 * rester dans le package {@code <domaine>.domain.model} concerné.
 */
