import { NgClass, NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

/**
 * Tone of a metric card.
 * - `default` — neutral, the common case.
 * - `accent` — fills the whole tile; marks the headline figure of a row, so at most one tile
 *   per group should carry it.
 * - `info` / `positive` / `warning` — tint the icon only (totals, ready/confirmed, pending).
 * - `negative` — a figure that reads as a cost (payouts, charges); tints the value.
 */
export type AdminMetricCardTone =
  | 'default'
  | 'accent'
  | 'info'
  | 'positive'
  | 'warning'
  | 'negative';

/**
 * The shared admin KPI card.
 *
 * Every admin surface showing a label + figure should use this rather than restyling a div.
 * The component owns only the API and markup: the look lives in `libs/ui/styles` as
 * `.tch-metric`, next to the other shared surface patterns, so it can be shared and themed in
 * one place. Rows of these belong in a `.tch-metric-grid`.
 *
 * Set `interactive` for tiles that act as filter shortcuts — the card then renders a real
 * `<button>`, so click, Enter/Space and focus come from the platform instead of being
 * reimplemented with `role="button"` and keydown handlers on each surface.
 */
@Component({
  selector: 'tch-admin-metric-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [NgClass, NgTemplateOutlet],
  templateUrl: './admin-metric-card.component.html',
})
export class AdminMetricCardComponent {
  readonly label = input.required<string>();
  readonly value = input.required<string>();
  /** Secondary line under the value (a total to compare against, a unit, a caption). */
  readonly hint = input<string | null>(null);
  /** Material symbol name; omit for a text-only tile. */
  readonly icon = input<string | null>(null);
  readonly tone = input<AdminMetricCardTone>('default');
  /** Renders the tile as a button and enables {@link action}. */
  readonly interactive = input(false);
  /** Accessible name for the button; falls back to the label. */
  readonly actionLabel = input<string | null>(null);
  readonly action = output<void>();
}
