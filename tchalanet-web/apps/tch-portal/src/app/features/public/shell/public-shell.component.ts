import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { LabelPipe } from '@tch/page-model';
import { PublicFooter } from '@tch/web';
import { AppRuntimeStore } from '../../../core/runtime';
import { ShellFeedbackOutletComponent } from '../../../core/feedback/shell-feedback-outlet.component';
import { PublicHeader } from './public-header';
import { PublicShellService } from './public-shell.service';

@Component({
  selector: 'tch-public-shell',
  imports: [LabelPipe, RouterOutlet, PublicHeader, PublicFooter, ShellFeedbackOutletComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <a class="public-shell__skip" href="#public-content">{{ 'public.nav.skip' | tchLabel }}</a>

    <tch-public-header [shell]="shellSvc.shell()" />

    <main id="public-content" class="public-shell__main">
      <tch-shell-feedback-outlet verbosity="minimal" />
      <router-outlet />
    </main>

    <tch-public-footer [shell]="shellSvc.shell()" />
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

    .public-shell__skip {
      position: fixed;
      z-index: var(--tch-z-toast, 60);
      left: 1rem;
      top: 1rem;
      transform: translateY(-6rem);
      padding: 0.625rem 0.875rem;
      border-radius: var(--tch-radius-md, 8px);
      background: var(--tch-color-accent);
      color: var(--tch-color-on-accent);
      text-decoration: none;
      font-weight: var(--tch-weight-extra-bold, 800);
      transition: transform var(--tch-duration-short, 200ms) var(--tch-ease-standard-decelerate);
    }

    .public-shell__skip:focus-visible {
      transform: translateY(0);
      outline: max(var(--tch-focus-ring-width, 2px), 0.15em) solid currentColor;
      outline-offset: var(--tch-focus-ring-offset, 2px);
    }

    .public-shell__main {
      padding-bottom: 0;
    }
  `],
})
export class TchPublicShellComponent {
  private readonly runtime = inject(AppRuntimeStore);
  protected readonly shellSvc = inject(PublicShellService);

  constructor() {
    this.runtime.initPublicRuntime();
  }
}
