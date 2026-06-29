import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { TchCard } from '@tch/ui/components';
import { DrawChannelProviderView } from '../../data-access/admin-draw-channels.models';

@Component({
  selector: 'tch-draw-channels-summary',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TchCard],
  templateUrl: './draw-channels-summary.component.html',
  styleUrls: ['./draw-channels-summary.component.scss'],
})
export class DrawChannelsSummaryComponent {
  readonly providers = input.required<DrawChannelProviderView[]>();

  readonly totalCount     = computed(() => this.providers().length);
  readonly activeCount    = computed(() => this.providers().filter(p => p.tenantStatus === 'ACTIVE').length);
  readonly activeChannels = computed(() =>
    this.providers().reduce((sum, p) => sum + p.slots.filter(s => s.enabled).length, 0),
  );
  readonly todoCount = computed(() =>
    this.providers().filter(p => p.tenantStatus === 'INACTIVE' || p.tenantStatus === 'NEEDS_CONFIG').length,
  );
}
