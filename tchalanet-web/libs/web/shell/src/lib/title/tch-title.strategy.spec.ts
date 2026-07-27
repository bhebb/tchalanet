import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Title } from '@angular/platform-browser';
import { Router, provideRouter } from '@angular/router';
import { TranslateService, provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it } from 'vitest';

import { NavigationSection } from '@tch/api';

import { PLATFORM_NAVIGATION, TENANT_ADMIN_NAVIGATION } from '../private-shell/private-navigation.model';
import { provideTchTitleStrategy } from './tch-title.strategy';

@Component({ standalone: true, template: '' })
class Blank {}

const NAVIGATION: readonly NavigationSection[] = [
  {
    id: 'admin',
    titleKey: 'nav.section',
    items: [
      {
        id: 'draws',
        labelKey: 'nav.draws',
        destination: { kind: 'route', value: '/draws' },
        children: [
          {
            id: 'draws-results',
            labelKey: 'nav.draws_results',
            destination: { kind: 'route', value: '/draws/results' },
          },
        ],
      },
    ],
  },
];

const TRANSLATIONS = {
  nav: { draws: 'Tirages', draws_results: 'Résultats des tirages' },
  page: { setup: 'Configuration' },
};

function configure(navigation: readonly NavigationSection[] = []) {
  TestBed.configureTestingModule({
    providers: [
      provideRouter([
        { path: 'setup', component: Blank, data: { titleKey: 'page.setup' } },
        { path: 'draws', component: Blank },
        { path: 'draws/results', component: Blank },
        { path: 'orphan', component: Blank },
        { path: 'unknown-key', component: Blank, data: { titleKey: 'page.missing' } },
      ]),
      provideTranslateService(),
      ...provideTchTitleStrategy(navigation),
    ],
  });

  // Le suffixe est le <title> d'index.html, capturé à la construction de la stratégie.
  TestBed.inject(Title).setTitle('Tchalanet Admin');
  const translate = TestBed.inject(TranslateService);
  translate.setTranslation('fr', TRANSLATIONS);
  translate.use('fr');
  return { router: TestBed.inject(Router), title: TestBed.inject(Title), translate };
}

describe('TchTitleStrategy', () => {
  beforeEach(() => TestBed.resetTestingModule());

  it('composes the page title from the route data key', async () => {
    const { router, title } = configure();

    await router.navigateByUrl('/setup');

    expect(title.getTitle()).toBe('Configuration · Tchalanet Admin');
  });

  it('falls back to the navigation model when the route declares nothing', async () => {
    const { router, title } = configure(NAVIGATION);

    await router.navigateByUrl('/draws');

    expect(title.getTitle()).toBe('Tirages · Tchalanet Admin');
  });

  it('prefers the most specific navigation destination', async () => {
    const { router, title } = configure(NAVIGATION);

    await router.navigateByUrl('/draws/results');

    expect(title.getTitle()).toBe('Résultats des tirages · Tchalanet Admin');
  });

  it('keeps the brand alone when nothing describes the page', async () => {
    const { router, title } = configure(NAVIGATION);

    await router.navigateByUrl('/orphan');

    expect(title.getTitle()).toBe('Tchalanet Admin');
  });

  it('does not leak a raw i18n key into the tab when the translation is missing', async () => {
    const { router, title } = configure();

    await router.navigateByUrl('/unknown-key');

    expect(title.getTitle()).toBe('Tchalanet Admin');
  });

  it('covers the real console navigation models', () => {
    // Le modèle de navigation est la source des titres pour les écrans qui ne déclarent rien :
    // chaque destination doit donc porter un libellé, sinon la page reste sans titre d'onglet.
    const destinations = [...TENANT_ADMIN_NAVIGATION, ...PLATFORM_NAVIGATION]
      .flatMap(section => section.items.flatMap(item => [item, ...(item.children ?? [])]))
      .filter(item => item.destination?.kind === 'route');

    expect(destinations.length).toBeGreaterThan(50);
    expect(destinations.filter(item => !item.labelKey)).toEqual([]);
  });

  it('re-applies the title when the language changes', async () => {
    const { router, title, translate } = configure();
    await router.navigateByUrl('/setup');

    translate.setTranslation('en', { page: { setup: 'Settings' } });
    translate.use('en');

    expect(title.getTitle()).toBe('Settings · Tchalanet Admin');
  });
});
