import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export interface HaitiLotsDisplayValue {
  readonly lot1?: unknown;
  readonly lot2?: unknown;
  readonly lot3?: unknown;
  readonly lot4?: unknown;
}

@Component({
  selector: 'tch-haiti-lots-display',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="tch-haiti-lots" [class.tch-haiti-lots--compact]="compact()">
      <span class="tch-haiti-lots__item">
        @if (showLabels() && !compact()) { <strong>Lot 1</strong> }
        <em>{{ lotLabel(lots().lot1) }}</em>
      </span>
      <span class="tch-haiti-lots__item">
        @if (showLabels() && !compact()) { <strong>Lot 2</strong> }
        <em>{{ lotLabel(lots().lot2) }}</em>
      </span>
      <span class="tch-haiti-lots__item">
        @if (showLabels() && !compact()) { <strong>Lot 3</strong> }
        <em>{{ lotLabel(lots().lot3) }}</em>
      </span>
    </div>
  `,
  styles: [`
    :host {
      display: block;
    }

    .tch-haiti-lots {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
      align-items: center;
    }

    .tch-haiti-lots__item {
      display: inline-flex;
      align-items: center;
      gap: 0.375rem;
      white-space: nowrap;
    }

    .tch-haiti-lots__item strong {
      color: var(--tch-color-on-surface-variant);
      font-size: var(--tch-font-size-label-sm);
      font-weight: 600;
    }

    .tch-haiti-lots__item em {
      display: grid;
      place-items: center;
      min-width: 2rem;
      height: 2rem;
      padding: 0 0.375rem;
      border-radius: var(--tch-radius-pill, 9999px);
      background: var(--tch-color-primary);
      color: var(--tch-color-on-primary);
      font-family: var(--tch-font-family-mono, monospace);
      font-size: 0.75rem;
      font-style: normal;
      font-weight: 800;
    }

    .tch-haiti-lots--compact {
      gap: 0.375rem;
    }

    .tch-haiti-lots--compact .tch-haiti-lots__item {
      gap: 0.25rem;
    }

    .tch-haiti-lots--compact .tch-haiti-lots__item em {
      min-width: 1.75rem;
      height: 1.75rem;
      font-size: 0.6875rem;
    }
  `],
})
export class HaitiLotsDisplayComponent {
  readonly lots = input.required<HaitiLotsDisplayValue>();
  readonly showLabels = input(true);
  readonly compact = input(false);

  lotLabel(value: unknown): string {
    const normalized = typeof value === 'string'
      ? value.trim()
      : value == null
        ? ''
        : String(value).trim();
    return normalized || '-';
  }
}
