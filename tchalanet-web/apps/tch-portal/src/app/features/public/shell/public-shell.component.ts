import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { PublicShellRuntime } from '../../../shared/types';
import { LabelPipe } from '@tch/page-model';
import { PublicBottomNav, PublicFooter } from '@tch/web';
import { PublicHeader } from './public-header';

@Component({
  selector: 'tch-page-shell',
  imports: [LabelPipe, PublicHeader, PublicFooter, PublicBottomNav],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <a class="shell__skip" href="#public-content">{{ 'public.nav.skip' | tchLabel }}</a>

    <tch-public-header [shell]="shell()" />

    <main id="public-content" class="shell__body">
      <ng-content />
    </main>

    <tch-public-footer [shell]="shell()" />

    <tch-public-bottom-nav [shell]="shell()" />
  `,
  styles: [
    `
      :host {
        --comp-shell-bg: var(--tch-color-background);
        --comp-shell-fg: var(--tch-color-on-background);
        display: grid;
        min-height: 100vh;
        grid-template-rows: auto 1fr auto;
        background: var(--comp-shell-bg);
        color: var(--comp-shell-fg);
      }

      .shell__skip {
        position: fixed;
        z-index: 100;
        left: 1rem;
        top: 1rem;
        transform: translateY(-5rem);
        padding: 0.625rem 0.875rem;
        border-radius: var(--tch-radius-control, 8px);
        background: var(--tch-color-accent, var(--mat-sys-tertiary));
        color: var(--tch-on-color-accent, var(--mat-sys-on-tertiary));
        text-decoration: none;
        font-weight: 800;
      }

      .shell__skip:focus {
        transform: translateY(0);
      }

      @media (max-width: 720px) {
        .shell__body {
          padding-bottom: calc(4.5rem + env(safe-area-inset-bottom, 0px));
        }
      }
    `,
  ],
})
export class PublicShellComponent {
  readonly shell = input<PublicShellRuntime | undefined>();
}
