import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
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
                    [routerLink]="actionRoute(child)"
                    [queryParams]="actionQueryParams(child)"
                    routerLinkActive="is-active"
                    [routerLinkActiveOptions]="{ exact: child.activeMatch === 'exact' }"
                  >
                    @if (child.icon) {
                      <span class="material-symbols-outlined" aria-hidden="true">{{ child.icon }}</span>
                    }
                    {{ actionText(child) | translate }}
                  </a>
                }
              }
            </div>
          }
        } @else if (isRouteAction(item)) {
          <a [routerLink]="actionRoute(item)" [queryParams]="actionQueryParams(item)"
             routerLinkActive="is-active"
             [routerLinkActiveOptions]="{ exact: item.activeMatch === 'exact' }">
            @if (item.icon) {
              <span class="material-symbols-outlined" aria-hidden="true">{{ item.icon }}</span>
            }
            {{ actionText(item) | translate }}
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
    a, .sidebar__group-toggle { display: flex; align-items: center; gap: .625rem; min-height: 2.75rem; padding-inline: .75rem; border-radius: var(--tch-radius-md); color: inherit; text-decoration: none; }
    .sidebar__group-toggle { width: 100%; border: 0; background: transparent; font: inherit; cursor: pointer; text-align: start; }
    a:hover, a.is-active, .sidebar__group-toggle:hover { background: var(--tch-color-surface-container-high); color: var(--tch-color-primary); }
    .material-symbols-outlined { font-size: 1.375rem; flex: none; }
    .sidebar__group-label { flex: 1; }
    .sidebar__chevron { font-size: 1.25rem; transition: transform .15s ease; }
    .sidebar__group-toggle.is-open .sidebar__chevron { transform: rotate(180deg); }
    .sidebar__children { display: grid; gap: .125rem; margin-inline-start: .75rem; padding-inline-start: .75rem; border-inline-start: 1px solid var(--tch-color-outline-variant); }
    .sidebar__child { min-height: 2.25rem; font-size: .8125rem; }
    .sidebar__child .material-symbols-outlined { font-size: 1.125rem; }
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

  /** Explicit open/close overrides from clicking a parent. Absent → follows the active route. */
  private readonly overrides = signal<ReadonlyMap<string, boolean>>(new Map());

  /** A group is open when the user opened it, else when one of its children is the active route. */
  isOpen(item: ActionItem): boolean {
    const override = this.overrides().get(item.id);
    return override ?? this.hasActiveChild(item);
  }

  toggle(item: ActionItem): void {
    const next = new Map(this.overrides());
    next.set(item.id, !this.isOpen(item));
    this.overrides.set(next);
  }

  private hasActiveChild(item: ActionItem): boolean {
    const url = this.currentUrl();
    return (item.children ?? []).some(child => {
      const route = actionRoute(child);
      if (!route) return false;
      return child.activeMatch === 'exact' ? url === route : url.startsWith(route);
    });
  }
}
