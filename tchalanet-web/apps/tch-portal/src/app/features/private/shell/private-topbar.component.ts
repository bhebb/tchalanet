import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { AuthSessionService } from '../../../core/auth/auth-session.service';
import { PrivateSpace } from './private-navigation.model';

const SPACE_TITLE_KEYS: Record<PrivateSpace, string> = {
  platform: 'nav.platform.spaceTitle',
  admin: 'nav.admin.spaceTitle',
  cashier: 'nav.cashier.spaceTitle',
};

@Component({
  selector: 'tch-private-topbar',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe],
  template: `
    <header class="topbar">
      <div class="topbar__brand">
        <span class="topbar__brand-logo material-symbols-outlined" aria-hidden="true">casino</span>
        <span class="topbar__brand-name">Tchalanet</span>
      </div>

      <div class="topbar__space-title">
        {{ spaceTitleKey() | translate }}
      </div>

      <div class="topbar__utils">
        <div class="topbar__status">
          <span class="topbar__status-dot"></span>
        </div>

        <button class="topbar__icon-btn" type="button" aria-label="Notifications">
          <span class="material-symbols-outlined">notifications</span>
        </button>

        <div class="topbar__user">
          <span class="topbar__user-name">{{ userDisplayName() }}</span>
          <button class="topbar__logout" type="button" (click)="logout()">
            <span class="material-symbols-outlined">logout</span>
          </button>
        </div>
      </div>
    </header>
  `,
  styles: [
    `
      :host {
        display: block;
        position: sticky;
        top: 0;
        z-index: 40;
      }

      .topbar {
        height: var(--tch-private-topbar-height, 64px);
        background: color-mix(
          in srgb,
          var(--tch-color-surface-container-lowest, #ffffff) 85%,
          transparent
        );
        backdrop-filter: blur(12px);
        border-bottom: 1px solid var(--tch-color-outline-variant, #c8c5d0);
        display: flex;
        align-items: center;
        gap: 1rem;
        padding: 0 1.5rem;
        box-sizing: border-box;
      }

      .topbar__brand {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        color: var(--tch-color-on-surface, #1a1c1e);
        font-weight: 800;
        font-size: 1rem;
        text-decoration: none;
        flex-shrink: 0;
      }

      .topbar__brand-logo {
        font-size: 1.25rem;
        color: var(--tch-color-primary, #020135);
      }

      .topbar__space-title {
        flex: 1;
        font-size: 1rem;
        font-weight: 700;
        color: var(--tch-color-on-surface, #1a1c1e);
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .topbar__utils {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        flex-shrink: 0;
      }

      .topbar__status {
        display: flex;
        align-items: center;
        gap: 0.375rem;
        padding: 0.25rem 0.625rem;
        border-radius: 9999px;
        background: color-mix(
          in srgb,
          var(--tch-status-ready, #10b981) 12%,
          transparent
        );
        font-size: 0.6875rem;
        font-weight: 700;
        color: var(--tch-status-ready, #10b981);
      }

      .topbar__status-dot {
        width: 0.5rem;
        height: 0.5rem;
        border-radius: 50%;
        background: var(--tch-status-ready, #10b981);
        animation: pulse 2s ease-in-out infinite;
      }

      @keyframes pulse {
        0%, 100% { opacity: 1; }
        50% { opacity: 0.4; }
      }

      .topbar__icon-btn {
        width: 2.5rem;
        height: 2.5rem;
        border-radius: 50%;
        border: none;
        background: transparent;
        color: var(--tch-color-on-surface-variant, #46464f);
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: background 150ms ease;
      }

      .topbar__icon-btn:hover {
        background: var(--tch-color-surface-container-high, #e8e8eb);
      }

      .topbar__user {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding-left: 0.75rem;
        border-left: 1px solid var(--tch-color-outline-variant, #c8c5d0);
      }

      .topbar__user-name {
        font-size: 0.8125rem;
        font-weight: 600;
        color: var(--tch-color-on-surface, #1a1c1e);
        max-width: 12rem;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .topbar__logout {
        width: 2rem;
        height: 2rem;
        border-radius: 50%;
        border: none;
        background: transparent;
        color: var(--tch-color-on-surface-variant, #46464f);
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: background 150ms ease;
      }

      .topbar__logout:hover {
        background: var(--tch-color-surface-container-high, #e8e8eb);
      }

      .material-symbols-outlined {
        font-family: 'Material Symbols Outlined';
        font-size: 1.25rem;
        font-variation-settings:
          'FILL' 0,
          'wght' 400,
          'GRAD' 0,
          'opsz' 24;
      }
    `,
  ],
})
export class PrivateTopbarComponent {
  readonly space = input.required<PrivateSpace>();

  private readonly auth = inject(AuthSessionService);

  readonly spaceTitleKey = computed(() => SPACE_TITLE_KEYS[this.space()]);

  readonly userDisplayName = computed(() => {
    const s = this.auth.session();
    return s.displayName ?? s.username ?? '';
  });

  logout(): void {
    void this.auth.logout();
  }
}
