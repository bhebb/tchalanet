export type PriceFrequency = 'monthly' | 'yearly';
export type PriceType = 'recurring' | 'one-time';
export type PriceStatus = 'trial' | 'promo' | 'active' | 'inactive';
export type PriceTier = 'free' | 'basic' | 'pro' | 'business';
export interface PlanItem {
  id: string;
  name: string;
  subtitle?: string;
  description: string;
  price: number;
  currency: string;
  frequency: PriceFrequency;
  type: PriceType;
  link?: string;
  services?: string[];
  highlight?: boolean;
  badgeKey?: string;
  status: PriceStatus;
  tier: PriceTier;
}
