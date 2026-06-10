import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { TchSidebarNav } from '@tch/ui/components';

import { ShellFeedbackOutletComponent } from '../../../shared/feedback/shell-feedback-outlet.component';
import { PrivateShellService } from './private-shell.service';
import { PrivateTopbarComponent } from './private-topbar.component';

@Component({
  selector: 'tch-private-shell',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, TranslatePipe, ShellFeedbackOutletComponent, TchSidebarNav, PrivateTopbarComponent],
  template: `
    <div class="private-layout">
      <tch-sidebar-nav
        class="private-layout__sidenav"
        [sections]="shellSvc.navigation()"
        [ariaLabel]="'nav.private.ariaLabel' | translate"
      />
      <div class="private-layout__body">
        <tch-private-topbar [space]="shellSvc.space()" />
        <section class="private-layout__content">
          <tch-shell-feedback-outlet verbosity="standard" />
          <router-outlet />
        </section>
      </div>
    </div>
  `,
  styles: [
    `
      :host {
        display: block;
        min-height: 100vh;
        background: var(--tch-color-surface, #f9f9fc);
        color: var(--tch-color-on-surface, #1a1c1e);
      }

      .private-layout {
        display: flex;
        min-height: 100vh;
      }

      .private-layout__sidenav {
        position: fixed;
        top: 0;
        left: 0;
        height: 100vh;
        overflow-y: auto;
        z-index: 50;
        flex-shrink: 0;
        width: var(--tch-private-sidenav-width, 256px);
      }

      .private-layout__body {
        flex: 1;
        min-width: 0;
        margin-left: var(--tch-private-sidenav-width, 256px);
        display: flex;
        flex-direction: column;
        min-height: 100vh;
      }

      .private-layout__content {
        flex: 1;
        padding: var(--tch-space-page-desktop, 2rem);
      }

      @media (max-width: 767px) {
        .private-layout__sidenav {
          display: none;
        }

        .private-layout__body {
          margin-left: 0;
        }

        .private-layout__content {
          padding: var(--tch-space-page-mobile, 1rem);
        }
      }
    `,
  ],
})
export class PrivateShellComponent {
  protected readonly shellSvc = inject(PrivateShellService);
}
