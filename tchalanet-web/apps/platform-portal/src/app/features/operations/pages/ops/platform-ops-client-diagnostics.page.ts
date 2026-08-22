import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatTabsModule } from '@angular/material/tabs';
import { RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { AdminPageShellComponent, AdminSectionCardComponent } from '@tch/ui/console';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import {
  ClientDiagnosticEventView,
  ClientDiagnosticEventDetailView,
  ClientDiagnosticDebugSessionView,
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
    MatTabsModule,
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
  readonly sessionsLoading = signal(false);
  readonly debugSessions = signal<readonly ClientDiagnosticDebugSessionView[]>([]);
  readonly events = signal<readonly ClientDiagnosticEventView[]>([]);
  readonly selectedEvent = signal<ClientDiagnosticEventDetailView | null>(null);
  readonly selectedEventIds = signal<ReadonlySet<string>>(new Set<string>());
  readonly eventDetailLoading = signal(false);
  readonly policy = signal<ClientDiagnosticPolicyView | null>(null);
  readonly policyPending = signal(false);
  readonly selectedTenantLabel = signal<string | null>(null);
  readonly selectedTerminalLabel = signal<string | null>(null);
  readonly targetCollapsed = signal(false);
  readonly controlCollapsed = signal(false);

  tenantId = '';
  sellerTerminalId = '';
  limit = 100;
  eventSearch = '';
  eventCategoryFilter = '';
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
    this.loadDebugSessions();
  }

  loadDebugSessions(): void {
    this.sessionsLoading.set(true);
    this.error.set(null);
    this.api.listClientDiagnosticDebugSessions({ suppressShellFeedback: true }).subscribe({
      next: sessions => {
        this.debugSessions.set(sessions);
        this.sessionsLoading.set(false);
      },
      error: () => {
        this.error.set(this.t('platform.ops.clientDiagnostics.error.sessions'));
        this.sessionsLoading.set(false);
      },
    });
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
        this.selectedEventIds.set(new Set<string>());
        if (this.tenantId.trim() && this.sellerTerminalId.trim()) {
          this.targetCollapsed.set(true);
          this.controlCollapsed.set(events.length > 0);
        }
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
    this.selectedTenantLabel.set(selection.tenant?.name ?? selection.tenant?.code ?? null);
    this.selectedTerminalLabel.set(this.terminalLabel(selection));
    this.policy.set(null);
    this.events.set([]);
    this.selectedEventIds.set(new Set<string>());
    this.selectedEvent.set(null);
    this.targetCollapsed.set(false);
    this.controlCollapsed.set(false);
    this.loadPolicyIfTargeted();
  }

  selectDebugSession(session: ClientDiagnosticDebugSessionView): void {
    this.tenantId = this.idValue(session.tenantId);
    this.sellerTerminalId = this.idValue(session.sellerTerminalId);
    this.selectedTenantLabel.set(this.tenantLabel(session));
    this.selectedTerminalLabel.set(this.debugSessionTerminalLabel(session));
    this.policy.set(null);
    this.events.set([]);
    this.selectedEventIds.set(new Set<string>());
    this.selectedEvent.set(null);
    this.targetCollapsed.set(true);
    this.controlCollapsed.set(true);
    this.load();
  }

  toggleTargetCollapsed(): void {
    this.targetCollapsed.set(!this.targetCollapsed());
  }

  toggleControlCollapsed(): void {
    this.controlCollapsed.set(!this.controlCollapsed());
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
        this.loadDebugSessions();
      },
      error: (err: unknown) => {
        this.error.set(this.errorMessage(err, 'platform.ops.clientDiagnostics.error.enable'));
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
        this.loadDebugSessions();
      },
      error: (err: unknown) => {
        this.error.set(this.errorMessage(err, 'platform.ops.clientDiagnostics.error.disable'));
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

  selectedEventCount(): number {
    return this.selectedEventIds().size;
  }

  filteredEvents(): readonly ClientDiagnosticEventView[] {
    const category = this.eventCategoryFilter.trim().toUpperCase();
    const search = this.eventSearch.trim().toLowerCase();
    return this.events().filter(event => {
      const matchesCategory = !category || event.category === category;
      const haystack = [
        event.category,
        event.severity,
        event.operation,
        event.errorCode,
        event.message,
        event.endpointKey,
        event.requestId,
        this.eventTenantLabel(event),
        this.eventTerminalLabel(event),
      ].filter(Boolean).join(' ').toLowerCase();
      return matchesCategory && (!search || haystack.includes(search));
    });
  }

  allVisibleEventsSelected(): boolean {
    const visible = this.filteredEvents();
    return visible.length > 0 && visible.every(event => this.selectedEventIds().has(event.id));
  }

  toggleEventSelection(eventId: string, checked: boolean): void {
    const next = new Set(this.selectedEventIds());
    if (checked) {
      next.add(eventId);
    } else {
      next.delete(eventId);
    }
    this.selectedEventIds.set(next);
  }

  toggleVisibleEvents(checked: boolean): void {
    const next = new Set(this.selectedEventIds());
    for (const event of this.filteredEvents()) {
      if (checked) {
        next.add(event.id);
      } else {
        next.delete(event.id);
      }
    }
    this.selectedEventIds.set(next);
  }

  deleteSelectedEvents(): void {
    const eventIds = [...this.selectedEventIds()];
    if (eventIds.length === 0) return;
    this.loading.set(true);
    this.error.set(null);
    this.api.deleteClientDiagnosticEvents(eventIds, { suppressShellFeedback: true }).subscribe({
      next: result => {
        this.notice.set(
          this.t('platform.ops.clientDiagnostics.notice.deleted', { count: result.deleted }),
        );
        this.selectedEventIds.set(new Set<string>());
        this.selectedEvent.set(null);
        this.loading.set(false);
        this.load();
        this.loadDebugSessions();
      },
      error: (err: unknown) => {
        this.error.set(this.errorMessage(err, 'platform.ops.clientDiagnostics.error.delete'));
        this.loading.set(false);
      },
    });
  }

  detailJson(detail: ClientDiagnosticEventDetailView): string {
    return JSON.stringify(detail, null, 2);
  }

  eventTenantLabel(event: ClientDiagnosticEventView): string {
    if (this.matchesSelectedTarget(event)) {
      return this.selectedTenantLabel() ?? this.idValue(event.tenantId);
    }
    return this.idValue(event.tenantId);
  }

  eventTerminalLabel(event: ClientDiagnosticEventView): string {
    if (this.matchesSelectedTarget(event)) {
      return this.selectedTerminalLabel() ?? this.idValue(event.sellerTerminalId);
    }
    return this.idValue(event.sellerTerminalId);
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
      error: (err: unknown) => {
        this.policy.set(null);
        this.error.set(this.errorMessage(err, 'platform.ops.clientDiagnostics.error.policy'));
      },
    });
  }

  private t(key: string, params?: Record<string, unknown>): string {
    return this.translate.instant(key, params);
  }

  private errorMessage(err: unknown, fallbackKey: string): string {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (!problem) return this.t(fallbackKey);
    const normalized = webAppErrorFromProblemDetail(problem, fallbackKey, 'section');
    const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
    return copy.message || copy.title || this.t(fallbackKey);
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

  private terminalLabel(selection: SellerTerminalTargetSelection): string | null {
    const terminal = selection.sellerTerminal;
    if (!terminal) return null;
    return [terminal.displayName, terminal.terminalCode].filter(Boolean).join(' · ');
  }

  private tenantLabel(session: ClientDiagnosticDebugSessionView): string {
    return [session.tenantName, session.tenantCode].filter(Boolean).join(' · ') || this.idValue(session.tenantId);
  }

  private debugSessionTerminalLabel(session: ClientDiagnosticDebugSessionView): string {
    return [session.terminalName, session.terminalCode].filter(Boolean).join(' · ') || this.idValue(session.sellerTerminalId);
  }

  private matchesSelectedTarget(event: ClientDiagnosticEventView): boolean {
    return (
      this.idValue(event.tenantId) === this.tenantId &&
      this.idValue(event.sellerTerminalId) === this.sellerTerminalId
    );
  }
}
