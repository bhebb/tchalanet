import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { TchRequestOptions } from '@tch/api';

import { AdminCrudShellComponent } from '@tch/ui/console';
import { AdminDataToolbarComponent } from '@tch/ui/console';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminStatusPillComponent, AdminStatusTone } from '@tch/ui/console';
import { TchAsyncReadyDirective, TchAsyncViewComponent, resourceErrorVm } from '@tch/web/async';
import {
  PageModelStatus,
  PageModelSummaryView,
  PlatformPageEngineApi,
  pageModelIdValue,
} from '../../data-access/platform-page-engine-api.service';

@Component({
  selector: 'tch-platform-page-engine-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormsModule,
    DatePipe,
    TranslatePipe,
    AdminCrudShellComponent,
    AdminDataToolbarComponent,
    AdminEmptyStateComponent,
    AdminPageShellComponent,
    AdminStatusPillComponent,
    TchAsyncReadyDirective,
    TchAsyncViewComponent,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatTableModule,
  ],
  templateUrl: './platform-page-engine.page.html',
  styleUrls: ['./platform-page-engine.page.scss'],
})
export class PlatformPageEnginePage {
  private readonly api = inject(PlatformPageEngineApi);
  private readonly snackBar = inject(MatSnackBar);
  private readonly translate = inject(TranslateService);

  readonly columns = [
    'tenantId',
    'logicalId',
    'scope',
    'slug',
    'status',
    'schemaVersion',
    'updatedAt',
  ];
  readonly q = signal('');
  readonly selectedId = signal<string>('');
  readonly selectedTenantId = signal<string>('');
  readonly jsonText = signal('');
  readonly jsonError = signal<string | null>(null);
  readonly busy = signal(false);

  readonly pageModels = this.api.listResource(() => ({ q: this.q(), page: 0, size: 50 }), {
    suppressShellFeedback: true,
  });
  readonly pageModelsError = resourceErrorVm(this.pageModels, 'platform.pageEngine.list');
  readonly page = computed(() => this.pageModels.value());
  readonly rows = computed(() => this.page()?.items ?? []);

  readonly detail = this.api.previewResource(
    () => this.selectedId() || undefined,
    () => this.tenantOptions(),
  );
  readonly detailError = resourceErrorVm(this.detail, 'platform.pageEngine.detail');
  readonly selectedDetail = computed(() => this.detail.value());

  constructor() {
    effect(() => {
      const detail = this.selectedDetail();
      if (!detail) {
        this.jsonText.set('');
        return;
      }
      this.jsonText.set(formatJson(detail.model));
      this.jsonError.set(null);
    });
  }

  search(term: string): void {
    this.q.set(term);
  }

  select(row: PageModelSummaryView): void {
    this.selectedId.set(pageModelIdValue(row.id));
    this.selectedTenantId.set(row.tenantId ?? '');
  }

  rowId(row: PageModelSummaryView): string {
    return pageModelIdValue(row.id);
  }

  reload(): void {
    this.pageModels.reload();
    this.detail.reload();
  }

  createDraft(): void {
    const tenantId =
      this.selectedTenantId() ||
      window.prompt(this.translate.instant('platform.pageEngine.prompt.tenantId'));
    if (!tenantId?.trim()) return;
    const logicalId = window.prompt(this.translate.instant('platform.pageEngine.prompt.logicalId'));
    if (!logicalId) return;
    const scope =
      window.prompt(this.translate.instant('platform.pageEngine.prompt.scope'), 'private') ??
      'private';
    const slug =
      window.prompt(this.translate.instant('platform.pageEngine.prompt.slug'), 'dashboard') ??
      'dashboard';
    const model = minimalModel(logicalId, scope, slug);

    this.busy.set(true);
    this.api
      .create({ logicalId, scope, slug, schemaVersion: 2, model }, this.tenantOptions(tenantId))
      .subscribe({
        next: created => {
          this.busy.set(false);
          this.selectedId.set(created.id);
          this.selectedTenantId.set(created.tenantId ?? tenantId);
          this.snack(this.translate.instant('platform.pageEngine.feedback.created'));
          this.pageModels.reload();
          this.detail.reload();
        },
        error: err => this.handleActionError(err, 'platform.pageEngine.error.create'),
      });
  }

