import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { PlatformOperationalIntegrityView } from '../platform-dashboard.model';

@Component({
  selector: 'tch-platform-operational-integrity-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (integrity()) {
      <div class="integrity" [attr.data-status]="integrity()!.status">
        <div class="integrity__score-ring">
          <span class="integrity__score-icon material-symbols-outlined" aria-hidden="true">
            verified_user
          </span>
        </div>
        @if (integrity()!.scorePercent !== null) {
          <p class="integrity__score">{{ integrity()!.scorePercent }}%</p>
        }
        @if (integrity()!.lastAuditLabel) {
          <p class="integrity__audit">{{ integrity()!.lastAuditLabel }}</p>
        }
        @if (integrity()!.message) {
          <p class="integrity__message">{{ integrity()!.message }}</p>
        }
      </div>
    } @else {
      <p class="integrity--missing">Integrity data unavailable.</p>
    }
  `,
  styles: [
    `
      .integrity {
        display: flex;
        flex-direction: column;
        align-items: center;
        text-align: center;
        gap: 0.5rem;
        padding: 1rem 0;
      }

      .integrity__score-ring {
        width: 5rem;
        height: 5rem;
        border-radius: 50%;
        border: 0.375rem solid var(--tch-color-secondary-container, #fecb01);
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 0.5rem;
      }

      .integrity[data-status='WARNING'] .integrity__score-ring {
        border-color: var(--tch-status-warning, #f59e0b);
      }
      .integrity[data-status='DEGRADED'] .integrity__score-ring,
      .integrity[data-status='MISSING'] .integrity__score-ring {
        border-color: var(--tch-status-blocked, #dc2626);
      }

      .integrity__score-icon {
        font-size: 2rem;
        color: var(--tch-color-secondary, #745b00);
        font-family: 'Material Symbols Outlined';
        font-variation-settings:
          'FILL' 0,
          'wght' 400,
          'GRAD' 0,
          'opsz' 24;
      }

      .integrity__score {
        margin: 0;
        font-size: 1.5rem;
        font-weight: 700;
        font-family: 'JetBrains Mono', monospace;
        color: var(--tch-color-on-surface, #1a1c1e);
      }

      .integrity__audit {
        margin: 0;
        font-size: 0.8125rem;
        color: var(--tch-color-on-surface-variant, #46464f);
      }

      .integrity__message {
        margin: 0;
        font-size: 0.875rem;
        color: var(--tch-color-on-surface-variant, #46464f);
        max-width: 20rem;
      }

      .integrity--missing {
        margin: 0;
        font-size: 0.875rem;
        color: var(--tch-color-on-surface-variant, #46464f);
        text-align: center;
        padding: 1.5rem 0;
      }
    `,
  ],
})
export class PlatformOperationalIntegrityCardComponent {
  readonly integrity = input<PlatformOperationalIntegrityView | null>(null);
}
