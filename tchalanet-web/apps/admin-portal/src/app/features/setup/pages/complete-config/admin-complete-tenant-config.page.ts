import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { webAppErrorFromNotice, webAppErrorFromProblemDetail } from '@tch/api';
import type { ApiResponse, ProblemDetail } from '@tch/api';
import { TchLoading, TchErrorPanel } from '@tch/ui/components';

import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { AdminPageShellComponent } from '@tch/ui/console';
import type { AdminSectionTargetError } from '@tch/ui/console';
import {
  TenantAdminOverviewApiService,
  TenantAdminOverviewView,
  ReadinessSection,
  TenantSetupView,
} from '../../../business-profile/data-access/tenant-admin-overview-api.service';
import {
  AdminSubscriptionApi,
  SubscriptionView,
} from '../../../subscription/data-access/admin-subscription-api.service';
import {
  TenantParametersApiService,
  tenantMaryajGratisEnabled,
} from '../../data-access/tenant-parameters-api.service';
import {
  SetupChecklistBadgeKind,
  SetupChecklistBodyVariant,
  SetupChecklistCardComponent,
  SetupChecklistStatus,
} from '../../components/setup-checklist-card/setup-checklist-card.component';
import { SetupProgressHeaderComponent } from '../../components/setup-progress-header/setup-progress-header.component';
import { SetupSellerTerminalCardComponent } from '../../components/setup-seller-terminal-card/setup-seller-terminal-card.component';

type PageState = 'loading' | 'ready' | 'error';

interface SetupChecklistCardViewModel {
  readonly id: string;
  readonly icon: string;
  readonly titleKey: string;
  readonly status: SetupChecklistStatus;
  readonly badgeKind: SetupChecklistBadgeKind;
  readonly body: string;
  readonly bodyVariant: SetupChecklistBodyVariant;
  readonly statusLabelKey?: string;
  readonly ctaKey: string;
  readonly route: string;
  readonly queryParams?: Record<string, string>;
  readonly fragment?: string;
  readonly emphasizeMissing: boolean;
  readonly sectionErrorTargets: readonly string[];
}

interface SetupPageErrorViewModel {
  readonly severity: 'error' | 'warn' | 'info';
  readonly title: string;
  readonly message: string;
}

interface SetupSettingsTarget {
  readonly route: string;
  readonly fragment?: string;
}

const TENANT_SETTINGS_OVERVIEW_ROUTE = '/app/admin/company/settings';
const TENANT_SETTINGS_RECEIPT_ROUTE = '/app/admin/company/settings/receipt';
const TENANT_SETTINGS_DELIVERY_ROUTE = '/app/admin/company/settings/delivery';
const TENANT_SETTINGS_CALENDAR_ROUTE = '/app/admin/company/settings/calendar';
const TENANT_BUSINESS_PROFILE_ROUTE = '/app/admin/business-profile';

@Component({
  selector: 'tch-admin-complete-tenant-config-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    MatButtonModule,
    MatIconModule,
    AdminPageShellComponent,
    TchLoading,
    TchErrorPanel,
    SetupProgressHeaderComponent,
    SetupChecklistCardComponent,
    SetupSellerTerminalCardComponent,
  ],
  templateUrl: './admin-complete-tenant-config.page.html',
  styleUrls: ['./admin-complete-tenant-config.page.scss'],
})
export class AdminCompleteTenantConfigPage implements OnInit {
  private readonly api = inject(TenantAdminOverviewApiService);
  private readonly subscriptionApi = inject(AdminSubscriptionApi);
  private readonly tenantParametersApi = inject(TenantParametersApiService);
  private readonly translate = inject(TranslateService);

  readonly pageState = signal<PageState>('loading');
  readonly pageError = signal<SetupPageErrorViewModel | null>(null);
  readonly overview = signal<TenantAdminOverviewView | null>(null);
  readonly sectionErrors = signal<readonly AdminSectionTargetError[]>([]);
  readonly subscription = signal<SubscriptionView | null>(null);
  readonly maryajGratisEnabled = signal(true);

