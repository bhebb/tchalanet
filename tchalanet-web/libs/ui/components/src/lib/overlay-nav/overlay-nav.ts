import { DOCUMENT } from '@angular/common';
import { ChangeDetectionStrategy, Component, effect, inject, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { ActionItem, actionRoute, actionText, isRouteAction } from '@tch/api';

@Component({
  selector: 'tch-overlay-nav',
  imports: [RouterLink, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (open()) {
      <div class="overlay">
        <button class="overlay__backdrop" type="button" aria-label="Close navigation" (click)="requestClose.emit()"></button>
        <nav class="overlay__panel" [attr.aria-label]="ariaLabel()">
          @for (item of items(); track item.id) {
            @if (isRouteAction(item)) {
              <a [routerLink]="actionRoute(item)" (click)="requestClose.emit()">{{ actionText(item) | translate }}</a>
            }
          }
        </nav>
      </div>
    }
  `,
  styles: [`
    :host { --comp-overlay-bg: var(--tch-color-surface); --comp-overlay-fg: var(--tch-color-on-surface); }
    .overlay { position: fixed; inset: 0; z-index: var(--tch-z-overlay, 50); padding: 5rem var(--tch-page-gutter) 1rem; }
    .overlay__backdrop { position: absolute; inset: 0; width: 100%; border: 0; background: var(--tch-color-scrim, rgb(0 0 0 / 0.45)); cursor: pointer; }
    .overlay__panel { position: relative; display: grid; gap: .5rem; max-width: 32rem; margin-inline: auto; padding: 1rem; border-radius: var(--tch-radius-lg); background: var(--comp-overlay-bg); color: var(--comp-overlay-fg); box-shadow: var(--tch-elevation-2); }
    a { color: inherit; padding: .875rem; text-decoration: none; border-radius: var(--tch-radius-md); }
    a:hover { background: var(--tch-color-surface-container); }
  `],
})
export class TchOverlayNav {
  private readonly document = inject(DOCUMENT);
  readonly open = input(false);
  readonly items = input<readonly ActionItem[]>([]);
  readonly ariaLabel = input('Navigation');
  readonly requestClose = output<void>();
  readonly actionRoute = actionRoute;
  readonly actionText = actionText;
  readonly isRouteAction = isRouteAction;

  constructor() {
    effect(() => {
      const open = this.open();
      this.document.documentElement.classList.toggle('tch-overlay-open', open);
      this.document.body.classList.toggle('tch-overlay-open', open);
    });
  }

}
