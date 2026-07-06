import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { ConsoleActorIdentity, ConsoleActorRowComponent } from '@tch/web/console';
import { TchPaginationComponent } from '@tch/ui/console';

import { PlatformRecipientSellerTerminalRow } from '../../../shared/data-access/platform-recipient-seller-terminals-api.service';
import { platformSellerTerminalActorIdentity } from '../../../shared/console-actor-adapters';

@Component({
  selector: 'tch-tenant-seller-terminals-table',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ConsoleActorRowComponent, TchPaginationComponent],
  templateUrl: './tenant-seller-terminals-table.component.html',
  styleUrls: ['./tenant-seller-terminals-table.component.scss'],
})
export class TenantSellerTerminalsTableComponent {
  private readonly translate = inject(TranslateService);

  readonly terminals = input<readonly PlatformRecipientSellerTerminalRow[]>([]);
  readonly page = input(0);
  readonly size = input(20);
  readonly totalElements = input(0);

  readonly pageChange = output<number>();

  actorIdentity(row: PlatformRecipientSellerTerminalRow): ConsoleActorIdentity {
    return platformSellerTerminalActorIdentity(row);
  }

  statusLabel(status: string): string {
    const key = `platform.tenants.detail.sellerTerminals.status.${status.toLowerCase()}`;
    const translated = this.translate.instant(key);
    return translated === key ? status : translated;
  }
}