  readonly setup = computed<TenantSetupView | null>(() => this.overview()?.setup ?? null);
  readonly header = computed(() => this.overview()?.header ?? null);

  readonly requiredTotalCount = computed(() => this.setup()?.totalSteps ?? 0);
  readonly requiredCompletedCount = computed(() => this.setup()?.completedSteps ?? 0);
  readonly progressPct = computed(() =>
    this.requiredTotalCount() > 0
      ? Math.min(100, Math.round((this.requiredCompletedCount() / this.requiredTotalCount()) * 100))
      : 0,
  );

  readonly sectionMap = computed<Map<string, ReadinessSection>>(() => {
    const sections = this.overview()?.sections ?? [];
    return new Map(sections.map(s => [s.id, s]));
  });

  readonly canCreateSellerTerminal = computed(() => this.setup()?.canCreateSellerTerminal ?? false);

  readonly terminalBlockingSteps = computed<readonly string[]>(() => {
    if (this.canCreateSellerTerminal()) return [];

    const requiredMissing = this.requiredSetupCards()
      .filter(
        card =>
          isReadinessCardBlocking(card.badgeKind) && card.status === 'MISSING',
      )
      .map(card => card.id);
    const backendBlocking = (this.setup()?.blockingSteps ?? []).map(step => step.toLowerCase());

    return this.normalizeBlockingSteps([...requiredMissing, ...backendBlocking]);
  });

  readonly requiredSetupCards = computed<readonly SetupChecklistCardViewModel[]>(() => {
    const h = this.header();
    const identityStatus = this.sectionStatus('identity');
    const addressStatus = this.sectionStatus('address');
    // Merged card: READY only once both identity and address are — same rule as the two
    // separate cards before the merge, just combined into a single status.
    const identityAddressStatus: SetupChecklistStatus =
      identityStatus === 'READY' && addressStatus === 'READY' ? 'READY' : 'MISSING';

    const addr = h?.address;
    const identityAddressBody = addr
      ? this.addressLabel(addr)
      : this.translate.instant('admin.setup.section.addressHint');

    const gamesStatus = this.sectionStatus('games_pricing');
    const drawsStatus = this.sectionStatus('draws');
    // Draw-channels status reflects the channel/schedule/provider config only (the real
    // blocking requirement). checkDraws() on the backend already folds the games×channel
    // matrix completeness in — no separate client-side gating needed.
    const drawChannelsStatus: SetupChecklistStatus = drawsStatus;
    // Real check now (readiness section "generated_draws"): at least one Draw instance
    // actually exists, not just that channels/games are configured.
    const generatedDrawsStatus: SetupChecklistStatus = this.sectionStatus('generated_draws');
    const cards: SetupChecklistCardViewModel[] = [
      {
        id: 'identity_address',
        icon: 'verified_user',
        titleKey: 'admin.setup.section.identityAddress',
        status: identityAddressStatus,
        badgeKind: this.isAnyBlocking(['identity', 'address']) ? 'blocking' : 'required',
        body: identityAddressBody || this.translate.instant('admin.setup.section.identityDesc'),
        bodyVariant: addr ? 'address' : 'hint',
        ctaKey: 'admin.setup.section.identityAddressCta',
        route: '/app/admin/business-profile',
        emphasizeMissing: true,
        sectionErrorTargets: ['admin.setup.identity', 'admin.setup.address'],
      },
      {
        id: 'games_pricing',
        icon: 'casino',
        titleKey: 'admin.setup.section.games',
        status: gamesStatus,
        badgeKind: this.isBlocking('games_pricing') ? 'blocking' : 'required',
        body: this.translate.instant('admin.setup.section.gamesDesc'),
        bodyVariant: 'default',
        ctaKey: 'admin.setup.section.gamesCta',
        route: '/app/admin/games',
        queryParams: { from: 'setup' },
        emphasizeMissing: true,
        sectionErrorTargets: ['admin.setup.games_pricing'],
      },
    ];

    cards.push(
      {
        id: 'draws',
        icon: 'event_repeat',
        titleKey: 'admin.setup.section.drawChannels',
        status: drawChannelsStatus,
        badgeKind: this.isBlocking('draws') ? 'blocking' : 'required',
        body: this.translate.instant('admin.setup.section.drawChannelsDesc'),
        bodyVariant: 'default',
        ctaKey: 'admin.setup.section.drawChannelsCta',
        route: '/app/admin/draw-channels',
        queryParams: { from: 'setup' },
        emphasizeMissing: true,
        sectionErrorTargets: ['admin.setup.draws', 'admin.setup.draw_sales_matrix'],
      },
      {
        id: 'generated_draws',
        icon: 'confirmation_number',
        titleKey: 'admin.setup.section.generatedDraws',
        // Required now: backend blocks canCreateSellerTerminal on this too (checkGeneratedDraws).
        status: generatedDrawsStatus,
        badgeKind: this.isBlocking('generated_draws') ? 'blocking' : 'required',
        body: this.translate.instant('admin.setup.section.generatedDrawsDesc'),
        bodyVariant: 'default',
        ctaKey: 'admin.setup.section.generatedDrawsCta',
        route: '/app/admin/draws',
        emphasizeMissing: true,
        sectionErrorTargets: ['admin.setup.generatedDraws'],
      },
    );

    return cards;
  });

