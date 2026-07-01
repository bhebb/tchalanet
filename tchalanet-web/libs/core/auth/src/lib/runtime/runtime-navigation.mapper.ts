import { ActionItem, NavigationSection } from '@tch/api';

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
  const path = text(entry.path);
  const children = (entry.children ?? [])
    .map(actionFromRuntime)
    .filter((item): item is ActionItem => item !== null);

  if (!id || (!labelKey && !label)) return null;

  return {
    id,
    kind: text(entry.type) || 'link',
    labelKey: labelKey || undefined,
    label: label || null,
    destination: path ? { kind: routeKind(entry.kind), value: path } : undefined,
    icon: text(entry.icon) || null,
    activeMatch: text(entry.activeMatch) || text(entry.active_match) || null,
    disabled: entry.disabled ?? false,
    reasonKey: text(entry.reasonKey) || text(entry.reason_key) || null,
    badge: null,
    children,
  };
}

function routeKind(kind: string | null | undefined): 'route' | 'url' {
  return kind === 'external' || kind === 'url' ? 'url' : 'route';
}

function text(value: string | null | undefined): string {
  return typeof value === 'string' ? value.trim() : '';
}
