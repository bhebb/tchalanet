export type PublicRuleGameId =
  | 'borlette'
  | 'mariage'
  | 'maryaj_gratis'
  | 'lotto3'
  | 'lotto4'
  | 'lotto5';

export interface NumberFieldDef {
  readonly id: string;
  readonly labelKey: string;
  readonly minLength: number;
  readonly maxLength: number;
  readonly pattern: string;
}

export interface BetOption {
  readonly id: string;
  readonly labelKey: string;
  readonly defaultMultiplier: number;
  readonly bonusMultiplier?: number;
  readonly numberFields: readonly NumberFieldDef[];
}

export interface PublicRuleGame {
  readonly id: PublicRuleGameId;
  readonly icon: string;
  readonly titleKey: string;
  readonly summaryKey: string;
  readonly principleKey: string;
  readonly betOptions: readonly BetOption[];
}

export interface PublicTchalaEntry {
  readonly id: string;
  readonly icon: string;
  readonly termKey: string;
  readonly descriptionKey: string;
  readonly numbers: readonly string[];
  readonly keywords: readonly string[];
}
