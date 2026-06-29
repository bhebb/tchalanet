import type { NumberFieldDef, PublicRuleGame, PublicTchalaEntry } from './public-rules.model';

const FIELD_2: NumberFieldDef = { id: 'number', labelKey: 'domain.entity.number', minLength: 2, maxLength: 2, pattern: '^[0-9]{2}$' };
const FIELD_3: NumberFieldDef = { id: 'number', labelKey: 'domain.entity.number', minLength: 3, maxLength: 3, pattern: '^[0-9]{3}$' };
const FIELD_4: NumberFieldDef = { id: 'number', labelKey: 'domain.entity.number', minLength: 4, maxLength: 4, pattern: '^[0-9]{4}$' };
const FIELD_5: NumberFieldDef = { id: 'number', labelKey: 'domain.entity.number', minLength: 5, maxLength: 5, pattern: '^[0-9]{5}$' };

export const PUBLIC_RULE_GAMES: readonly PublicRuleGame[] = [
  {
    id: 'borlette',
    icon: 'confirmation_number',
    titleKey: 'public.rules.games.borlette.title',
    summaryKey: 'public.rules.games.borlette.summary',
    principleKey: 'public.rules.games.borlette.principle',
    betOptions: [
      { id: 'lot1', labelKey: 'public.rules.bet.borlette.lot1', defaultMultiplier: 50,  numberFields: [FIELD_2] },
      { id: 'lot2', labelKey: 'public.rules.bet.borlette.lot2', defaultMultiplier: 20,  numberFields: [FIELD_2] },
      { id: 'lot3', labelKey: 'public.rules.bet.borlette.lot3', defaultMultiplier: 10,  numberFields: [FIELD_2] },
    ],
  },
  {
    id: 'mariage',
    icon: 'join_inner',
    titleKey: 'public.rules.games.mariage.title',
    summaryKey: 'public.rules.games.mariage.summary',
    principleKey: 'public.rules.games.mariage.principle',
    betOptions: [
      {
        id: 'lot1_lot2', labelKey: 'public.rules.bet.mariage.lot1_lot2', defaultMultiplier: 800,
        numberFields: [
          { id: 'numberA', labelKey: 'public.rules.simulation.number_a_label', minLength: 2, maxLength: 2, pattern: '^[0-9]{2}$' },
          { id: 'numberB', labelKey: 'public.rules.simulation.number_b_label', minLength: 2, maxLength: 2, pattern: '^[0-9]{2}$' },
        ],
      },
      {
        id: 'lot1_lot3', labelKey: 'public.rules.bet.mariage.lot1_lot3', defaultMultiplier: 400,
        numberFields: [
          { id: 'numberA', labelKey: 'public.rules.simulation.number_a_label', minLength: 2, maxLength: 2, pattern: '^[0-9]{2}$' },
          { id: 'numberB', labelKey: 'public.rules.simulation.number_b_label', minLength: 2, maxLength: 2, pattern: '^[0-9]{2}$' },
        ],
      },
      {
        id: 'lot2_lot3', labelKey: 'public.rules.bet.mariage.lot2_lot3', defaultMultiplier: 200,
        numberFields: [
          { id: 'numberA', labelKey: 'public.rules.simulation.number_a_label', minLength: 2, maxLength: 2, pattern: '^[0-9]{2}$' },
          { id: 'numberB', labelKey: 'public.rules.simulation.number_b_label', minLength: 2, maxLength: 2, pattern: '^[0-9]{2}$' },
        ],
      },
    ],
  },
  {
    id: 'maryaj_gratis',
    icon: 'redeem',
    titleKey: 'public.rules.games.maryaj_gratis.title',
    summaryKey: 'public.rules.games.maryaj_gratis.summary',
    principleKey: 'public.rules.games.maryaj_gratis.principle',
    betOptions: [
      { id: 'gratis_lot1', labelKey: 'public.rules.bet.maryaj_gratis.gratis_lot1', defaultMultiplier: 50, bonusMultiplier: 600, numberFields: [FIELD_2] },
    ],
  },
  {
    id: 'lotto3',
    icon: 'filter_3',
    titleKey: 'public.rules.games.lotto3.title',
    summaryKey: 'public.rules.games.lotto3.summary',
    principleKey: 'public.rules.games.lotto3.principle',
    betOptions: [
      { id: 'direct', labelKey: 'public.rules.bet.lotto3.direct', defaultMultiplier: 500, numberFields: [FIELD_3] },
      { id: 'combo2', labelKey: 'public.rules.bet.lotto3.combo2', defaultMultiplier: 80,  numberFields: [FIELD_3] },
      { id: 'combo3', labelKey: 'public.rules.bet.lotto3.combo3', defaultMultiplier: 25,  numberFields: [FIELD_3] },
    ],
  },
  {
    id: 'lotto4',
    icon: 'filter_4',
    titleKey: 'public.rules.games.lotto4.title',
    summaryKey: 'public.rules.games.lotto4.summary',
    principleKey: 'public.rules.games.lotto4.principle',
    betOptions: [
      { id: 'direct', labelKey: 'public.rules.bet.lotto4.direct', defaultMultiplier: 3000, numberFields: [FIELD_4] },
      { id: 'combo',  labelKey: 'public.rules.bet.lotto4.combo',  defaultMultiplier: 150,  numberFields: [FIELD_4] },
    ],
  },
  {
    id: 'lotto5',
    icon: 'filter_5',
    titleKey: 'public.rules.games.lotto5.title',
    summaryKey: 'public.rules.games.lotto5.summary',
    principleKey: 'public.rules.games.lotto5.principle',
    betOptions: [
      { id: 'direct', labelKey: 'public.rules.bet.lotto5.direct', defaultMultiplier: 80000, numberFields: [FIELD_5] },
      { id: 'combo',  labelKey: 'public.rules.bet.lotto5.combo',  defaultMultiplier: 500,   numberFields: [FIELD_5] },
    ],
  },
];

export const PUBLIC_TCHALA_ENTRIES: readonly PublicTchalaEntry[] = [
  { id: 'water',  icon: 'water_drop',            termKey: 'public.rules.tchala.entries.water.term',  descriptionKey: 'public.rules.tchala.entries.water.description',  numbers: ['45', '01'], keywords: ['eau', 'water', 'dlo'] },
  { id: 'dog',    icon: 'pets',                  termKey: 'public.rules.tchala.entries.dog.term',    descriptionKey: 'public.rules.tchala.entries.dog.description',    numbers: ['12', '98'], keywords: ['chien', 'dog', 'chen'] },
  { id: 'money',  icon: 'account_balance_wallet', termKey: 'public.rules.tchala.entries.money.term',  descriptionKey: 'public.rules.tchala.entries.money.description',  numbers: ['33', '67'], keywords: ['argent', 'money', 'lajan'] },
  { id: 'travel', icon: 'airplane_ticket',        termKey: 'public.rules.tchala.entries.travel.term', descriptionKey: 'public.rules.tchala.entries.travel.description', numbers: ['04', '82'], keywords: ['voyage', 'travel', 'vwayaj'] },
];
