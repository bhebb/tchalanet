import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import {
  TchLoading,
  TchMultiSearchSelect,
  TchSearchOption,
  TchSearchSelect,
  TchSectionError,
} from '@tch/ui/components';
import { AdminEmptyStateComponent, AdminPageShellComponent, AdminSectionCardComponent } from '@tch/ui/console';
import { Observable, map, of } from 'rxjs';

import {
  DrawView,
  OpsSalesSimulationResponse,
  PlatformOpsApi,
} from '../../data-access/platform-ops-api.service';
import {
  PlatformRecipientSellerTerminalRow,
  PlatformRecipientSellerTerminalsApi,
} from '../../../shared/data-access/platform-recipient-seller-terminals-api.service';
import {
  PlatformTenantsApi,
  TenantSummaryView,
} from '../../../tenants/data-access/platform-tenants-api.service';

type SalesSimulationMode = 'dry-run' | 'execute';

interface SelectedSellerTerminal {
  id: string;
  terminalCode: string;
  displayName: string;
  status: string;
}

const DEFAULT_TICKET_MIX = {
  bolet: 100,
  maryaj: 100,
  loto3: 100,
  loto4: 100,
  loto5: 100,
};

@Component({
  selector: 'tch-platform-ops-sales-simulation-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    AdminEmptyStateComponent,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    TchLoading,
    TchMultiSearchSelect,
    TchSearchSelect,
    TchSectionError,
    TranslatePipe,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './platform-ops-sales-simulation.page.html',
  styleUrl: './platform-ops-sales-simulation.page.scss',
})
export class PlatformOpsSalesSimulationPage {
  private readonly api = inject(PlatformOpsApi);
  private readonly tenantsApi = inject(PlatformTenantsApi);
  private readonly sellerTerminalApi = inject(PlatformRecipientSellerTerminalsApi);
  private readonly translate = inject(TranslateService);
  private readonly destroyRef = inject(DestroyRef);

  readonly selectedTenant = signal<TenantSummaryView | null>(null);
  readonly sellerTerminals = signal<SelectedSellerTerminal[]>([]);
  readonly draws = signal<DrawView[]>([]);
  readonly selectedDrawIds = signal<ReadonlySet<string>>(new Set());
  readonly loadingDraws = signal(false);
  readonly running = signal(false);
  readonly error = signal<string | null>(null);
  readonly notice = signal<string | null>(null);
  readonly result = signal<OpsSalesSimulationResponse | null>(null);

  readonly statusControl = new FormControl<string>('OPEN', { nonNullable: true });
  readonly fromControl = new FormControl<string>(relativeIsoDate(-1), { nonNullable: true });
  readonly toControl = new FormControl<string>(relativeIsoDate(1), { nonNullable: true });
  readonly slotKeyControl = new FormControl<string>('', { nonNullable: true });
  readonly modeControl = new FormControl<SalesSimulationMode>('dry-run', { nonNullable: true });
  readonly currencyControl = new FormControl<string>('HTG', { nonNullable: true });
  readonly stakeAmountControl = new FormControl<number>(1, { nonNullable: true });
  readonly seedControl = new FormControl<number | null>(42);
  readonly maxTicketsControl = new FormControl<number>(5000, { nonNullable: true });
  readonly reasonControl = new FormControl<string>('QA sales simulation', { nonNullable: true });
  readonly boletControl = new FormControl<number>(DEFAULT_TICKET_MIX.bolet, { nonNullable: true });
  readonly maryajControl = new FormControl<number>(DEFAULT_TICKET_MIX.maryaj, { nonNullable: true });
  readonly loto3Control = new FormControl<number>(DEFAULT_TICKET_MIX.loto3, { nonNullable: true });
  readonly loto4Control = new FormControl<number>(DEFAULT_TICKET_MIX.loto4, { nonNullable: true });
  readonly loto5Control = new FormControl<number>(DEFAULT_TICKET_MIX.loto5, { nonNullable: true });

  readonly selectedTenantId = computed(() => {
    const tenant = this.selectedTenant();
    return tenant?.id ?? tenant?.tenantId ?? null;
  });
  readonly selectedDraws = computed(() => this.draws().filter(draw => this.selectedDrawIds().has(draw.id)));
  readonly requestedTickets = computed(() =>
    this.selectedDrawIds().size * this.sellerTerminals().length * this.ticketMixTotal(),
  );
  readonly canRun = computed(() =>
    !!this.selectedTenantId()
    && this.selectedDrawIds().size > 0
    && this.sellerTerminals().length > 0
    && this.ticketMixTotal() > 0
    && !this.running(),
  );

  readonly searchTenants = (query: string): Observable<readonly TchSearchOption<TenantSummaryView>[]> =>
    this.tenantsApi.listTenants({ q: query, page: 0, size: 12, status: 'ACTIVE' }).pipe(
      map(page => (page.items ?? []).map(tenant => this.tenantOption(tenant))),
    );

  readonly searchSellerTerminals = (query: string): Observable<readonly TchSearchOption<SelectedSellerTerminal>[]> => {
    const tenantId = this.selectedTenantId();
    if (!tenantId) {
      return of([]);
    }
    return this.sellerTerminalApi.list({ tenantId, q: query, page: 0, size: 20 }).pipe(
      map(page => (page.items ?? []).map(terminal => this.sellerTerminalOption(terminal))),
    );
  };

  failureMessage(code: string): string {
    const key = `common.errors.codes.${code}.message`;
    const translated = this.translate.instant(key);
    return translated === key ? this.t('common.errors.fallback.message') : translated;
  }

