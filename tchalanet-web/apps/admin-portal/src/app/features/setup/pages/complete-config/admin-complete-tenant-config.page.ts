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
  AdminOverviewApiService,
  TenantAdminOverviewView,
  ReadinessSection,
  TenantSetupView,
} from '../../../business-profile/data-access/admin-overview-api.service';
import {
  AdminSubscriptionApi,
  SubscriptionView,
} from '../../../subscription/data-access/admin-subscription-api.service';
import {
  TenantConfigApiService,
  tenantMaryajGratisEnabled,
} from '../../data-access/tenant-config-api.service';
import {
  SetupChecklistBadgeKind,
  SetupChecklistBodyVariant,
  SetupChecklistCardComponent,
  SetupChecklistStatus,
} from '../../components/setup-checklist-card/setup-checklist-card.component';
import { SetupProgressHeaderComponent } from '../../components/setup-progress-header/setup-progress-header.component';
import { SetupSellerTerminalCardComponent } from '../../components/setup-seller-terminal-card/setup-seller-terminal-card.component';

// Mirrors TenantReadinessAssembler.REQUIRED_STEP_GROUPS (tchalanet-server) — identity+address
// count as one group since they're a single merged card here. generated_draws is required too:
// configured channels/games alone aren't enough to sell without an actual generated Draw.
// settings counts toward the percentage (its card is badged "required") without being blocking.
const REQUIRED_STEP_GROUPS: readonly (readonly string[])[] = [
  ['identity', 'address'],
  ['settings'],
  ['games_pricing'],
  ['draws'],
  ['generated_draws'],
];

type PageState = 'loading' | 'ready' | 'error';

interface SetupChecklistCardViewModel {
  readonly id: string;
  readonly icon: string;
  readonly titleKey: string;
  readonly status: SetupChecklistStatus;
  readonly badgeKind: SetupChecklistBadgeKind;
  readonly body: string;
  readonly bodyVariant: SetupChecklistBodyVariant;
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
  private readonly api = inject(AdminOverviewApiService);
  private readonly subscriptionApi = inject(AdminSubscriptionApi);
  private readonly tenantConfigApi = inject(TenantConfigApiService);
  private readonly translate = inject(TranslateService);

  readonly pageState = signal<PageState>('loading');
  readonly pageError = signal<SetupPageErrorViewModel | null>(null);
  readonly overview = signal<TenantAdminOverviewView | null>(null);
  readonly sectionErrors = signal<readonly AdminSectionTargetError[]>([]);
  readonly subscription = signal<SubscriptionView | null>(null);
  readonly maryajGratisEnabled = signal(true);

  readonly setup = computed<TenantSetupView | null>(() => this.overview()?.setup ?? null);
  readonly header = computed(() => this.overview()?.header ?? null);

  readonly requiredTotalCount = computed(() => this.setup()?.totalSteps || REQUIRED_STEP_GROUPS.length);
  readonly requiredCompletedCount = computed(() =>
    this.setup()?.completedSteps ??
    REQUIRED_STEP_GROUPS.filter(group => group.every(id => this.sectionMap().get(id)?.status === 'READY')).length,
  );
  readonly progressPct = computed(() =>
    Math.round((this.requiredCompletedCount() / this.requiredTotalCount()) * 100),
  );

  readonly sectionMap = computed<Map<string, ReadinessSection>>(() => {
    const sections = this.overview()?.sections ?? [];
    return new Map(sections.map(s => [s.id, s]));
  });

  readonly canCreateSellerTerminal = computed(() => this.setup()?.canCreateSellerTerminal ?? false);

  readonly terminalBlockingSteps = computed<readonly string[]>(() => {
    if (this.canCreateSellerTerminal()) return [];

    const requiredMissing = this.setupCards()
      .filter(card => card.badgeKind !== 'optional' && card.status !== 'READY')
      .map(card => card.id);
    const backendBlocking = (this.setup()?.blockingSteps ?? []).map(step => step.toLowerCase());

    return this.normalizeBlockingSteps([...requiredMissing, ...backendBlocking]);
  });

  readonly setupCards = computed<readonly SetupChecklistCardViewModel[]>(() => {
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
    const settingsStatus: SetupChecklistStatus = this.sectionStatus('settings');
    const settingsTarget = this.settingsTarget();
    const subscription = this.subscription();

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
        id: 'settings',
        icon: 'tune',
        titleKey: 'admin.setup.section.settings',
        // Real check now (readiness section "settings"): backend evaluates tenant settings
        // structurally and returns stable missing-reason codes.
        status: settingsStatus,
        badgeKind: 'required',
        body: this.translate.instant('admin.setup.section.settingsDesc'),
        bodyVariant: 'default',
        ctaKey: 'admin.setup.section.settingsCta',
        route: settingsTarget.route,
        queryParams: { from: 'setup' },
        fragment: settingsTarget.fragment,
        emphasizeMissing: true,
        sectionErrorTargets: ['admin.setup.settings'],
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
        id: 'subscription',
        icon: 'workspace_premium',
        titleKey: 'admin.setup.section.subscription',
        // No readiness section for subscription (not a config-completeness domain) — status
        // reflects whether a plan is actually applied, from GET /tenant/subscription.
        status: subscription?.status === 'ACTIVE' || subscription?.status === 'TRIAL'
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
        this.pageError.set(adminSetupPageError(problem, key => this.translate.instant(key)));
        this.pageState.set('error');
      },
    });

    // Optional/informational card — its own call so a subscription hiccup never blocks the checklist.
    this.subscriptionApi.get({ suppressShellFeedback: true }).subscribe({
      next: subscription => this.subscription.set(subscription),
      error: () => this.subscription.set(null),
    });

    this.tenantConfigApi.getTenantConfig({ suppressShellFeedback: true }).subscribe({
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
    return this.sectionErrors().filter(error => error.target != null && targets.includes(error.target));
  }

  private settingsTarget(): { route: string; fragment?: string } {
    const issue = this.sectionMap().get('settings')?.issues?.find(item => item.messageKey?.startsWith('settings.'));
    const reason = issue?.messageKey ?? '';
    if (reason.startsWith('settings.print.')) {
      return { route: '/app/admin/company/settings/config', fragment: 'print' };
    }
    if (reason.startsWith('settings.send.')) {
      return { route: '/app/admin/company/settings/config', fragment: 'send' };
    }
    if (reason.startsWith('settings.calendar.')) {
      return { route: '/app/admin/company/settings/config', fragment: 'calendar' };
    }
    if (reason.startsWith('settings.locale.')) {
      return { route: '/app/admin/company/settings/config', fragment: 'languages' };
    }
    if (reason.startsWith('settings.identity.') || reason.startsWith('settings.defaults.')) {
      return { route: '/app/admin/company/settings/config' };
    }
    return { route: '/app/admin/company/settings/config' };
  }

  private addressLabel(addr: NonNullable<TenantAdminOverviewView['header']['address']>): string {
    return [
      [addr.line1, addr.line2].filter(Boolean).join(' · '),
      [addr.city, addr.region].filter(Boolean).join(', '),
      [addr.postalCode, addr.country].filter(Boolean).join(' '),
    ].filter(Boolean).join(' · ');
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
      .map(notice => webAppErrorFromNotice(notice, response.trace, 'admin.setup.overview', 'section'))
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
