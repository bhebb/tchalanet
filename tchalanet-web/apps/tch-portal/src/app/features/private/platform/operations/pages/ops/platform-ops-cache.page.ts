import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { TranslateService } from '@ngx-translate/core';

import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { TchLoading, TchErrorPanel, TchSectionError } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminPageShellComponent } from '../../../../shared/admin-ui/admin-page-shell.component';
import { AdminEmptyStateComponent } from '../../../../shared/admin-ui/admin-empty-state.component';
import { PlatformOpsApi, CacheView } from '../../data-access/platform-ops-api.service';
import { ClearAllCachesDialog } from '../../components/dialogs/clear-all-caches.dialog';
import { ClearCacheGroupDialog } from '../../components/dialogs/clear-cache-group.dialog';

type OpsCacheGroup = 'plans' | 'catalog' | 'tenant' | 'access' | 'pagemodel' | 'batch' | 'other';

interface OpsCacheRow extends CacheView {
  group: OpsCacheGroup;
  critical: boolean;
}

@Component({
  selector: 'tch-platform-ops-cache-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DecimalPipe,
    AdminPageShellComponent,
    AdminEmptyStateComponent,
    TchLoading,
    TchErrorPanel,
    TchSectionError,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
  ],
  templateUrl: './platform-ops-cache.page.html',
  styleUrls: ['./platform-ops-cache.page.scss'],
})
export class PlatformOpsCachePage implements OnInit {
  private readonly api = inject(PlatformOpsApi);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);

  readonly displayedColumns = ['cacheName', 'group', 'size', 'hitRate', 'lastClearedAt', 'critical', 'actions'];
  readonly loading = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly actionError = signal<ErrorViewModel | null>(null);
  readonly actionNotice = signal<{ title: string; message: string } | null>(null);
  readonly caches = signal<CacheView[]>([]);
  readonly groupedCaches = computed<OpsCacheRow[]>(() =>
    this.caches().map(cache => ({
      ...cache,
      group: cacheGroup(cache.cacheName),
      critical: criticalCache(cache.cacheName),
    })),
  );

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.actionError.set(null);
    this.api.listCaches({ suppressShellFeedback: true }).subscribe({
      next: v => { this.caches.set(v); this.loading.set(false); },
      error: (err: unknown) => {
        this.error.set(this.errorViewModel(err, 'platform.ops.cache.list'));
        this.loading.set(false);
      },
    });
  }

  clearOne(cache: CacheView): void {
    this.actionError.set(null);
    this.actionNotice.set(null);
    this.api.clearCache(cache.cacheName, { suppressShellFeedback: true }).subscribe({
      next: () => {
        this.load();
        this.actionNotice.set({
          title: 'Cache vidé',
          message: cache.cacheName,
        });
      },
      error: (err: unknown) => {
        this.actionError.set(this.errorViewModel(err, 'platform.ops.cache.clear'));
      },
    });
  }

  openClearAll(): void {
    const ref = this.dialog.open(ClearAllCachesDialog, { width: '480px' });
    ref.afterClosed().subscribe(ok => {
      if (ok) this.load();
    });
  }

  openClearPlans(): void {
    this.openClearGroup('plans');
  }

  openClearGroup(group: OpsCacheGroup): void {
    const ref = this.dialog.open(ClearCacheGroupDialog, {
      width: '480px',
      data: {
        group,
        title: `Vider les caches ${group}`,
        description: `Cette action vide uniquement les caches du groupe ${group}.`,
        confirmLabel: `Vider ${group}`,
      },
    });
    ref.afterClosed().subscribe(ok => {
      if (ok) this.load();
    });
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

function cacheGroup(cacheName: string): OpsCacheGroup {
  const name = cacheName.toLowerCase();
  if (name.includes('plan') || name.includes('pricing') || name.includes('odds')) return 'plans';
  if (name.includes('catalog') || name.includes('game') || name.includes('draw-channel')) return 'catalog';
  if (name.includes('tenant')) return 'tenant';
  if (name.includes('access') || name.includes('auth') || name.includes('permission') || name.includes('role')) return 'access';
  if (name.includes('page') || name.includes('pagemodel')) return 'pagemodel';
  if (name.includes('batch') || name.includes('job') || name.includes('gate')) return 'batch';
  return 'other';
}

function criticalCache(cacheName: string): boolean {
  const group = cacheGroup(cacheName);
  return group === 'access' || group === 'tenant' || group === 'plans' || group === 'batch';
}
