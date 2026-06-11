import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'tch-lang-theme-group',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div class="lang-theme-group"><ng-content /></div>`,
  styles: [`
    :host { --comp-lang-theme-bg: var(--tch-color-surface-container); }
    .lang-theme-group { display: flex; align-items: center; flex-wrap: wrap; gap: .5rem; padding: .375rem; border-radius: var(--tch-radius-pill); background: var(--comp-lang-theme-bg); }
  `],
})
export class TchLangThemeGroup {}
