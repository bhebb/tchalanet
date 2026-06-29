import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { forkJoin, Observable, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '../../../../../../core/api/error-feedback-copy';
import { AdminPageShellComponent } from '../../../../shared/admin-ui/admin-page-shell.component';
import { AdminSectionCardComponent } from '../../../../shared/admin-ui/admin-section-card.component';
import {
  AdminSectionErrorTargetDirective,
  AdminSectionTargetError,
} from '../../../../shared/admin-ui/admin-section-error-target.directive';
import { PlatformOpsApi } from '../../data-access/platform-ops-api.service';
import { PlatformTenantsApi } from '../../../tenants/data-access/platform-tenants-api.service';

interface OpsOverviewState {
  provisionalResults: number | null;
  lowQualityResults: number | null;
  openDraws: number | null;
  jobs: number | null;
  disabledGates: number | null;
  cacheRegions: number | null;
  criticalCaches: number | null;
}

const OPS_RESULTS_TARGET = 'platform.ops.overview.results';
const OPS_DRAWS_TARGET = 'platform.ops.overview.draws';
const OPS_JOBS_TARGET = 'platform.ops.overview.jobs';
const OPS_CACHE_TARGET = 'platform.ops.overview.cache';
const LOCAL_ERROR_OPTIONS = { suppressShellFeedback: true } as const;

@Component({
  selector: 'tch-platform-ops-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    AdminSectionErrorTargetDirective,
    TchErrorPanel,
    TchLoading,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './platform-ops.page.html',
  styleUrls: ['./platform-ops.page.scss'],
})
export class PlatformOpsPage implements OnInit {
  private readonly api = inject(PlatformOpsApi);
  private readonly tenantsApi = inject(PlatformTenantsApi);
  private readonly translate = inject(TranslateService);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly overview = signal<OpsOverviewState | null>(null);
  readonly sectionErrors = signal<readonly AdminSectionTargetError[]>([]);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.sectionErrors.set([]);

    this.resolveDefaultDrawTenantId().pipe(switchMap(defaultDrawTenantId => forkJoin({
      provisionalResults: this.api.listDrawResults(
        { status: 'PROVISIONAL', page: 0, size: 1 },
        LOCAL_ERROR_OPTIONS,
      ).pipe(
        map(page => page.totalElements),
        catchError((err: unknown) => this.sectionFallback(err, OPS_RESULTS_TARGET, null)),
      ),
      lowQualityResults: this.api.listDrawResults(
        { quality: 'SUSPECT', page: 0, size: 1 },
        LOCAL_ERROR_OPTIONS,
      ).pipe(
        map(page => page.totalElements),
        catchError((err: unknown) => this.sectionFallback(err, OPS_RESULTS_TARGET, null)),
      ),
      openDraws: defaultDrawTenantId
        ? this.api.listDraws({
            status: 'OPEN',
            from: relativeIsoDate(-1),
            to: todayIsoDate(),
            page: 0,
            size: 1,
            suppressShellFeedback: true,
          }, defaultDrawTenantId).pipe(
            map(page => page.totalElements),
            catchError((err: unknown) => this.sectionFallback(err, OPS_DRAWS_TARGET, null)),
          )
        : of(null),
      jobs: this.api.listJobs(LOCAL_ERROR_OPTIONS).pipe(
        map(jobs => jobs.length),
        catchError((err: unknown) => this.sectionFallback(err, OPS_JOBS_TARGET, null)),
      ),
      disabledGates: this.api.listGates(undefined, true).pipe(
        map(gates => Object.values(gates).filter(enabled => !enabled).length),
        catchError((err: unknown) => this.sectionFallback(err, OPS_JOBS_TARGET, null)),
      ),
      cache: this.api.listCaches(LOCAL_ERROR_OPTIONS).pipe(
        map(caches => ({
          regions: caches.length,
          critical: caches.filter(cache => criticalCache(cache.cacheName)).length,
        })),
        catchError((err: unknown) => this.sectionFallback(err, OPS_CACHE_TARGET, { regions: null, critical: null })),
      ),
    }))).subscribe({
      next: result => {
        this.overview.set({
          provisionalResults: result.provisionalResults,
          lowQualityResults: result.lowQualityResults,
          openDraws: result.openDraws,
          jobs: result.jobs,
          disabledGates: result.disabledGates,
          cacheRegions: result.cache.regions,
          criticalCaches: result.cache.critical,
        });
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Vue d’ensemble indisponible.');
        this.loading.set(false);
      },
    });
  }

  valueOrDash(value: number | null): string {
    return value === null ? '—' : String(value);
  }

  private resolveDefaultDrawTenantId(): Observable<string | null> {
    return this.tenantsApi.listTenants(
      { q: DEFAULT_DRAW_TENANT_CODE, page: 0, size: 10, status: 'ACTIVE' },
      LOCAL_ERROR_OPTIONS,
    ).pipe(
      map(page => {
        const tenants = page.items ?? [];
        const tenant = tenants.find(item => item.code?.toLowerCase() === DEFAULT_DRAW_TENANT_CODE) ?? tenants[0] ?? null;
        return tenant?.id ?? tenant?.tenantId ?? null;
      }),
      catchError((err: unknown) => this.sectionFallback(err, OPS_DRAWS_TARGET, null)),
    );
  }

  private sectionFallback<T>(err: unknown, target: string, fallback: T): Observable<T> {
    this.setSectionError(this.sectionErrorFromUnknown(err, target));
    return of(fallback);
  }

  private sectionErrorFromUnknown(err: unknown, target: string): AdminSectionTargetError {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (problem) {
      const normalized = webAppErrorFromProblemDetail(problem, target, 'section');
      const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
      return {
        target,
        severity: normalized.severity,
        title: copy.title,
        message: copy.message,
      };
    }

    return {
      target,
      severity: 'warn',
      title: this.translate.instant('common.errors.categories.service_unavailable.title'),
      message: this.translate.instant('common.errors.categories.service_unavailable.message'),
    };
  }

  private setSectionError(error: AdminSectionTargetError): void {
    this.sectionErrors.update(errors => [
      ...errors.filter(item => item.target !== error.target),
      error,
    ]);
  }
}

function todayIsoDate(): string {
  return relativeIsoDate(0);
}

function relativeIsoDate(offsetDays: number): string {
  const now = new Date();
  now.setDate(now.getDate() + offsetDays);
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function criticalCache(cacheName: string): boolean {
  const name = cacheName.toLowerCase();
  return ['access', 'auth', 'permission', 'role', 'tenant', 'plan', 'pricing', 'odds', 'batch', 'job', 'gate']
    .some(part => name.includes(part));
}

const DEFAULT_DRAW_TENANT_CODE = 'tchalanet';
