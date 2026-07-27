import { Injectable, InjectionToken, Provider, inject } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterStateSnapshot, TitleStrategy } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

import { ActionItem, NavigationSection, actionRoute } from '@tch/api';

/**
 * Modèle de navigation consulté quand une route ne déclare pas son propre `titleKey`.
 *
 * Accepte une fonction plutôt qu'un tableau figé : le menu des consoles vient du **backend**
 * (`navigationDrawer()`), le modèle statique n'en étant que le repli. Sans indirection, le titre
 * d'onglet viendrait du repli pendant que le menu affiche autre chose.
 */
export type TitleNavigation = readonly NavigationSection[] | (() => readonly NavigationSection[]);

export const TCH_TITLE_NAVIGATION = new InjectionToken<TitleNavigation>('TCH_TITLE_NAVIGATION');

/** Séparateur entre le titre de page et le nom du portail. */
const SEPARATOR = ' · ';

/**
 * Titre du document dérivé de la route active.
 *
 * Deux sources, dans cet ordre :
 *
 * 1. `data.titleKey` de la route la plus profonde — la convention déjà en place dans les fichiers
 *    de routes. Une route enfant peut ainsi préciser le titre de son parent.
 * 2. À défaut, le **modèle de navigation** : il associe déjà chaque destination à un `labelKey`,
 *    donc l'entrée de menu qui a mené à la page en donne le titre. C'est la source unique — un
 *    nouvel écran ajouté au menu obtient son titre d'onglet sans configuration supplémentaire.
 *
 * Sans l'un ni l'autre, le titre retombe sur le seul nom du portail.
 *
 * Ce nom est le `<title>` d'`index.html` (« Tchalanet », « Tchalanet Admin »,
 * « Tchalanet Platform »), capturé au démarrage : c'est une marque, pas une chaîne à traduire.
 *
 * Le titre est réappliqué au changement de langue — sans ça l'onglet resterait figé dans la langue
 * active au moment de la navigation.
 */
@Injectable()
export class TchTitleStrategy extends TitleStrategy {
  private readonly title = inject(Title);
  private readonly translate = inject(TranslateService);
  private readonly navigationSource = inject(TCH_TITLE_NAVIGATION, { optional: true }) ?? [];

  private readonly appTitle = this.title.getTitle();
  private currentTitleKey: string | null = null;

  constructor() {
    super();
    this.translate.onLangChange.subscribe(() => this.apply());
  }

  override updateTitle(snapshot: RouterStateSnapshot): void {
    this.currentTitleKey =
      this.buildTitle(snapshot)
      ?? this.deepestTitleKey(snapshot)
      ?? this.navigationTitleKey(snapshot.url)
      ?? null;
    this.apply();
  }

  private apply(): void {
    const pageTitle = this.currentTitleKey ? this.translateKey(this.currentTitleKey) : '';
    this.title.setTitle(pageTitle ? `${pageTitle}${SEPARATOR}${this.appTitle}` : this.appTitle);
  }

  /**
   * `data.titleKey` de la route la plus profonde. `buildTitle()` ne couvre que le `title` natif
   * d'Angular ; le workspace déclare ses titres en clés i18n dans `data`.
   */
  private deepestTitleKey(snapshot: RouterStateSnapshot): string | undefined {
    let route = snapshot.root.firstChild;
    let found: string | undefined;
    while (route) {
      const key = route.data?.['titleKey'];
      if (typeof key === 'string' && key) {
        found = key;
      }
      route = route.firstChild;
    }
    return found;
  }

  /** Destination de navigation la plus spécifique qui préfixe l'URL courante. */
  private navigationTitleKey(url: string): string | undefined {
    const path = url.split('?')[0].split('#')[0].replace(/\/$/, '') || '/';
    let best: { key: string; length: number } | undefined;

    for (const item of this.flatItems()) {
      const route = actionRoute(item).replace(/\/$/, '');
      if (!route) continue;
      const matches = item.activeMatch === 'exact'
        ? path === route
        : path === route || path.startsWith(`${route}/`);
      if (!matches) continue;
      const key = item.labelKey;
      if (key && (!best || route.length > best.length)) {
        best = { key, length: route.length };
      }
    }
    return best?.key;
  }

  private flatItems(): readonly ActionItem[] {
    const sections =
      typeof this.navigationSource === 'function' ? this.navigationSource() : this.navigationSource;
    return sections.flatMap(section =>
      section.items.flatMap(item => [item, ...(item.children ?? [])]),
    );
  }

  /** `instant()` renvoie la clé quand elle manque : on préfère alors n'afficher que la marque. */
  private translateKey(key: string): string {
    const value = this.translate.instant(key);
    return value === key ? '' : value;
  }
}

/**
 * @param navigation modèle de navigation du portail, consulté à défaut de `data.titleKey`.
 *   Omettre l'argument laisse l'application fournir `TCH_TITLE_NAVIGATION` elle-même — utile pour
 *   brancher une source runtime via `useFactory`.
 */
export function provideTchTitleStrategy(navigation?: TitleNavigation): Provider[] {
  return [
    ...(navigation ? [{ provide: TCH_TITLE_NAVIGATION, useValue: navigation }] : []),
    { provide: TitleStrategy, useClass: TchTitleStrategy },
  ];
}
