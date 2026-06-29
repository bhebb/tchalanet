import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';

import { I18nFacade } from '@tch/core/i18n';
import { LanguageOption } from '@tch/ui/components';
import { PublicRuntimeStore, PublicShellLayoutComponent, PublicShellService } from '@tch/web/shell';

const LANG_META: Record<string, { label: string; shortLabel: string; flag: string }> = {
  fr: { label: 'Français', shortLabel: 'FR', flag: '🇫🇷' },
  en: { label: 'English', shortLabel: 'EN', flag: '🇬🇧' },
  ht: { label: 'Kreyòl', shortLabel: 'HT', flag: '🇭🇹' },
};

@Component({
  selector: 'tch-public-shell',
  imports: [PublicShellLayoutComponent, RouterOutlet],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './public-shell.component.html',
})
export class TchPublicShellComponent {
  private readonly runtime = inject(PublicRuntimeStore);
  private readonly router = inject(Router);
  protected readonly i18n = inject(I18nFacade);
  protected readonly shellSvc = inject(PublicShellService);
  protected readonly languages = computed<readonly LanguageOption[]>(() =>
    this.i18n.languages().map(id => ({
      id,
      label: LANG_META[id]?.label ?? this.i18n.label(id),
      shortLabel: LANG_META[id]?.shortLabel ?? id.toUpperCase(),
      flag: LANG_META[id]?.flag,
    })),
  );

  constructor() {
    this.runtime.init();
  }

  protected login(): void {
    void this.router.navigate(['/login']);
  }
}
