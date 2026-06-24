import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { WidgetConfig } from '@tch/page-model';
import { LabelPipe } from '@tch/page-model';
import {
  actionsFrom,
  destinationHref,
  isRecord,
  stringProp,
  stringValue,
  WidgetAction,
} from '@tch/page-model';
import { TchActionButton } from '@tch/ui/components';

/**
 * `HeroWidget` — section d'entrée de la page publique.
 * Mobile-first : copy (eyebrow → titre → description → actions) puis visuel ticket.
 * Desktop : deux colonnes [copy | visuel].
 * Props lues depuis config.props (camelCase), actions depuis config.props.actions.
 */
@Component({
  selector: 'tch-hero-widget',
  imports: [LabelPipe, TchActionButton],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './hero.widget.html',
  styleUrl: './hero.widget.scss',
})
export class HeroWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly eyebrowKey = computed(() => stringProp(this.config(), 'taglineKey'));
  readonly titleKey = computed(
    () => stringProp(this.config(), 'titleKey') ?? 'home.hero.title',
  );
  readonly descriptionKey = computed(
    () => stringProp(this.config(), 'subtitleKey') ?? 'home.hero.subtitle',
  );

  private readonly visual = computed(() => {
    const v = this.config()?.props?.['visual'];
    return isRecord(v) ? v : undefined;
  });

  readonly ticketCodeLiteral = computed(
    () => stringValue(this.visual()?.['ticketCode']),
  );
  readonly statusLabelKey = computed(
    () => stringValue(this.visual()?.['statusLabelKey']) ?? 'ticket.status.pending',
  );
  readonly helperTextKey = computed(
    () => stringValue(this.visual()?.['helperTextKey']) ?? 'public.check.description',
  );

  private readonly allActions = computed(() =>
    actionsFrom(this.config()?.props?.['actions']).filter(
      a => a.id !== 'LOGIN' && a.id !== 'login',
    ),
  );

  readonly primaryAction = computed(() => this.allActions()[0]);
  readonly secondaryActions = computed(() => this.allActions().slice(1));

  href(action: WidgetAction): string {
    return destinationHref(action.destination);
  }
}
