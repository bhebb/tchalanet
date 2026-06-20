import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { TchErrorPanel, TchLoading } from '@tch/ui/components';

import { AdminEmptyStateComponent } from '../../../../private/shared/admin-ui/admin-empty-state.component';
import { AdminPageShellComponent } from '../../../../private/shared/admin-ui/admin-page-shell.component';
import { AdminSectionCardComponent } from '../../../../private/shared/admin-ui/admin-section-card.component';
import {
  AdminStatusPillComponent,
  AdminStatusTone,
} from '../../../../private/shared/admin-ui/admin-status-pill.component';
import { PlatformTenantsApi, TenantSummaryView } from '../../../platform-tenants-api.service';
import { TenantProvisioningResultView } from '../../../platform-provisioning-api.service';

type ProblemLike = { title?: string; detail?: string; traceId?: string; errorId?: string; requestId?: string };

@Component({
  selector: 'tch-platform-tenant-detail-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    RouterLink,
    DatePipe,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    AdminEmptyStateComponent,
    AdminStatusPillComponent,
    TchLoading,
    TchErrorPanel,
    MatButtonModule,
  ],
  template: `
    <tch-admin-page-shell
      [title]="title()"
      [description]="'platform.tenants.detail.description' | translate"
    >
      <div actions>
        <a mat-button routerLink="/app/platform/tenants">
          <span class="material-symbols-outlined" aria-hidden="true">arrow_back</span>
          {{ 'common.back' | translate }}
        </a>
      </div>

      @if (loading()) {
        <tch-loading [label]="'platform.tenants.detail.loading' | translate" />
      } @else if (errorTitle()) {
        <tch-error-panel
          [title]="errorTitle()!"
          [message]="errorDetail() ?? ''"
          [showRetry]="true"
          [retryLabel]="'common.retry' | translate"
          (retry)="load()"
        />
        @if (traceId()) {
          <p class="tenant-detail__trace">{{ 'common.traceId' | translate }}: {{ traceId() }}</p>
        }
      } @else if (provisionResult()) {
        <tch-admin-section-card [title]="'platform.tenants.detail.createdTitle' | translate" icon="check_circle">
          <dl class="tenant-detail__facts">
            <dt>{{ 'platform.tenants.column.code' | translate }}</dt>
            <dd>{{ provisionResult()!.tenantCode }}</dd>
            <dt>{{ 'platform.tenants.column.profile' | translate }}</dt>
            <dd>{{ profileLabel(provisionResult()!.profile) }}</dd>
            <dt>{{ 'platform.tenants.column.primaryAdmin' | translate }}</dt>
            <dd>{{ provisionResult()!.initialAdminUserId || ('common.not_available' | translate) }}</dd>
            <dt>{{ 'platform.tenants.column.readiness' | translate }}</dt>
            <dd>
              <tch-admin-status-pill
                [label]="provisionResult()!.readiness.status"
                [tone]="readinessTone(provisionResult()!.readiness.status)"
              />
            </dd>
          </dl>

          @if (domainEntries().length > 0) {
            <h2 class="tenant-detail__section-title">{{ 'platform.tenants.detail.domainStatuses' | translate }}</h2>
            <dl class="tenant-detail__facts">
              @for (entry of domainEntries(); track entry.key) {
                <dt>{{ provisioningDomainLabel(entry.key) }}</dt>
                <dd>{{ provisioningStatusLabel(entry.value) }}</dd>
              }
            </dl>
          }

          @if (provisionResult()!.nextSteps.length > 0) {
            <h2 class="tenant-detail__section-title">{{ 'platform.tenants.detail.nextSteps' | translate }}</h2>
            <ul class="tenant-detail__list">
              @for (step of provisionResult()!.nextSteps; track step) {
                <li>{{ nextStepLabel(step) }}</li>
              }
            </ul>
          }
        </tch-admin-section-card>
      } @else if (tenant()) {
        <tch-admin-section-card [title]="tenant()!.name" icon="business">
          <dl class="tenant-detail__facts">
            <dt>{{ 'platform.tenants.column.code' | translate }}</dt>
            <dd>{{ tenant()!.code }}</dd>
            <dt>{{ 'platform.tenants.column.status' | translate }}</dt>
            <dd>{{ tenant()!.status }}</dd>
            <dt>{{ 'platform.tenants.field.timezone' | translate }}</dt>
            <dd>{{ tenant()!.timezone || ('common.not_available' | translate) }}</dd>
            <dt>{{ 'platform.tenants.field.currency' | translate }}</dt>
            <dd>{{ tenant()!.currency || ('common.not_available' | translate) }}</dd>
            <dt>{{ 'platform.tenants.column.createdAt' | translate }}</dt>
            <dd>{{ tenant()!.createdAt | date: 'medium' }}</dd>
          </dl>
        </tch-admin-section-card>
      } @else {
        <tch-admin-empty-state
          icon="business"
          [title]="'platform.tenants.detail.empty.title' | translate"
          [message]="'platform.tenants.detail.empty.message' | translate"
        />
      }
    </tch-admin-page-shell>
  `,
  styles: [
    `
      .tenant-detail__facts {
        display: grid;
        grid-template-columns: minmax(8rem, max-content) 1fr;
        gap: 0.625rem 1rem;
        margin: 0;
      }

      .tenant-detail__facts dt {
        color: var(--tch-color-on-surface-variant);
        font-size: 0.8125rem;
      }

      .tenant-detail__facts dd {
        margin: 0;
        color: var(--tch-color-on-surface);
      }

      .tenant-detail__section-title {
        margin: 1.25rem 0 0.75rem;
        font-size: 1rem;
      }

      .tenant-detail__list {
        margin: 0;
        padding-inline-start: 1.25rem;
      }

      .tenant-detail__trace {
        color: var(--tch-color-on-surface-variant);
        font-size: 0.8125rem;
      }
    `,
  ],
})
export class PlatformTenantDetailPage implements OnInit {
  private readonly api = inject(PlatformTenantsApi);
  private readonly route = inject(ActivatedRoute);
  private readonly translate = inject(TranslateService);