  selectTenant(option: TchSearchOption | null): void {
    this.selectedTenant.set((option?.data as TenantSummaryView | undefined) ?? null);
    this.sellerTerminals.set([]);
    this.draws.set([]);
    this.selectedDrawIds.set(new Set());
    this.result.set(null);
  }

  updateSellerTerminals(options: readonly TchSearchOption[]): void {
    this.sellerTerminals.set(
      options
        .map(option => option.data as SelectedSellerTerminal | undefined)
        .filter((item): item is SelectedSellerTerminal => !!item),
    );
  }

  loadDraws(): void {
    const tenantId = this.selectedTenantId();
    if (!tenantId) {
      this.error.set(this.t('platform.ops.salesSimulation.error.selectTenantBeforeDraws'));
      return;
    }
    this.loadingDraws.set(true);
    this.error.set(null);
    this.api.listDraws({
      status: this.statusControl.value || undefined,
      from: this.fromControl.value || undefined,
      to: this.toControl.value || undefined,
      resultSlotKey: this.slotKeyControl.value || undefined,
      page: 0,
      size: 50,
      suppressShellFeedback: true,
    }, tenantId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: page => {
        this.draws.set(page.items ?? []);
        this.selectedDrawIds.set(new Set());
        this.loadingDraws.set(false);
      },
      error: err => {
        this.error.set(errorMessage(err, this.t('platform.ops.salesSimulation.error.loadDraws')));
        this.loadingDraws.set(false);
      },
    });
  }

  toggleDraw(drawId: string, checked: boolean): void {
    const next = new Set(this.selectedDrawIds());
    if (checked) {
      next.add(drawId);
    } else {
      next.delete(drawId);
    }
    this.selectedDrawIds.set(next);
  }

  selectAllDraws(): void {
    this.selectedDrawIds.set(new Set(this.draws().map(draw => draw.id)));
  }

  clearDraws(): void {
    this.selectedDrawIds.set(new Set());
  }

  run(): void {
    const tenantId = this.selectedTenantId();
    if (!tenantId || !this.canRun()) {
      this.error.set(this.t('platform.ops.salesSimulation.error.missingRunInputs'));
      return;
    }
    const dryRun = this.modeControl.value === 'dry-run';
    this.running.set(true);
    this.error.set(null);
    this.notice.set(null);
    this.api.simulateSales({
      tenantId,
      drawIds: [...this.selectedDrawIds()],
      sellerTerminalIds: this.sellerTerminals().map(seller => seller.id),
      ticketMix: {
        bolet: safeCount(this.boletControl.value),
        maryaj: safeCount(this.maryajControl.value),
        loto3: safeCount(this.loto3Control.value),
        loto4: safeCount(this.loto4Control.value),
        loto5: safeCount(this.loto5Control.value),
      },
      currency: this.currencyControl.value,
      stakeAmount: Number(this.stakeAmountControl.value),
      seed: this.seedControl.value ?? undefined,
      dryRun,
      maxTickets: safeCount(this.maxTicketsControl.value),
      reason: this.reasonControl.value,
    }, { suppressShellFeedback: true }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: result => {
        this.result.set(result);
        this.notice.set(this.t(dryRun
          ? 'platform.ops.salesSimulation.notice.dryRun'
          : 'platform.ops.salesSimulation.notice.executed'));
        this.running.set(false);
      },
      error: err => {
        this.error.set(errorMessage(err, this.t('platform.ops.salesSimulation.error.run')));
        this.running.set(false);
      },
    });
  }

  tenantLabel(): string {
    const tenant = this.selectedTenant();
    return tenant ? `${tenant.name} (${tenant.code})` : this.t('platform.ops.salesSimulation.noTenant');
  }

  trackDraw(_: number, draw: DrawView): string {
    return draw.id;
  }

  trackSeller(_: number, seller: SelectedSellerTerminal): string {
    return seller.id;
  }

  private ticketMixTotal(): number {
    return safeCount(this.boletControl.value)
      + safeCount(this.maryajControl.value)
      + safeCount(this.loto3Control.value)
      + safeCount(this.loto4Control.value)
      + safeCount(this.loto5Control.value);
  }

  private tenantOption(tenant: TenantSummaryView): TchSearchOption<TenantSummaryView> {
    return {
      id: tenant.id ?? tenant.tenantId ?? tenant.code,
      title: tenant.name,
      subtitle: tenant.code,
      badge: tenant.status,
      icon: 'apartment',
      data: tenant,
    };
  }

  private sellerTerminalOption(row: PlatformRecipientSellerTerminalRow): TchSearchOption<SelectedSellerTerminal> {
    const id = row.id?.value ?? '';
    const terminal: SelectedSellerTerminal = {
      id,
      terminalCode: row.terminalCode,
      displayName: row.displayName,
      status: row.status,
    };
    return {
      id,
      title: row.displayName || row.terminalCode,
      subtitle: row.terminalCode,
      badge: row.status,
      icon: 'point_of_sale',
      disabled: !id || row.status !== 'ACTIVE',
      data: terminal,
    };
  }

  private t(key: string): string {
    return this.translate.instant(key);
  }
}

function safeCount(value: number | null | undefined): number {
  const n = Number(value ?? 0);
  return Number.isFinite(n) && n > 0 ? Math.floor(n) : 0;
}

function relativeIsoDate(offsetDays: number): string {
  const now = new Date();
  now.setDate(now.getDate() + offsetDays);
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function errorMessage(err: unknown, fallback: string): string {
  const detail = (err as { error?: { detail?: string; title?: string }; message?: string })?.error;
  return detail?.detail || detail?.title || (err as { message?: string })?.message || fallback;
}
