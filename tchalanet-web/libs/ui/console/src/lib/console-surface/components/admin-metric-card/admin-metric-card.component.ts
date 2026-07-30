import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * Tone of a metric card.
 * - `default` — neutral surface, the common case.
 * - `accent` — the headline figure of a group (net revenue, total); exactly one tile per row.
 * - `negative` — a figure that reads as a cost (payouts, charges); only the value is tinted.
 */
export type AdminMetricCardTone = 'default' | 'accent' | 'negative';

/**
 * The shared admin KPI card.
 *
 * Every admin surface showing a label + figure should use this rather than restyling a div.
 * The component owns only the API and markup: the look lives in `libs/ui/styles` as
 * `.tch-metric`, next to the other shared surface patterns, so it can be shared and themed in
 * one place. Rows of these belong in a `.tch-metric-grid`.
 */
@Component({
  selector: 'tch-admin-metric-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-metric-card.component.html',
})
export class AdminMetricCardComponent {
  readonly label = input.required<string>();
  readonly value = input.required<string>();
  readonly hint = input<string | null>(null);
  readonly tone = input<AdminMetricCardTone>('default');
}
