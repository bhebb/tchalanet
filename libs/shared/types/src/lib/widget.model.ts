import { TchLink } from './page.model';
import { DrawResult, LotteryLite, NextDrawInfo } from '@tchl/web/widgets';

export type WidgetKind = 'HeroWidget' | 'FeatureCardWidget';

export interface HeroWidgetProps {
  imageUrl: string | undefined;
  eyebrow?: string; // petit surtitre
  icon?: string; // petit surtitre
  title: string; // H1
  lead?: string; // paragraphe d’accroche
  primaryCta?: TchLink;
  secondaryCta?: TchLink;
  // Image d'illustration
  image?: {
    src: string; // /assets/hero/borlette-hero.png
    alt: string;
    srcset?: string; // "img@1x.png 1x, img@2x.png 2x"
    sizes?: string; // "(min-width: 1024px) 560px, 90vw"
    width?: number;
    height?: number; // pour éviter layout shift
  };
  align?: 'left' | 'center';
}

export interface FeatureCardWidgetProps {
  icon?: string; // nom d’icône (si tu veux)
  image?: { src: string; alt?: string }; // nom d’icône (si tu veux)
  title: string;
  description?: string;
  link?: TchLink;
}

export interface DrawSwitcherConfig {
  initialLotteryId?: string;
  ui?: { showIcons?: boolean; compact?: boolean };
}
export interface DrawSwitcherData {
  lotteries: LotteryLite[];
  results: Record<string, DrawResult[]>;
  next: Record<string, NextDrawInfo>;
}
export interface DrawSwitcherProps {
  title?: string;
  config: DrawSwitcherConfig;
  data?: DrawSwitcherData; // optional; if your renderer resolves dataSource into data
  dataSource?: unknown; // your renderer can hydrate this and pass into data
}
export interface NewsItem {
  id: string;
  title: string;
  source?: string;
  href?: string;
  ts?: string;
  region?: 'global' | 'ht' | 'caribbean';
}
export interface NewsBannerProps {
  title?: string;
  labelKey?: string; // e.g. "news.label"
  region?: string; // "ht|global"
  autoScroll?: boolean;
  pauseOnHover?: boolean;
  maxItems?: number;
  emptyStateKey?: string; // "news.empty"
  ui?: { compact?: boolean };
  data?: { items: NewsItem[] };
  // optional dataSource handled by your renderer; we just accept local data
  dataSource?: unknown;
}
export type Widget =
  | { component: 'HeroWidget'; properties: HeroWidgetProps }
  | { component: 'DrawSwitcherWidget'; properties: DrawSwitcherProps }
  | { component: 'NewsBannerWidget'; properties: NewsBannerProps }
  | { component: 'FeatureCardWidget'; properties: FeatureCardWidgetProps };
