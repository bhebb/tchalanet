import {
  Directive,
  TemplateRef,
  ViewContainerRef,
  effect,
  inject,
  input,
} from '@angular/core';

import { AccessRequirement, AccessService } from './access.service';

/**
 * Structural directive gating content behind a combined feature + entitlement requirement, so
 * templates avoid `*ngIf="isFeatureEnabled('x') && hasEntitlement('y')"` chains.
 *
 * Usage:
 *   <a *tchCan="{ feature: 'web.payouts', entitlement: 'payouts' }">Payouts</a>
 *   <a *tchCan="{ feature: 'web.x' }; else off">…</a>
 *   <ng-template #off>…</ng-template>
 *
 * Reactive: re-evaluates when flags/entitlements resolve. For feature-only gating, `*tchFeature`
 * remains a lighter convenience.
 */
@Directive({ selector: '[tchCan]' })
export class CanDirective {
  private readonly access = inject(AccessService);
  private readonly templateRef = inject(TemplateRef<unknown>);
  private readonly viewContainer = inject(ViewContainerRef);

  readonly requirement = input.required<AccessRequirement>({ alias: 'tchCan' });
  // eslint-disable-next-line @angular-eslint/no-input-rename
  readonly elseTemplate = input<TemplateRef<unknown> | null>(null, { alias: 'tchCanElse' });

  private rendered: 'main' | 'else' | 'none' | null = null;

  constructor() {
    effect(() => {
      this.render(this.access.can(this.requirement()));
    });
  }

  private render(allowed: boolean): void {
    const elseTemplate = this.elseTemplate();
    const target = allowed ? 'main' : elseTemplate ? 'else' : 'none';
    if (this.rendered === target) {
      return;
    }

    this.viewContainer.clear();
    if (allowed) {
      this.viewContainer.createEmbeddedView(this.templateRef);
    } else if (elseTemplate) {
      this.viewContainer.createEmbeddedView(elseTemplate);
    }
    this.rendered = target;
  }
}
