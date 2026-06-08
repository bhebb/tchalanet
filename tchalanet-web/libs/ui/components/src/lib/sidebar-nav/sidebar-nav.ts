import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { ActionItem, NavigationSection, actionRoute, actionText, isRouteAction } from '@tch/api';

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
        @if (isRouteAction(item)) {
          <a [routerLink]="actionRoute(item)" routerLinkActive="is-active"
             [routerLinkActiveOptions]="{ exact: item.activeMatch === 'exact' }">
            @if (item.icon) { <span aria-hidden="true">{{ item.icon }}</span> }
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
    a { display: flex; align-items: center; gap: .625rem; min-height: 2.75rem; padding-inline: .75rem; border-radius: var(--tch-radius-md); color: inherit; text-decoration: none; }
    a:hover, a.is-active { background: var(--tch-color-surface-container-high); color: var(--tch-color-primary); }
    .sidebar__secondary { display: grid; gap: .25rem; margin-top: auto; }
  `],
})
export class TchSidebarNav {
  readonly primary = input<readonly ActionItem[]>([]);
  readonly sections = input<readonly NavigationSection[]>([]);
  readonly secondary = input<readonly ActionItem[]>([]);
  readonly ariaLabel = input('Navigation principale');
  readonly actionRoute = actionRoute;
  readonly actionText = actionText;
  readonly isRouteAction = isRouteAction;
}
