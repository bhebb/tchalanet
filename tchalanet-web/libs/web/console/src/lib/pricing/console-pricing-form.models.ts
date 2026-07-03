export interface ConsolePricingFormValue {
  readonly gameCode: string;
  readonly betType: string;
  readonly betOption: number;
  readonly odds: number;
  readonly active: boolean;
}

export const EMPTY_CONSOLE_PRICING_FORM_VALUE: ConsolePricingFormValue = {
  gameCode: '',
  betType: '',
  betOption: 0,
  odds: 0,
  active: true,
};
