import { ActionItem, NavigationDestination, NavigationSection } from '@tch/api';

import {
  RuntimeNavigationDrawer,
  RuntimeNavigationEntry,
  RuntimeNavigationSection,
} from './private-bootstrap.model';

export function sectionsFromRuntimeNavigation(
  drawer: RuntimeNavigationDrawer | null,
): readonly NavigationSection[] | null {
  const sections = drawer?.sections;
  if (!sections?.length) return null;
  const mapped = sections
    .map(sectionFromRuntime)
    .filter((section): section is NavigationSection => section !== null);
  return mapped.length ? mapped : null;
}

/**
 * Bas de menu du contrat runtime.
 *
 * Le record Java `NavigationDrawer` (aspirational — rien ne le construit) nomme ce champ
 * `footerDestinations`. Le resolver qui sert vraiment ce payload
 * (`PrivateShellNavigationResolver`) contourne ce record : il repasse le fragment JSON tel quel,
 * et ce fragment nomme le même champ `secondary`. Lire uniquement `footerDestinations` — ce que
 * faisait cette fonction jusqu'ici — le laissait `undefined` sur toute vraie requête, donc ce bas
 * de menu retombait silencieusement sur le repli statique en permanence. Les deux noms sont donc
 * lus, `secondary` (le vrai) en premier.
 */
export function footerFromRuntimeNavigation(
  drawer: RuntimeNavigationDrawer | null,
): readonly ActionItem[] | null {
  const items = (drawer?.secondary ?? drawer?.footerDestinations ?? [])
    .map(actionFromRuntime)
    .filter((item): item is ActionItem => item !== null);
  return items.length ? items : null;
}

function sectionFromRuntime(section: RuntimeNavigationSection): NavigationSection | null {
  const items = (section.items ?? [])
    .map(actionFromRuntime)
    .filter((item): item is ActionItem => item !== null);
  if (!items.length) return null;
  return {
    id: text(section.id) || text(section.titleKey) || text(section.labelKey) || text(section.label_key) || 'section',
    titleKey: text(section.titleKey) || text(section.labelKey) || text(section.label_key) || text(section.label) || '',
    items,
  };
}

function actionFromRuntime(entry: RuntimeNavigationEntry): ActionItem | null {
  const id = text(entry.id);
  const labelKey = text(entry.labelKey) || text(entry.label_key);
  const label = text(entry.label);
  const destination = destinationFromRuntime(entry);
  const children = (entry.children ?? [])
    .map(actionFromRuntime)
    .filter((item): item is ActionItem => item !== null);

  if (!id || (!labelKey && !label)) return null;

  return {
    id,
    kind: text(entry.type) || 'link',
    labelKey: labelKey || undefined,
    label: label || null,
    destination,
    icon: text(entry.icon) || null,
    activeRoutes: stringList(entry.activeRoutes) ?? stringList(entry.active_routes),
    activeMatch: text(entry.activeMatch) || text(entry.active_match) || null,
    disabled: entry.disabled ?? false,
    reasonKey: text(entry.reasonKey) || text(entry.reason_key) || null,
    badge: null,
    children,
  };
}

function destinationFromRuntime(entry: RuntimeNavigationEntry): NavigationDestination | undefined {
  const destinationValue = text(entry.destination?.value);
  if (destinationValue) {
    return { kind: routeKind(entry.destination?.kind), value: destinationValue };
  }
  const path = text(entry.path);
  return path ? { kind: routeKind(entry.kind), value: path } : undefined;
}

function routeKind(kind: string | null | undefined): 'route' | 'url' {
  return kind === 'external' || kind === 'url' ? 'url' : 'route';
}

function stringList(value: readonly string[] | null | undefined): readonly string[] | undefined {
  if (!Array.isArray(value)) return undefined;
  const items = value.map(text).filter(Boolean);
  return items.length ? items : undefined;
}

function text(value: string | null | undefined): string {
  return typeof value === 'string' ? value.trim() : '';
}
