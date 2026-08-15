import { provideHttpClient } from '@angular/common/http';
import { Component, Signal, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { provideState, provideStore } from '@ngrx/store';
import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it } from 'vitest';

import { NavigationSection } from '@tch/api';
import { AUTH_CLIENT, AuthClient } from '@tch/core/auth';
import { i18nFeature } from '@tch/core/i18n';
import { TchBreakpointService } from '@tch/ui/components';
import { themeStoreProvider } from '@tch/ui/theme';

import { PrivateShellLayoutComponent } from './private-shell-layout.component';

/**
 * Couverture de l'état actif **au travers du shell** — il vaut pour les deux rendus, `tch-drawer-nav`
 * sous 840px et `tch-sidebar-nav` au-dessus, qui partagent `route-activity`.
 *
 * Couverture : c'est là que la densité et l'état actif
 * sont réellement câblés, et `ui-components:test` tourne sur Vitest brut, sans le compilateur
 * Angular — il ne sait pas instancier un composant à `input()` signal.
 */

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
    titleKey: 'nav.admin.section.admin',
    items: [
      {
        id: 'dashboard',
        labelKey: 'nav.dashboard',
        destination: { kind: 'route', value: '/app/admin' },
        activeMatch: 'exact',
      },
      {
        id: 'draws',
        labelKey: 'nav.draws',
        destination: { kind: 'route', value: '/app/admin/draws' },
      },
    ],
  },
];

function configure(isWide: Signal<boolean>) {
  TestBed.configureTestingModule({
    providers: [
      provideRouter([
        { path: 'app/admin', component: Blank },
        { path: 'app/admin/draws', component: Blank },
      ]),
      provideTranslateService(),
      provideStore({}),
      provideState(i18nFeature),
      provideHttpClient(),
      themeStoreProvider,
      { provide: AUTH_CLIENT, useValue: authClient },
      { provide: TchBreakpointService, useValue: { isWide } as unknown as TchBreakpointService },
    ],
  });
}

async function renderAt(url: string): Promise<ComponentFixture<PrivateShellLayoutComponent>> {
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

const linkFor = (fixture: ComponentFixture<unknown>, route: string) =>
  fixture.nativeElement.querySelector(`a[href="${route}"]`) as HTMLAnchorElement;

describe('private shell navigation', () => {
  describe('drawer replié (< 840px)', () => {
    beforeEach(() => configure(signal(false)));

    it('announces the current page on the active item', async () => {
      const fixture = await renderAt('/app/admin/draws');

      expect(linkFor(fixture, '/app/admin/draws').getAttribute('aria-current')).toBe('page');
      expect(linkFor(fixture, '/app/admin').getAttribute('aria-current')).toBeNull();
    });

    it('keeps aria-current in step with the exact-match rule', async () => {
      const fixture = await renderAt('/app/admin');

      expect(linkFor(fixture, '/app/admin').getAttribute('aria-current')).toBe('page');
      expect(linkFor(fixture, '/app/admin/draws').getAttribute('aria-current')).toBeNull();
    });
  });

  describe('sidebar permanente (≥ 840px)', () => {
    beforeEach(() => configure(signal(true)));

    it('names each navigation section from its own heading', async () => {
      const fixture = await renderAt('/app/admin');

      const section = fixture.nativeElement.querySelector('.sidebar__section') as HTMLElement;
      const heading = section.querySelector('h2') as HTMLElement;
      expect(heading.id).toBeTruthy();
      expect(section.getAttribute('aria-labelledby')).toBe(heading.id);
    });
  });
});
