import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { mapHttpErrorToProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { forkJoin } from 'rxjs';
import { AdminPageShellComponent, AdminSectionCardComponent } from '@tch/ui/console';
import { TchEmptyState, TchErrorPanel, TchLoading } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { consoleBetOptionLabel, consoleBetTypeLabel, consoleGameIdentity } from '@tch/web/console';

import {
  BaremesAdminApi,
  OddsOverrideEntry,
  PricingOddsEntry,
} from '../../../controls/data-access/baremes-admin.api.service';

type PageState = 'loading' | 'ready' | 'error';

interface OverrideGroup {
  readonly gameCode: string;
  readonly gameLabel: string;
  readonly logoText: string;
  readonly logoUrl: string | null;
  readonly rows: readonly PricingOddsEntry[];
}

@Component({
  selector: 'tch-admin-seller-terminal-overrides-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DecimalPipe,
    FormsModule,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    TchEmptyState,
    TchErrorPanel,
    TchLoading,
    TranslatePipe,
  ],
  templateUrl: './admin-seller-terminal-overrides.page.html',
  styleUrls: ['./admin-seller-terminal-overrides.page.scss'],
})
export class AdminSellerTerminalOverridesPage {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(BaremesAdminApi);
  private readonly translate = inject(TranslateService);

  readonly sellerTerminalId = this.route.snapshot.paramMap.get('sellerTerminalId') ?? '';
  readonly state = signal<PageState>('loading');
  readonly odds = signal<readonly PricingOddsEntry[]>([]);
  readonly overrides = signal<readonly OddsOverrideEntry[]>([]);
  readonly draftValues = signal<Record<string, number | null>>({});
  readonly savingKeys = signal<ReadonlySet<string>>(new Set());
  readonly rowErrors = signal<Record<string, string>>({});

  readonly groups = computed<readonly OverrideGroup[]>(() => {
    const map = new Map<string, PricingOddsEntry[]>();
    for (const row of this.odds().filter(item => item.active)) {
      const rows = map.get(row.gameCode) ?? [];
      rows.push(row);
      map.set(row.gameCode, rows);
    }
    return Array.from(map.entries()).map(([gameCode, rows]) => {
      const game = consoleGameIdentity(gameCode);
      return {
        gameCode,
        gameLabel: game.name,
        logoText: game.logoText,
        logoUrl: game.logoUrl,
        rows,
      };
    });
  });

  constructor() {
    this.load();
  }

  load(): void {
    if (!this.sellerTerminalId) {
      this.state.set('error');
      return;
    }
    this.state.set('loading');
    this.rowErrors.set({});
    forkJoin({
      odds: this.api.listTenantOdds(),
      overrides: this.api.listSellerOverrides(this.sellerTerminalId, {
        suppressShellFeedback: true,
      }),
    }).subscribe({
      next: ({ odds, overrides }) => {
        this.odds.set(odds);
        this.overrides.set(overrides);
        this.draftValues.set(
          Object.fromEntries(
            odds.map(row => [this.rowKey(row), this.overrideValue(this.findOverride(row))]),
          ),
        );
        this.state.set('ready');
      },
      error: () => this.state.set('error'),
    });
  }

  rowKey(row: PricingOddsEntry): string {
    return [row.gameCode, row.pricingVariantCode, row.betType, row.betOption ?? 'none'].join('|');
  }

  gameLabel(gameCode: string): string {
    return consoleGameIdentity(gameCode).name;
  }

  betTypeLabel(betType: string): string {
    return consoleBetTypeLabel(betType);
  }

  optionLabel(row: PricingOddsEntry): string {
    return consoleBetOptionLabel(row.betType, row.betOption) ?? 'Standard';
  }

  tenantValue(row: PricingOddsEntry): number | null {
    return row.payoutRuleType === 'FIXED_AMOUNT' ? (row.fixedAmount ?? null) : (row.odds ?? null);
  }

  tenantPlaceholder(row: PricingOddsEntry): string {
    const value = this.tenantValue(row);
    return value === null ? '' : String(value);
  }

  draftValue(row: PricingOddsEntry): number | null {
    return this.draftValues()[this.rowKey(row)] ?? null;
  }

