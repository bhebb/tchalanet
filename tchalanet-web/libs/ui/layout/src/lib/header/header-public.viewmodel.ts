import { Brand, TchLink } from '@tchl/types';
import { ThemeMode } from '@tchl/ui/theme';

export interface HeaderPublicVm {
  // responsive
  handset: boolean;
  tablet?: boolean;
  desktop: boolean;

  // brand / session
  brand?: Brand;
  onBrandClick: () => void;

  // CTA
  ctaLabel?: string;
  ctaPath?: string;
  showCtaInMainRow: boolean; // visible tablet+/desktop

  // feature toggles
  showSearch: boolean;
  showLangTheme: boolean;

  // lang/theme data
  langState: {
    currentLang: string;
    availableLangs: string[];
  };
  themeMode: ThemeMode;

  // account
  isAuthenticated: boolean;
  avatarUrl?: string;
  avatarText?: string;
  accountAriaLabel?: string;
  onAccountClick: () => void;
  onSignOut?: () => void; // for menu if needed
  accountMenu?: {
    showMenu: boolean;
    accountPath: string;
    signOutLabelKey: string;
  };

  // nav
  navItems: TchLink[];
  currentPath: string;

  // burger
  showBurger: boolean;
  onBurgerClick: () => void;

  // actions handlers
  onSearchClick: () => void;
  onChangeLang: (lang: string) => void;
  onToggleTheme: (themeMode: string) => void;
  onNavSelect: (path: string) => void;
}
