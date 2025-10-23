import { Params } from '@angular/router';

import {TenantThemePayload } from '@tchl/ui/theme';

export interface PageModel {
  context: string;
  version?: string;
  theme: TenantThemePayload;
  header: PageElement<HeaderProperties>;
  nav?: { header?: TchLink[]; sidenav?: TchLink[] };
  layout: Layout;
  footer: PageElement<FooterProperties>;
  i18n: Record<string, Record<string, string>>;
  issues: any[];
  currentLang: string;
  langs: string[];
  /** whitelist des features actives côté UI (vient du JSON ou enrichi par le back) */
  flags?: string[];
}

export interface TchLink {
  labelKey: string;
  path: string;
  query?: Params;
  icon?: string;
  flag?: string;
  external?: string;
  children?: TchLink[];
}

export type TchAccountLink =
  | (TchLink & { avatar: 'generic' }) // visiteur
  | (TchLink & { avatar: 'user' }); // connecté

export type ActionToggle = { flag?: string };

export interface Brand {
  name: string;
  logo?: string;
  href?: string;
}

export interface HeaderProperties {
  brand?: Brand;
  actions: { search: ActionToggle; lang: ActionToggle; theme: ActionToggle };
  navigation?: Array<TchLink>;
  cta: { public: TchLink; private: TchLink };
  account: {
    public: TchAccountLink;
    private: TchAccountLink;
  };
}

export interface FooterProperties {
  brand?: Brand;
  columns: FooterColumn[]; // 3 colonnes (Legal, Support, Product)
  socials?: {
    icon: 'twitter' | 'facebook' | 'youtube' | 'instagram' | 'linkedin';
    href: string;
    aria?: string;
  }[];
  copyrightKey: string; // "© 2025 Tchalanet. Tous droits réservés."
  accentHex?: string; // optionnel: override couleur de fond
}

export interface PageElement<T = any> {
  component: string;
  properties?: T;
  showOn?: ('web' | 'mobile')[];
}

export interface Layout {
  component: 'GridLayout' | string;
  rows: LayoutRow[];
}

export interface FooterColumn {
  titleKey: string;
  links: TchLink[];
}

export interface LayoutRow {
  columns: LayoutColumn[];
}

export interface LayoutColumn {
  span: number;
  widgets: PageElement[];
}
