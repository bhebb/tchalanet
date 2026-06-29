import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TchBackendClient } from '@tch/api';
import { SupportAccessStore } from '@tch/core/auth';

@Component({
  selector: 'tch-admin-override-banner',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatIconModule],
  template: `
    @if (store.isActive()) {
      <div class="override-banner" [class.readonly]="store.mode() === 'SUPPORT_READONLY'">
        <span class="material-symbols-outlined banner-icon" aria-hidden="true">
          {{ store.mode() === 'SUPPORT_READONLY' ? 'visibility' : 'admin_panel_settings' }}
        </span>
        <span class="banner-text">
          @if (store.mode() === 'SUPPORT_OVERRIDE') {
            Mode support superadmin — vous agissez sur le tenant
            <strong>{{ store.tenantName() }}</strong>. Les informations sensibles sont masquées par
            défaut.
          } @else {
            Mode diagnostic superadmin — vous consultez le tenant
            <strong>{{ store.tenantName() }}</strong> en lecture seule.
          }
        </span>
        <button
          mat-stroked-button
          class="quit-btn"
          [disabled]="quitting()"
          (click)="quitSession()"
        >
          @if (quitting()) {
            <span class="material-symbols-outlined spin" aria-hidden="true">progress_activity</span>
          }
          Quitter le mode support
        </button>
      </div>
    }
  `,
  styles: [
    `
      .override-banner {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        padding: 0.625rem 1.25rem;
        background: var(--tch-color-warning-container, #fff3cd);
        border-bottom: 2px solid var(--tch-color-warning, #f59e0b);
        color: var(--tch-color-on-warning-container, #92400e);
      }

      .override-banner.readonly {
        background: var(--tch-color-secondary-container, #e8def8);
        border-color: var(--tch-color-secondary, #6750a4);
        color: var(--tch-color-on-secondary-container, #21005d);
      }

      .banner-icon {
        font-size: 1.25rem;
        flex-shrink: 0;
      }

      .banner-text {
        flex: 1;
        font-size: 0.875rem;
        line-height: 1.4;
      }

      .quit-btn {
        flex-shrink: 0;
        font-size: 0.8125rem;
      }

      .spin {
        display: inline-block;
        animation: spin 0.8s linear infinite;
        vertical-align: middle;
        margin-right: 0.25rem;
      }

      @keyframes spin {
        to {
          transform: rotate(360deg);
        }
      }
    `,
  ],
})
export class AdminOverrideBanner {
  protected readonly store = inject(SupportAccessStore);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly backend = inject(TchBackendClient);

  readonly quitting = signal(false);

  quitSession(): void {
    if (this.quitting()) return;
    this.quitting.set(true);
    this.backend.delete<void>('/platform/tenants/admin-access/current').subscribe({
      next: () => this.finishQuit('Session de support terminée.'),
      error: () => {
        this.finishQuit('Session de support terminée localement.');
      },
    });
  }

  private finishQuit(message: string): void {
    this.store.clearSession();
    this.quitting.set(false);
    void this.router.navigate(['/app/platform/tenants']);
    this.snackBar.open(message, 'OK', { duration: 4000 });
  }
}
