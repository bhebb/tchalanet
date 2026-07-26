import { Component, Signal, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it } from 'vitest';

import { PublicShellRuntime } from '@tch/page-model';
import { TchBreakpointService } from '@tch/ui/components';

import { PublicHeader } from './public-header';

/**
 * Couverture de `tch-overlay-nav` **au travers du header public** : c'est son seul point de
 * montage, et `ui-components:test` tourne sur Vitest brut, sans le compilateur Angular — il ne
 * sait pas instancier un composant à `input()` signal.
 */

@Component({ standalone: true, template: '' })
class Blank {}

const SHELL = {
  header: {
    brand: { id: 'brand', labelKey: 'public.brand', destination: { kind: 'route', value: '/public' } },
    primary: [
      {
        id: 'results',
        labelKey: 'public.nav.results',
        icon: 'emoji_events',
        destination: { kind: 'route', value: '/public/results' },
        activeMatch: 'prefix',
      },
      {
        id: 'docs',
        labelKey: 'public.nav.docs',
        icon: 'menu_book',
        destination: { kind: 'url', value: 'https://docs.tchalanet.com/' },
      },
    ],
    actions: [],
    secondary: [],
  },
} as unknown as PublicShellRuntime;

function configure(isWide: Signal<boolean> = signal(false)) {
  TestBed.configureTestingModule({
    providers: [
      provideRouter([
        { path: 'public', component: Blank },
        { path: 'public/results', component: Blank },
      ]),
      provideTranslateService(),
      { provide: TchBreakpointService, useValue: { isWide } as unknown as TchBreakpointService },
    ],
  });
}

async function renderAt(url: string): Promise<ComponentFixture<PublicHeader>> {
  await TestBed.inject(Router).navigateByUrl(url);

  const fixture = TestBed.createComponent(PublicHeader);
  fixture.componentRef.setInput('shell', SHELL);
  fixture.detectChanges();
  return fixture;
}

const overlayOf = (fixture: ComponentFixture<unknown>) =>
  fixture.nativeElement.querySelector('.overlay__panel') as HTMLElement | null;
const burgerOf = (fixture: ComponentFixture<unknown>) =>
  fixture.nativeElement.querySelector('.public-header__menu-button') as HTMLButtonElement;

describe('public header navigation', () => {
  describe('sous 840px', () => {
    beforeEach(() => configure());

    it('renders nothing until the menu is opened', async () => {
      const fixture = await renderAt('/public');

      expect(overlayOf(fixture)).toBeNull();
      expect(document.body.classList.contains('tch-overlay-open')).toBe(false);
    });

    it('exposes the open menu as a modal dialog and locks the page scroll', async () => {
      const fixture = await renderAt('/public');

      fixture.componentInstance.toggleMobileMenu();
      fixture.detectChanges();

      const panel = overlayOf(fixture);
      expect(panel?.getAttribute('role')).toBe('dialog');
      expect(panel?.getAttribute('aria-modal')).toBe('true');
      expect(document.body.classList.contains('tch-overlay-open')).toBe(true);
    });

    it('renders external destinations, which the route-only markup used to drop', async () => {
      const fixture = await renderAt('/public');

      fixture.componentInstance.toggleMobileMenu();
      fixture.detectChanges();

      const links = overlayOf(fixture)?.querySelectorAll('a') ?? [];
      expect(links).toHaveLength(2);
      expect(links[1].getAttribute('href')).toBe('https://docs.tchalanet.com/');
      expect(links[1].querySelector('.material-symbols-outlined')?.textContent?.trim()).toBe(
        'menu_book',
      );
    });

    it('announces the current page inside the menu', async () => {
      const fixture = await renderAt('/public/results');

      fixture.componentInstance.toggleMobileMenu();
      fixture.detectChanges();
      // `routerLinkActive` calcule son état une microtâche après la création du lien.
      await fixture.whenStable();
      fixture.detectChanges();

      const active = overlayOf(fixture)?.querySelector('a[href="/public/results"]');
      expect(active?.getAttribute('aria-current')).toBe('page');
    });

    it('closes on Escape and gives the focus back to the burger', async () => {
      const fixture = await renderAt('/public');
      const burger = burgerOf(fixture);
      burger.focus();

      fixture.componentInstance.toggleMobileMenu();
      fixture.detectChanges();
      expect(overlayOf(fixture)).not.toBeNull();

      document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
      fixture.detectChanges();

      expect(fixture.componentInstance.mobileMenuOpen()).toBe(false);
      expect(overlayOf(fixture)).toBeNull();
      expect(document.activeElement).toBe(burger);
      expect(document.body.classList.contains('tch-overlay-open')).toBe(false);
    });
  });

  describe('à partir de 840px', () => {
    it('closes an overlay left open when the window grows past the inline-nav boundary', async () => {
      const isWide = signal(false);
      configure(isWide);
      const fixture = await renderAt('/public');

      fixture.componentInstance.toggleMobileMenu();
      fixture.detectChanges();
      expect(overlayOf(fixture)).not.toBeNull();

      isWide.set(true);
      fixture.detectChanges();

      expect(fixture.componentInstance.mobileMenuOpen()).toBe(false);
      expect(overlayOf(fixture)).toBeNull();
    });
  });
});
