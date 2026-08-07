import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { TchPubActionCard, TchPubCard } from '@tch/ui/components';

interface FaqItem {
  readonly qKey: string;
  readonly aKey: string;
  open: boolean;
}

interface HelpCategory {
  readonly icon: string;
  readonly labelKey: string;
}

const HELP_CATEGORIES: readonly HelpCategory[] = [
  { icon: 'payments',             labelKey: 'public.help.cat_payments' },
  { icon: 'account_balance_wallet', labelKey: 'public.help.cat_withdrawals' },
  { icon: 'security',             labelKey: 'public.help.cat_security' },
  { icon: 'casino',               labelKey: 'domain.entity.games' },
];

@Component({
  selector: 'tch-public-help-page',
  imports: [TranslatePipe, RouterLink, TchPubActionCard, TchPubCard],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './public-help.page.html',
  styleUrls: ['./public-help.page.scss'],
})
export class PublicHelpPage {
  readonly categories = HELP_CATEGORIES;
  readonly query = signal('');

  faqItems: FaqItem[] = [
    { qKey: 'public.help.faq1_q', aKey: 'public.help.faq1_a', open: false },
    { qKey: 'public.help.faq2_q', aKey: 'public.help.faq2_a', open: false },
    { qKey: 'public.help.faq3_q', aKey: 'public.help.faq3_a', open: false },
    { qKey: 'public.help.faq4_q', aKey: 'public.help.faq4_a', open: false },
  ];

  updateQuery(event: Event): void {
    this.query.set(event.target instanceof HTMLInputElement ? event.target.value : '');
  }

  toggleFaq(index: number): void {
    this.faqItems = this.faqItems.map((item, i) =>
      i === index ? { ...item, open: !item.open } : item,
    );
  }
}
