import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { filter, map } from 'rxjs';

import { ActionItem, NavigationSection, actionQueryParams, actionRoute, actionText, isRouteAction } from '@tch/api';

@Component({
  selector: 'tch-sidebar-nav',
  imports: [NgTemplateOutlet, RouterLink, RouterLinkActive, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <nav class="sidebar" [attr.aria-label]="ariaLabel()">
      <ng-container *ngTemplateOutlet="itemsTemplate; context: { $implicit: primary() }" />
      @for (section of sections(); track section.id) {
        <section class="sidebar__section">
          <h2>{{ section.titleKey | translate }}</h2>
          <ng-container *ngTemplateOutlet="itemsTemplate; context: { $implicit: section.items }" />
        </section>
      }
      <div class="sidebar__secondary">
        <ng-container *ngTemplateOutlet="itemsTemplate; context: { $implicit: secondary() }" />
      </div>
    </nav>

    <ng-template #itemsTemplate let-items>
      @for (item of items; track item.id) {
        @if (item.children?.length) {
          <!-- Accordion group: the parent toggles (never navigates). -->
          <button
            type="button"
            class="sidebar__group-toggle"
            [class.is-open]="isOpen(item)"
            [class.is-active-group]="isActiveGroup(item)"
            [attr.aria-expanded]="isOpen(item)"
            (click)="toggle(item)"
          >
            @if (item.icon) {
              <span class="material-symbols-outlined" aria-hidden="true">{{ item.icon }}</span>
            }
            <span class="sidebar__group-label">{{ actionText(item) | translate }}</span>
            <span class="material-symbols-outlined sidebar__chevron" aria-hidden="true">
              expand_more
            </span>
          </button>
          @if (isOpen(item)) {
            <div class="sidebar__children">
              @for (child of item.children; track child.id) {
                @if (isRouteAction(child)) {
                  <a
                    class="sidebar__child"
                    [class.is-disabled]="child.disabled"
                    [class.is-active]="isActionActive(child, item.children)"
                    [routerLink]="actionRoute(child)"
                    [queryParams]="actionQueryParams(child)"
                    routerLinkActive="is-active"
                    [routerLinkActiveOptions]="{ exact: isExactActiveMatch(child, item.children) }"
                    [attr.aria-disabled]="child.disabled ? 'true' : null"
                    [attr.tabindex]="child.disabled ? -1 : null"
                    (click)="onItemClick($event, child)"
                  >
                    @if (child.icon) {
                      <span class="material-symbols-outlined" aria-hidden="true">{{ child.icon }}</span>
                    }
                    <span class="sidebar__label">{{ actionText(child) | translate }}</span>
                    @if ($safeNavigationMigration(child.badge?.value) !== undefined && $safeNavigationMigration(child.badge?.value) !== null) {
                      <span class="sidebar__badge" [attr.data-severity]="child.badge?.severity ?? 'info'">
                        {{ child.badge?.value }}
                      </span>
                    }
                  </a>
                }
              }
            </div>
          }
        } @else if (isRouteAction(item)) {
          <a [routerLink]="actionRoute(item)" [queryParams]="actionQueryParams(item)"
             [class.is-disabled]="item.disabled"
             [class.is-active]="isActionActive(item)"
             routerLinkActive="is-active"
             [routerLinkActiveOptions]="{ exact: isExactActiveMatch(item) }"
             [attr.aria-disabled]="item.disabled ? 'true' : null"
             [attr.tabindex]="item.disabled ? -1 : null"
             (click)="onItemClick($event, item)">
            @if (item.icon) {
              <span class="material-symbols-outlined" aria-hidden="true">{{ item.icon }}</span>
            }
            <span class="sidebar__label">{{ actionText(item) | translate }}</span>
            @if ($safeNavigationMigration(item.badge?.value) !== undefined && $safeNavigationMigration(item.badge?.value) !== null) {
              <span class="sidebar__badge" [attr.data-severity]="item.badge?.severity ?? 'info'">
                {{ item.badge?.value }}
              </span>
            }
          </a>
        }
      }
    </ng-template>
  `,
  styles: [`
    :host { --comp-sidebar-bg: var(--tch-color-surface-container-low); --comp-sidebar-fg: var(--tch-color-on-surface); display: block; height: 100%; }
    .sidebar { display: flex; flex-direction: column; gap: 1rem; min-height: 100%; padding: 1rem; background: var(--comp-sidebar-bg); color: var(--comp-sidebar-fg); }
    .sidebar__section { display: grid; gap: .375rem; }
    h2 { margin: .5rem .75rem 0; color: var(--tch-color-on-surface-variant); font-size: .75rem; text-transform: uppercase; }
    a, .sidebar__group-toggle { display: flex; align-items: center; gap: .625rem; min-height: 2.75rem; padding-inline: .75rem; border-radius: var(--tch-radius-md); color: inherit; text-decoration: none; min-width: 0; }
    .sidebar__group-toggle { width: 100%; border: 0; background: transparent; font: inherit; cursor: pointer; text-align: start; }
    a:hover, .sidebar__group-toggle:hover { background: var(--tch-color-surface-container-high); color: var(--tch-color-primary); }
    a.is-active, a.is-active:hover { background: var(--tch-color-accent); color: var(--tch-color-on-accent); font-weight: 600; }
    .sidebar__group-toggle.is-active-group { background: color-mix(in oklab, var(--tch-color-primary) 10%, transparent); color: var(--tch-color-primary); font-weight: 700; }
    .material-symbols-outlined { font-size: 1.375rem; flex: none; }
    .sidebar__group-label, .sidebar__label { flex: 1; min-width: 0; overflow-wrap: anywhere; }
    .sidebar__chevron { font-size: 1.25rem; transition: transform .15s ease; }
    .sidebar__group-toggle.is-open .sidebar__chevron { transform: rotate(180deg); }
    .sidebar__children { display: grid; gap: .125rem; margin-inline-start: .75rem; padding-inline-start: .75rem; border-inline-start: 1px solid var(--tch-color-outline-variant); }
    .sidebar__child { min-height: 2.25rem; font-size: .8125rem; }
    .sidebar__child .material-symbols-outlined { font-size: 1.125rem; }
    .sidebar__badge { flex: none; min-width: 1.25rem; border-radius: 9999px; padding: .125rem .375rem; background: var(--tch-color-secondary-container); color: var(--tch-color-on-secondary-container); font-size: .6875rem; font-weight: 700; line-height: 1rem; text-align: center; }
    .sidebar__badge[data-severity="success"] { background: var(--tch-color-tertiary-container); color: var(--tch-color-on-tertiary-container); }
    .sidebar__badge[data-severity="warning"] { background: var(--tch-color-warning-container, #ffddb7); color: var(--tch-color-on-warning-container, #2b1700); }
    .sidebar__badge[data-severity="danger"] { background: var(--tch-color-error-container); color: var(--tch-color-on-error-container); }
    a.is-disabled { opacity: .48; cursor: default; pointer-events: auto; }
    .sidebar__secondary { display: grid; gap: .25rem; margin-top: auto; }
  `],
})
export class TchSidebarNav {
  private readonly router = inject(Router);

  readonly primary = input<readonly ActionItem[]>([]);
  readonly sections = input<readonly NavigationSection[]>([]);
  readonly secondary = input<readonly ActionItem[]>([]);
  readonly ariaLabel = input('Navigation principale');
  readonly actionRoute = actionRoute;
  readonly actionQueryParams = actionQueryParams;
  readonly actionText = actionText;
  readonly isRouteAction = isRouteAction;

  /** Current URL, tracked reactively so an active group opens automatically. */
  private readonly currentUrl = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map(() => this.router.url),
    ),
    { initialValue: this.router.url },
  );

  /**
   * Manual accordion state from clicking a parent:
   *  - `undefined` → follow the active route (the active group is open);
   *  - `null`      → user collapsed everything;
   *  - `<id>`      → user opened exactly that group (accordion: only one open).
   * Reset to `undefined` whenever the active route group changes, so navigation re-opens
   * the relevant group.
   */
  private readonly openOverride = signal<string | null | undefined>(undefined);

  constructor() {
    effect(() => {
      this.activeGroupId(); // re-run when the route's active group changes
      untracked(() => this.openOverride.set(undefined));
    });
  }

  private readonly activeGroupId = computed(() => {
    const groups = [
      ...this.primary(),
      ...this.sections().flatMap(section => section.items),
      ...this.secondary(),
    ].filter(item => item.children?.length);
    let best: { id: string; length: number } | null = null;
    for (const group of groups) {
      for (const child of group.children ?? []) {
        const route = actionRoute(child);
        if (!route || !this.isRouteActive(child, route, group.children ?? [])) continue;
        if (!best || route.length > best.length) {
          best = { id: group.id, length: route.length };
        }
      }
    }
    return best?.id ?? null;
  });

  /** Follows the active route by default; once the user toggles, their choice wins (accordion). */
  isOpen(item: ActionItem): boolean {
    const override = this.openOverride();
    if (override === undefined) return this.isActiveGroup(item);
    return override === item.id;
  }

  isActiveGroup(item: ActionItem): boolean {
    return this.activeGroupId() === item.id;
  }

  isActionActive(item: ActionItem, siblings: readonly ActionItem[] = []): boolean {
    const route = actionRoute(item);
    return !!route && this.isRouteActive(item, route, siblings);
  }

  isExactActiveMatch(item: ActionItem, siblings: readonly ActionItem[] = []): boolean {
    const route = actionRoute(item);
    return item.activeMatch === 'exact' || (!!route && this.hasLongerSiblingRoute(route, siblings));
  }

  toggle(item: ActionItem): void {
    // Collapsing the currently-open group (including the active one) closes everything;
    // otherwise open this group exclusively.
    this.openOverride.set(this.isOpen(item) ? null : item.id);
  }

  onItemClick(event: Event, item: ActionItem): void {
    if (!item.disabled) return;
    event.preventDefault();
    event.stopPropagation();
  }

  private isRouteActive(item: ActionItem, route: string, siblings: readonly ActionItem[] = []): boolean {
    const url = this.activeComparableUrl();
    return this.isExactActiveMatch(item, siblings) ? url === route : url.startsWith(route);
  }

  private hasLongerSiblingRoute(route: string, siblings: readonly ActionItem[]): boolean {
    return siblings.some(sibling => {
      const siblingRoute = actionRoute(sibling).replace(/\/$/, '');
      return siblingRoute.length > route.length && siblingRoute.startsWith(`${route}/`);
    });
  }

  private activeComparableUrl(): string {
    return this.currentUrl().split('?')[0].split('#')[0].replace(/\/$/, '') || '/';
  }
}
