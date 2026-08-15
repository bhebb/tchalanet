import { provideHttpClient } from '@angular/common/http';
import { Signal, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideState, provideStore } from '@ngrx/store';
import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it } from 'vitest';

import { AUTH_CLIENT, AuthClient, PrivateBootstrapStore } from '@tch/core/auth';
import { I18nFacade, i18nFeature } from '@tch/core/i18n';
import { TchBreakpointService } from '@tch/ui/components';
import { ThemeStore, themeStoreProvider } from '@tch/ui/theme';

import { PrivateShellLayoutComponent } from './private-shell-layout.component';

const authClient: AuthClient = {
  isAuthenticated: async () => false,
  login: async () => undefined,
  logout: async () => undefined,
  getAccessToken: async () => null,
  getTokenExpiresAt: async () => undefined,
};

/** Seul `isWide()` est consommé par le shell — le reste du service n'a pas à être simulé. */
function breakpointStub(isWide: Signal<boolean>): TchBreakpointService {
  return { isWide } as unknown as TchBreakpointService;
}

function configure(isWide = signal(false)) {
  TestBed.configureTestingModule({
    providers: [
      provideRouter([]),
      provideTranslateService(),
      provideStore({}),
      provideState(i18nFeature),
      provideHttpClient(),
      themeStoreProvider,
      { provide: AUTH_CLIENT, useValue: authClient },
      { provide: TchBreakpointService, useValue: breakpointStub(isWide) },
    ],
  });
}

function render(): ComponentFixture<PrivateShellLayoutComponent> {
  const fixture = TestBed.createComponent(PrivateShellLayoutComponent);
  fixture.componentRef.setInput('brand', {
    id: 'brand',
    label: 'Tchalanet',
    destination: { kind: 'route', value: '/' },
  });
  fixture.componentRef.setInput('primary', [
    { id: 'dashboard', label: 'Dashboard', destination: { kind: 'url', value: '#dashboard' } },
  ]);
  fixture.detectChanges();
  return fixture;
}

const drawerOf = (fixture: ComponentFixture<unknown>) =>
  fixture.nativeElement.querySelector('.drawer') as HTMLElement;
const contentOf = (fixture: ComponentFixture<unknown>) =>
  fixture.nativeElement.querySelector('.content') as HTMLElement;
const burgerOf = (fixture: ComponentFixture<unknown>) =>
  fixture.nativeElement.querySelector('.burger') as HTMLButtonElement;
const textOf = (fixture: ComponentFixture<unknown>, selector: string) =>
  (fixture.nativeElement.querySelector(selector) as HTMLElement | null)?.textContent?.trim() ?? '';

function setBootstrap(
  options: { tenantName?: string | null; tenantCode?: string | null } = {},
): void {
  TestBed.inject(PrivateBootstrapStore).setBootstrap({
    space: options.tenantName === null ? 'PLATFORM' : 'ADMIN',
    user: {
      userId: 'user-1',
      username: 'admin',
      displayName: 'Admin',
      email: 'admin@example.test',
      roles: ['TENANT_ADMIN'],
      defaultSpace: options.tenantName === null ? 'PLATFORM' : 'ADMIN',
      preferredLocale: 'ht',
      preferredTimezone: 'America/Port-au-Prince',
    },
    tenantContext:
      options.tenantName === null
        ? null
        : {
            tenantId: 'tenant-1',
            tenantCode: options.tenantCode ?? 'TITI',
            tenantName: options.tenantName ?? 'Chez Titi',
          },
    entitlements: { roles: [], permissions: [] },
    readiness: { status: 'READY', checks: [] },
    notifications: { unreadCount: 0, criticalCount: 0 },
    navigationDrawer: null,
    pageModelRef: { route: '/app/admin', endpoint: '/runtime/page' },
    partial: false,
  });
}