  readonly operationalSetupCards = computed<readonly SetupChecklistCardViewModel[]>(() => {
    const gamesStatus = this.sectionStatus('games_pricing');
    const subscription = this.subscription();
    const settingsTarget = this.settingsTarget();
    const cards: SetupChecklistCardViewModel[] = [
      {
        id: 'pos_settings',
        icon: 'tune',
        titleKey: 'admin.setup.section.posSettings',
        status: this.sectionStatus('settings'),
        badgeKind: 'optional',
        body: this.translate.instant('admin.setup.section.posSettingsDesc'),
        bodyVariant: 'default',
        ctaKey: 'admin.setup.section.posSettingsCta',
        route: settingsTarget.route,
        queryParams: { from: 'setup' },
        fragment: settingsTarget.fragment,
        emphasizeMissing: false,
        sectionErrorTargets: ['admin.setup.settings'],
      },
    ];

    if (this.maryajGratisEnabled()) {
      cards.push({
        id: 'maryaj_gratis',
        icon: 'redeem',
        titleKey: 'admin.setup.section.maryajGratis',
        status: gamesStatus === 'READY' ? 'READY' : 'MISSING',
        badgeKind: 'optional',
        body: this.translate.instant('admin.setup.section.maryajGratisDesc'),
        bodyVariant: 'default',
        ctaKey: 'admin.setup.section.maryajGratisCta',
        route: '/app/admin/maryaj-gratis',
        emphasizeMissing: false,
        sectionErrorTargets: ['admin.setup.maryaj_gratis'],
      });
    }

    cards.push(
      {
        id: 'theme',
        icon: 'palette',
        titleKey: 'admin.setup.section.theme',
        status: this.sectionStatus('theme'),
        badgeKind: 'optional',
        body: this.translate.instant('admin.setup.section.themeDesc'),
        bodyVariant: 'default',
        ctaKey: 'admin.setup.section.themeCta',
        route: '/app/admin/appearance',
        emphasizeMissing: false,
        sectionErrorTargets: ['admin.setup.theme'],
      },
      {
        id: 'commission',
        icon: 'percent',
        titleKey: 'admin.setup.section.commission',
        status: this.sectionStatus('commission'),
        badgeKind: 'optional',
        body: this.translate.instant('admin.setup.section.commissionDesc'),
        bodyVariant: 'default',
        ctaKey: 'admin.setup.section.commissionCta',
        route: '/app/admin/controls/commissions',
        emphasizeMissing: false,
        sectionErrorTargets: ['admin.setup.commission'],
      },
      {
        id: 'limits',
        icon: 'shield',
        titleKey: 'admin.setup.section.limits',
        status: this.sectionStatus('limits'),
        badgeKind: 'optional',
        body: this.translate.instant('admin.setup.section.limitsDesc'),
        bodyVariant: 'default',
        ctaKey: 'admin.setup.section.limitsCta',
        route: '/app/admin/limits',
        emphasizeMissing: false,
        sectionErrorTargets: ['admin.setup.limits'],
      },
      {
        id: 'subscription',
        icon: 'workspace_premium',
        titleKey: 'admin.setup.section.subscription',
        // No readiness section for subscription (not a config-completeness domain) — status
        // reflects whether a plan is actually applied, from GET /tenant/subscription.
        status:
          subscription?.status === 'ACTIVE' || subscription?.status === 'TRIAL'
            ? 'READY'
            : subscription
              ? 'UNKNOWN'
              : 'MISSING',
        badgeKind: 'optional',
        body: subscription
          ? `${subscription.planCode} · ${subscription.status}`
          : this.translate.instant('admin.setup.section.subscriptionNoPlan'),
        bodyVariant: subscription ? 'default' : 'hint',
        ctaKey: 'admin.setup.section.subscriptionCta',
        route: '/app/admin/subscription',
        emphasizeMissing: false,
        sectionErrorTargets: ['admin.setup.subscription'],
      },
    );

    return cards;
  });

