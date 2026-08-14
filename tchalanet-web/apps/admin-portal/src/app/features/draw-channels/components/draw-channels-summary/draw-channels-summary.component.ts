import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { DrawChannelProviderView } from '../../data-access/admin-draw-channels.models';

@Component({
  selector: 'tch-draw-channels-summary',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe],
  styles: [`
    :host {
      display: block;
      margin-block-end: 1rem;
    }

    .draw-channels-summary {
      align-items: center;
      color: var(--tch-color-on-surface-variant, #46464f);
      display: flex;
      flex-wrap: wrap;
      font-size: var(--tch-font-size-body-sm, 0.8125rem);
      gap: 0.35rem 0.75rem;
      line-height: 1.4;
      margin: 0;
    }

    .draw-channels-summary__item {
      align-items: baseline;
      display: inline-flex;
      gap: 0.25rem;
      white-space: nowrap;
    }

    .draw-channels-summary__value {
      color: var(--tch-color-on-surface, #1a1c1e);
      font-weight: var(--tch-weight-bold, 700);
    }
  `],
  templateUrl: './draw-channels-summary.component.html',
})
export class DrawChannelsSummaryComponent {
  readonly providers = input.required<DrawChannelProviderView[]>();

  readonly totalCount = computed(() => this.providers().length);
  readonly activeChannels = computed(() =>
    this.providers().reduce((sum, p) => sum + p.slots.filter(s => s.enabled).length, 0),
  );
  readonly todoCount = computed(() =>
    this.providers().filter(p => p.tenantStatus === 'INACTIVE' || p.tenantStatus === 'NEEDS_CONFIG').length,
  );
}
