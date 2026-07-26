import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output,
  signal,
} from '@angular/core';

@Component({
  selector: 'tch-error-panel',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="tch-error-panel" role="alert">
      @if (titleValue()) { <h2 tabindex="-1">{{ titleValue() }}</h2> }
      @if (messageValue()) { <p>{{ messageValue() }}</p> }
      @if (supportReferenceValue()) {
        <div class="tch-error-panel__support">
          @if (supportReferenceLabelValue()) {
            <span>{{ supportReferenceLabelValue() }}</span>
          }
          <code>{{ supportReferenceValue() }}</code>
          @if (copySupportLabelValue()) {
            <button type="button" class="tch-error-panel__copy" (click)="copySupport.emit()">
              {{ copySupportLabelValue() }}
            </button>
          }
        </div>
      }
      @if ((showRetryValue() && retryLabelValue()) || (showBackValue() && backLabelValue())) {
        <div class="tch-error-panel__actions">
          @if (showRetryValue() && retryLabelValue()) {
            <button type="button" (click)="retry.emit()" [disabled]="retryPendingValue()">
              {{ retryLabelValue() }}
            </button>
          }
          @if (showBackValue() && backLabelValue()) {
            <button type="button" class="tch-error-panel__secondary" (click)="back.emit()">
              {{ backLabelValue() }}
            </button>
          }
        </div>
      }
    </section>
  `,
  styles: [`
    @use 'index' as ui;

    :host {
      --comp-error-panel-bg: var(--tch-color-error-container);
      --comp-error-panel-fg: var(--tch-color-on-error-container);
      --comp-error-panel-radius: var(--tch-radius-md);
    }
    .tch-error-panel {
      display: grid;
      gap: .75rem;
      padding: 1rem;
      border-radius: var(--comp-error-panel-radius);
      background: var(--comp-error-panel-bg);
      color: var(--comp-error-panel-fg);
    }
    h2, p { margin: 0; }
    h2:focus { outline: 2px solid currentcolor; outline-offset: .25rem; }
    .tch-error-panel__support {
      display: grid;
      gap: .375rem;
      padding: .75rem;
      border: 1px solid currentcolor;
      border-radius: var(--tch-radius-sm);
      font-size: var(--tch-font-size-body-sm);
    }
    code { overflow-wrap: anywhere; }
    .tch-error-panel__actions { display: flex; flex-wrap: wrap; gap: .5rem; }
    button {
      min-height: 2.75rem;
      padding-inline: 1rem;
      border: 1px solid currentcolor;
      border-radius: var(--tch-radius-md);
      background: transparent;
      color: inherit;
      cursor: pointer;
    }
    button:disabled { cursor: wait; opacity: .65; }
    .tch-error-panel__secondary, .tch-error-panel__copy { background: var(--tch-color-surface); }
    @include ui.down(medium) {
      .tch-error-panel__actions > button { flex: 1 1 100%; }
      .tch-error-panel__copy { width: 100%; }
    }
  `],
})
export class TchErrorPanel {
  protected readonly titleValue = signal('');
  protected readonly messageValue = signal('');
  protected readonly retryLabelValue = signal('');
  protected readonly showRetryValue = signal(false);
  protected readonly retryPendingValue = signal(false);
  protected readonly backLabelValue = signal('');
  protected readonly showBackValue = signal(false);
  protected readonly supportReferenceValue = signal('');
  protected readonly supportReferenceLabelValue = signal('');
  protected readonly copySupportLabelValue = signal('');

  @Input() set title(value: string) { this.titleValue.set(value); }
  @Input() set message(value: string) { this.messageValue.set(value); }
  @Input() set retryLabel(value: string) { this.retryLabelValue.set(value); }
  @Input() set showRetry(value: boolean) { this.showRetryValue.set(value); }
  @Input() set retryPending(value: boolean) { this.retryPendingValue.set(value); }
  @Input() set backLabel(value: string) { this.backLabelValue.set(value); }
  @Input() set showBack(value: boolean) { this.showBackValue.set(value); }
  @Input() set supportReference(value: string) { this.supportReferenceValue.set(value); }
  @Input() set supportReferenceLabel(value: string) { this.supportReferenceLabelValue.set(value); }
  @Input() set copySupportLabel(value: string) { this.copySupportLabelValue.set(value); }

  @Output() readonly retry = new EventEmitter<void>();
  @Output() readonly back = new EventEmitter<void>();
  @Output() readonly copySupport = new EventEmitter<void>();
}
