import { Router } from '@angular/router';

import { ActionItem, actionQueryParams, actionRoute } from '@tch/api';

/**
 * Décide si une entrée de navigation correspond à l'URL courante.
 *
 * Extrait de `tch-sidebar-nav` pour être partagé avec `tch-drawer-nav` : les deux rendent le même
 * modèle, ils doivent s'accorder sur ce qui est actif. Fonctions pures, donc testables sans routeur.
 */
export interface RouteActivity {
  /** URL courante, sans query ni fragment, sans slash final. */
  readonly url: string;
  readonly queryParams: Record<string, string>;
}

/** Normalise une URL pour la comparaison : sans query, sans fragment, sans slash final. */
export function comparableUrl(url: string): string {
  return url.split('?')[0].split('#')[0].replace(/\/$/, '') || '/';
}

/** Query params de l'URL, aplatis en chaînes pour la comparaison avec ceux d'une `ActionItem`. */
export function routeQueryParams(router: Router, url: string): Record<string, string> {
  const params: Record<string, string> = {};
  for (const [key, value] of Object.entries(router.parseUrl(url).queryParams)) {
    if (value == null) continue;
    params[key] = String(value);
  }
  return params;
}

export function isActionActive(
  activity: RouteActivity,
  item: ActionItem,
  siblings: readonly ActionItem[] = [],
): boolean {
  const route = actionRoute(item);
  return !!route && isRouteActive(activity, item, route, siblings);
}

/**
 * Une correspondance exacte est demandée explicitement (`activeMatch: 'exact'`) ou déduite : si un
 * frère couvre une route plus longue sous la même racine, le préfixe seul ne suffit pas à
 * distinguer les deux.
 */
export function isExactActiveMatch(item: ActionItem, siblings: readonly ActionItem[] = []): boolean {
  const route = actionRoute(item);
  return item.activeMatch === 'exact' || (!!route && hasLongerSiblingRoute(route, siblings));
}

function isRouteActive(
  activity: RouteActivity,
  item: ActionItem,
  route: string,
  siblings: readonly ActionItem[],
): boolean {
  const matches =
    isActiveRouteAlias(item, activity.url)
    || (isExactActiveMatch(item, siblings)
      ? activity.url === route
      : activity.url.startsWith(route));
  return matches && queryParamsMatch(activity, item);
}

function isActiveRouteAlias(item: ActionItem, url: string): boolean {
  return (item.activeRoutes ?? [])
    .map(route => comparableUrl(route))
    .some(route => url === route || url.startsWith(`${route}/`));
}

function hasLongerSiblingRoute(route: string, siblings: readonly ActionItem[]): boolean {
  return siblings.some(sibling => {
    const siblingRoute = actionRoute(sibling).replace(/\/$/, '');
    return siblingRoute.length > route.length && siblingRoute.startsWith(`${route}/`);
  });
}

function queryParamsMatch(activity: RouteActivity, item: ActionItem): boolean {
  const expected = actionQueryParams(item);
  if (!expected) {
    return Object.keys(activity.queryParams).length === 0;
  }
  return (
    Object.entries(expected).every(([key, value]) => activity.queryParams[key] === value)
    && Object.keys(activity.queryParams).every(key => expected[key] !== undefined)
  );
}
