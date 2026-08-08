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
 * Le panneau liste tous les enfants sans filtrage, y compris celui qui mène à la même route que le
 * groupe (« Lis tikè », « Apèsi »…) — masquer cette entrée forçait à taper le titre du panneau pour
 * l'atteindre, un texte qui ne ressemble à rien de cliquable. La sidebar desktop ne l'a jamais
 * masquée non plus ; les deux rendus s'accordent maintenant.
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
  readonly sectionCollapseOverrides = signal<Readonly<Record<string, boolean>>>({});

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
    this.sections().map((section, index) => ({
      id: section.id,
      titleKey: section.titleKey,
      items: section.items,
      collapsible: index > 0,
    })),
  );

  readonly openCategory = computed(
    () => this.allCategories().find(item => item.id === this.openCategoryId()) ?? null,
  );

  /** Enfants du panneau, sans filtrage — voir la note de classe sur l'entrée d'atterrissage. */
  readonly openCategoryItems = computed(() => this.openCategory()?.children ?? []);

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

  isSectionCollapsed(section: {
    id: string;
    items: readonly ActionItem[];
    collapsible: boolean;
  }): boolean {
    if (!section.collapsible) return false;
    if (this.sectionHasActiveItem(section.items)) return false;
    const override = this.sectionCollapseOverrides()[section.id];
    return override ?? true;
  }

  toggleSection(section: { id: string; items: readonly ActionItem[]; collapsible: boolean }): void {
    if (!section.collapsible) return;
    this.sectionCollapseOverrides.update(overrides => ({
      ...overrides,
      [section.id]: !this.isSectionCollapsed(section),
    }));
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
      isItemActive(this.activity(), item, children) ||
      children.some(child => isItemActive(this.activity(), child, children))
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

  private label(item: ActionItem): string {
    const text = actionText(item);
    const translated = this.translate.instant(text);
    return typeof translated === 'string' ? translated : text;
  }

  private sectionHasActiveItem(items: readonly ActionItem[]): boolean {
    return items.some(
      item =>
        this.isCategoryActive(item) ||
        isItemActive(this.activity(), item, items) ||
        item.children?.some(child => isItemActive(this.activity(), child, item.children ?? [])),
    );
  }
}
