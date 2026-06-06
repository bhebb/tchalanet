import { Directive, TemplateRef, ViewContainerRef, effect, inject, input } from '@angular/core';

import { FeatureFlags } from './feature-flags';

/**
 * Structural directive that gates content behind a runtime feature flag.
 *
 * Flags are resolved through the `FeatureFlags` seam (settings-backed today, Unleash-ready). Because
 * the underlying settings are a signal, the gated view re-evaluates automatically once settings
 * resolve. For combined feature + entitlement gating, use `*tchCan`.
 *
 * Usage:
 *   <section *tchFeature="'web.public.demo_enabled'">…</section>
 *   <section *tchFeature="'web.x'; default: true">…</section>
 *   <section *tchFeature="'web.x'; else disabled">…</section>
 *   <ng-template #disabled>…</ng-template>
 *
 * This gates features that are simply not ready. Paid/entitlement gating is a separate concern
 * (see docs/conventions/pagemodel-rendering.md) and must not be expressed through feature flags.
 */
@Directive({ selector: '[tchFeature]' })
export class FeatureFlagDirective {
  private readonly features = inject(FeatureFlags);
  private readonly templateRef = inject(TemplateRef<unknown>);
  private readonly viewContainer = inject(ViewContainerRef);

  readonly key = input.required<string>({ alias: 'tchFeature' });
  // Aliases are required so the structural-directive microsyntax (`*tchFeature="key; default: x;
  // else tpl"`) binds to `tchFeatureDefault` / `tchFeatureElse`, mirroring NgIf's `ngIfElse`.
  // eslint-disable-next-line @angular-eslint/no-input-rename
  readonly fallback = input<boolean>(false, { alias: 'tchFeatureDefault' });
  // eslint-disable-next-line @angular-eslint/no-input-rename
  readonly elseTemplate = input<TemplateRef<unknown> | null>(null, { alias: 'tchFeatureElse' });

  private rendered: 'main' | 'else' | 'none' | null = null;

  constructor() {
    effect(() => {
      const enabled = this.features.isEnabled(this.key(), this.fallback());
      this.render(enabled);
    });
  }

  private render(enabled: boolean): void {
    const elseTemplate = this.elseTemplate();
    const target = enabled ? 'main' : elseTemplate ? 'else' : 'none';
    if (this.rendered === target) {
      return;
    }

    this.viewContainer.clear();
    if (enabled) {
      this.viewContainer.createEmbeddedView(this.templateRef);
    } else if (elseTemplate) {
      this.viewContainer.createEmbeddedView(elseTemplate);
    }
    this.rendered = target;
  }
}
