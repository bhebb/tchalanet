import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { EntityHistoryType, EntityRevisionItem } from '@tch/api';
import { AdminListStatusOption, AdminListSurface, TchErrorPanel, TchLoading } from '@tch/ui/components';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';
import {
  AdminStatusPillComponent,
  AdminStatusTone,
} from '@tch/ui/console';
import { PlatformEntityHistoryApi } from '../../data-access/platform-entity-history-api.service';

interface EntityHistoryOption {
  value: EntityHistoryType;
  labelKey: string;
}

const ENTITY_TYPES: readonly EntityHistoryOption[] = [
  { value: 'SELLER_TERMINAL', labelKey: 'platform.entityHistory.entityType.sellerTerminal' },
  { value: 'DRAW_RESULT', labelKey: 'platform.entityHistory.entityType.drawResult' },
  { value: 'LIMIT_ASSIGNMENT', labelKey: 'platform.entityHistory.entityType.limitAssignment' },
];

@Component({
  selector: 'tch-platform-entity-history-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    AdminEmptyStateComponent,
    AdminPageShellComponent,
    AdminListSurface,
    AdminStatusPillComponent,
    TchErrorPanel,
    TchLoading,
    MatButtonModule,
    MatChipsModule,
    MatIconModule,
    MatTableModule,
    MatTooltipModule,
    TranslatePipe,
  ],
  templateUrl: './platform-entity-history.page.html',
  styleUrls: ['./platform-entity-history.page.scss'],
})
export class PlatformEntityHistoryPage {
  private readonly api = inject(PlatformEntityHistoryApi);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);

  readonly entityTypes = ENTITY_TYPES;
  readonly displayedColumns = ['changedAt', 'operation', 'entity', 'changedFields', 'tenantId', 'changedBy', 'detail'];

  readonly filterForm = this.fb.nonNullable.group({
    entityType: ['SELLER_TERMINAL' as EntityHistoryType],
    entityId: [''],
  });

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly revisions = signal<readonly EntityRevisionItem[]>([]);
  readonly page = signal(0);
  readonly totalElements = signal(0);
  readonly totalPages = signal(1);
  readonly hasQueried = signal(false);
  readonly hasActiveFilters = signal(false);
  readonly expandedRevisionId = signal<string | null>(null);

  load(): void {
    const v = this.filterForm.getRawValue();
    const entityId = v.entityId.trim();
    if (!entityId) {
      this.hasQueried.set(false);
      this.revisions.set([]);
      this.totalElements.set(0);
      this.totalPages.set(1);
      this.hasActiveFilters.set(false);
      return;
    }

    this.hasQueried.set(true);
    this.hasActiveFilters.set(true);
    this.loading.set(true);
    this.error.set(null);
    this.api.listRevisions({
      entityType: v.entityType,
      entityId,
      page: this.page(),
      size: 20,
    }).subscribe({
      next: p => {
        this.revisions.set(p.items);
        this.totalElements.set(p.totalElements);
        this.totalPages.set(p.totalPages || 1);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(
          (err as { error?: { title?: string } })?.error?.title ??
            this.translate.instant('platform.entityHistory.feedback.loadError'),
        );
        this.loading.set(false);
      },
    });
  }

  applyFilters(): void {
    this.page.set(0);
    this.load();
  }

  resetFilters(): void {
    this.filterForm.reset({ entityType: 'SELLER_TERMINAL', entityId: '' });
    this.page.set(0);
    this.hasQueried.set(false);
    this.revisions.set([]);
    this.totalElements.set(0);
    this.totalPages.set(1);
    this.error.set(null);
    this.hasActiveFilters.set(false);
    this.expandedRevisionId.set(null);
  }

  prevPage(): void {
    this.page.set(this.page() - 1);
    this.load();
  }

  nextPage(): void {
    this.page.set(this.page() + 1);
    this.load();
  }

  operationTone(operation: EntityRevisionItem['operation']): AdminStatusTone {
    if (operation === 'CREATE') return 'success';
    if (operation === 'DELETE') return 'danger';
    return 'warning';
  }

  shortId(value: string | null | undefined, length = 8): string {
    if (!value) return '—';
    return value.length > length ? `${value.slice(0, length)}…` : value;
  }

  entityTypeOptions(): readonly AdminListStatusOption[] {
    return this.entityTypes.map(option => ({
      value: option.value,
      label: this.translate.instant(option.labelKey),
    }));
  }

  toggleDetail(revision: EntityRevisionItem): void {
    this.expandedRevisionId.update(current => current === revision.revisionId ? null : revision.revisionId);
  }

  onEntityIdDraft(entityId: string): void {
    this.filterForm.patchValue({ entityId }, { emitEvent: false });
  }

  onEntityIdFilter(entityId: string): void {
    this.filterForm.patchValue({ entityId }, { emitEvent: false });
    this.applyFilters();
  }

  onEntityTypeFilter(entityType: string): void {
    this.filterForm.patchValue({ entityType: (entityType || 'SELLER_TERMINAL') as EntityHistoryType }, { emitEvent: false });
    this.applyFilters();
  }

  entityLabel(value: EntityHistoryType): string {
    const key = this.entityTypes.find(option => option.value === value)?.labelKey;
    return key ? this.translate.instant(key) : value;
  }
}
