import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'tch-loading',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="loading" role="status" [attr.aria-label]="ariaLabel() || label() || 'Chargement'">
      <span class="loading__spinner" aria-hidden="true"></span>
      @if (label()) { <p>{{ label() }}</p> }
    </div>
  `,
  styles: [`
    :host { --comp-loading-track: var(--tch-color-surface-container-high); --comp-loading-active: var(--tch-color-primary); }
    .loading { display: grid; justify-items: center; gap: .75rem; padding: 2rem 1rem; color: var(--tch-color-on-surface-variant); }
    .loading__spinner { width: 2rem; height: 2rem; border: 3px solid var(--comp-loading-track); border-top-color: var(--comp-loading-active); border-radius: 50%; animation: spin .8s linear infinite; }
    p { margin: 0; }
    @keyframes spin { to { transform: rotate(360deg); } }
  `],
})
export class TchLoading {
  readonly label = input('');
  readonly ariaLabel = input('');
}
