import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { TchActionButton, TchCard } from '@tch/ui/components';

interface Benefit {
  readonly icon: string;
  readonly titleKey: string;
  readonly bodyKey: string;
}

interface WorkflowStep {
  readonly num: number;
  readonly titleKey: string;
  readonly bodyKey: string;
}

interface PlanDef {
  readonly id: string;
  readonly titleKey: string;
  readonly priceKey: string;
  readonly bodyKey: string;
  readonly ctaKey: string;
  readonly featured: boolean;
  readonly featureKeys: readonly string[];
}

interface FaqItem {
  readonly qKey: string;
  readonly aKey: string;
  open: boolean;
}

const P = 'public.operator';

const BENEFITS: readonly Benefit[] = [
  { icon: 'qr_code_2',             titleKey: `${P}.benefit_tickets_title`,  bodyKey: `${P}.benefit_tickets_body`  },
  { icon: 'public',                titleKey: `${P}.benefit_verify_title`,   bodyKey: `${P}.benefit_verify_body`   },
  { icon: 'group',                 titleKey: `${P}.benefit_agents_title`,   bodyKey: `${P}.benefit_agents_body`   },
  { icon: 'analytics',             titleKey: `${P}.benefit_results_title`,  bodyKey: `${P}.benefit_results_body`  },
  { icon: 'payments',              titleKey: `${P}.benefit_payments_title`, bodyKey: `${P}.benefit_payments_body` },
  { icon: 'description',           titleKey: `${P}.benefit_reports_title`,  bodyKey: `${P}.benefit_reports_body`  },
];

const STEPS: readonly WorkflowStep[] = [
  { num: 1, titleKey: `${P}.step1_title`, bodyKey: `${P}.step1_body` },
  { num: 2, titleKey: `${P}.step2_title`, bodyKey: `${P}.step2_body` },
  { num: 3, titleKey: `${P}.step3_title`, bodyKey: `${P}.step3_body` },
  { num: 4, titleKey: `${P}.step4_title`, bodyKey: `${P}.step4_body` },
];

const PLANS: readonly PlanDef[] = [
  {
    id: 'trial', featured: false,
    titleKey: `${P}.plan_trial_title`, priceKey: `${P}.plan_trial_price`,
    bodyKey:  `${P}.plan_trial_body`,  ctaKey:   `${P}.plan_trial_cta`,
    featureKeys: [`${P}.feat_limits`, `${P}.feat_verify`, `${P}.feat_results`, `${P}.feat_reports_basic`],
  },
  {
    id: 'essential', featured: false,
    titleKey: `${P}.plan_essential_title`, priceKey: `${P}.plan_essential_price`,
    bodyKey:  `${P}.plan_essential_body`,  ctaKey:   `${P}.plan_essential_cta`,
    featureKeys: [`${P}.feat_sell`, `${P}.feat_verify_qr`, `${P}.feat_access_results`, `${P}.feat_vendor_basic`],
  },
  {
    id: 'network', featured: true,
    titleKey: `${P}.plan_network_title`, priceKey: `${P}.plan_network_price`,
    bodyKey:  `${P}.plan_network_body`,  ctaKey:   `${P}.plan_network_cta`,
    featureKeys: [`${P}.feat_agent_mgmt`, `${P}.feat_reports_full`, `${P}.feat_payments`, `${P}.feat_pos_config`],
  },
  {
    id: 'operator', featured: false,
    titleKey: `${P}.plan_operator_title`, priceKey: `${P}.plan_operator_price`,
    bodyKey:  `${P}.plan_operator_body`,  ctaKey:   `${P}.plan_operator_cta`,
    featureKeys: [`${P}.feat_multi_pos`, `${P}.feat_advanced_perms`, `${P}.feat_reports_consolidated`, `${P}.feat_integration`],
  },
];

