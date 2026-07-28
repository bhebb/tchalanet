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
 * Racine — une **liste plate** : chaque entrée est une ligne icône + libellé + chevron, qu'elle
 * mène directement à une route ou qu'elle ouvre un panneau. Une grille de cartes façon dashboard a
 * été essayée puis écartée : le menu ne doit pas ressembler à un second tableau de bord, et le
 * chevron communique déjà « ceci ouvre autre chose » sans avoir besoin d'un habillage différent
 * pour les entrées à enfants.
 *
 * Le tap sur une ligne à enfants ouvre un second panneau par-dessus, dont le retour ne referme que
 * ce niveau. Les entrées sont regroupées par **section du modèle** (les activités quotidiennes
 * séparées de la configuration) plutôt que par leur nombre d'enfants : sans ça, une page de
 * réglages sans sous-page atterrit mécaniquement en tête de menu, devant les activités que la
 * console admin ouvre tous les jours.
 *
 * L'accordéon de `tch-sidebar-nav` reste le rendu de la sidebar permanente (≥ 840px), où la place
 * verticale ne manque pas. Les deux composants rendent **le même modèle** et partagent la logique
 * d'activité de route (`../navigation/route-activity`).
 *
 * **L'en-tête de panneau absorbe l'enfant qui pointe vers la même route que le groupe.** Sans ça
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
  readonly expandLabel = input('Développer');
  readonly collapseLabel = input('Réduire');
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
   * Un bloc de liste par **section** du modèle. Les sections portent déjà un titre et sont la
   * façon dont le modèle sépare les domaines — les activités quotidiennes d'un côté, la
   * configuration de l'autre. S'en servir évite de déduire le rang d'une entrée de son nombre
   * d'enfants, ce qui plaçait des réglages rarement ouverts en tête de menu.
   */
  readonly groups = computed(() =>
    this.sections().map(section => ({
      id: section.id,
      titleKey: section.titleKey,
      items: section.items,
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

  isCategoryOpen(item: ActionItem): boolean {
    return this.openCategoryId() === item.id;
  }

  /**
   * Le chevron d'une catégorie qui a aussi sa propre destination : il ne fait que
   * replier/déplier le panneau, jamais naviguer — la navigation passe par le lien du libellé, un
   * élément séparé. Bascule plutôt qu'ouvre : contrairement au bouton pleine ligne (catégorie sans
   * destination), le chevron reste un contrôle à part entière avec `aria-expanded`.
   */
  toggleCategoryPanel(item: ActionItem, event: Event): void {
    event.stopPropagation();
    if (this.isCategoryOpen(item)) {
      this.closeCategoryPanel();
      return;
    }
    this.trigger = event.currentTarget as HTMLElement | null;
    this.openCategoryId.set(item.id);
  }

  toggleButtonLabel(item: ActionItem): string {
    const verb = this.isCategoryOpen(item) ? this.collapseLabel() : this.expandLabel();
    return `${verb} ${this.label(item)}`;
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
