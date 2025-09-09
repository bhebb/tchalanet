export interface NavItem {
  id: string;
  label: string;
  route?: string;
  href?: string;
  active?: boolean;
  children?: NavItem[];
}
export interface HeaderModel {
  logoUrl?: string;
  menu: NavItem[];
  cta?: { label: string; href: string };
  langs?: string[];
  currentLang?: string;
}
export interface FooterModel {
  columns: { title: string; links: { label: string; href: string }[] }[];
  social?: { type: 'facebook'|'x'|'instagram'|'youtube'|'linkedin'; url: string }[];
  legal?: { label: string; href: string }[];
  note?: string;
  logoUrl?: string;
}
