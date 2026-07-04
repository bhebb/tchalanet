import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateService } from '@ngx-translate/core';

import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { TchErrorPanel, TchLoading, TchSectionError } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminCrudShellComponent } from '@tch/ui/console';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';
import {
  AdminStatusPillComponent,
  AdminStatusTone,
} from '@tch/ui/console';
import {
  PlatformSupportApi,
  PublicContentAdminItemView,
  PublicContentStatus,
  PublicContentSurface,
} from '../../support/data-access/platform-support-api.service';

const SURFACES: PublicContentSurface[] = [
  'PUBLIC_HOME',
  'PLATFORM_ADMIN_DASHBOARD',
  'TENANT_ADMIN_DASHBOARD',
  'POS_DASHBOARD',
];

const DEFAULT_EXPIRES_AFTER_DAYS = 7;

@Component({
  selector: 'tch-platform-news-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    ReactiveFormsModule,
    AdminCrudShellComponent,
    AdminEmptyStateComponent,
    AdminPageShellComponent,
    AdminStatusPillComponent,
    TchErrorPanel,
    TchLoading,
    TchSectionError,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatTableModule,
    MatTooltipModule,
  ],
  templateUrl: './platform-news.page.html',
  styleUrl: './platform-news.page.scss',
})
export class PlatformNewsPage implements OnInit {
  private readonly api = inject(PlatformSupportApi);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);

  readonly surfaces = SURFACES;
  readonly displayedColumns = ['publishedAt', 'source', 'title', 'surfaces', 'expiresAt', 'status', 'actions'];

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly actionError = signal<ErrorViewModel | null>(null);
  readonly actionNotice = signal<{ title: string; message: string } | null>(null);
  readonly items = signal<PublicContentAdminItemView[]>([]);
  readonly editing = signal<PublicContentAdminItemView | null>(null);
  readonly showForm = signal(false);

  readonly internalCount = computed(() => this.items().filter(item => item.sourceType === 'INTERNAL').length);
  readonly externalCount = computed(() => this.items().filter(item => item.sourceType === 'EXTERNAL_RSS').length);
  readonly sortedItems = computed(() => {
    return [...this.items()].sort((a, b) => {
      if (a.sourceType !== b.sourceType) return a.sourceType === 'INTERNAL' ? -1 : 1;
      return new Date(b.publishedAt ?? 0).getTime() - new Date(a.publishedAt ?? 0).getTime();
    });
  });

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(180)]],
    content: ['', [Validators.maxLength(2000)]],
    sourceUrl: [''],
    status: ['DRAFT' as PublicContentStatus],
    expiresAfterDays: [DEFAULT_EXPIRES_AFTER_DAYS],
    targetSurfaces: this.fb.nonNullable.control<PublicContentSurface[]>(['PUBLIC_HOME']),
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.actionError.set(null);
    this.api.listNews({ suppressShellFeedback: true }).subscribe({
      next: items => {
        this.items.set(items ?? []);
        this.loading.set(false);
      },
      error: err => {
        this.error.set(this.errorViewModel(err, 'platform.news.list'));
        this.loading.set(false);
      },
    });
  }

  newInternal(): void {
    this.editing.set(null);
    this.form.reset({
      title: '',
      content: '',
      sourceUrl: '',
      status: 'DRAFT',
      expiresAfterDays: DEFAULT_EXPIRES_AFTER_DAYS,
      targetSurfaces: ['PUBLIC_HOME'],
    });
    this.showForm.set(true);
  }

  edit(item: PublicContentAdminItemView): void {
    if (item.sourceType !== 'INTERNAL') return;
    this.editing.set(item);
    this.form.reset({
      title: item.title,
      content: item.content ?? '',
      sourceUrl: item.sourceUrl ?? '',
      status: item.status,
      expiresAfterDays: this.expiresAfterDays(item.expiresAt),
      targetSurfaces: item.targetSurfaces?.length ? item.targetSurfaces : ['PUBLIC_HOME'],
    });
    this.showForm.set(true);
  }

  cancelForm(): void {
    this.showForm.set(false);
    this.editing.set(null);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const now = new Date();
    this.saving.set(true);
    this.actionError.set(null);
    this.actionNotice.set(null);
    this.api.upsertNews({
      id: this.editing()?.id ?? null,
      title: value.title.trim(),
      content: value.content.trim() || null,
      sourceUrl: value.sourceUrl.trim() || null,
      status: value.status,
      targetSurfaces: value.targetSurfaces,
      publishedAt: value.status === 'PUBLISHED'
        ? (this.editing()?.publishedAt ?? now.toISOString())
        : null,
      expiresAt: this.resolveExpiresAt(value.expiresAfterDays, now),
    }, { suppressShellFeedback: true }).subscribe({
      next: () => {
        this.saving.set(false);
        this.showForm.set(false);
        this.load();
        this.actionNotice.set({
          title: 'Actualité enregistrée',
          message: value.title.trim(),
        });
      },
      error: err => {
        this.saving.set(false);
        this.actionError.set(this.errorViewModel(err, 'platform.news.save'));
      },
    });
  }

  changeStatus(item: PublicContentAdminItemView, status: PublicContentStatus): void {
    this.saving.set(true);
    this.actionError.set(null);
    this.actionNotice.set(null);
    this.api.changeNewsStatus(item.id, status, { suppressShellFeedback: true }).subscribe({
      next: () => {
        this.saving.set(false);
        this.load();
        this.actionNotice.set({
          title: 'Statut mis à jour',
          message: this.statusLabel(status),
        });
      },
      error: err => {
        this.saving.set(false);
        this.actionError.set(this.errorViewModel(err, 'platform.news.status'));
      },
    });
  }

  hide(item: PublicContentAdminItemView): void {
    this.saving.set(true);
    this.actionError.set(null);
    this.actionNotice.set(null);
    this.api.hideNews(item.id, { suppressShellFeedback: true }).subscribe({
      next: () => {
        this.saving.set(false);
        this.load();
        this.actionNotice.set({
          title: 'Actualité masquée',
          message: item.title,
        });
      },
      error: err => {
        this.saving.set(false);
        this.actionError.set(this.errorViewModel(err, 'platform.news.hide'));
      },
    });
  }

  forceRefresh(): void {
    this.saving.set(true);
    this.actionError.set(null);
    this.actionNotice.set(null);
    this.api.forceRefreshNews({ suppressShellFeedback: true }).subscribe({
      next: () => {
        this.saving.set(false);
        this.load();
        this.actionNotice.set({
          title: 'Flux RSS rafraîchi',
          message: 'Les actualités externes ont été synchronisées.',
        });
      },
      error: err => {
        this.saving.set(false);
        this.actionError.set(this.errorViewModel(err, 'platform.news.refresh'));
      },
    });
  }

  statusTone(status: PublicContentStatus): AdminStatusTone {
    if (status === 'PUBLISHED') return 'success';
    if (status === 'ARCHIVED') return 'neutral';
    return 'warning';
  }

  statusLabel(status: PublicContentStatus): string {
    return {
      DRAFT: 'Brouillon',
      PUBLISHED: 'Publié',
      ARCHIVED: 'Archivé',
    }[status];
  }

  sourceLabel(item: PublicContentAdminItemView): string {
    return item.sourceType === 'INTERNAL' ? 'Interne' : 'RSS externe';
  }

  surfaceLabel(surface: PublicContentSurface): string {
    return {
      PUBLIC_HOME: 'Public',
      TENANT_ADMIN_DASHBOARD: 'Admin tenant',
      PLATFORM_ADMIN_DASHBOARD: 'Superadmin',
      POS_DASHBOARD: 'POS',
    }[surface];
  }

  durationLabel(days: number): string {
    if (days <= 0) return 'Sans expiration';
    if (days === 1) return '1 jour';
    return `${days} jours`;
  }

  private resolveExpiresAt(days: number, from: Date): string | null {
    if (!Number.isFinite(days) || days <= 0) return null;
    const expiresAt = new Date(from);
    expiresAt.setDate(expiresAt.getDate() + days);
    return expiresAt.toISOString();
  }

  private expiresAfterDays(raw: string | null): number {
    if (!raw) return DEFAULT_EXPIRES_AFTER_DAYS;
    const expiresAt = new Date(raw).getTime();
    if (!Number.isFinite(expiresAt)) return DEFAULT_EXPIRES_AFTER_DAYS;
    const days = Math.ceil((expiresAt - Date.now()) / 86_400_000);
    return days > 0 ? days : DEFAULT_EXPIRES_AFTER_DAYS;
  }

  private errorViewModel(err: unknown, source: string): ErrorViewModel {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (problem) {
      const normalized = webAppErrorFromProblemDetail(problem, source, 'page');
      const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
      return toErrorViewModel(normalized, copy);
    }

    return {
      title: this.translate.instant('common.errors.fallback.title'),
      message: this.translate.instant('common.errors.fallback.message'),
      severity: 'error',
    };
  }
}
