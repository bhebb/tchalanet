import { provideHttpClient } from '@angular/common/http';
import { Component, Signal, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it } from 'vitest';

import { ActionItem, NavigationSection, actionRoute } from '@tch/api';
import { AUTH_CLIENT, AuthClient } from '@tch/core/auth';
import { TchBreakpointService } from '@tch/ui/components';
import { themeStoreProvider } from '@tch/ui/theme';

import { PLATFORM_NAVIGATION, TENANT_ADMIN_NAVIGATION } from './private-navigation.model';
import { PrivateShellLayoutComponent } from './private-shell-layout.component';

const authClient: AuthClient = {
  isAuthenticated: async () => false,
  login: async () => undefined,
  logout: async () => undefined,
  getAccessToken: async () => null,
  getTokenExpiresAt: async () => undefined,
};

@Component({ standalone: true, template: '' })
class Blank {}

const SECTIONS: readonly NavigationSection[] = [
  {
    id: 'admin',
    titleKey: 'nav.section',
    items: [
      {
        id: 'dashboard',
        labelKey: 'nav.dashboard',
        destination: { kind: 'route', value: '/app/admin' },
        activeMatch: 'exact',
      },
      {
        id: 'limits',
        labelKey: 'nav.limits',
        icon: 'shield',
        destination: { kind: 'route', value: '/app/admin/limits' },
        children: [
          {
            id: 'limits-overview',
            labelKey: 'nav.limits_overview',
            destination: { kind: 'route', value: '/app/admin/limits' },
            activeMatch: 'exact',
          },
          {
            id: 'limits-global',
            labelKey: 'nav.limits_global',
            destination: { kind: 'route', value: '/app/admin/limits/global' },
          },
        ],
      },
    ],
  },
];

function configure(isWide: Signal<boolean>) {
  TestBed.configureTestingModule({
    providers: [
      provideRouter([
        { path: 'app/admin', component: Blank },
        { path: 'app/admin/limits', component: Blank },
        { path: 'app/admin/limits/global', component: Blank },
      ]),
      provideTranslateService(),
      provideHttpClient(),
      themeStoreProvider,
      { provide: AUTH_CLIENT, useValue: authClient },
      { provide: TchBreakpointService, useValue: { isWide } as unknown as TchBreakpointService },
    ],
  });
}

async function render(url = '/app/admin'): Promise<ComponentFixture<PrivateShellLayoutComponent>> {
  await TestBed.inject(Router).navigateByUrl(url);
  const fixture = TestBed.createComponent(PrivateShellLayoutComponent);
  fixture.componentRef.setInput('brand', {
    id: 'brand',
    label: 'Tchalanet',
    destination: { kind: 'route', value: '/' },
  });
  fixture.componentRef.setInput('sections', SECTIONS);
  fixture.detectChanges();
  return fixture;
}

const el = (fixture: ComponentFixture<unknown>, selector: string) =>
  fixture.nativeElement.querySelector(selector) as HTMLElement | null;
const all = (fixture: ComponentFixture<unknown>, selector: string) =>
  [...fixture.nativeElement.querySelectorAll(selector)] as HTMLElement[];

