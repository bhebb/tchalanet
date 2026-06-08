import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { LabelPipe, PublicShellRuntime } from '@tch/page-model';
import { actionRoute, actionText } from '@tch/api';

@Component({
  selector: 'tch-public-bottom-nav',
  imports: [RouterLink, LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <nav class="bottom-nav" aria-label="Navigation principale">
      @for (item of bottomNav(); track item.id) {
        <a [routerLink]="actionRoute(item)">{{ actionText(item) | tchLabel }}</a>
      }
    </nav>
  `,
  styles: [`
    /* Mobile-first: visible on compact, fixed at bottom */
    .bottom-nav {
      position: fixed;
      z-index: var(--tch-z-nav, 20);
      left: 0;
      right: 0;
      bottom: 0;
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 0.25rem;
      padding: 0.5rem var(--tch-page-margin-mobile, 16px)
        calc(0.5rem + env(safe-area-inset-bottom, 0px));
      background: var(--tch-color-surface-container-lowest);
      border-top: 1px solid var(--tch-color-outline-variant);
      box-shadow: 0 -2px 8px rgb(0 0 0 / 0.08);
    }

    .bottom-nav a {
      min-height: var(--tch-touch-target, 48px);
      display: grid;
      place-items: center;
      border-radius: var(--tch-radius-md, 8px);
      color: var(--tch-color-on-surface-variant);
      text-decoration: none;
      font-weight: var(--tch-weight-bold, 700);
      font-size: 0.8125rem;
    }

    .bottom-nav a:nth-child(2) {
      background: var(--tch-color-secondary-container);
      color: var(--tch-color-on-secondary-container);
    }

    /* Expanded: hide bottom nav ≥ 840px */
    @media (min-width: 840px) {
      .bottom-nav {
        display: none;
      }
    }
  `],
})
export class PublicBottomNav {
  readonly shell = input<PublicShellRuntime | undefined>();
  readonly bottomNav = computed(() => this.shell()?.header.primary?.slice(0, 3) ?? []);
  readonly actionText = actionText;
  readonly actionRoute = actionRoute;
}
