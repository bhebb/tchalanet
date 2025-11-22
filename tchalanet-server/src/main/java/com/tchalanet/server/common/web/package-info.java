package com.tchalanet.server.common.web;

/**
 * Couche web technique commune :
 *
 * <ul>
 *   <li>Exception handlers globaux (ex: {@code @RestControllerAdvice})
 *   <li>DTO de réponse ou de requête vraiment transverses
 *   <li>Résolveurs d'arguments / {@code HandlerMethodArgumentResolver} globaux
 *   <li>Éventuels filtres ou interceptors HTTP partagés (si pas spécifiques à un domaine)
 * </ul>
 *
 * Règle d'or :
 *
 * <ul>
 *   <li>Si un type HTTP n'est utilisé que par un domaine (ticket, tenant, user, draw, ...), il doit
 *       vivre dans {@code <domaine>.web} et non ici.
 * </ul>
 */
