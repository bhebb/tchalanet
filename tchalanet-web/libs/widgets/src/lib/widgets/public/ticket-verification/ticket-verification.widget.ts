import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';

import { WidgetConfig } from '@tch/page-model';
import { LabelPipe } from '@tch/page-model';
import { stringProp, toPublicPath } from '@tch/page-model';
import { TchActionButton, TchCard } from '@tch/ui/components';

@Component({
  selector: 'tch-ticket-verification-widget',
  imports: [LabelPipe, TchCard, TchActionButton],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './ticket-verification.widget.html',
  styleUrl: './ticket-verification.widget.scss',
})
export class TicketVerificationWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly titleKey = computed(
    () => stringProp(this.config(), 'titleKey') ?? 'public.check.title',
  );
  readonly descriptionKey = computed(
    () => stringProp(this.config(), 'descriptionKey') ?? 'public.check.description',
  );
  readonly ctaKey = computed(
    () => stringProp(this.config(), 'ctaKey') ?? 'public.check.cta',
  );
  readonly path = computed(() =>
    toPublicPath(stringProp(this.config(), 'path') ?? '/public/check-ticket'),
  );

  readonly code = signal('');

  readonly submitHref = computed(() => {
    const c = this.code().trim();
    return c ? `${this.path()}?code=${encodeURIComponent(c)}` : this.path();
  });

  onCodeInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const formatted = formatWidgetCode(input.value);
    this.code.set(formatted);
    input.value = formatted;
  }
}

function formatWidgetCode(value: string): string {
  const compact = value.replace(/[^a-zA-Z0-9]/g, '').toUpperCase().slice(0, 8);
  if (compact.length <= 4) return compact;
  return `${compact.slice(0, 4)}-${compact.slice(4)}`;
}
