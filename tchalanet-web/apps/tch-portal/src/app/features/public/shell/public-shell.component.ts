import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { LabelPipe } from '@tch/page-model';
import { PublicBottomNav, PublicFooter } from '@tch/web';
import { PublicHeader } from './public-header';
import { PublicShellService } from './public-shell.service';

@Component({
  selector: 'tch-page-shell',
  imports: [LabelPipe, PublicHeader, PublicFooter, PublicBottomNav],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <a class="shell__skip" href="#public-content">{{ 'public.nav.skip' | tchLabel }}</a>

    <tch-public-header [shell]="shellSvc.shell()" />

    <main id="public-content" class="shell__body">
      <ng-content />
    </main>

    <tch-public-footer [shell]="shellSvc.shell()" />

    <tch-public-bottom-nav [shell]="shellSvc.shell()" />
  `,
  styles: [`
    :host {
      --comp-shell-bg: var(--tch-color-background);
      --comp-shell-fg: var(--tch-color-on-background);
      display: grid;
      min-height: 100dvh;
      grid-template-rows: auto 1fr auto;
      background: var(--comp-shell-bg);
      color: var(--comp-shell-fg);
    }

    .shell__skip {
      position: fixed;
      z-index: var(--tch-z-toast, 60);
      left: 1rem;
      top: 1rem;
      transform: translateY(-6rem);
      padding: 0.625rem 0.875rem;
      border-radius: var(--tch-radius-md, 8px);
      background: var(--tch-color-accent);
      color: var(--tch-on-color-accent);
      text-decoration: none;
      font-weight: var(--tch-weight-extra-bold, 800);
      transition: transform var(--tch-duration-short, 200ms) var(--tch-ease-standard-decelerate);
    }

    .shell__skip:focus-visible {
      transform: translateY(0);
      outline: max(var(--tch-focus-ring-width, 2px), 0.15em) solid currentColor;
      outline-offset: var(--tch-focus-ring-offset, 2px);
    }

    /* Bottom nav padding for mobile (safe-area + tab bar height) */
    .shell__body {
      padding-bottom: calc(4.5rem + env(safe-area-inset-bottom, 0px));
    }

    @media (min-width: 840px) {
      .shell__body {
        padding-bottom: 0;
      }
    }
  `],
})
export class PublicShellComponent {
  protected readonly shellSvc = inject(PublicShellService);
}
