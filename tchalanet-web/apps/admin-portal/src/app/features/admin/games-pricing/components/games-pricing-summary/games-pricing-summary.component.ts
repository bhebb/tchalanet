import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { TchCard } from '@tch/ui/components';
import { TenantGamePricingView } from '../../data-access/admin-games-pricing.models';

@Component({
  selector: 'tch-games-pricing-summary',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TchCard],
  templateUrl: './games-pricing-summary.component.html',
  styleUrls: ['./games-pricing-summary.component.scss'],
})
export class GamesPricingSummaryComponent {
  readonly games = input.required<TenantGamePricingView[]>();

  readonly activeCount    = computed(() => this.games().filter(g => g.tenantStatus === 'ACTIVE').length);
  readonly todoCount      = computed(() => this.games().filter(g => g.tenantStatus === 'NEEDS_CONFIG').length);
  readonly availableCount = computed(() => this.games().filter(g => g.tenantStatus === 'INACTIVE').length);
  readonly totalCount     = computed(() => this.games().filter(g => g.catalogStatus === 'AVAILABLE').length);
}
