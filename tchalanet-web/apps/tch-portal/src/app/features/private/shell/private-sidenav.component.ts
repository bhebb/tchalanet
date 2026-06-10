import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { NavigationSection, actionRoute } from '@tch/api';

@Component({
  selector: 'tch-private-sidenav',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, RouterLinkActive, TranslatePipe],
  template: `
    <nav class="sidenav" aria-label="Private navigation">
      @for (section of sections(); track section.id) {
        <div class="sidenav__section">
          <span class="sidenav__section-label">{{ section.titleKey | translate }}</span>
          <ul class="sidenav__list" role="list">
            @for (item of section.items; track item.id) {
              <li>
                <a
                  class="sidenav__item"
                  [routerLink]="actionRoute(item)"
                  routerLinkActive="sidenav__item--active"
                  [routerLinkActiveOptions]="{ exact: item.activeMatch === 'exact' }"
                  [attr.data-nav-id]="item.id"
                >
                  @if (item.icon) {
                    <span class="sidenav__icon material-symbols-outlined" aria-hidden="true">
                      {{ item.icon }}
                    </span>
                  }
                  <span class="sidenav__label">{{ item.labelKey | translate }}</span>
                </a>
              </li>
            }
          </ul>
        </div>
      }
    </nav>
  `,
  styles: [
    `
      :host {
        display: block;
        width: var(--tch-private-sidenav-width, 256px);
        min-height: 100%;
        background: var(--tch-color-surface-container, #edeef1);
        padding: 1.5rem 1rem;
        box-sizing: border-box;
      }
    `,
  ],
})
export class PrivateSidenavComponent {
  readonly sections = input.required<readonly NavigationSection[]>();
  protected readonly actionRoute = actionRoute;
}
