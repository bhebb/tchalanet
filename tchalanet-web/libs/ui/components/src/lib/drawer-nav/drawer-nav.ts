import { NgTemplateOutlet } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { TranslateService, TranslatePipe } from '@ngx-translate/core';
import { filter, map } from 'rxjs';

import {
  ActionItem,
  NavigationSection,
  actionHref,
  actionQueryParams,
  actionRoute,
  actionText,
  isExternalAction,
  isRouteAction,
} from '@tch/api';

import {
  RouteActivity,
  comparableUrl,
  isActionActive as isItemActive,
  routeQueryParams,
} from '../navigation/route-activity';

/**
 * Navigation repliée des consoles : deux niveaux.
 *
 * Racine — les entrées sans enfants sont des lignes, celles qui en ont deviennent des cartes de
 * catégorie dans une grille. Ouvrir une catégorie fait glisser un second panneau par-dessus, dont
 * le retour ne referme que ce niveau.
 *
 * L'accordéon de `tch-sidebar-nav` reste le rendu de la sidebar permanente (≥ 840px), où la place
 * verticale ne manque pas. Les deux composants rendent **le même modèle** et partagent la logique
 * d'activité de route (`../navigation/route-activity`).
 *
 * **L'en-tête de catégorie absorbe l'enfant qui pointe vers la même route que le groupe.** Sans ça
 * la liste répète l'entrée d'atterrissage (« Apèsi », « Lis tèminal »…) alors que le titre du
 * panneau y mène déjà.
 */
