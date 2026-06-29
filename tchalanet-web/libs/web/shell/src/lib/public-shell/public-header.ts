import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { ActionItem, actionText } from '@tch/api';
import { LabelPipe, PublicShellRuntime } from '@tch/page-model';
import { TCH_BRAND_ASSETS } from '@tch/shared-assets';
import {
  LanguageOption,
  TchActionButton,
  TchBrand,
  TchLangSwitcher,
  TchNav,
  TchOverlayNav,
} from '@tch/ui/components';

const COMPACT_LABEL_MAP: Record<string, string> = {
  'public.nav.check_ticket': 'public.nav.verify_short',
  'public.nav.operators': 'public.nav.operators_short',
};

@Component({
  selector: 'tch-public-header',
  imports: [
    MatButtonModule,
    MatIconModule,
    LabelPipe,
    TchActionButton,
    TchBrand,
    TchLangSwitcher,
    TchNav,
    TchOverlayNav,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './public-header.html',
  styleUrls: ['./public-header.scss'],
})
export class PublicHeader {
  readonly shell = input<PublicShellRuntime | undefined>();
  readonly languages = input<readonly LanguageOption[]>([]);
  readonly currentLanguage = input('');
  readonly loginRequested = output<ActionItem | undefined>();
  readonly languageSelected = output<string>();

  readonly mobileMenuOpen = signal(false);
  readonly brand = computed(() => publicBrand(this.shell()));
  readonly nav = computed(() => publicHeaderNav(this.shell()));
  readonly navCompact = computed(() => toCompactNav(this.nav()));
  readonly loginAction = computed(() => publicLoginAction(this.shell()));

  login(): void {
    this.loginRequested.emit(this.loginAction());
  }

  toggleMobileMenu(): void {
    this.mobileMenuOpen.update(open => !open);
  }

  closeMobileMenu(): void {
    this.mobileMenuOpen.set(false);
  }

  protected readonly actionText = actionText;
}

function publicBrand(shell: PublicShellRuntime | undefined): ActionItem | undefined {
  const brand = shell?.header.brand;
  return {
    id: brand?.id ?? 'public-brand',
    ...brand,
    image: brand?.image ?? TCH_BRAND_ASSETS.logo,
    destination: brand?.destination ?? { kind: 'route', value: '/public' },
  };
}

function publicHeaderNav(shell: PublicShellRuntime | undefined): readonly ActionItem[] {
  return shell?.header.primary ?? [];
}

function publicLoginAction(shell: PublicShellRuntime | undefined): ActionItem | undefined {
  const actions = [...(shell?.header.actions ?? []), ...(shell?.header.secondary ?? [])];
  return actions.find(item => item.id === 'login') ?? actions[0];
}

function toCompactNav(items: readonly ActionItem[]): readonly ActionItem[] {
  return items.map(item => {
    const shortKey = item.labelKey ? COMPACT_LABEL_MAP[item.labelKey] : undefined;
    return shortKey ? { ...item, labelKey: shortKey } : item;
  });
}
