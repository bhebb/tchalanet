import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

import { Brand } from '@tchl/types';

@Component({
  selector: 'tchl-brand',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  template: `
    <div class="tch-brand">
      @if (brand()?.logo) {
      <img class="tch-brand__icon" [src]="brand()!.logo!" alt="" aria-hidden="true" />
      } @if (showName()) {
      <span class="tch-brand__text">{{ brand()?.name || 'TCHALANET' }}</span>
      }
    </div>
  `,
  styles: `
    .tch-brand {
      --comp-brand-icon-size: 34px;
      --comp-brand-gap: 0.5rem;
      --comp-brand-text-size: 1rem;
      --comp-brand-fg: var(--tch-on-surface-header, currentColor);

      display: inline-flex;
      align-items: center;
      gap: var(--comp-brand-gap);
      color: var(--comp-brand-fg);
    }

    .tch-brand__icon {
      inline-size: var(--comp-brand-icon-size);
      block-size: var(--comp-brand-icon-size);
      border-radius: 6px;
      flex-shrink: 0;
    }

    .tch-brand__text {
      line-height: 1.2;
      font-size: var(--comp-brand-text-size);
      font-weight: 600;
    }

  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BrandComponent {
  brand = input<Brand>();
  showName = input<boolean>(true);
}