describe('private shell drawer — replié (< 840px)', () => {
  beforeEach(() => configure(signal(false)));

  it('replaces the accordion with the two-level drawer', async () => {
    const fixture = await render();

    expect(el(fixture, 'tch-drawer-nav')).not.toBeNull();
    expect(el(fixture, 'tch-sidebar-nav')).toBeNull();
  });

  it('lists childless entries as rows and groups as category cards', async () => {
    const fixture = await render();

    const rows = all(fixture, '.drawer-nav__row').map(node => node.textContent?.trim());
    const cards = all(fixture, '.drawer-nav__card').map(node => node.textContent?.trim());

    expect(rows.some(text => text?.includes('nav.dashboard'))).toBe(true);
    expect(cards).toHaveLength(1);
    expect(cards[0]).toContain('nav.limits');
    // Le compteur retombe sur le nombre brut quand la clé n'est pas traduite.
    expect(cards[0]).toContain('2');
  });

  it('opens a category panel and drops the entry the header already leads to', async () => {
    const fixture = await render();

    el(fixture, '.drawer-nav__card')!.click();
    fixture.detectChanges();

    const panel = el(fixture, '.drawer-nav__panel');
    expect(panel).not.toBeNull();
    expect(panel!.querySelector('.drawer-nav__panel-link')?.getAttribute('href')).toBe(
      '/app/admin/limits',
    );

    const items = all(fixture, '.drawer-nav__panel .drawer-nav__row').map(n => n.textContent);
    expect(items.some(text => text?.includes('nav.limits_global'))).toBe(true);
    expect(items.some(text => text?.includes('nav.limits_overview'))).toBe(false);
  });

  it('closes the category panel before the drawer on Escape', async () => {
    const fixture = await render();
    fixture.componentInstance.drawerOpen.set(true);
    fixture.detectChanges();

    el(fixture, '.drawer-nav__card')!.click();
    fixture.detectChanges();
    expect(el(fixture, '.drawer-nav__panel')).not.toBeNull();

    fixture.componentInstance.onEscape();
    fixture.detectChanges();

    expect(el(fixture, '.drawer-nav__panel')).toBeNull();
    expect(fixture.componentInstance.drawerOpen()).toBe(true);

    fixture.componentInstance.onEscape();
    fixture.detectChanges();

    expect(fixture.componentInstance.drawerOpen()).toBe(false);
  });

  it('reopens on the root level after being closed', async () => {
    const fixture = await render();
    fixture.componentInstance.drawerOpen.set(true);
    fixture.detectChanges();

    el(fixture, '.drawer-nav__card')!.click();
    fixture.detectChanges();
    expect(el(fixture, '.drawer-nav__panel')).not.toBeNull();

    fixture.componentInstance.closeDrawer();
    fixture.detectChanges();

    expect(el(fixture, '.drawer-nav__panel')).toBeNull();
  });

  it('marks the category holding the active route', async () => {
    const fixture = await render('/app/admin/limits/global');

    expect(el(fixture, '.drawer-nav__card')?.classList.contains('is-active')).toBe(true);
  });
});

describe('private shell drawer — déployé (≥ 840px)', () => {
  beforeEach(() => configure(signal(true)));

  it('keeps the accordion sidebar', async () => {
    const fixture = await render();

    expect(el(fixture, 'tch-sidebar-nav')).not.toBeNull();
    expect(el(fixture, 'tch-drawer-nav')).toBeNull();
  });
});

/**
 * Garde sur les modèles réels. La règle d'absorption est structurelle, donc silencieuse : si un
 * groupe gagne une `destination` raccourci vers son premier enfant, cet enfant disparaîtrait du
 * panneau sans que rien ne le signale.
 */
describe('absorption de l’entrée d’atterrissage sur les modèles réels', () => {
  const absorbed = (group: ActionItem): ActionItem | undefined => {
    const route = actionRoute(group);
    if (!route) return undefined;
    return (group.children ?? []).find(
      child => child.activeMatch === 'exact' && actionRoute(child) === route,
    );
  };

  const groupsOf = (sections: readonly NavigationSection[]) =>
    sections.flatMap(section => section.items).filter(item => item.children?.length);

  it('absorbs exactly the landing entries, and nothing else', () => {
    const outcome = [...groupsOf(TENANT_ADMIN_NAVIGATION), ...groupsOf(PLATFORM_NAVIGATION)]
      .map(group => [group.id, absorbed(group)?.id] as const)
      .filter(([, childId]) => childId !== undefined);

    expect(Object.fromEntries(outcome)).toEqual({
      sellers: 'sellers-list',
      draws: 'draws-all',
      games: 'games-overview',
      limits: 'limits-overview',
      tickets: 'tickets-list',
      tenants: 'tenant-list',
      operations: 'ops-overview',
    });
  });

  it('cannot absorb anything from a group that declares no destination', () => {
    // `archives` et `audit` ont bien un « Apèsi » / « Odit fonctionnel », mais leur groupe n'a pas
    // de `destination` : l'en-tête n'a nulle part où mener, la ligne reste donc nécessaire.
    // Le modèle venant du backend avec ce fichier en simple fallback, l'uniformiser se décide
    // côté contrat, pas ici.
    for (const id of ['archives', 'audit']) {
      const group = groupsOf(PLATFORM_NAVIGATION).find(item => item.id === id);
      expect([id, actionRoute(group!)]).toEqual([id, '']);
    }
  });

  it('leaves shortcut destinations alone', () => {
    // « Référentiels » pointe sur « Jeux », un item parmi dix : ce n'est pas une vue d'ensemble.
    const shortcuts = ['references', 'access', 'support-and-content', 'tchala', 'reports', 'company'];
    for (const id of shortcuts) {
      const group = [...groupsOf(TENANT_ADMIN_NAVIGATION), ...groupsOf(PLATFORM_NAVIGATION)].find(
        item => item.id === id,
      );
      if (!group) continue;
      expect([id, absorbed(group)?.id]).toEqual([id, undefined]);
    }
  });
});
