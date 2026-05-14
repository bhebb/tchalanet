export interface NextDrawInfo {
  date: string;
  jackpot?: number;
  isSpecial?: boolean;
}

export interface LotteryLite {
  id: string;
  name: string;
  icon?: string;
  color?: string;
}

export interface DrawResult {
  id: string;
  date: string;
  lotteryId: string;
  lotteryName: string;
  drawTime: string; // ISO
  numbers: number[];
  bonus?: number[];
}

export interface NextDrawInfo {
  date: string;
  jackpot?: number;
  isSpecial?: boolean;
  lotteryId: string;
  lotteryName: string;
  nextDrawTime: string; // ISO future
}