describe('PrivateShellLayoutComponent', () => {
  beforeEach(() => configure());

  it('keeps the drawer closed by default and toggles it explicitly', () => {
    const component = TestBed.createComponent(PrivateShellLayoutComponent).componentInstance;

    expect(component.drawerOpen()).toBe(false);

    component.toggleDrawer();
    expect(component.drawerOpen()).toBe(true);

    component.closeDrawer();
    expect(component.drawerOpen()).toBe(false);
  });

  it('emits a theme toggle request without owning theme state', () => {
    const component = TestBed.createComponent(PrivateShellLayoutComponent).componentInstance;
    let emitted = 0;

    component.themeToggled.subscribe(() => {
      emitted += 1;
    });

    component.themeToggled.emit();

    expect(emitted).toBe(1);
    expect(component.themeIcon()).toBe('dark_mode');
  });

  it('closes the drawer when a navigation item is activated', () => {
    const fixture = render();
    fixture.componentInstance.drawerOpen.set(true);
    fixture.detectChanges();

    fixture.nativeElement.querySelector('tch-drawer-nav a').click();

    expect(fixture.componentInstance.drawerOpen()).toBe(false);
  });

  it('moves the tenant context into the mobile drawer instead of a second header row', () => {
    setBootstrap({ tenantName: 'Chez Titi avec un nom vraiment très long' });
    const fixture = render();

    expect(fixture.nativeElement.querySelector('.context-bar')).toBeNull();
    expect(textOf(fixture, '[data-testid="private-shell-drawer-context"]')).toContain(
      'Chez Titi avec un nom vraiment très long',
    );
    expect(
      fixture.nativeElement.querySelector('.drawer-context__name')?.getAttribute('title'),
    ).toBe('Chez Titi avec un nom vraiment très long');
  });

  it('shows the platform scope when no effective tenant is present', () => {
    setBootstrap({ tenantName: null });
    const fixture = render();

    expect(textOf(fixture, '[data-testid="private-shell-drawer-context"]')).toContain(
      'surface.platform_admin',
    );
    expect(textOf(fixture, '[data-testid="private-shell-drawer-context"]')).toContain(
      'surface.tenant_admin',
    );
  });

  it('changes language from the drawer quick settings through the i18n facade', () => {
    const fixture = render();

    fixture.nativeElement.querySelector('[data-testid="private-shell-language-trigger"]').click();
    fixture.detectChanges();
    (
      fixture.nativeElement.querySelectorAll('.drawer-settings__option')[1] as HTMLButtonElement
    ).click();
    fixture.detectChanges();

    expect(TestBed.inject(I18nFacade).currentLanguage()).toBe('fr');
    expect(fixture.componentInstance.settingsPanel()).toBeNull();
  });

  it('changes theme mode from the drawer quick settings through ThemeStore', () => {
    const fixture = render();

    fixture.nativeElement.querySelector('[data-testid="private-shell-theme-trigger"]').click();
    fixture.detectChanges();
    (
      fixture.nativeElement.querySelectorAll('.drawer-settings__option')[2] as HTMLButtonElement
    ).click();
    fixture.detectChanges();

    expect(TestBed.inject(ThemeStore).activeTheme().mode).toBe('dark');
    expect(fixture.componentInstance.settingsPanel()).toBeNull();
  });
});

describe('PrivateShellLayoutComponent — drawer replié (< 840px)', () => {
  beforeEach(() => configure(signal(false)));

  it('exposes the open drawer as a modal dialog and neutralises the content behind it', () => {
    const fixture = render();

    fixture.componentInstance.drawerOpen.set(true);
    fixture.detectChanges();

    const drawer = drawerOf(fixture);
    expect(drawer.getAttribute('role')).toBe('dialog');
    expect(drawer.getAttribute('aria-modal')).toBe('true');
    expect(drawer.hasAttribute('inert')).toBe(false);
    expect(contentOf(fixture).hasAttribute('inert')).toBe(true);
    expect(document.body.classList.contains('tch-overlay-open')).toBe(true);
  });

  it('keeps the collapsed drawer out of the keyboard path', () => {
    const fixture = render();

    const drawer = drawerOf(fixture);
    expect(drawer.hasAttribute('inert')).toBe(true);
    expect(drawer.getAttribute('role')).toBeNull();
    expect(contentOf(fixture).hasAttribute('inert')).toBe(false);
  });

  it('closes on Escape and restores focus to the burger', () => {
    const fixture = render();
    const burger = burgerOf(fixture);
    burger.focus();

    fixture.componentInstance.toggleDrawer();
    fixture.detectChanges();
    expect(fixture.componentInstance.drawerModal()).toBe(true);

    fixture.componentInstance.onEscape();
    fixture.detectChanges();

    expect(fixture.componentInstance.drawerOpen()).toBe(false);
    expect(document.activeElement).toBe(burger);
    expect(contentOf(fixture).hasAttribute('inert')).toBe(false);
    expect(document.body.classList.contains('tch-overlay-open')).toBe(false);
  });
});

