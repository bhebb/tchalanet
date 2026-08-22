import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import {
  AdminDetailLayoutComponent,
  AdminPageShellComponent,
  AdminStatusTone,
  TchIdentityCardComponent,
  type TchIdentityCardMeta,
} from '@tch/ui/console';
import { AdminLimitsSectionComponent } from '../../../limits/components/limits-section/admin-limits-section.component';
import { resourceErrorVm, TchAsyncReadyDirective, TchAsyncViewComponent } from '@tch/web/async';

import {
  SellerTerminalApi,
  SellerTerminalDailyFinancialRow,
  SellerTerminalView,
} from '../../data-access/seller-terminal-api.service';
import { TchNotice } from '@tch/ui/components';
import {
  SellerTerminalDetailFact,
  SellerTerminalDetailFactsCardComponent,
} from '../../components/seller-terminal-detail-facts-card/seller-terminal-detail-facts-card.component';
import { SellerTerminalTodayStatsCardComponent } from '../../components/seller-terminal-today-stats-card/seller-terminal-today-stats-card.component';
import { BlockSellerTerminalDialog } from '../list/dialogs/block-seller-terminal.dialog';
import { ConfirmUnblockDialog } from '../list/dialogs/confirm-unblock.dialog';
import { ResetPinDialog } from '../list/dialogs/reset-pin.dialog';
import { SellerTerminalDialogResult } from '../list/dialogs/seller-terminal-dialog-result';

@Component({
  selector: 'tch-admin-seller-terminal-detail-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    AdminDetailLayoutComponent,
    AdminPageShellComponent,
    TchIdentityCardComponent,
    SellerTerminalDetailFactsCardComponent,
    SellerTerminalTodayStatsCardComponent,
    TchAsyncReadyDirective,
    TchAsyncViewComponent,
    TchNotice,
    TranslatePipe,
    MatButtonModule,
    MatIconModule,
    AdminLimitsSectionComponent,
  ],
  templateUrl: './admin-seller-terminal-detail.page.html',
  styleUrls: ['./admin-seller-terminal-detail.page.scss'],
})
export class AdminSellerTerminalDetailPage {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(SellerTerminalApi);
  private readonly translate = inject(TranslateService);
  private readonly dialog = inject(MatDialog);

  readonly actionSuccess = signal<string | null>(null);

  readonly sellerTerminalId = this.route.snapshot.paramMap.get('sellerTerminalId');
  readonly sellerTerminalDetail = this.api.getDetailResource(
    () => this.sellerTerminalId,
    { suppressShellFeedback: true },
  );
  readonly sellerTerminalError = resourceErrorVm(
    this.sellerTerminalDetail,
    'admin.sellerTerminals.detail',
  );
  readonly terminal = computed(() => {
    if (this.sellerTerminalDetail.status() === 'error') return null;
    return this.sellerTerminalDetail.value()?.terminal ?? null;
  });
  readonly tenant = computed(() => this.sellerTerminalDetail.value()?.tenant ?? null);
  readonly limits = computed(() => this.sellerTerminalDetail.value()?.limits ?? null);
  readonly todayStats = computed<SellerTerminalDailyFinancialRow>(() => {
    const id = this.sellerTerminalId;
    return this.sellerTerminalDetail.value()?.todayStats ?? emptyTodayStats(id ?? '');
  });

  readonly title = computed(() => {
    const terminal = this.terminal();
    return terminal?.displayName || terminal?.terminalCode || this.translate.instant('admin.sellerTerminals.detail.title');
  });

  readonly identityMeta = computed<readonly TchIdentityCardMeta[]>(() => {
    const terminal = this.terminal();
    if (!terminal) return [];

    return [
      {
        label: this.translate.instant('admin.sellerTerminals.field.email'),
        value: terminal.email,
      },
      {
        label: this.translate.instant('admin.sellerTerminals.field.phone'),
        value: terminal.phoneNumber,
      },
    ];
  });

  statusTone(status: string): AdminStatusTone {
    switch (status) {
      case 'ACTIVE':
        return 'success';
      case 'PENDING':
        return 'warning';
      case 'BLOCKED':
        return 'danger';
      default:
        return 'neutral';
    }
  }

  readonly controlFacts = computed<readonly SellerTerminalDetailFact[]>(() => {
    const terminal = this.terminal();
    if (!terminal) return [];
    return [
      { labelKey: 'admin.sellerTerminals.field.seller_terminal_code', value: terminal.terminalCode },
      {
        labelKey: 'admin.sellerTerminals.detail.field.commissionRate',
        value: terminal.commissionRate,
        kind: 'percent',
      },
      { labelKey: 'admin.sellerTerminals.detail.field.addressId', value: idValue(terminal.addressId) },
      { labelKey: 'admin.sellerTerminals.detail.field.tenantId', value: this.tenantLabel() },
    ];
  });

  readonly stateFacts = computed<readonly SellerTerminalDetailFact[]>(() => {
    const terminal = this.terminal();
    if (!terminal) return [];
    return [
      {
        labelKey: 'admin.sellerTerminals.detail.field.currentStatus',
        valueKey: `admin.sellerTerminals.status.${terminal.status}`,
      },
      {
        labelKey: 'admin.sellerTerminals.detail.field.canSell',
        value: terminal.status === 'ACTIVE',
        kind: 'boolean',
      },
      {
        labelKey: 'admin.sellerTerminals.detail.field.mustChangePin',
        value: !!terminal.mustChangePin,
        kind: 'boolean',
      },
      {
        labelKey: 'admin.sellerTerminals.detail.field.blocked',
        value: terminal.status === 'BLOCKED',
        kind: 'boolean',
      },
      {
        labelKey: 'admin.sellerTerminals.detail.field.disabled',
        value: terminal.status === 'DISABLED',
        kind: 'boolean',
      },
    ];
  });

