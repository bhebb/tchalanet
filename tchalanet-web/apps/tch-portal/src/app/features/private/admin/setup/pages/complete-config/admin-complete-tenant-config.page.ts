import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { webAppErrorFromNotice, webAppErrorFromProblemDetail } from '@tch/api';
import type { ApiResponse, ProblemDetail } from '@tch/api';
import { TchLoading, TchErrorPanel, TchSectionError } from '@tch/ui/components';

import { resolveErrorFeedbackCopy } from '../../../../../../core/api/error-feedback-copy';
import { AdminPageShellComponent } from '../../../../shared/admin-ui/admin-page-shell.component';
import type { AdminSectionTargetError } from '../../../../shared/admin-ui/admin-section-error-target.directive';
import {
  AdminOverviewApiService,
  TenantAdminOverviewView,
  ReadinessSection,
  TenantSetupView,
} from '../../../admin-overview-api.service';

const REQUIRED_SETUP_SECTION_IDS = [
  'identity',
  'address',
  'games_pricing',
  'draws',
  'generated_draws',
  'promotions',
  'seller_terminals',
] as const;

type PageState = 'loading' | 'ready' | 'error';

@Component({
  selector: 'tch-admin-complete-tenant-config-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    TranslatePipe,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    AdminPageShellComponent,
    TchLoading,
    TchErrorPanel,
    TchSectionError,
  ],
  templateUrl: './admin-complete-tenant-config.page.html',
  styleUrls: ['./admin-complete-tenant-config.page.scss'],
})
export class AdminCompleteTenantConfigPage implements OnInit {
  private readonly api = inject(AdminOverviewApiService);
  private readonly translate = inject(TranslateService);

  readonly pageState = signal<PageState>('loading');
  readonly pageError = signal<string | null>(null);
  readonly overview = signal<TenantAdminOverviewView | null>(null);
  readonly sectionErrors = signal<readonly AdminSectionTargetError[]>([]);

  readonly setup = computed<TenantSetupView | null>(() => this.overview()?.setup ?? null);
  readonly header = computed(() => this.overview()?.header ?? null);

  readonly requiredTotalCount = computed(() => this.setup()?.totalSteps || REQUIRED_SETUP_SECTION_IDS.length);
  readonly requiredCompletedCount = computed(() =>
    this.setup()?.completedSteps ??
    REQUIRED_SETUP_SECTION_IDS.filter(id => this.sectionMap().get(id)?.status === 'READY').length,
  );
  readonly progressPct = computed(() =>
    Math.round((this.requiredCompletedCount() / this.requiredTotalCount()) * 100),
  );

  readonly sectionMap = computed<Map<string, ReadinessSection>>(() => {
    const sections = this.overview()?.sections ?? [];
    return new Map(sections.map(s => [s.id, s]));
  });

  readonly canCreateSellerTerminal = computed(() => this.setup()?.canCreateSellerTerminal ?? false);

  readonly setupCards = computed(() => {
    const map = this.sectionMap();
    return [
      this.cardOf(map, 'identity',      'admin.setup.section.identity',      'verified_user',   null),
      this.cardOf(map, 'address',       'admin.setup.section.address',       'location_on',     null),
      this.cardOf(map, 'games_pricing', 'admin.setup.section.games',         'casino',          '/app/admin/games-pricing'),
      this.cardOf(map, 'draws',         'admin.setup.section.drawChannels',  'event_repeat',        '/app/admin/draw-channels'),
      this.cardOf(map, 'theme',         'admin.setup.section.theme',         'palette',         '/app/admin/appearance'),
      this.cardOf(map, 'promotions',    'admin.setup.section.promotions',    'redeem',          '/app/admin/promotions'),
    ];
  });

  readonly loading = computed(() => this.pageState() === 'loading');
  readonly error = computed(() => this.pageState() === 'error' ? this.pageError() : null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.pageState.set('loading');
    this.pageError.set(null);
    this.sectionErrors.set([]);
    this.api.getOverviewResponse().subscribe({
      next: response => {
        this.overview.set(response.data);
        this.sectionErrors.set(this.sectionErrorsFromResponse(response));
        this.pageState.set('ready');
      },
      error: (err: unknown) => {
        const problem = (err as { error?: ProblemDetail })?.error;
        this.pageError.set(this.pageErrorTitle(problem));
        this.pageState.set('error');
      },
    });
  }

  sectionStatus(id: string): 'READY' | 'MISSING' | 'UNKNOWN' {
    const s = this.sectionMap().get(id);
    return (s?.status as 'READY' | 'MISSING' | 'UNKNOWN') ?? 'UNKNOWN';
  }

  statusIcon(status: string): string {
    if (status === 'READY') return 'check_circle';
    if (status === 'MISSING') return 'radio_button_unchecked';
    return 'help_outline';
  }

  statusClass(status: string): string {
    if (status === 'READY') return 'status--ready';
    if (status === 'MISSING') return 'status--missing';
    return 'status--unknown';
  }

  isBlocking(id: string): boolean {
    return this.setup()?.blockingSteps?.includes(id.toUpperCase()) ?? false;
  }

  sectionError(target: string): AdminSectionTargetError | null {
    return this.sectionErrors().find(error => error.target === target) ?? null;
  }

  private cardOf(
    map: Map<string, ReadinessSection>,
    id: string,
    labelKey: string,
    icon: string,
    route: string | null,
  ) {
    const s = map.get(id);
    return { id, labelKey, icon, route: route ?? s?.route ?? null, status: s?.status ?? 'UNKNOWN' };
  }

  private sectionErrorsFromResponse(
    response: ApiResponse<TenantAdminOverviewView>,
  ): readonly AdminSectionTargetError[] {
    return response.notices
      .map(notice => webAppErrorFromNotice(notice, response.trace, 'admin.setup.overview', 'section'))
      .filter(error => error.surface === 'section' && !!error.target)
      .map(error => {
        const copy = resolveErrorFeedbackCopy(error, key => this.translate.instant(key));
        return {
          target: error.target,
          severity: error.severity,
          title: copy.title,
          message: copy.message,
        } satisfies AdminSectionTargetError;
      });
  }

  private pageErrorTitle(problem: ProblemDetail | undefined): string {
    if (!problem) return this.translate.instant('admin.setup.error.load');

    const normalized = webAppErrorFromProblemDetail(problem, 'admin.setup.overview', 'page');
    return resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key)).title;
  }
}
