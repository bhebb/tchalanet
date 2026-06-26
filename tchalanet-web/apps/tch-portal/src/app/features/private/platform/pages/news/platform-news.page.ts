import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';

import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { AdminCrudShellComponent } from '../../../shared/admin-ui/admin-crud-shell.component';
import { AdminEmptyStateComponent } from '../../../shared/admin-ui/admin-empty-state.component';
import { AdminPageShellComponent } from '../../../shared/admin-ui/admin-page-shell.component';
import {
  AdminStatusPillComponent,
  AdminStatusTone,
} from '../../../shared/admin-ui/admin-status-pill.component';
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
  private readonly snackBar = inject(MatSnackBar);

  readonly surfaces = SURFACES;
  readonly displayedColumns = ['publishedAt', 'source', 'title', 'surfaces', 'expiresAt', 'status', 'actions'];

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
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
    this.api.listNews().subscribe({
      next: items => {
        this.items.set(items ?? []);
        this.loading.set(false);
      },
      error: err => {
        this.error.set(this.errorMessage(err));
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
    }).subscribe({
      next: () => {
        this.saving.set(false);
        this.showForm.set(false);
        this.snackBar.open('Actualité enregistrée.', 'OK', { duration: 3000 });
        this.load();
      },
      error: err => {
        this.saving.set(false);
        this.snackBar.open(this.errorMessage(err), 'OK', { duration: 5000 });
      },
    });
  }

  changeStatus(item: PublicContentAdminItemView, status: PublicContentStatus): void {
    this.saving.set(true);
    this.api.changeNewsStatus(item.id, status).subscribe({
      next: () => {
        this.saving.set(false);
        this.snackBar.open('Statut mis à jour.', 'OK', { duration: 3000 });
        this.load();
      },
      error: err => {
        this.saving.set(false);
        this.snackBar.open(this.errorMessage(err), 'OK', { duration: 5000 });
      },
    });
  }

  hide(item: PublicContentAdminItemView): void {
    this.saving.set(true);
    this.api.hideNews(item.id).subscribe({
      next: () => {
        this.saving.set(false);
        this.snackBar.open('Actualité masquée.', 'OK', { duration: 3000 });
        this.load();
      },
      error: err => {
        this.saving.set(false);
        this.snackBar.open(this.errorMessage(err), 'OK', { duration: 5000 });
      },
    });
  }

  forceRefresh(): void {
    this.saving.set(true);
    this.api.forceRefreshNews().subscribe({
      next: () => {
        this.saving.set(false);
        this.snackBar.open('Flux RSS rafraîchi.', 'OK', { duration: 3000 });
        this.load();
      },
      error: err => {
        this.saving.set(false);
        this.snackBar.open(this.errorMessage(err), 'OK', { duration: 5000 });
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

  private errorMessage(err: unknown): string {
    return (err as { error?: { title?: string; detail?: string } })?.error?.title
      ?? (err as { error?: { detail?: string } })?.error?.detail
      ?? 'Erreur de chargement.';
  }
}
