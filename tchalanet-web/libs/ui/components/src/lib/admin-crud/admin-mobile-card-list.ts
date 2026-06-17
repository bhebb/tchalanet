import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * Responsive wrapper: visible on compact only (<600px), hidden on medium+.
 * Place card items inside — typically .tch-list-row anchor elements.
 */
@Component({
  selector: 'tch-admin-mobile-card-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div class="mobile-list tch-list"><ng-content /></div>`,
  host: { class: 'tch-admin-mobile-list' },
  styles: [
    `
      :host {
        display: block;
      }

      @media (min-width: 600px) {
        :host {
          display: none;
        }
      }

      .mobile-list {
        display: flex;
        flex-direction: column;
        gap: 0.375rem;
      }
    `,
  ],
})
export class AdminMobileCardList {}
