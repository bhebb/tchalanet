import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { BadgeStatus, TchStatusBadge } from '@tch/ui/components';
import { AdminSectionCardComponent } from '@tch/ui/console';

import { TenantReadinessStatus } from '../../data-access/platform-tenant-contracts';

@Component({
  selector: 'tch-tenant-readiness-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe, AdminSectionCardComponent, TchStatusBadge],
  templateUrl: './tenant-readiness-card.component.html',
})
export class TenantReadinessCardComponent {
  private readonly translate = inject(TranslateService);

  readonly status = input<TenantReadinessStatus | null | undefined>(null);

  badge(): BadgeStatus {
    const map: Record<string, BadgeStatus> = {
      READY: 'ready', INCOMPLETE: 'warning', MISSING: 'missing', BLOCKED: 'blocked', UNKNOWN: 'missing',
    };
    const status = this.status();
    return status ? (map[status] ?? 'missing') : 'missing';
  }

  label(): string {
    const status = this.status();
    if (!status || status === 'UNKNOWN') return this.translate.instant('platform.tenants.readiness.unknown');
    return this.translate.instant(`platform.tenants.readiness.${status.toLowerCase()}`);
  }
}
