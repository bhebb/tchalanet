import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import {
  GeneratedDrawSalesStatus,
  GeneratedDrawResultStatus,
  GeneratedDrawPublicationStatus,
} from '../../data-access/admin-generated-draws.models';

type BadgeVariant = 'open' | 'closed' | 'upcoming' | 'cancelled'
  | 'not-due' | 'expected' | 'missing' | 'provisional' | 'confirmed' | 'source-error'
  | 'published' | 'not-published';

interface BadgeMeta {
  label: string;
  variant: BadgeVariant;
  icon?: string;
  pulse?: boolean;
}

const SALES_META: Record<GeneratedDrawSalesStatus, BadgeMeta> = {
  OPEN:      { label: 'Vente ouverte',  variant: 'open',      icon: 'circle', pulse: true },
  CLOSED:    { label: 'Vente fermée',   variant: 'closed' },
  UPCOMING:  { label: 'À venir',        variant: 'upcoming' },
  CANCELLED: { label: 'Annulé',         variant: 'cancelled' },
};

const RESULT_META: Record<GeneratedDrawResultStatus, BadgeMeta> = {
  NOT_DUE:     { label: 'Résultat à venir',  variant: 'not-due' },
  EXPECTED:    { label: 'Résultat attendu',  variant: 'expected' },
  MISSING:     { label: 'Résultat manquant', variant: 'missing' },
  PROVISIONAL: { label: 'Provisoire',        variant: 'provisional' },
  CONFIRMED:   { label: 'Confirmé',          variant: 'confirmed' },
  SOURCE_ERROR: { label: 'Erreur source',    variant: 'source-error', icon: 'report' },
};

const PUB_META: Record<GeneratedDrawPublicationStatus, BadgeMeta> = {
  PUBLISHED:     { label: 'Publié',      variant: 'published' },
  NOT_PUBLISHED: { label: 'Non publié',  variant: 'not-published' },
};

@Component({
  selector: 'tch-generated-draw-status-badge',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './generated-draw-status-badge.component.html',
  styleUrls: ['./generated-draw-status-badge.component.scss'],
})
export class GeneratedDrawStatusBadgeComponent {
  readonly type   = input.required<'sales' | 'result' | 'publication'>();
  readonly status = input.required<string>();

  readonly meta = computed(() => {
    const s = this.status();
    switch (this.type()) {
      case 'sales':       return SALES_META[s as GeneratedDrawSalesStatus] ?? { label: s, variant: 'closed' as BadgeVariant };
      case 'result':      return RESULT_META[s as GeneratedDrawResultStatus] ?? { label: s, variant: 'not-due' as BadgeVariant };
      case 'publication': return PUB_META[s as GeneratedDrawPublicationStatus] ?? { label: s, variant: 'not-published' as BadgeVariant };
    }
  });
}
