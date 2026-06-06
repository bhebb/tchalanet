import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { LabelPipe, PublicShellRuntime } from '@tch/page-model';
import { actionRoute, actionText } from '@tch/ui/components';

@Component({
  selector: 'tch-public-bottom-nav',
  imports: [RouterLink, LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <nav class="shell__bottom-nav" aria-label="Public">
      @for (item of bottomNav(); track item.id) {
        <a [routerLink]="actionRoute(item)">{{ actionText(item) | tchLabel }}</a>
      }
    </nav>
  `,
  styles: [
    `
      .shell__bottom-nav {
        display: none;
      }

      @media (max-width: 720px) {
        .shell__bottom-nav {
          position: fixed;
          z-index: 20;
          left: 0;
          right: 0;
          bottom: 0;
          display: grid;
          grid-template-columns: repeat(3, 1fr);
          gap: 0.25rem;
          padding: 0.5rem var(--tch-page-margin-mobile, 16px)
            calc(0.5rem + env(safe-area-inset-bottom, 0px));
          background: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
          border-top: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
          box-shadow: var(--mat-sys-level2, 0 -4px 16px rgba(0, 0, 0, 0.12));
        }

        .shell__bottom-nav a {
          min-height: var(--tch-touch-target, 48px);
          display: grid;
          place-items: center;
          border-radius: var(--tch-radius-control, 8px);
          color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
          text-decoration: none;
          font-weight: 700;
        }

        .shell__bottom-nav a:nth-child(2) {
          background: var(--tch-color-secondary-container, var(--mat-sys-secondary-container));
          color: var(--tch-color-on-secondary-container, var(--mat-sys-on-secondary-container));
        }
      }
    `,
  ],
})
export class PublicBottomNav {
  readonly shell = input<PublicShellRuntime | undefined>();
  readonly bottomNav = computed(() => this.shell()?.header.primary?.slice(0, 3) ?? []);
  readonly actionText = actionText;
  readonly actionRoute = actionRoute;
}
