import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { AdminPageShellComponent, AdminSectionCardComponent } from '@tch/ui/console';
import {
  ClientDiagnosticEventView,
  ClientDiagnosticEventDetailView,
  ClientDiagnosticPolicyView,
  PlatformOpsApi,
} from '../../data-access/platform-ops-api.service';
import {
  SellerTerminalTargetPickerComponent,
  SellerTerminalTargetSelection,
} from '../../../shared/seller-terminal-target-picker/seller-terminal-target-picker.component';

@Component({
  selector: 'tch-platform-ops-client-diagnostics-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    FormsModule,
    RouterLink,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    TchErrorPanel,
    TchLoading,
    SellerTerminalTargetPickerComponent,
    TranslatePipe,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
  ],
  templateUrl: './platform-ops-client-diagnostics.page.html',
  styleUrls: ['./platform-ops-client-diagnostics.page.scss'],
})
export class PlatformOpsClientDiagnosticsPage implements OnInit {
  private readonly api = inject(PlatformOpsApi);
  private readonly translate = inject(TranslateService);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly notice = signal<string | null>(null);
  readonly events = signal<readonly ClientDiagnosticEventView[]>([]);
  readonly selectedEvent = signal<ClientDiagnosticEventDetailView | null>(null);
  readonly eventDetailLoading = signal(false);
  readonly policy = signal<ClientDiagnosticPolicyView | null>(null);
  readonly policyPending = signal(false);

  tenantId = '';
  sellerTerminalId = '';
  limit = 100;
  debugHours = 2;
  reason = '';
  includeApi = true;
  includeConnectivity = true;
  includeSale = true;
  includePrint = true;
  includeScanner = true;
  includePrinterConfig = true;
  includeFlutter = true;
  includeAsync = true;
  includeDevice = true;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listClientDiagnosticEvents(
      {
        tenantId: this.tenantId.trim() || undefined,
        sellerTerminalId: this.sellerTerminalId.trim() || undefined,
        limit: this.limit,
      },
      { suppressShellFeedback: true },
    ).subscribe({
      next: events => {
        this.events.set(events);
        this.loading.set(false);
        this.loadPolicyIfTargeted();
      },
      error: () => {
        this.error.set(this.t('platform.ops.clientDiagnostics.error.load'));
        this.loading.set(false);
      },
    });
  }

  updateTarget(selection: SellerTerminalTargetSelection): void {
    this.tenantId = selection.tenantId ?? '';
    this.sellerTerminalId = selection.sellerTerminalId ?? '';
    this.policy.set(null);
    this.events.set([]);
    this.selectedEvent.set(null);
  }

  enableDebug(): void {
    const tenantId = this.tenantId.trim();
    const sellerTerminalId = this.sellerTerminalId.trim();
    const reason = this.reason.trim();
    if (!tenantId || !sellerTerminalId || !reason) {
      this.error.set(this.t('platform.ops.clientDiagnostics.error.missingEnableInputs'));
      return;
    }
    const categories = this.selectedCategories();
    if (categories.length === 0) {
      this.error.set(this.t('platform.ops.clientDiagnostics.error.missingCategories'));
      return;
    }

    this.policyPending.set(true);
    this.notice.set(null);
    const expiresAt = new Date(Date.now() + Math.max(1, this.debugHours) * 60 * 60 * 1000);
    this.api.enableClientDiagnosticPolicy(
      tenantId,
      sellerTerminalId,
      {
        expiresAt: expiresAt.toISOString(),
        maxEvents: 100,
        categories,
        reason,
      },
      { suppressShellFeedback: true },
    ).subscribe({
      next: policy => {
        this.policy.set(policy);
        this.notice.set(this.t('platform.ops.clientDiagnostics.notice.enabled'));
        this.policyPending.set(false);
      },
      error: () => {
        this.error.set(this.t('platform.ops.clientDiagnostics.error.enable'));
        this.policyPending.set(false);
      },
    });
  }

  disableDebug(): void {
    const tenantId = this.tenantId.trim();
    const sellerTerminalId = this.sellerTerminalId.trim();
    if (!tenantId || !sellerTerminalId) {
      this.error.set(this.t('platform.ops.clientDiagnostics.error.missingDisableInputs'));
      return;
    }

    this.policyPending.set(true);
    this.notice.set(null);
    this.api.disableClientDiagnosticPolicy(tenantId, sellerTerminalId, { suppressShellFeedback: true }).subscribe({
      next: policy => {
        this.policy.set(policy);
        this.notice.set(this.t('platform.ops.clientDiagnostics.notice.disabled'));
        this.policyPending.set(false);
      },
      error: () => {
        this.error.set(this.t('platform.ops.clientDiagnostics.error.disable'));
        this.policyPending.set(false);
      },
    });
  }

  idValue(value: { value?: string | null } | string | null | undefined): string {
    if (!value) return '—';
    return typeof value === 'string' ? value : value.value ?? '—';
  }

  openEvent(event: ClientDiagnosticEventView): void {
    this.eventDetailLoading.set(true);
    this.api.getClientDiagnosticEvent(event.id, { suppressShellFeedback: true }).subscribe({
      next: detail => {
        this.selectedEvent.set(detail);
        this.eventDetailLoading.set(false);
      },
      error: () => {
        this.error.set(this.t('platform.ops.clientDiagnostics.error.detail'));
        this.eventDetailLoading.set(false);
      },
    });
  }

  closeEvent(): void {
    this.selectedEvent.set(null);
  }

  detailJson(detail: ClientDiagnosticEventDetailView): string {
    return JSON.stringify(detail, null, 2);
  }

  private loadPolicyIfTargeted(): void {
    const tenantId = this.tenantId.trim();
    const sellerTerminalId = this.sellerTerminalId.trim();
    if (!tenantId || !sellerTerminalId) {
      this.policy.set(null);
      return;
    }
    this.api.getClientDiagnosticPolicy(tenantId, sellerTerminalId, { suppressShellFeedback: true }).subscribe({
      next: policy => this.policy.set(policy),
      error: () => this.policy.set(null),
    });
  }

  private t(key: string): string {
    return this.translate.instant(key);
  }

  private selectedCategories(): readonly string[] {
    const categories: string[] = [];
    if (this.includeApi) categories.push('API');
    if (this.includeConnectivity) categories.push('CONNECTIVITY');
    if (this.includeSale) categories.push('SALE');
    if (this.includePrint) categories.push('PRINT');
    if (this.includeScanner) categories.push('SCANNER');
    if (this.includePrinterConfig) categories.push('PRINTER_CONFIG');
    if (this.includeFlutter) categories.push('FLUTTER');
    if (this.includeAsync) categories.push('ASYNC');
    if (this.includeDevice) categories.push('DEVICE');
    return categories;
  }

}