  saveJson(): void {
    const detail = this.selectedDetail();
    if (!detail) return;
    const model = this.parseJson();
    if (model == null) return;

    this.busy.set(true);
    this.api
      .update(
        detail.id,
        {
          logicalId: detail.logicalId,
          scope: detail.scope,
          slug: detail.slug,
          schemaVersion: detail.schemaVersion,
          model,
        },
        this.tenantOptions(),
      )
      .subscribe({
        next: () => {
          this.busy.set(false);
          this.snack(this.translate.instant('platform.pageEngine.feedback.saved'));
          this.pageModels.reload();
          this.detail.reload();
        },
        error: err => this.handleActionError(err, 'platform.pageEngine.error.save'),
      });
  }

  publish(): void {
    const detail = this.selectedDetail();
    if (!detail) return;
    this.busy.set(true);
    this.api.publish(detail.id, this.tenantOptions()).subscribe({
      next: () => {
        this.busy.set(false);
        this.snack(this.translate.instant('platform.pageEngine.feedback.published'));
        this.pageModels.reload();
        this.detail.reload();
      },
      error: err => this.handleActionError(err, 'platform.pageEngine.error.publish'),
    });
  }

  duplicate(): void {
    const detail = this.selectedDetail();
    if (!detail) return;
    const logicalId = window.prompt(
      this.translate.instant('platform.pageEngine.prompt.duplicateLogicalId'),
      `${detail.logicalId}.copy`,
    );
    if (!logicalId) return;
    const slug =
      window.prompt(this.translate.instant('platform.pageEngine.prompt.slug'), detail.slug) ??
      detail.slug;

    this.busy.set(true);
    this.api.duplicate(detail.id, logicalId, slug, this.tenantOptions()).subscribe({
      next: created => {
        this.busy.set(false);
        this.selectedId.set(created.id);
        this.selectedTenantId.set(created.tenantId ?? this.selectedTenantId());
        this.snack(this.translate.instant('platform.pageEngine.feedback.duplicated'));
        this.pageModels.reload();
        this.detail.reload();
      },
      error: err => this.handleActionError(err, 'platform.pageEngine.error.duplicate'),
    });
  }

  reset(): void {
    const detail = this.selectedDetail();
    if (!detail || !confirm(this.translate.instant('platform.pageEngine.confirm.reset'))) return;

    this.busy.set(true);
    this.api.reset(detail.id, this.tenantOptions()).subscribe({
      next: () => {
        this.busy.set(false);
        this.snack(this.translate.instant('platform.pageEngine.feedback.reset'));
        this.pageModels.reload();
        this.detail.reload();
      },
      error: err => this.handleActionError(err, 'platform.pageEngine.error.reset'),
    });
  }

  archiveUnavailable(): void {
    this.snack(this.translate.instant('platform.pageEngine.feedback.archiveUnavailable'));
  }

  statusTone(status: PageModelStatus): AdminStatusTone {
    const tones: Record<PageModelStatus, AdminStatusTone> = {
      DRAFT: 'warning',
      PUBLISHED: 'success',
      ARCHIVED: 'neutral',
    };
    return tones[status] ?? 'neutral';
  }

  statusLabel(status: PageModelStatus): string {
    return this.translate.instant(`platform.pageEngine.status.${status}`);
  }

  private parseJson(): unknown | null {
    try {
      const parsed = JSON.parse(this.jsonText());
      this.jsonError.set(null);
      return parsed;
    } catch {
      this.jsonError.set(this.translate.instant('platform.pageEngine.error.invalidJson'));
      return null;
    }
  }

  private tenantOptions(tenantId = this.selectedTenantId()): TchRequestOptions | undefined {
    const normalizedTenantId = tenantId.trim();
    return normalizedTenantId
      ? {
          suppressShellFeedback: true,
          asTenantAdmin: {
            tenantId: normalizedTenantId,
            reason: 'SUPER_ADMIN: manage tenant PageModel',
          },
        }
      : undefined;
  }

  private handleActionError(err: unknown, fallbackKey: string): void {
    this.busy.set(false);
    this.snack(
      (err as { error?: { title?: string } })?.error?.title ?? this.translate.instant(fallbackKey),
    );
  }

  private snack(message: string): void {
    this.snackBar.open(message, 'OK', { duration: 5000 });
  }
}

function formatJson(value: unknown): string {
  return JSON.stringify(value ?? {}, null, 2);
}

function minimalModel(logicalId: string, scope: string, slug: string): unknown {
  return {
    meta: {
      id: logicalId,
      scope,
      slug,
      schemaVersion: 2,
      langs: ['fr', 'en', 'ht'],
      defaultLang: 'ht',
    },
    content: {
      layout: {
        component: 'GridLayout',
        rows: [],
      },
      widgets: {},
    },
  };
}
