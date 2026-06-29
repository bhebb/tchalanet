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
  { icon: 'group',        titleKey: `${P}.benefit_agents_title`,   bodyKey: `${P}.benefit_agents_body`   },
  { icon: 'qr_code_2',    titleKey: `${P}.benefit_tickets_title`,  bodyKey: `${P}.benefit_tickets_body`  },
  { icon: 'public',       titleKey: `${P}.benefit_verify_title`,   bodyKey: `${P}.benefit_verify_body`   },
  { icon: 'analytics',    titleKey: `${P}.benefit_results_title`,  bodyKey: `${P}.benefit_results_body`  },
  { icon: 'payments',     titleKey: `${P}.benefit_payments_title`, bodyKey: `${P}.benefit_payments_body` },
  { icon: 'description',  titleKey: `${P}.benefit_reports_title`,  bodyKey: `${P}.benefit_reports_body`  },
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
  templateUrl: './public-operators.page.html',
  styleUrls: ['./public-operators.page.scss'],
})
export class PublicOperatorsPage {
  readonly benefits = BENEFITS;
  readonly steps = STEPS;
  readonly plans = PLANS;

  readonly formState = signal<'idle' | 'sending' | 'sent'>('idle');

  readonly accessItems = [
    { icon: 'manage_accounts',  titleKey: `${P}.access_rights_title`,       bodyKey: `${P}.access_rights_body`       },
    { icon: 'point_of_sale',    titleKey: `${P}.access_subscription_title`, bodyKey: `${P}.access_subscription_body` },
    { icon: 'price_check',      titleKey: `${P}.access_plan_title`,         bodyKey: `${P}.access_plan_body`         },
  ];

  readonly faqItems: FaqItem[] = [
    { qKey: `${P}.faq_trial_q`,    aKey: `${P}.faq_trial_a`,    open: false },
    { qKey: `${P}.faq_terminals_q`, aKey: `${P}.faq_terminals_a`, open: false },
    { qKey: `${P}.faq_verify_q`,   aKey: `${P}.faq_verify_a`,   open: false },
    { qKey: `${P}.faq_expired_q`,  aKey: `${P}.faq_expired_a`,  open: false },
    { qKey: `${P}.faq_multipos_q`, aKey: `${P}.faq_multipos_a`, open: false },
    { qKey: `${P}.faq_mobile_q`,   aKey: `${P}.faq_mobile_a`,   open: false },
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

  scrollToDemo(): void {
    document.getElementById('ops-demo-form')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  scrollToFeatures(): void {
    document.getElementById('ops-benefits')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  toggleFaq(index: number): void {
    this.faqItems[index].open = !this.faqItems[index].open;
  }

  submitForm(): void {
    this.formState.set('sending');
    setTimeout(() => this.formState.set('sent'), 1500);
  }
}