  readonly securityFacts = computed<readonly SellerTerminalDetailFact[]>(() => {
    const terminal = this.terminal();
    if (!terminal) return [];
    return [
      {
        labelKey: 'admin.sellerTerminals.detail.field.mustChangePin',
        value: !!terminal.mustChangePin,
        kind: 'boolean',
      },
      {
        labelKey: 'admin.sellerTerminals.detail.field.pinResetAt',
        value: terminal.pinResetAt,
        kind: 'date',
      },
      { labelKey: 'admin.sellerTerminals.detail.field.blockedReason', value: terminal.blockedReason },
    ];
  });

  readonly clientDiagnosticsFacts = computed<readonly SellerTerminalDetailFact[]>(() => {
    const diagnostics = this.sellerTerminalDetail.value()?.clientDiagnostics;
    if (!diagnostics) {
      return [
        {
          labelKey: 'admin.sellerTerminals.detail.field.clientDiagnosticsStatus',
          valueKey: 'admin.sellerTerminals.detail.clientDiagnostics.status.loading',
        },
      ];
    }

    return [
      {
        labelKey: 'admin.sellerTerminals.detail.field.clientDiagnosticsStatus',
        valueKey: diagnostics.enabled
          ? 'admin.sellerTerminals.detail.clientDiagnostics.status.enabled'
          : 'admin.sellerTerminals.detail.clientDiagnostics.status.disabled',
      },
      {
        labelKey: 'admin.sellerTerminals.detail.field.clientDiagnosticsExpiresAt',
        value: diagnostics.expiresAt,
        kind: 'date',
      },
      {
        labelKey: 'admin.sellerTerminals.detail.field.clientDiagnosticsCategories',
        value: diagnostics.categories.length ? diagnostics.categories.join(', ') : null,
      },
      {
        labelKey: 'admin.sellerTerminals.detail.field.clientDiagnosticsMaxEvents',
        value: diagnostics.maxEvents || null,
      },
    ];
  });

  readonly activityFacts = computed<readonly SellerTerminalDetailFact[]>(() => {
    const terminal = this.terminal();
    if (!terminal) return [];
    return [
      { labelKey: 'admin.sellerTerminals.detail.field.activatedAt', value: terminal.activatedAt, kind: 'date' },
      { labelKey: 'admin.sellerTerminals.detail.field.lastSeenAt', value: terminal.lastSeenAt, kind: 'date' },
      { labelKey: 'admin.sellerTerminals.detail.field.blockedAt', value: terminal.blockedAt, kind: 'date' },
      { labelKey: 'admin.sellerTerminals.detail.field.disabledAt', value: terminal.disabledAt, kind: 'date' },
    ];
  });

  reload(): void {
    this.sellerTerminalDetail.reload();
  }

  reloadTodayStats(): void {
    this.sellerTerminalDetail.reload();
  }

  openBlock(terminal: SellerTerminalView): void {
    this.actionSuccess.set(null);
    const ref = this.dialog.open(BlockSellerTerminalDialog, { data: terminal, width: '480px' });
    ref.afterClosed().subscribe((result?: SellerTerminalDialogResult) => {
      if (!result?.reload) return;
      this.actionSuccess.set(result.noticeKey);
      this.reload();
    });
  }

  unblock(terminal: SellerTerminalView): void {
    this.actionSuccess.set(null);
    const ref = this.dialog.open(ConfirmUnblockDialog, { data: terminal, width: '400px' });
    ref.afterClosed().subscribe((unblocked?: boolean) => {
      if (!unblocked) return;
      this.actionSuccess.set('admin.sellerTerminals.list.notice.unblocked');
      this.reload();
    });
  }

  openResetPin(terminal: SellerTerminalView): void {
    this.actionSuccess.set(null);
    const ref = this.dialog.open(ResetPinDialog, {
      data: terminal,
      width: '480px',
      disableClose: true,
    });
    ref.afterClosed().subscribe((result?: { reload: boolean }) => {
      if (!result?.reload) return;
      this.actionSuccess.set('admin.sellerTerminals.list.notice.pinReset');
      this.reload();
    });
  }

  private tenantLabel(): string | null {
    const tenant = this.tenant();
    if (!tenant) return idValue(this.terminal()?.tenantId);
    return tenant.displayName || tenant.name || tenant.code || idValue(tenant.tenantId);
  }
}

function idValue(value: { value?: string | null } | string | null | undefined): string | null {
  if (!value) return null;
  return typeof value === 'string' ? value : value.value ?? null;
}

function emptyTodayStats(sellerTerminalId: string): SellerTerminalDailyFinancialRow {
  return {
    sellerTerminalId,
    refDate: '',
    ticketsSold: 0,
    grossSales: 0,
    sellerCommission: 0,
    buyerCharges: 0,
    sellerCharges: 0,
    tenantCharges: 0,
    waivedCharges: 0,
    promotionLines: 0,
    promotionPricedLines: 0,
    promotionPayoutBase: 0,
    netRevenueEstimated: 0,
    netRevenuePaidBasis: 0,
  };
}
