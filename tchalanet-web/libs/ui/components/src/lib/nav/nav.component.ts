import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { ActionItem, actionRoute, actionText, isRouteAction } from '../navigation/action-item';

@Component({
  selector: 'tch-nav',
  imports: [RouterLink, RouterLinkActive, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <nav class="nav" [attr.aria-label]="ariaLabel()">
      @for (item of items(); track item.id) {
        @if (isRouteAction(item)) {
          <a class="nav__link" [routerLink]="actionRoute(item)" routerLinkActive="is-active"
             [routerLinkActiveOptions]="{ exact: item.activeMatch === 'exact' }"
             [attr.aria-disabled]="item.disabled || null">{{ actionText(item) | translate }}</a>
        }
      }
    </nav>
  `,
  styles: [`
    :host { --comp-nav-fg: var(--tch-color-on-surface-variant); --comp-nav-active: var(--tch-color-primary); }
    .nav { display: flex; align-items: center; gap: .25rem; }
    .nav__link { color: var(--comp-nav-fg); padding: .625rem; text-decoration: none; border-radius: var(--tch-radius-pill); font-weight: 700; }
    .nav__link:hover, .nav__link.is-active { color: var(--comp-nav-active); background: var(--tch-color-surface-container); }
  `],
})
export class TchNav {
  readonly items = input<readonly ActionItem[]>([]);
  readonly ariaLabel = input('Navigation');
  readonly actionRoute = actionRoute;
  readonly actionText = actionText;
  readonly isRouteAction = isRouteAction;
}
