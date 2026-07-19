import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  Input,
  signal,
  ViewChild,
} from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

/**
 * Form-owned summary for safe, already-localized violations that could not be attached to a field.
 */
@Component({
  selector: 'tch-form-error-summary',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe],
  template: `
    @if (messagesValue().length) {
      <section #summary class="tch-form-error-summary" role="alert" tabindex="-1">
        <h2>{{ titleValue() | translate }}</h2>
        <ul>
          @for (message of messagesValue(); track message) {
            <li>{{ message | translate }}</li>
          }
        </ul>
      </section>
    }
  `,
  styles: [`
    :host {
      --comp-form-error-summary-bg: var(--tch-color-error-container);
      --comp-form-error-summary-fg: var(--tch-color-on-error-container);
      --comp-form-error-summary-border: var(--tch-color-error);
      display: block;
    }
    .tch-form-error-summary {
      display: grid;
      gap: .5rem;
      margin-block: .5rem;
      padding: .75rem .875rem;
      border: 1px solid var(--comp-form-error-summary-border);
      border-radius: var(--tch-radius-sm);
      background: var(--comp-form-error-summary-bg);
      color: var(--comp-form-error-summary-fg);
    }
    h2, ul { margin: 0; }
    h2 { font-size: var(--tch-font-size-title-sm); font-weight: var(--tch-weight-bold, 700); }
    ul { padding-inline-start: 1.25rem; }
  `],
})
export class TchFormErrorSummary implements AfterViewInit {
  protected readonly titleValue = signal('common.errors.categories.validation.title');
  protected readonly messagesValue = signal<readonly string[]>([]);

  @Input() set title(value: string) { this.titleValue.set(value); }
  @Input() set messages(value: readonly string[] | null | undefined) {
    this.messagesValue.set(value ?? []);
    if (value?.length) {
      queueMicrotask(() => this.summary?.nativeElement.focus());
    }
  }

  @ViewChild('summary') private readonly summary?: ElementRef<HTMLElement>;

  ngAfterViewInit(): void {
    if (this.messagesValue().length) {
      queueMicrotask(() => this.summary?.nativeElement.focus());
    }
  }
}