  setDraftValue(row: PricingOddsEntry, value: string | number | null): void {
    const parsed = value === '' || value === null ? null : Number(value);
    const key = this.rowKey(row);
    this.draftValues.update(values => ({
      ...values,
      [key]: Number.isFinite(parsed) ? parsed : null,
    }));
    this.rowErrors.update(errors => ({ ...errors, [key]: '' }));
  }

  findOverride(row: PricingOddsEntry): OddsOverrideEntry | null {
    return (
      this.overrides().find(
        item =>
          item.active &&
          item.gameCode === row.gameCode &&
          item.pricingVariantCode === row.pricingVariantCode &&
          item.betType === row.betType &&
          (item.betOption ?? null) === (row.betOption ?? null),
      ) ?? null
    );
  }

  hasOverride(row: PricingOddsEntry): boolean {
    return !!this.findOverride(row);
  }

  overrideId(row: PricingOddsEntry): string | null {
    return idValue(this.findOverride(row)?.id);
  }

  payoutKindLabel(row: PricingOddsEntry): string {
    return row.payoutRuleType === 'FIXED_AMOUNT' ? 'Montant fixe' : 'Multiplicateur';
  }

  save(row: PricingOddsEntry): void {
    const key = this.rowKey(row);
    const value = this.draftValue(row);
    if (value === null || value <= 0) {
      this.rowErrors.update(errors => ({
        ...errors,
        [key]: this.translate.instant(this.errorMessageKeyFor(row)),
      }));
      return;
    }

    this.setSaving(key, true);
    const payoutRuleType = row.payoutRuleType ?? 'STAKE_MULTIPLIER';
    this.api
      .upsertOverride(
        this.sellerTerminalId,
        {
          gameCode: row.gameCode,
          pricingVariantCode: row.pricingVariantCode,
          betType: row.betType,
          betOption: row.betOption,
          payoutRuleType,
          odds: payoutRuleType === 'FIXED_AMOUNT' ? null : value,
          fixedAmount: payoutRuleType === 'FIXED_AMOUNT' ? value : null,
          reason: 'Override seller-terminal',
        },
        { suppressShellFeedback: true },
      )
      .subscribe({
        next: () => {
          this.setSaving(key, false);
          this.load();
        },
        error: (err: unknown) => {
          this.setSaving(key, false);
          this.setRowError(row, err);
        },
      });
  }

  remove(row: PricingOddsEntry): void {
    const overrideId = this.overrideId(row);
    if (!overrideId) return;

    const key = this.rowKey(row);
    this.setSaving(key, true);
    this.api
      .deleteOverride(this.sellerTerminalId, overrideId, { suppressShellFeedback: true })
      .subscribe({
        next: () => {
          this.setSaving(key, false);
          this.load();
        },
        error: (err: unknown) => {
          this.setSaving(key, false);
          this.setRowError(row, err);
        },
      });
  }

  isSaving(row: PricingOddsEntry): boolean {
    return this.savingKeys().has(this.rowKey(row));
  }

  errorFor(row: PricingOddsEntry): string | null {
    return this.rowErrors()[this.rowKey(row)] || null;
  }

  private overrideValue(override: OddsOverrideEntry | null): number | null {
    if (!override) return null;
    return override.payoutRuleType === 'FIXED_AMOUNT'
      ? (override.fixedAmount ?? null)
      : (override.odds ?? null);
  }

  private setSaving(key: string, saving: boolean): void {
    this.savingKeys.update(current => {
      const next = new Set(current);
      if (saving) next.add(key);
      else next.delete(key);
      return next;
    });
  }

  private setRowError(row: PricingOddsEntry, err: unknown): void {
    const problem = mapHttpErrorToProblemDetail(err);
    const normalized = webAppErrorFromProblemDetail(
      problem,
      'admin.sellerTerminal.override',
      'field',
    );
    const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
    this.rowErrors.update(errors => ({ ...errors, [this.rowKey(row)]: copy.message }));
  }

  private errorMessageKeyFor(row: PricingOddsEntry): string {
    return row.payoutRuleType === 'FIXED_AMOUNT'
      ? 'errors.codes.pricing.fixed_amount_invalid.message'
      : 'errors.codes.pricing.odds_invalid.message';
  }
}

function idValue(value: { value?: string | null } | string | null | undefined): string | null {
  if (!value) return null;
  return typeof value === 'string' ? value : (value.value ?? null);
}