  readonly setupCards = computed<readonly SetupChecklistCardViewModel[]>(() => [
    ...this.requiredSetupCards(),
    ...this.operationalSetupCards(),
  ]);

  readonly loading = computed(() => this.pageState() === 'loading');
  readonly error = computed(() => (this.pageState() === 'error' ? this.pageError() : null));

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
        this.pageError.set(adminSetupPageError(problem, key => this.translate.instant(key)));
        this.pageState.set('error');
      },
    });

    // Optional/informational card — its own call so a subscription hiccup never blocks the checklist.
    this.subscriptionApi.get({ suppressShellFeedback: true }).subscribe({
      next: subscription => this.subscription.set(subscription),
      error: () => this.subscription.set(null),
    });

    this.tenantParametersApi.getTenantConfig({ suppressShellFeedback: true }).subscribe({
      next: config => this.maryajGratisEnabled.set(tenantMaryajGratisEnabled(config)),
      error: () => this.maryajGratisEnabled.set(true),
    });
  }

  sectionStatus(id: string): 'READY' | 'MISSING' | 'UNKNOWN' {
    const s = this.sectionMap().get(id);
    return (s?.status as 'READY' | 'MISSING' | 'UNKNOWN') ?? 'UNKNOWN';
  }

  isBlocking(id: string): boolean {
    return this.setup()?.blockingSteps?.includes(id.toUpperCase()) ?? false;
  }

  isAnyBlocking(ids: readonly string[]): boolean {
    return ids.some(id => this.isBlocking(id));
  }

  sectionError(target: string): AdminSectionTargetError | null {
    return this.sectionErrors().find(error => error.target === target) ?? null;
  }

  sectionErrorsFor(targets: readonly string[]): readonly AdminSectionTargetError[] {
    return this.sectionErrors().filter(
      error => error.target != null && targets.includes(error.target),
    );
  }

  private settingsTarget(): SetupSettingsTarget {
    return setupSettingsTarget(this.sectionMap().get('settings'));
  }

  private addressLabel(addr: NonNullable<TenantAdminOverviewView['header']['address']>): string {
    return [
      [addr.line1, addr.line2].filter(Boolean).join(' · '),
      [addr.city, addr.region].filter(Boolean).join(', '),
      [addr.postalCode, addr.country].filter(Boolean).join(' '),
    ]
      .filter(Boolean)
      .join(' · ');
  }

  private normalizeBlockingSteps(steps: readonly string[]): readonly string[] {
    const normalized = steps.map(step => step.toLowerCase());
    const blocksIdentityAddress =
      normalized.includes('identity_address') ||
      normalized.includes('identity') ||
      normalized.includes('address');
    const unique = normalized.filter(step => step !== 'identity' && step !== 'address');

    if (blocksIdentityAddress) {
      unique.unshift('identity_address');
    }

    return [...new Set(unique)];
  }

  private sectionErrorsFromResponse(
    response: ApiResponse<TenantAdminOverviewView>,
  ): readonly AdminSectionTargetError[] {
    return response.notices
      .map(notice =>
        webAppErrorFromNotice(notice, response.trace, 'admin.setup.overview', 'section'),
      )
      .filter(error => error.surface === 'section' && !!error.target)
      .map(error => {
        const view = adminSetupSectionError(error, key => this.translate.instant(key));
        return view satisfies AdminSectionTargetError;
      });
  }
}

