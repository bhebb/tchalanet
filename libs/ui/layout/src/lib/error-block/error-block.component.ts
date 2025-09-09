// libs/ui/feedback/error-block/error-block.component.ts
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'tchl-error-block',
  standalone: true,
  imports: [CommonModule, TranslateModule, RouterLink],
  template: `
    <div class="card stack" role="alert" aria-live="assertive">
      <h2 class="h2">{{ titleKey | translate }}</h2>
      @if (descKey) {
      <p class="subtle">{{ descKey | translate }}</p>
      }
      <div class="cluster">
        <button class="btn btn--primary" type="button" (click)="retry.emit()">
          {{ retryKey | translate }}
        </button>
        <a class="btn btn--ghost" routerLink="/">
          {{ homeKey | translate }}
        </a>
      </div>
    </div>
  `,
  styles: [
    `
      :host {
        display: block;
      }
      .stack > * + * {
        margin-block-start: var(--space-4, 16px);
      }
      .cluster {
        display: flex;
        gap: 0.5rem;
        flex-wrap: wrap;
        align-items: center;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ErrorBlockComponent {
  /** Clés i18n (avec valeurs par défaut vers 'errors.*') */
  @Input() titleKey = 'errors.title';
  @Input() descKey?: string;
  @Input() retryKey = 'errors.retry';
  @Input() homeKey = 'errors.home';
  @Output() retry = new EventEmitter<void>();
}
