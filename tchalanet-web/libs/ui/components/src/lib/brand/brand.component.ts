import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { ActionItem, actionRoute, actionText, isRouteAction } from '../navigation/action-item';

@Component({
  selector: 'tch-brand',
  imports: [NgTemplateOutlet, RouterLink, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (isRouteAction(brand())) {
      <a class="brand" [routerLink]="actionRoute(brand())" [attr.aria-label]="actionText(brand()) | translate">
        <ng-container *ngTemplateOutlet="content" />
      </a>
    } @else {
      <span class="brand"><ng-container *ngTemplateOutlet="content" /></span>
    }
    <ng-template #content>
      @if (brand()?.image; as image) {
        <img [src]="image" [alt]="showName() ? '' : (actionText(brand()) || fallbackLabel()) | translate" />
      }
      @if (showName()) {
        <span>{{ actionText(brand()) || fallbackLabel() | translate }}</span>
      }
    </ng-template>
  `,
  styles: [`
    :host { --comp-brand-fg: var(--tch-color-primary); display: inline-flex; }
    .brand { display: inline-flex; align-items: center; gap: .625rem; color: var(--comp-brand-fg); font-weight: 800; text-decoration: none; }
    img { display: block; max-width: 11rem; max-height: 3rem; }
  `],
})
export class TchBrand {
  readonly brand = input<ActionItem | undefined>();
  readonly showName = input(true);
  readonly fallbackLabel = input('Tchalanet');
  readonly actionRoute = actionRoute;
  readonly actionText = actionText;
  readonly isRouteAction = isRouteAction;
}
