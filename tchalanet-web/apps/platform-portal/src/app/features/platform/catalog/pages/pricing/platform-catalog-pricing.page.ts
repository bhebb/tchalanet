import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { AdminCrudShellComponent } from '@tch/ui/console';
import { AdminDataToolbarComponent } from '@tch/ui/console';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';
import {
  ConsolePricingActionEvent,
  ConsolePricingRow,
  ConsolePricingTableComponent,
} from '@tch/web/console';
import {
  TchAsyncReadyDirective,
  TchAsyncViewComponent,
  resourceErrorVm,
} from '@tch/web/async';
import {
  BetType,
  CatalogPricingView,
  PlatformCatalogApi,
} from '../../data-access/platform-catalog-api.service';
import { CATALOG_BET_TYPES } from '../../components/dialogs/catalog-pricing-options';
import { CreatePricingDialog } from '../../components/dialogs/create-pricing.dialog';
import { EditPricingDialog } from '../../components/dialogs/edit-pricing.dialog';

// ── Main Page ─────────────────────────────────────────────────────────────────
@Component({
  selector: 'tch-platform-catalog-pricing-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminCrudShellComponent,
    AdminDataToolbarComponent,
    AdminEmptyStateComponent,
    AdminPageShellComponent,
    ConsolePricingTableComponent,
    TchAsyncReadyDirective,
    TchAsyncViewComponent,
    TranslatePipe,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
  ],
  templateUrl: './platform-catalog-pricing.page.html',
})
export class PlatformCatalogPricingPage {
  private readonly api = inject(PlatformCatalogApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly translate = inject(TranslateService);

  readonly betTypes = CATALOG_BET_TYPES;

  readonly pricing = this.api.listPricingResource({ suppressShellFeedback: true });
  readonly pricingError = resourceErrorVm(this.pricing, 'platform.catalog.pricing');
  readonly allRows = computed(() => this.pricing.value() ?? []);
  readonly gameFilter = signal('');
  readonly betTypeFilter = signal<BetType | ''>('');

  readonly rows = computed(() => {
    let r = this.allRows();
    const gf = this.gameFilter().toUpperCase();
    if (gf) r = r.filter(x => x.gameCode.toUpperCase().includes(gf));
    const btf = this.betTypeFilter();
    if (btf) r = r.filter(x => x.betType === btf);
    return r;
  });
  readonly pricingRows = computed<readonly ConsolePricingRow[]>(() =>
    this.rows().map(row => ({
      id: row.id,
      gameCode: row.gameCode,
      betType: row.betType,
      betOption: row.betOption,
      odds: row.odds,
      tenantLabel: this.tenantLabel(row),
      statusLabel: this.translate.instant(row.active ? 'common.enabled' : 'common.disabled'),
      statusTone: row.active ? 'success' : 'neutral',
      actions: [
        { id: 'edit', label: this.translate.instant('common.edit'), icon: 'edit', variant: 'icon' },
        { id: 'delete', label: this.translate.instant('common.delete'), icon: 'delete', tone: 'danger', variant: 'icon' },
      ],
    })),
  );

  load(): void {
    this.pricing.reload();
  }

  onGameFilter(v: string): void { this.gameFilter.set(v); }
  onBetTypeFilter(v: BetType | ''): void { this.betTypeFilter.set(v); }

  openCreate(): void {
    const ref = this.dialog.open(CreatePricingDialog, { width: 'min(100vw - 32px, 560px)' });
    ref.afterClosed().subscribe((created: CatalogPricingView | null) => {
      if (created) {
        this.snackBar.open(this.translate.instant('platform.catalog.pricing.feedback.created'), 'OK', { duration: 4000 });
        this.load();
      }
    });
  }

  openEdit(row: CatalogPricingView): void {
    const ref = this.dialog.open(EditPricingDialog, { data: row, width: 'min(100vw - 32px, 520px)' });
    ref.afterClosed().subscribe((updated: CatalogPricingView | null) => {
      if (updated) {
        this.snackBar.open(this.translate.instant('platform.catalog.pricing.feedback.updated'), 'OK', { duration: 4000 });
        this.load();
      }
    });
  }

  delete(row: CatalogPricingView): void {
    if (!confirm(this.translate.instant('platform.catalog.pricing.confirm.delete', { gameCode: row.gameCode, betType: row.betType }))) return;
    this.api.deletePricing(row.id).subscribe({
      next: () => {
        this.snackBar.open(this.translate.instant('platform.catalog.pricing.feedback.deleted'), 'OK', { duration: 4000 });
        this.load();
      },
      error: (err: unknown) => {
        this.snackBar.open((err as { error?: { title?: string } })?.error?.title ?? this.translate.instant('common.error.title'), 'OK', { duration: 5000 });
      },
    });
  }

  onPricingAction(event: ConsolePricingActionEvent): void {
    const row = this.rows().find(item => item.id === event.row.id);
    if (!row) return;
    switch (event.action.id) {
      case 'edit':
        this.openEdit(row);
        break;
      case 'delete':
        this.delete(row);
        break;
    }
  }

  tenantLabel(row: CatalogPricingView): string {
    if (!row.tenantId) return this.translate.instant('platform.catalog.pricing.scope.global');
    return row.tenantId.slice(0, 8) + '…';
  }
}
