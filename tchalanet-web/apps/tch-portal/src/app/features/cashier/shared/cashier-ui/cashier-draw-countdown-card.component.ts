import {
  ChangeDetectionStrategy,
  Component,
  OnDestroy,
  OnInit,
  input,
  signal,
} from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { AdminSectionCardComponent } from '../../../private/shared/admin-ui/admin-section-card.component';
import { CashierCurrentDrawView } from '../../pages/dashboard/cashier-dashboard.model';

@Component({
  selector: 'tch-cashier-draw-countdown-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe, AdminSectionCardComponent],
  template: `
    <tch-admin-section-card
      [title]="'cashier.draw.title' | translate"
      icon="casino"
    >
      <div class="draw-body">
        <p class="draw-label">{{ draw().label }}</p>
        <p class="draw-time">{{ draw().drawAt }}</p>
        @if (remaining() !== null) {
          <div class="countdown" [class.countdown--urgent]="remaining()! < 300">
            <span class="material-symbols-outlined countdown-icon" aria-hidden="true">hourglass_bottom</span>
            <span class="countdown-value">{{ formatRemaining(remaining()!) }}</span>
          </div>
        }
      </div>
    </tch-admin-section-card>
  `,
  styles: [
    `
      .draw-body {
        display: flex;
        flex-direction: column;
        gap: 0.5rem;
      }

      .draw-label {
        margin: 0;
        font-size: 0.875rem;
        font-weight: 600;
        color: var(--tch-color-on-surface, #1a1c1e);
      }

      .draw-time {
        margin: 0;
        font-family: 'JetBrains Mono', monospace;
        font-size: 1.5rem;
        font-weight: 700;
        color: var(--tch-color-primary, #006590);
      }

      .countdown {
        display: inline-flex;
        align-items: center;
        gap: 0.375rem;
        font-size: 0.8125rem;
        color: var(--tch-color-on-surface-variant, #46464f);
      }

      .countdown--urgent {
        color: var(--tch-color-error, #ba1a1a);
        font-weight: 600;
      }

      .countdown-icon {
        font-size: 1rem;
        font-variation-settings: 'FILL' 1;
      }

      .countdown-value {
        font-family: 'JetBrains Mono', monospace;
      }
    `,
  ],
})
export class CashierCurrentDrawCountdownComponent implements OnInit, OnDestroy {
  readonly draw = input.required<CashierCurrentDrawView>();

  readonly remaining = signal<number | null>(null);

  private interval?: ReturnType<typeof setInterval>;

  ngOnInit(): void {
    this.remaining.set(this.draw().remainingSeconds);
    if (this.remaining() !== null && this.remaining()! > 0) {
      this.interval = setInterval(() => {
        const current = this.remaining();
        if (current !== null && current > 0) {
          this.remaining.set(current - 1);
        } else {
          clearInterval(this.interval);
        }
      }, 1000);
    }
  }

  ngOnDestroy(): void {
    clearInterval(this.interval);
  }

  formatRemaining(seconds: number): string {
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = seconds % 60;
    if (h > 0) {
      return `${h}h ${String(m).padStart(2, '0')}m ${String(s).padStart(2, '0')}s`;
    }
    return `${String(m).padStart(2, '0')}m ${String(s).padStart(2, '0')}s`;
  }
}