@Component({
  selector: 'tch-drawer-nav',
  imports: [NgTemplateOutlet, RouterLink, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './drawer-nav.html',
  styleUrl: './drawer-nav.scss',
})
export class TchDrawerNav {
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);

  readonly primary = input<readonly ActionItem[]>([]);
  readonly sections = input<readonly NavigationSection[]>([]);
  readonly secondary = input<readonly ActionItem[]>([]);
  readonly ariaLabel = input('Navigation');
  readonly searchLabel = input('');
  readonly searchPlaceholder = input('');
  readonly backLabel = input('Retour');
  readonly categoriesLabel = input('');
  /**
   * Clé i18n du compteur de pages — une **clé** et non un libellé résolu, contrairement aux autres
   * entrées : elle a besoin du paramètre `count`, que seul ce composant connaît.
   */
  readonly pagesLabelKey = input('');
  readonly noResultLabel = input('');

  readonly itemActivated = output<ActionItem>();

  readonly actionRoute = actionRoute;
  readonly actionHref = actionHref;
  readonly actionQueryParams = actionQueryParams;
  readonly actionText = actionText;
  readonly isExternalAction = isExternalAction;
  readonly isRouteAction = isRouteAction;

  readonly query = signal('');
  readonly openCategoryId = signal<string | null>(null);

  private readonly panelRef = viewChild<ElementRef<HTMLElement>>('panel');
  private trigger: HTMLElement | null = null;

  private readonly currentUrl = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map(() => this.router.url),
    ),
    { initialValue: this.router.url },
  );

  private readonly activity = computed<RouteActivity>(() => ({
    url: comparableUrl(this.currentUrl()),
    queryParams: routeQueryParams(this.router, this.currentUrl()),
  }));

  /** Entrées de premier niveau, dans l'ordre du modèle. */
  private readonly rootItems = computed(() => [
    ...this.primary(),
    ...this.sections().flatMap(section => section.items),
  ]);

  /** Bas de menu : ce qui n'est pas une activité métier (l'entreprise, l'aide). */
  readonly footerItems = computed(() => this.secondary());

  /** Tout ce qui peut ouvrir un panneau ou remonter dans une recherche, footer compris. */
  private readonly allItems = computed(() => [...this.rootItems(), ...this.footerItems()]);

  /** Une catégorie peut vivre dans n'importe quelle section, ou dans le bas de menu. */
  private readonly allCategories = computed(() =>
    this.allItems().filter(item => !!item.children?.length),
  );

  /** Raccourcis de tête : les entrées hors section, avant tout regroupement. */
  readonly shortcuts = computed(() => this.primary());

  /**
   * Un groupe de grille par **section** du modèle. Les sections portent déjà un titre et sont la
   * façon dont le modèle sépare les domaines — les activités quotidiennes d'un côté, la
   * configuration de l'autre. S'en servir évite de déduire le rang d'une entrée de son nombre
   * d'enfants, ce qui plaçait des réglages rarement ouverts en tête de menu.
   */
  readonly groups = computed(() =>
    this.sections().map(section => ({
      id: section.id,
      titleKey: section.titleKey,
      rows: section.items.filter(item => !item.children?.length),
      cards: section.items.filter(item => !!item.children?.length),
    })),
  );

  readonly openCategory = computed(
    () => this.allCategories().find(item => item.id === this.openCategoryId()) ?? null,
  );

  /** Enfants du panneau, moins celui que l'en-tête absorbe. */
  readonly openCategoryItems = computed(() => {
    const category = this.openCategory();
    if (!category) return [];
    const absorbed = this.absorbedChild(category);
    return (category.children ?? []).filter(child => child.id !== absorbed?.id);
  });

  /** Résultats de recherche : entrées de tout niveau dont le libellé traduit correspond. */
  readonly searchResults = computed(() => {
    const needle = this.query().trim().toLowerCase();
    if (!needle) return [];
    return this.allItems()
      .flatMap(item => (item.children?.length ? item.children : [item]))
      .filter(item => this.label(item).toLowerCase().includes(needle));
  });

  readonly searching = computed(() => this.query().trim().length > 0);

  constructor() {
    effect(() => {
      const panel = this.panelRef()?.nativeElement;
      if (panel) {
        // Le panneau vient d'apparaître : y amener le focus, sinon le clavier reste sur la carte
        // désormais masquée.
        panel.querySelector<HTMLElement>('[data-drawer-back]')?.focus();
      }
    });
  }

  hasChildren(item: ActionItem): boolean {
    return !!item.children?.length;
  }

  childCount(item: ActionItem): number {
    return item.children?.length ?? 0;
  }

  pagesCount(item: ActionItem): string {
    const count = this.childCount(item);
    const key = this.pagesLabelKey();
    if (!key) return String(count);
    const label = this.translate.instant(key, { count });
    return typeof label === 'string' && label !== key ? label : String(count);
  }

  /** Route d'atterrissage de la catégorie, quand son en-tête doit être un lien. */
  categoryRoute(item: ActionItem): string {
    return actionRoute(item);
  }

  isActionActive(item: ActionItem, siblings: readonly ActionItem[] = []): boolean {
    return isItemActive(this.activity(), item, siblings);
  }

  /** Une catégorie est active si elle-même ou l'un de ses enfants l'est. */
  isCategoryActive(item: ActionItem): boolean {
    const children = item.children ?? [];
    return (
      isItemActive(this.activity(), item, children)
      || children.some(child => isItemActive(this.activity(), child, children))
    );
  }

  openCategoryPanel(item: ActionItem, event: Event): void {
    this.trigger = event.currentTarget as HTMLElement | null;
    this.openCategoryId.set(item.id);
  }

  closeCategoryPanel(): void {
    this.openCategoryId.set(null);
    const trigger = this.trigger;
    this.trigger = null;
    trigger?.focus();
  }

  onSearchInput(event: Event): void {
    this.query.set((event.target as HTMLInputElement).value);
  }

  onItemClick(event: Event, item: ActionItem): void {
    if (item.disabled) {
      event.preventDefault();
      event.stopPropagation();
      return;
    }
    this.itemActivated.emit(item);
  }

  /**
   * L'enfant qui mène exactement là où mène le groupe : le titre du panneau le remplace.
   *
   * `activeMatch: 'exact'` est exigé, et ce n'est pas un détail. Beaucoup de groupes déclarent une
   * `destination` qui n'est qu'un raccourci vers leur premier enfant (« Référentiels » pointe sur
   * « Jeux », un item parmi dix) : sans cette condition, cet enfant disparaîtrait de la liste alors
   * qu'il n'est pas la page d'atterrissage de la catégorie. Une vraie vue d'ensemble, elle, est
   * toujours marquée `exact` — sinon son préfixe avalerait ses frères.
   */
  private absorbedChild(item: ActionItem): ActionItem | undefined {
    const route = actionRoute(item);
    if (!route) return undefined;
    return (item.children ?? []).find(
      child => child.activeMatch === 'exact' && actionRoute(child) === route,
    );
  }

  private label(item: ActionItem): string {
    const text = actionText(item);
    const translated = this.translate.instant(text);
    return typeof translated === 'string' ? translated : text;
  }
}
