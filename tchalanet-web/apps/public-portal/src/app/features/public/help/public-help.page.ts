import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { TchPubActionCard, TchPubCard } from '@tch/ui/components';

interface FaqItem {
  readonly q: string;
  readonly a: string;
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
    {
      q: 'Comment encaisser mes gains ?',
      a: "Présentez votre fiche originale auprès d'un point de vente participant. Les gains sont vérifiés à partir du code public imprimé sur le reçu.",
      open: false,
    },
    {
      q: 'Quels sont les horaires des tirages ?',
      a: "Les tirages New York et Florida suivent les horaires officiels publiés. Consultez la section Résultats pour voir le statut en temps réel de chaque session.",
      open: false,
    },
    {
      q: 'Ma transaction est "En attente", pourquoi ?',
      a: "Un ticket peut être en attente durant la validation du tirage. Le statut est mis à jour dès que les résultats sont confirmés par les sources officielles.",
      open: false,
    },
    {
      q: 'Comment vérifier un ticket ?',
      a: "Rendez-vous sur la page Vérification et saisissez le code public figurant au bas de votre reçu imprimé. Vous pouvez aussi scanner le QR code.",
      open: false,
    },
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
