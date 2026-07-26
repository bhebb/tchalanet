import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  ElementRef,
  Input,
  Output,
  signal,
  ViewChild,
} from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'tch-page-error',
  standalone: true,
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <main #surface class="tch-page-error" role="alert" tabindex="-1" aria-labelledby="tch-page-error-title">
      @if (codeValue()) { <p class="tch-page-error__code">{{ codeValue() }}</p> }
      <h1 id="tch-page-error-title">{{ titleValue() }}</h1>
      @if (messageValue()) { <p>{{ messageValue() }}</p> }
      @if (supportReferenceValue()) {
        <div class="tch-page-error__support">
          @if (supportReferenceLabelValue()) { <span>{{ supportReferenceLabelValue() }}</span> }
          <code>{{ supportReferenceValue() }}</code>
          @if (copySupportLabelValue()) {
            <button type="button" (click)="copySupport.emit()">{{ copySupportLabelValue() }}</button>
          }
        </div>
      }
      <div class="tch-page-error__actions">
        @if (showRetryValue() && retryLabelValue()) {
          <button type="button" (click)="retry.emit()" [disabled]="retryPendingValue()">
            {{ retryLabelValue() }}
          </button>
        }
        @if (backRouteValue()) { <a [routerLink]="backRouteValue()">{{ backLabelValue() }}</a> }
      </div>
    </main>
  `,
  styles: [`
    @use 'index' as ui;

    :host {
      --comp-page-error-fg: var(--tch-color-on-surface);
      --comp-page-error-code-fg: var(--tch-color-error);
      display: block;
    }
    .tch-page-error { display: grid; place-content: center; justify-items: center; gap: 1rem; min-height: 60vh; max-width: var(--tch-page-max); margin: auto; padding: max(var(--tch-page-gutter), 1rem); color: var(--comp-page-error-fg); text-align: center; }
    .tch-page-error__code { color: var(--comp-page-error-code-fg); font-weight: var(--tch-weight-extra-bold, 800); }
    h1, p { margin: 0; }
    .tch-page-error:focus { outline: none; }
    .tch-page-error__support { display: grid; gap: .375rem; max-width: 100%; padding: .75rem; border: 1px solid var(--tch-color-outline-variant); border-radius: var(--tch-radius-sm); }
    code { overflow-wrap: anywhere; }
    .tch-page-error__actions { display: flex; flex-wrap: wrap; justify-content: center; gap: .5rem; }
    a, button { min-height: 2.75rem; padding: .625rem 1rem; border: 1px solid var(--tch-color-outline); border-radius: var(--tch-radius-md); background: var(--tch-color-surface); color: var(--tch-color-primary); font: inherit; text-decoration: none; cursor: pointer; }
    button:disabled { cursor: wait; opacity: .65; }
    @include ui.down(medium) {
      .tch-page-error { min-height: 100dvh; place-content: center; text-align: left; }
      .tch-page-error__actions { width: 100%; }
      .tch-page-error__actions > * { flex: 1 1 100%; text-align: center; }
    }
  `],
})
export class TchPageError implements AfterViewInit {
  protected readonly codeValue = signal('');
  protected readonly titleValue = signal('');
  protected readonly messageValue = signal('');
  protected readonly backRouteValue = signal('');
  protected readonly backLabelValue = signal('Retour');
  protected readonly showRetryValue = signal(false);
  protected readonly retryPendingValue = signal(false);
  protected readonly retryLabelValue = signal('');
  protected readonly supportReferenceValue = signal('');
  protected readonly supportReferenceLabelValue = signal('');
  protected readonly copySupportLabelValue = signal('');

  @Input() set code(value: string) { this.codeValue.set(value); }
  @Input({ required: true }) set title(value: string) { this.titleValue.set(value); }
  @Input() set message(value: string) { this.messageValue.set(value); }
  @Input() set backRoute(value: string) { this.backRouteValue.set(value); }
  @Input() set backLabel(value: string) { this.backLabelValue.set(value); }
  @Input() set showRetry(value: boolean) { this.showRetryValue.set(value); }
  @Input() set retryPending(value: boolean) { this.retryPendingValue.set(value); }
  @Input() set retryLabel(value: string) { this.retryLabelValue.set(value); }
  @Input() set supportReference(value: string) { this.supportReferenceValue.set(value); }
  @Input() set supportReferenceLabel(value: string) { this.supportReferenceLabelValue.set(value); }
  @Input() set copySupportLabel(value: string) { this.copySupportLabelValue.set(value); }

  @Output() readonly retry = new EventEmitter<void>();
  @Output() readonly copySupport = new EventEmitter<void>();

  @ViewChild('surface', { static: true }) private readonly surface?: ElementRef<HTMLElement>;

  ngAfterViewInit(): void {
    queueMicrotask(() => this.surface?.nativeElement.focus());
  }
}
