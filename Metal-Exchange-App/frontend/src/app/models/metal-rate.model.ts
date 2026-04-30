// models/metal-rate.model.ts

export type MetalType = 'GOLD' | 'SILVER';

export interface MetalRate {
  id: number;
  rateDate: string;            // ISO date "2025-04-30"
  metal: MetalType;
  pricePerGramInr: number;
  pricePer10GramInr: number;
  pricePerOunceInr: number;
  pricePerOunceUsd: number;
  usdToInrRate: number;
  changeInr: number;
  changePercent: number;
  dayHighInr: number;
  dayLowInr: number;
  openPriceInr: number;
  fetchedAt: string;
  source: string;
}

export interface TodayRates {
  gold: MetalRate | null;
  silver: MetalRate | null;
  timestamp: number;
}

export interface ChartDataPoint {
  date: string;
  goldPrice: number;
  silverPrice: number;
}

export type TimeRange = '7D' | '30D' | '90D' | '1Y';
