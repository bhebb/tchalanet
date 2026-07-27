import { ConfigurableFocusTrapFactory, FocusTrap } from '@angular/cdk/a11y';
import { DOCUMENT } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  effect,
  inject,
  input,
  output,
  viewChild,
} from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import {
  ActionItem,
  actionHref,
  actionRoute,
  actionText,
  isExternalAction,
  isRouteAction,
} from '@tch/api';

/**
 * Navigation repliée du shell public.
 *
 * Replié = dialogue modal : focus piégé, `Escape` ferme, focus rendu au déclencheur,
 * scroll du document verrouillé. Voir `docs/conventions/style.md` §10.1.
 */
@Component({
  selector: 'tch-overlay-nav',
  imports: [RouterLink, RouterLinkActive, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '(document:keydown.escape)': 'onEscape()' },
  template: `
    @if (open()) {
      <div class="overlay">
        <button
          class="overlay__backdrop"
          type="button"
          [attr.aria-label]="closeLabel()"
          (click)="requestClose.emit()"
        ></button>
        <div
          #panel
          class="overlay__panel"
          role="dialog"
          aria-modal="true"
          [attr.aria-label]="ariaLabel()"
        >
          <nav>
            @for (item of items(); track item.id) {
              @if (isRouteAction(item)) {
                <a
                  [routerLink]="actionRoute(item)"
                  routerLinkActive="is-active"
                  ariaCurrentWhenActive="page"
                  [routerLinkActiveOptions]="{ exact: item.activeMatch === 'exact' }"
                  [attr.aria-disabled]="item.disabled || null"
                  (click)="requestClose.emit()"
                >
                  @if (item.icon) {
                    <span class="material-symbols-outlined" aria-hidden="true">{{ item.icon }}</span>
                  }
                  <span class="overlay__label">{{ actionText(item) | translate }}</span>
                </a>
              } @else if (isExternalAction(item)) {
                <a
                  [href]="actionHref(item)"
                  [attr.aria-disabled]="item.disabled || null"
                  (click)="requestClose.emit()"
                >
                  @if (item.icon) {
                    <span class="material-symbols-outlined" aria-hidden="true">{{ item.icon }}</span>
                  }
                  <span class="overlay__label">{{ actionText(item) | translate }}</span>
                </a>
              }
            }
          </nav>
        </div>
      </div>
    }
  `,
  styles: [`
    :host { --comp-overlay-bg: var(--tch-color-surface); --comp-overlay-fg: var(--tch-color-on-surface); }
    .overlay { position: fixed; inset: 0; z-index: var(--tch-z-overlay, 50); padding: 5rem var(--tch-page-gutter) 1rem; }
    .overlay__backdrop { position: absolute; inset: 0; width: 100%; border: 0; background: var(--tch-color-scrim, rgb(0 0 0 / 0.45)); cursor: pointer; }
    .overlay__panel { position: relative; max-width: 32rem; margin-inline: auto; padding: 1rem; border-radius: var(--tch-radius-lg); background: var(--comp-overlay-bg); color: var(--comp-overlay-fg); box-shadow: var(--tch-elevation-2); }
    nav { display: grid; gap: .5rem; }
    a {
      display: flex;
      align-items: center;
      gap: .75rem;
      min-height: var(--tch-touch-target, 48px);
      padding-inline: .875rem;
      color: inherit;
      text-decoration: none;
      border-radius: var(--tch-radius-md);
    }
    a:hover { background: var(--tch-color-surface-container); }
    a.is-active { background: var(--tch-color-secondary-container); color: var(--tch-color-on-secondary-container); font-weight: 700; }
    .material-symbols-outlined { font-size: 1.375rem; flex: none; }
    .overlay__label { flex: 1; min-width: 0; }
  `],
})
export class TchOverlayNav {
  private readonly document = inject(DOCUMENT);
  private readonly focusTrapFactory = inject(ConfigurableFocusTrapFactory);

  readonly open = input(false);
  readonly items = input<readonly ActionItem[]>([]);
  readonly ariaLabel = input('Navigation');
  readonly closeLabel = input('Close navigation');
  readonly requestClose = output<void>();
  readonly actionRoute = actionRoute;
  readonly actionHref = actionHref;
  readonly actionText = actionText;
  readonly isExternalAction = isExternalAction;
  readonly isRouteAction = isRouteAction;

  private readonly panel = viewChild<ElementRef<HTMLElement>>('panel');
  private focusTrap: FocusTrap | null = null;
  private trigger: HTMLElement | null = null;

  constructor() {
    effect(() => {
      const open = this.open();
      // Le panneau n'existe qu'une fois le template rendu : la query signal fait re-jouer l'effet.
      const panel = this.panel()?.nativeElement;

      this.document.documentElement.classList.toggle('tch-overlay-open', open);
      this.document.body.classList.toggle('tch-overlay-open', open);

      if (open && panel && !this.focusTrap) {
        this.trigger = this.document.activeElement as HTMLElement | null;
        this.focusTrap = this.focusTrapFactory.create(panel);
        void this.focusTrap.focusInitialElementWhenReady();
      } else if (!open && this.focusTrap) {
        this.focusTrap.destroy();
        this.focusTrap = null;
        this.trigger?.focus();
        this.trigger = null;
      }
    });

    inject(DestroyRef).onDestroy(() => {
      // Pas de restitution du focus ici : le composant peut disparaître pendant une navigation.
      this.focusTrap?.destroy();
      this.focusTrap = null;
      this.document.documentElement.classList.remove('tch-overlay-open');
      this.document.body.classList.remove('tch-overlay-open');
    });
  }

  onEscape(): void {
    if (this.open()) {
      this.requestClose.emit();
    }
  }
}
