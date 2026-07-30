import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * Tone of a metric card.
 * - `default` — neutral surface, the common case.
 * - `accent` — the headline figure of a group (net revenue, total), filled with the primary
 *   container so exactly one card in a row stands out.
 * - `negative` — a figure that reads as a cost (payouts, charges); only the value is tinted.
 */
export type AdminMetricCardTone = 'default' | 'accent' | 'negative';

/**
 * The shared admin KPI card.
 *
 * Every admin surface that shows a label + figure should use this rather than restyling a div:
 * the hand-rolled copies had already drifted apart (differing paddings, radii, label casing) and
 * one of them silently rendered with no border or background at all because its `var(--tch-…)`
 * token names were missing the `color-` segment — an unresolved custom property makes the whole
 * declaration invalid, so `border` and `background` fall back to none/transparent.
 */
@Component({
  selector: 'tch-admin-metric-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-metric-card.component.html',
  styleUrls: ['./admin-metric-card.component.scss'],
  host: { '[attr.data-tone]': 'tone()' },
})
export class AdminMetricCardComponent {
  readonly label = input.required<string>();
  readonly value = input.required<string>();
  readonly hint = input<string | null>(null);
  readonly tone = input<AdminMetricCardTone>('default');
}