  readonly loading = signal(false);
  readonly errorTitle = signal<string | null>(null);
  readonly errorDetail = signal<string | null>(null);
  readonly traceId = signal<string | null>(null);
  readonly tenant = signal<TenantSummaryView | null>(null);
  readonly provisionResult = signal<TenantProvisioningResultView | null>(null);
  readonly title = computed(() =>
    this.tenant()?.name
      ?? this.provisionResult()?.tenantCode
      ?? this.translate.instant('platform.tenants.detail.title'),
  );
  readonly domainEntries = computed(() =>
    Object.entries(this.provisionResult()?.domainStatuses ?? {}).map(([key, value]) => ({ key, value })),
  );

  ngOnInit(): void {
    const state = history.state as { provisionResult?: TenantProvisioningResultView };
    if (state.provisionResult) {
      this.provisionResult.set(state.provisionResult);
      return;
    }
    this.load();
  }

  load(): void {
    const id = this.route.snapshot.paramMap.get('tenantId');
    if (!id) return;
    this.loading.set(true);
    this.errorTitle.set(null);
    this.errorDetail.set(null);
    this.traceId.set(null);
    this.api.getTenant(id).subscribe({
      next: tenant => {
        this.tenant.set(tenant);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        const pd = ((err as { error?: ProblemLike })?.error ?? {}) as ProblemLike;
        this.errorTitle.set(pd.title ?? this.translate.instant('platform.tenants.detail.error'));
        this.errorDetail.set(pd.detail ?? null);
        this.traceId.set(pd.traceId ?? pd.errorId ?? pd.requestId ?? null);
        this.loading.set(false);
      },
    });
  }

  readinessTone(status: string): AdminStatusTone {
    if (status === 'READY') return 'success';
    if (status === 'BLOCKED') return 'danger';
    if (status === 'INCOMPLETE' || status === 'MISSING') return 'warning';
    return 'neutral';
  }

  profileLabel(profile: string): string {
    const keys: Record<string, string> = {
      MINIMAL: 'platform.tenants.profile.minimal',
      DEFAULT_HAITI_LOTTERY: 'platform.tenants.profile.defaultHaitiLottery',
      DEMO: 'platform.tenants.profile.demo',
    };
    return this.translate.instant(keys[profile] ?? profile);
  }

  provisioningDomainLabel(key: string): string {
    return this.translatedProvisioningKey('domains', key);
  }

  provisioningStatusLabel(key: string): string {
    return this.translatedProvisioningKey('statuses', key);
  }

  nextStepLabel(key: string): string {
    return this.translatedProvisioningKey('nextSteps', key);
  }

  private translatedProvisioningKey(group: string, key: string): string {
    const i18nKey = `platform.tenants.provisioning.${group}.${key}`;
    const translated = this.translate.instant(i18nKey);
    return translated === i18nKey ? key : translated;
  }
}