@Component({
  selector: 'tch-public-operators-page',
  imports: [TranslatePipe, TchCard, TchActionButton],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="ops">

      <!-- Hero -->
      <section class="ops__hero" aria-label="{{ 'public.operator.hero_aria' | translate }}">
        <div class="ops__container ops__hero-inner">
          <p class="ops__eyebrow">{{ 'public.operator.eyebrow' | translate }}</p>
          <h1 class="ops__hero-title">{{ 'public.operator.hero_title' | translate }}</h1>
          <p class="ops__hero-body">{{ 'public.operator.hero_body' | translate }}</p>
          <div class="ops__hero-ctas">
            <button tch-action class="ops__cta-primary" type="button">
              {{ 'public.operator.cta_demo' | translate }}
            </button>
            <button tch-action class="ops__cta-ghost" type="button">
              {{ 'public.operator.cta_plans' | translate }}
            </button>
          </div>
        </div>
      </section>

      <!-- Benefits -->
      <section class="ops__section ops__benefits">
        <div class="ops__container">
          <div class="ops__section-header">
            <h2>{{ 'public.operator.benefits_title' | translate }}</h2>
            <p>{{ 'public.operator.benefits_body' | translate }}</p>
          </div>
          <div class="ops__benefits-grid">
            @for (b of benefits; track b.icon) {
              <tch-card class="ops__benefit-card">
                <span class="ops__benefit-icon material-symbols-outlined" aria-hidden="true">{{ b.icon }}</span>
                <h3 class="ops__benefit-title">{{ b.titleKey | translate }}</h3>
                <p class="ops__benefit-body">{{ b.bodyKey | translate }}</p>
              </tch-card>
            }
          </div>
        </div>
      </section>

      <!-- Workflow -->
      <section class="ops__section ops__workflow">
        <div class="ops__container">
          <h2 class="ops__section-title-centered">{{ 'public.operator.workflow_title' | translate }}</h2>
          <div class="ops__steps">
            @for (step of steps; track step.num) {
              <div class="ops__step">
                <div class="ops__step-num" aria-hidden="true">{{ step.num }}</div>
                <h3 class="ops__step-title">{{ step.titleKey | translate }}</h3>
                <p class="ops__step-body">{{ step.bodyKey | translate }}</p>
              </div>
            }
          </div>
        </div>
      </section>

      <!-- Plans -->
      <section class="ops__section ops__plans">
        <div class="ops__container">
          <div class="ops__section-header">
            <h2>{{ 'public.operator.plans_title' | translate }}</h2>
            <p>{{ 'public.operator.plans_body' | translate }}</p>
          </div>
          <div class="ops__plans-grid">
            @for (plan of plans; track plan.id) {
              <tch-card class="ops__plan-card" [class.ops__plan-card--featured]="plan.featured">
                @if (plan.featured) {
                  <span class="ops__plan-badge">{{ 'public.operator.plan_badge_recommended' | translate }}</span>
                }
                <h3 class="ops__plan-title">{{ plan.titleKey | translate }}</h3>
                <p class="ops__plan-price">{{ plan.priceKey | translate }}</p>
                <p class="ops__plan-body">{{ plan.bodyKey | translate }}</p>
                <ul class="ops__plan-features" role="list">
                  @for (fk of plan.featureKeys; track fk) {
                    <li class="ops__plan-feature">
                      <span class="material-symbols-outlined ops__plan-check" aria-hidden="true">check_circle</span>
                      {{ fk | translate }}
                    </li>
                  }
                </ul>
                <button tch-action class="ops__plan-cta" [class.ops__plan-cta--featured]="plan.featured" type="button">
                  {{ plan.ctaKey | translate }}
                </button>
              </tch-card>
            }
          </div>
        </div>
      </section>

      <!-- Access control -->
      <section class="ops__section ops__access">
        <div class="ops__container ops__access-inner">
          <div class="ops__access-text">
            <h2>{{ 'public.operator.access_title' | translate }}</h2>
            <p class="ops__access-body">{{ 'public.operator.access_body' | translate }}</p>
            <div class="ops__access-cards">
              @for (item of accessItems; track item.icon) {
                <div class="ops__access-item">
                  <span class="material-symbols-outlined ops__access-item-icon" aria-hidden="true">{{ item.icon }}</span>
                  <strong class="ops__access-item-title">{{ item.titleKey | translate }}</strong>
                  <p class="ops__access-item-body">{{ item.bodyKey | translate }}</p>
                </div>
              }
            </div>
          </div>
          <div class="ops__access-terminal" aria-hidden="true">
            <div class="ops__terminal-dots">
              <span></span><span></span><span></span>
              <span class="ops__terminal-label">system_access_log.v1</span>
            </div>
            <pre class="ops__terminal-code">{{ accessLogSample }}</pre>
          </div>
        </div>
      </section>

      <!-- Demo form -->
      <section class="ops__section ops__form-section">
        <div class="ops__container ops__form-container">
          <tch-card class="ops__form-card">
            <div class="ops__form-header">
              <h2>{{ 'public.operator.form_title' | translate }}</h2>
              <p>{{ 'public.operator.form_body' | translate }}</p>
            </div>
            <form class="ops__form" (ngSubmit)="submitForm()">
              <div class="ops__form-row">
                <div class="ops__field">
                  <label class="ops__label" for="ops-name">{{ 'public.operator.form_name_label' | translate }}</label>
                  <input id="ops-name" class="ops__input" type="text" [placeholder]="'public.operator.form_name_placeholder' | translate" autocomplete="name" />
                </div>
                <div class="ops__field">
                  <label class="ops__label" for="ops-org">{{ 'public.operator.form_org_label' | translate }}</label>
                  <input id="ops-org" class="ops__input" type="text" [placeholder]="'public.operator.form_org_placeholder' | translate" autocomplete="organization" />
                </div>
              </div>
              <div class="ops__form-row">
                <div class="ops__field">
                  <label class="ops__label" for="ops-contact">{{ 'public.operator.form_contact_label' | translate }}</label>
                  <input id="ops-contact" class="ops__input" type="text" [placeholder]="'public.operator.form_contact_placeholder' | translate" autocomplete="email" />
                </div>
                <div class="ops__field">
                  <label class="ops__label" for="ops-city">{{ 'public.operator.form_city_label' | translate }}</label>
                  <input id="ops-city" class="ops__input" type="text" [placeholder]="'public.operator.form_city_placeholder' | translate" autocomplete="address-level2" />
                </div>
              </div>
              <div class="ops__field">
                <label class="ops__label" for="ops-message">{{ 'public.operator.form_message_label' | translate }}</label>
                <textarea id="ops-message" class="ops__input ops__textarea" rows="4" [placeholder]="'public.operator.form_message_placeholder' | translate"></textarea>
              </div>
              <button
                tch-action
                class="ops__form-submit"
                type="submit"
                [disabled]="formState() === 'sending'"
                style="--comp-action-bg: var(--tch-color-primary); --comp-action-fg: var(--tch-color-on-primary);"
              >
                @switch (formState()) {
                  @case ('sending') { {{ 'public.operator.form_sending' | translate }} }
                  @case ('sent') { {{ 'public.operator.form_sent' | translate }} }
                  @default { {{ 'public.operator.form_cta' | translate }} }
                }
              </button>
            </form>
          </tch-card>
        </div>
      </section>

      <!-- FAQ -->
      <section class="ops__section ops__faq">
        <div class="ops__container ops__faq-container">
          <h2 class="ops__section-title-centered">{{ 'public.operator.faq_title' | translate }}</h2>
          <div class="ops__faq-list" role="list">
            @for (item of faqItems; track item.qKey; let i = $index) {
              <div class="ops__faq-item" role="listitem">
                <button
                  class="ops__faq-q"
                  type="button"
                  [attr.aria-expanded]="item.open"
                  [attr.aria-controls]="'faq-' + i"
                  (click)="toggleFaq(i)"
                >
                  <span>{{ item.qKey | translate }}</span>
                  <span class="material-symbols-outlined ops__faq-chevron" [class.ops__faq-chevron--open]="item.open" aria-hidden="true">expand_more</span>
                </button>
                <div [id]="'faq-' + i" class="ops__faq-a" [class.ops__faq-a--open]="item.open">
                  <p>{{ item.aKey | translate }}</p>
                </div>
              </div>
            }
          </div>
        </div>
      </section>

    </div>
  `,
  styles: [`
    @use 'breakpoints' as bp;

    /* ── Layout ── */
    .ops {
      display: flex;
      flex-direction: column;
    }

    .ops__container {
      width: min(100% - 2 * var(--tch-page-margin-mobile, 16px), var(--tch-page-max, 1120px));
      margin-inline: auto;
    }

    .ops__section {
      padding-block: clamp(3rem, 8vw, 5rem);
    }

    .ops__section-header {
      text-align: center;
      margin-bottom: 3rem;
      h2 {
        font-size: var(--tch-font-size-headline-mobile, 1.5rem);
        font-weight: var(--tch-weight-bold, 700);
        color: var(--tch-color-primary);
        margin: 0 0 0.75rem;
      }
      p {
        color: var(--tch-color-on-surface-variant);
        margin: 0;
        max-width: 42ch;
        margin-inline: auto;
      }
    }

    .ops__section-title-centered {
      font-size: var(--tch-font-size-headline-mobile, 1.5rem);
      font-weight: var(--tch-weight-bold, 700);
      color: var(--tch-color-primary);
      text-align: center;
      margin: 0 0 3rem;
    }

    /* ── Hero ── */
    .ops__hero {
      background-color: var(--tch-color-primary);
      background-image: radial-gradient(color-mix(in oklab, var(--tch-color-primary-container) 60%, transparent) 0.5px, transparent 0.5px),
                        radial-gradient(color-mix(in oklab, var(--tch-color-primary-container) 60%, transparent) 0.5px, var(--tch-color-primary) 0.5px);
      background-size: 20px 20px;
      background-position: 0 0, 10px 10px;
      padding-block: clamp(3.5rem, 10vw, 6rem);
      overflow: hidden;
    }

    .ops__hero-inner {
      display: flex;
      flex-direction: column;
      gap: 2rem;
    }

    .ops__hero-content {
      flex: 1;
    }

    .ops__eyebrow {
      font-size: var(--tch-font-size-label-sm, 0.75rem);
      font-weight: var(--tch-weight-extra-bold, 800);
      text-transform: uppercase;
      letter-spacing: 0.08em;
      color: var(--tch-color-accent, #fecb00);
      margin: 0 0 1rem;
    }

    .ops__hero-title {
      font-size: clamp(1.75rem, 5vw, 2.5rem);
      font-weight: var(--tch-weight-extra-bold, 800);
      line-height: 1.2;
      color: #fff;
      margin: 0 0 1.25rem;
    }

    .ops__hero-body {
      color: var(--tch-color-primary-fixed, #e1e0ff);
      font-size: 1rem;
      line-height: 1.6;
      margin: 0 0 2.5rem;
      max-width: 54ch;
    }

    .ops__hero-ctas {
      display: flex;
      flex-wrap: wrap;
      gap: 0.75rem;
    }

    .ops__cta-primary {
      --comp-action-bg: var(--tch-color-accent, #fecb00);
      --comp-action-fg: var(--tch-color-primary, #1a1b4b);
      font-weight: var(--tch-weight-bold, 700);
    }

    .ops__cta-ghost {
      --comp-action-bg: rgb(255 255 255 / 0.12);
      --comp-action-fg: #fff;
      --comp-action-border: rgb(255 255 255 / 0.25);
      border: 1px solid var(--comp-action-border);
      font-weight: var(--tch-weight-bold, 700);
    }

    /* ── Benefits ── */
    .ops__benefits { background: var(--tch-color-background); }

    .ops__benefits-grid {
      display: grid;
      grid-template-columns: 1fr;
      gap: 1rem;
    }

    tch-card.ops__benefit-card {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
      padding: 1.5rem;
    }

    .ops__benefit-icon {
      color: var(--tch-color-primary);
      font-size: 1.75rem;
    }

    .ops__benefit-title {
      font-size: var(--tch-font-size-title-md, 1.125rem);
      font-weight: var(--tch-weight-semibold, 600);
      color: var(--tch-color-on-surface);
      margin: 0;
    }

    .ops__benefit-body {
      font-size: 0.875rem;
      color: var(--tch-color-on-surface-variant);
      margin: 0;
      line-height: 1.5;
    }

    /* ── Workflow ── */
    .ops__workflow { background: var(--tch-color-surface-tonal, #ebebf5); }

    .ops__steps {
      display: flex;
      flex-direction: column;
      gap: 2rem;
      position: relative;
    }

    .ops__step {
      display: grid;
      grid-template-columns: 3rem 1fr;
      grid-template-rows: auto auto;
      column-gap: 1rem;
      row-gap: 0.25rem;
    }

    .ops__step-num {
      grid-row: span 2;
      width: 3rem;
      height: 3rem;
      border-radius: var(--tch-radius-pill, 9999px);
      background: var(--tch-color-primary);
      color: var(--tch-color-on-primary, #fff);
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: var(--tch-weight-bold, 700);
      font-size: 1.125rem;
      flex-shrink: 0;
    }

    .ops__step-title {
      font-size: var(--tch-font-size-title-md, 1.125rem);
      font-weight: var(--tch-weight-semibold, 600);
      color: var(--tch-color-on-surface);
      margin: 0;
      align-self: end;
    }

    .ops__step-body {
      color: var(--tch-color-on-surface-variant);
      margin: 0;
      font-size: 0.9375rem;
    }

    /* ── Plans ── */
    .ops__plans { background: var(--tch-color-background); }

    .ops__plans-grid {
      display: grid;
      grid-template-columns: 1fr;
      gap: 1.25rem;
    }

    tch-card.ops__plan-card {
      display: flex;
      flex-direction: column;
      padding: 1.75rem;
      position: relative;
      --comp-card-border: var(--tch-color-outline-variant);
    }

    tch-card.ops__plan-card--featured {
      --comp-card-bg: var(--tch-color-primary);
      --comp-card-border: var(--tch-color-primary);
      color: var(--tch-color-on-primary, #fff);

      .ops__plan-title  { color: var(--tch-color-accent, #fecb00); }
      .ops__plan-price  { color: #fff; }
      .ops__plan-body   { color: var(--tch-color-primary-fixed, #e1e0ff); }
      .ops__plan-check  { color: var(--tch-color-accent, #fecb00); }
      .ops__plan-feature { color: var(--tch-color-primary-fixed, #e1e0ff); }
    }

    .ops__plan-badge {
      align-self: flex-start;
      background: var(--tch-color-accent, #fecb00);
      color: var(--tch-color-primary, #1a1b4b);
      font-size: 0.625rem;
      font-weight: var(--tch-weight-extra-bold, 800);
      letter-spacing: 0.1em;
      text-transform: uppercase;
      padding: 0.2rem 0.625rem;
      border-radius: var(--tch-radius-pill, 9999px);
      margin-bottom: 0.5rem;
    }

    .ops__plan-title {
      font-size: var(--tch-font-size-title-md, 1.125rem);
      font-weight: var(--tch-weight-semibold, 600);
      color: var(--tch-color-primary);
      margin: 0 0 0.25rem;
    }

    .ops__plan-price {
      font-family: var(--tch-font-family-mono, monospace);
      font-size: 1.25rem;
      font-weight: var(--tch-weight-medium, 500);
      letter-spacing: 0.04em;
      color: var(--tch-color-on-surface);
      margin: 0 0 0.75rem;
    }

    .ops__plan-body {
      font-size: 0.875rem;
      color: var(--tch-color-on-surface-variant);
      margin: 0 0 1.25rem;
      line-height: 1.5;
    }

    .ops__plan-features {
      list-style: none;
      margin: 0 0 1.5rem;
      padding: 0;
      display: flex;
      flex-direction: column;
      gap: 0.625rem;
      flex: 1;
    }

    .ops__plan-feature {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-size: 0.875rem;
      color: var(--tch-color-on-surface-variant);
    }

    .ops__plan-check {
      font-size: 1rem;
      color: var(--tch-color-status-ready, #10b981);
      flex-shrink: 0;
    }

    .ops__plan-cta {
      width: 100%;
      box-sizing: border-box;
      --comp-action-bg: transparent;
      --comp-action-fg: var(--tch-color-primary);
      border: 2px solid var(--tch-color-primary);
      font-weight: var(--tch-weight-bold, 700);
    }

    .ops__plan-cta--featured {
      --comp-action-bg: var(--tch-color-accent, #fecb00);
      --comp-action-fg: var(--tch-color-primary, #1a1b4b);
      border-color: transparent;
    }

    /* ── Access ── */
    .ops__access {
      background: var(--tch-color-surface-container-low, #f3f3f6);
      border-top: 1px solid var(--tch-color-outline-variant);
      border-bottom: 1px solid var(--tch-color-outline-variant);
    }

    .ops__access-inner {
      display: flex;
      flex-direction: column;
      gap: 2.5rem;
    }

    .ops__access-text h2 {
      font-size: var(--tch-font-size-headline-mobile, 1.5rem);
      font-weight: var(--tch-weight-bold, 700);
      color: var(--tch-color-primary);
      margin: 0 0 1rem;
    }

    .ops__access-body {
      color: var(--tch-color-on-surface-variant);
      margin: 0 0 2rem;
      font-size: 1.0625rem;
      line-height: 1.6;
    }

    .ops__access-cards {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 1rem;
    }

    .ops__access-item {
      background: var(--tch-color-surface-container-lowest, #fff);
      border: 1px solid var(--tch-color-outline-variant);
      border-radius: var(--tch-radius-lg, 12px);
      padding: 1.25rem;
      display: flex;
      flex-direction: column;
      align-items: center;
      text-align: center;
      gap: 0.5rem;
    }

    .ops__access-item-icon {
      color: var(--tch-color-primary);
      font-size: 1.5rem;
    }

    .ops__access-item-title {
      font-size: 0.875rem;
      font-weight: var(--tch-weight-semibold, 600);
      color: var(--tch-color-on-surface);
    }

    .ops__access-item-body {
      font-size: 0.75rem;
      color: var(--tch-color-on-surface-variant);
      margin: 0;
      line-height: 1.4;
    }

    .ops__access-terminal {
      background: var(--tch-color-primary);
      border-radius: var(--tch-radius-xl, 20px);
      padding: 1.5rem;
      overflow: hidden;
    }

    .ops__terminal-dots {
      display: flex;
      align-items: center;
      gap: 0.375rem;
      margin-bottom: 1rem;
      padding-bottom: 0.75rem;
      border-bottom: 1px solid rgb(255 255 255 / 0.12);

      span:not(.ops__terminal-label) {
        width: 0.625rem;
        height: 0.625rem;
        border-radius: 50%;
        &:nth-child(1) { background: var(--tch-color-error, #ba1a1a); }
        &:nth-child(2) { background: var(--tch-color-status-warning, #f59e0b); }
        &:nth-child(3) { background: var(--tch-color-status-ready, #10b981); }
      }
    }

    .ops__terminal-label {
      margin-left: auto;
      font-family: var(--tch-font-family-mono, monospace);
      font-size: 0.6875rem;
      color: rgb(255 255 255 / 0.35);
    }

    .ops__terminal-code {
      font-family: var(--tch-font-family-mono, monospace);
      font-size: 0.8125rem;
      line-height: 1.6;
      color: var(--tch-color-accent, #fecb00);
      margin: 0;
      white-space: pre-wrap;
      overflow-x: auto;
    }

    /* ── Demo form ── */
    .ops__form-section { background: var(--tch-color-background); }

    .ops__form-container { max-width: 56rem; }

    tch-card.ops__form-card {
      padding: clamp(1.5rem, 4vw, 2.5rem);
    }

    .ops__form-header {
      text-align: center;
      margin-bottom: 2rem;
      h2 {
        font-size: var(--tch-font-size-headline-mobile, 1.5rem);
        font-weight: var(--tch-weight-bold, 700);
        color: var(--tch-color-primary);
        margin: 0 0 0.5rem;
      }
      p {
        color: var(--tch-color-on-surface-variant);
        margin: 0;
      }
    }

    .ops__form { display: flex; flex-direction: column; gap: 1.25rem; }

    .ops__form-row { display: grid; grid-template-columns: 1fr; gap: 1.25rem; }

    .ops__field { display: flex; flex-direction: column; gap: 0.375rem; }

    .ops__label {
      font-size: var(--tch-font-size-label-sm, 0.75rem);
      font-weight: var(--tch-weight-semibold, 600);
      color: var(--tch-color-on-surface-variant);
    }

    .ops__input {
      background: var(--tch-color-surface-container, #edeef1);
      border: none;
      border-radius: var(--tch-radius-lg, 12px);
      min-height: var(--tch-touch-target, 48px);
      padding: 0 1rem;
      font-family: var(--tch-font-family);
      font-size: 1rem;
      color: var(--tch-color-on-surface);
      outline: none;
      width: 100%;
      box-sizing: border-box;

      &:focus {
        box-shadow: 0 0 0 2px var(--tch-color-primary);
      }
    }

    .ops__textarea {
      min-height: unset;
      padding: 0.75rem 1rem;
      resize: vertical;
    }

    .ops__form-submit {
      width: 100%;
      box-sizing: border-box;
      font-weight: var(--tch-weight-extra-bold, 800);
      font-size: 1.0625rem;
    }

    /* ── FAQ ── */
    .ops__faq { background: var(--tch-color-surface-container-low, #f3f3f6); }

    .ops__faq-container { max-width: 48rem; }

    .ops__faq-list { display: flex; flex-direction: column; gap: 0.75rem; }

    .ops__faq-item {
      background: var(--tch-color-surface-container-lowest, #fff);
      border: 1px solid var(--tch-color-outline-variant);
      border-radius: var(--tch-radius-lg, 12px);
      overflow: hidden;
    }

    .ops__faq-q {
      width: 100%;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
      padding: 1rem 1.25rem;
      background: transparent;
      border: none;
      cursor: pointer;
      text-align: left;
      font-family: var(--tch-font-family);
      font-size: var(--tch-font-size-title-md, 1.125rem);
      font-weight: var(--tch-weight-semibold, 600);
      color: var(--tch-color-on-surface);

      &:hover { background: var(--tch-color-surface-container-low, #f3f3f6); }
    }

    .ops__faq-chevron {
      flex-shrink: 0;
      color: var(--tch-color-outline);
      transition: transform 0.2s ease;
    }

    .ops__faq-chevron--open { transform: rotate(180deg); }

    .ops__faq-a {
      display: none;
      padding: 0 1.25rem 1.25rem;

      p {
        margin: 0;
        color: var(--tch-color-on-surface-variant);
        font-size: 0.9375rem;
        line-height: 1.6;
      }
    }

    .ops__faq-a--open { display: block; }

    /* ── Breakpoints ── */
    @include bp.up(medium) {
      .ops__container {
        width: min(100% - 2 * var(--tch-page-margin-desktop, 32px), var(--tch-page-max, 1120px));
      }

      .ops__section-header h2,
      .ops__section-title-centered {
        font-size: var(--tch-font-size-headline-lg, 2rem);
      }

      .ops__hero-title {
        font-size: clamp(2rem, 4vw, 2.5rem);
      }

      .ops__benefits-grid {
        grid-template-columns: repeat(2, 1fr);
        gap: 1.5rem;
      }

      .ops__steps {
        flex-direction: row;
        align-items: flex-start;
        gap: 0;
        &::before {
          content: '';
          position: absolute;
          top: 1.5rem;
          left: 10%;
          right: 10%;
          height: 2px;
          background: var(--tch-color-outline-variant);
        }
      }

      .ops__step {
        flex: 1;
        display: flex;
        flex-direction: column;
        align-items: center;
        text-align: center;
        padding: 0 1rem;
        position: relative;
        z-index: 1;
      }

      .ops__step-num {
        width: 3.5rem;
        height: 3.5rem;
        font-size: 1.25rem;
        margin-bottom: 1rem;
      }

      .ops__step-title, .ops__step-body { grid-column: unset; }

      .ops__plans-grid {
        grid-template-columns: repeat(2, 1fr);
        gap: 1.25rem;
      }

      .ops__access-inner {
        flex-direction: row;
        align-items: center;
        gap: 4rem;
      }

      .ops__access-text { flex: 1; }
      .ops__access-terminal { flex: 1; }

      .ops__form-row { grid-template-columns: repeat(2, 1fr); }
    }

    @media (min-width: 1024px) {
      .ops__benefits-grid { grid-template-columns: repeat(3, 1fr); }
      .ops__plans-grid { grid-template-columns: repeat(4, 1fr); }
    }
  `],
})
export class PublicOperatorsPage {
  readonly benefits = BENEFITS;
  readonly steps = STEPS;
  readonly plans = PLANS;

  readonly formState = signal<'idle' | 'sending' | 'sent'>('idle');

  readonly accessItems = [
    { icon: 'card_membership', titleKey: `${P}.access_plan_title`,         bodyKey: `${P}.access_plan_body`         },
    { icon: 'event_repeat',    titleKey: `${P}.access_subscription_title`, bodyKey: `${P}.access_subscription_body` },
    { icon: 'admin_panel_settings', titleKey: `${P}.access_rights_title`,  bodyKey: `${P}.access_rights_body`       },
  ];

  readonly faqItems: FaqItem[] = [
    { qKey: `${P}.faq_trial_q`,    aKey: `${P}.faq_trial_a`,    open: false },
    { qKey: `${P}.faq_terminals_q`, aKey: `${P}.faq_terminals_a`, open: false },
    { qKey: `${P}.faq_verify_q`,   aKey: `${P}.faq_verify_a`,   open: false },
    { qKey: `${P}.faq_expired_q`,  aKey: `${P}.faq_expired_a`,  open: false },
  ];

  readonly accessLogSample = `{
  "plan": "Réseau",
  "statut": "Actif",
  "vendeurs": "50 / 100",
  "terminaux": "8 / 20",
  "fonctions": [
    "Vente QR",
    "Rapports",
    "Sessions"
  ]
}`;

  toggleFaq(index: number): void {
    this.faqItems[index].open = !this.faqItems[index].open;
  }

  submitForm(): void {
    this.formState.set('sending');
    setTimeout(() => this.formState.set('sent'), 1500);
  }
}