export function adminSetupPageError(
  problem: ProblemDetail | undefined,
  translate: (key: string) => string,
): SetupPageErrorViewModel {
  if (!problem) {
    return {
      severity: 'error',
      title: translate('admin.setup.error.load'),
      message: translate('common.errors.fallback.message'),
    };
  }

  const normalized = webAppErrorFromProblemDetail(problem, 'admin.setup.overview', 'page');
  const copy = resolveErrorFeedbackCopy(normalized, translate);
  return {
    severity: normalized.severity,
    title: copy.title,
    message: copy.message,
  };
}

export function adminSetupSectionError(
  error: ReturnType<typeof webAppErrorFromNotice>,
  translate: (key: string) => string,
): AdminSectionTargetError {
  const copy = resolveErrorFeedbackCopy(error, translate);
  return {
    target: error.target,
    severity: error.severity,
    title: copy.title,
    message: copy.message,
  };
}

export function isReadinessCardBlocking(kind: SetupChecklistBadgeKind): boolean {
  return kind === 'required' || kind === 'blocking';
}

export function setupSettingsTarget(
  settings: { readonly issues?: readonly { readonly messageKey?: string | null }[] } | undefined,
): SetupSettingsTarget {
  // Print issues are addressed by the dedicated POS Printing operational card —
  // the main "Paramèt" required card should never land directly on receipt.
  const reason =
    settings?.issues?.find(
      item =>
        item.messageKey?.startsWith('settings.') &&
        !item.messageKey.startsWith('settings.print.'),
    )?.messageKey ?? '';
  return setupSettingsTargetFromReason(reason);
}

export function setupSettingsTargetFromReason(reason: string): SetupSettingsTarget {
  if (reason.startsWith('settings.print.')) {
    return { route: TENANT_SETTINGS_RECEIPT_ROUTE };
  }
  if (reason.startsWith('settings.send.')) {
    return { route: TENANT_SETTINGS_DELIVERY_ROUTE };
  }
  if (reason.startsWith('settings.calendar.')) {
    return { route: TENANT_SETTINGS_CALENDAR_ROUTE };
  }
  if (reason.startsWith('settings.locale.')) {
    return { route: TENANT_BUSINESS_PROFILE_ROUTE };
  }
  return { route: TENANT_SETTINGS_OVERVIEW_ROUTE };
}

export function setupPosPrintingStatus(
  settings: { readonly issues?: readonly { readonly messageKey?: string | null }[] } | undefined,
): SetupChecklistStatus {
  if (!settings) return 'UNKNOWN';

  const hasPrintIssue =
    settings.issues?.some(item => item.messageKey?.startsWith('settings.print.')) ?? false;
  return hasPrintIssue ? 'MISSING' : 'READY';
}

export function setupPosPrintingStatusLabelKey(status: SetupChecklistStatus): string {
  switch (status) {
    case 'READY':
      return 'admin.setup.operationalStatus.configured';
    case 'MISSING':
      return 'admin.setup.operationalStatus.notConfigured';
    default:
      return 'admin.setup.operationalStatus.recommended';
  }
}