describe('PrivateShellLayoutComponent — sidebar permanente (≥ 840px)', () => {
  beforeEach(() => configure(signal(true)));

  it('is a plain navigation panel, never a dialog', () => {
    const fixture = render();

    fixture.componentInstance.drawerOpen.set(true);
    fixture.detectChanges();

    const drawer = drawerOf(fixture);
    expect(fixture.componentInstance.drawerModal()).toBe(false);
    expect(drawer.getAttribute('role')).toBeNull();
    expect(drawer.getAttribute('aria-modal')).toBeNull();
    expect(drawer.hasAttribute('inert')).toBe(false);
    expect(contentOf(fixture).hasAttribute('inert')).toBe(false);
    expect(document.body.classList.contains('tch-overlay-open')).toBe(false);
  });

  it('ignores Escape when the drawer is permanent', () => {
    const fixture = render();
    fixture.componentInstance.drawerOpen.set(true);
    fixture.detectChanges();

    fixture.componentInstance.onEscape();

    expect(fixture.componentInstance.drawerOpen()).toBe(true);
  });

  it('keeps the workspace grid to exactly drawer, backdrop and content', () => {
    // Le focus trap CDK insère deux sentinelles comme enfants directs de `.workspace`. Non
    // détruites hors overlay, elles deviennent deux items de plus dans la grille à 2 colonnes et
    // poussent le drawer et le contenu sur deux lignes empilées au lieu d'être côte à côte —
    // constaté à 1280px avant correction. `.workspace` doit rester à exactement 3 enfants.
    const fixture = render();
    fixture.detectChanges();

    const workspace = fixture.nativeElement.querySelector('.workspace') as HTMLElement;
    expect(workspace.children).toHaveLength(3);
    expect(workspace.querySelector('.cdk-focus-trap-anchor')).toBeNull();
  });
});

describe('PrivateShellLayoutComponent — franchissement de la borne', () => {
  it('destroys the focus trap sentinels when the sidebar becomes permanent', () => {
    const isWide = signal(false);
    configure(isWide);
    const fixture = render();
    fixture.componentInstance.drawerOpen.set(true);
    fixture.detectChanges();

    const workspace = fixture.nativeElement.querySelector('.workspace') as HTMLElement;
    expect(workspace.querySelector('.cdk-focus-trap-anchor')).not.toBeNull();

    isWide.set(true);
    fixture.detectChanges();

    expect(workspace.children).toHaveLength(3);
    expect(workspace.querySelector('.cdk-focus-trap-anchor')).toBeNull();
  });

  it('destroys the focus trap sentinels when the overlay drawer closes, without becoming wide', () => {
    // Live regression on staging at 768px (overlay mode throughout, sidebar never becomes
    // permanent): the sentinels used to be created as soon as the layout left "wide" mode and
    // stayed until the layout became wide again — closing the drawer didn't destroy them. Each
    // sentinel rendered with real height in `.workspace`'s single-column grid, pushing `<main>`
    // ~240px down. The trap must not outlive the moment the drawer is actually open (modal).
    configure(signal(false));
    const fixture = render();
    fixture.componentInstance.drawerOpen.set(true);
    fixture.detectChanges();

    const workspace = fixture.nativeElement.querySelector('.workspace') as HTMLElement;
    expect(workspace.querySelector('.cdk-focus-trap-anchor')).not.toBeNull();

    fixture.componentInstance.drawerOpen.set(false);
    fixture.detectChanges();

    expect(workspace.children).toHaveLength(3);
    expect(workspace.querySelector('.cdk-focus-trap-anchor')).toBeNull();
  });
});
