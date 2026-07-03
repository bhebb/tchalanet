import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { BadgeStatus, TchStatusBadge } from '@tch/ui/components';
import {
  AdminDetailLayoutComponent,
  AdminPageShellComponent,
  AdminSectionCardComponent,
} from '@tch/ui/console';
import { resourceErrorVm, TchAsyncReadyDirective, TchAsyncViewComponent } from '@tch/web/async';

import {
  SellerTerminalApi,
  SellerTerminalStatus,
  SellerTerminalView,
} from '../../../data-access/seller-terminal-api.service';

interface DetailFact {
  readonly labelKey: string;
  readonly value: string | number | null | undefined;
}

@Component({
  selector: 'tch-admin-seller-terminal-detail-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    DecimalPipe,
    RouterLink,
    AdminDetailLayoutComponent,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    TchAsyncReadyDirective,
    TchAsyncViewComponent,
    TchStatusBadge,
    TranslatePipe,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './admin-seller-terminal-detail.page.html',
  styleUrls: ['./admin-seller-terminal-detail.page.scss'],
})
export class AdminSellerTerminalDetailPage {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(SellerTerminalApi);
  private readonly translate = inject(TranslateService);

  readonly sellerTerminalId = this.route.snapshot.paramMap.get('sellerTerminalId');
  readonly sellerTerminal = this.api.getResource(
    () => this.sellerTerminalId,
    { suppressShellFeedback: true },
  );
  readonly sellerTerminalError = resourceErrorVm(
    this.sellerTerminal,
    'admin.sellerTerminals.detail',
  );
  readonly terminal = computed(() => this.sellerTerminal.value() ?? null);

  readonly title = computed(() => {
    const terminal = this.terminal();
    return terminal?.displayName || terminal?.terminalCode || this.translate.instant('admin.sellerTerminals.detail.title');
  });

  readonly identityFacts = computed<readonly DetailFact[]>(() => {
    const terminal = this.terminal();
    if (!terminal) return [];
    return [
      { labelKey: 'admin.sellerTerminals.field.seller_terminal_code', value: terminal.terminalCode },
      { labelKey: 'admin.sellerTerminals.field.first_name', value: terminal.firstName },
      { labelKey: 'admin.sellerTerminals.field.last_name', value: terminal.lastName },
      { labelKey: 'admin.sellerTerminals.field.email', value: terminal.email },
      { labelKey: 'admin.sellerTerminals.field.phone', value: terminal.phoneNumber },
    ];
  });

  readonly controlFacts = computed<readonly DetailFact[]>(() => {
    const terminal = this.terminal();
    if (!terminal) return [];
    return [
      { labelKey: 'admin.sellerTerminals.detail.field.commissionRate', value: terminal.commissionRate },
      { labelKey: 'admin.sellerTerminals.detail.field.addressId', value: idValue(terminal.addressId) },
      { labelKey: 'admin.sellerTerminals.detail.field.tenantId', value: idValue(terminal.tenantId) },
    ];
  });

  readonly stateFacts = computed<readonly DetailFact[]>(() => {
    const terminal = this.terminal();
    if (!terminal) return [];
    return [
      { labelKey: 'admin.sellerTerminals.detail.field.currentStatus', value: this.statusLabel(terminal.status) },
      {
        labelKey: 'admin.sellerTerminals.detail.field.canSell',
        value: this.translate.instant(terminal.status === 'ACTIVE' ? 'common.yes' : 'common.no'),
      },
      {
        labelKey: 'admin.sellerTerminals.detail.field.mustChangePin',
        value: this.translate.instant(terminal.mustChangePin ? 'common.yes' : 'common.no'),
      },
      {
        labelKey: 'admin.sellerTerminals.detail.field.blocked',
        value: this.translate.instant(terminal.status === 'BLOCKED' ? 'common.yes' : 'common.no'),
      },
      {
        labelKey: 'admin.sellerTerminals.detail.field.disabled',
        value: this.translate.instant(terminal.status === 'DISABLED' ? 'common.yes' : 'common.no'),
      },
    ];
  });

  readonly securityFacts = computed<readonly DetailFact[]>(() => {
    const terminal = this.terminal();
    if (!terminal) return [];
    return [
      {
        labelKey: 'admin.sellerTerminals.detail.field.mustChangePin',
        value: this.translate.instant(
          terminal.mustChangePin ? 'common.yes' : 'common.no',
        ),
      },
      { labelKey: 'admin.sellerTerminals.detail.field.pinResetAt', value: terminal.pinResetAt },
      { labelKey: 'admin.sellerTerminals.detail.field.blockedReason', value: terminal.blockedReason },
    ];
  });

  readonly activityFacts = computed<readonly DetailFact[]>(() => {
    const terminal = this.terminal();
    if (!terminal) return [];
    return [
      { labelKey: 'admin.sellerTerminals.detail.field.activatedAt', value: terminal.activatedAt },
      { labelKey: 'admin.sellerTerminals.detail.field.lastSeenAt', value: terminal.lastSeenAt },
      { labelKey: 'admin.sellerTerminals.detail.field.blockedAt', value: terminal.blockedAt },
      { labelKey: 'admin.sellerTerminals.detail.field.disabledAt', value: terminal.disabledAt },
    ];
  });

  reload(): void {
    this.sellerTerminal.reload();
  }

  statusLabel(status: SellerTerminalStatus): string {
    return this.translate.instant(`admin.sellerTerminals.status.${status}`);
  }

  statusBadge(status: SellerTerminalStatus): BadgeStatus {
    if (status === 'ACTIVE') return 'ready';
    if (status === 'BLOCKED') return 'blocked';
    if (status === 'PENDING') return 'pending';
    return 'missing';
  }

  value(value: string | number | null | undefined): string | number {
    return value ?? this.translate.instant('common.not_available');
  }
}

function idValue(value: { value?: string | null } | string | null | undefined): string | null {
  if (!value) return null;
  return typeof value === 'string' ? value : value.value ?? null;
}
