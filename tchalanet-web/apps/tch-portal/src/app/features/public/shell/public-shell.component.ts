import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { PageShell } from '../../../shared/types';
import { LabelPipe } from '../../pagemodel/label.pipe';
import { PublicBottomNav } from './public-bottom-nav';
import { PublicFooter } from './public-footer';
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
        display: grid;
        min-height: 100vh;
        grid-template-rows: auto 1fr auto;
        background: var(--tch-color-background, var(--mat-sys-background));
        color: var(--tch-color-foreground, var(--mat-sys-on-background));
      }

      .shell__skip {
        position: fixed;
        z-index: 100;
        left: 1rem;
        top: 1rem;
        transform: translateY(-5rem);
        padding: 0.625rem 0.875rem;
        border-radius: var(--tch-radius-control, 8px);
        background: var(--tch-color-secondary-container, var(--mat-sys-secondary-container));
        color: var(--tch-color-on-secondary-container, var(--mat-sys-on-secondary-container));
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
  readonly shell = input<PageShell | undefined>();
}
