import { Component, Input, inject } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { MetalRate, MetalType } from '../../models/metal-rate.model';
import { RateService } from '../../services/rate.service';

@Component({
  selector: 'app-rate-card',
  standalone: true,
  imports: [CommonModule, DecimalPipe],
  template: `
    <div class="rate-card" [class.gold-card]="metalType === 'GOLD'" [class.silver-card]="metalType === 'SILVER'">

      <!-- Card header -->
      <div class="card-header">
        <div class="metal-info">
          <span class="metal-icon">{{ metalType === 'GOLD' ? '🥇' : '🥈' }}</span>
          <div>
            <div class="metal-name">{{ metalType === 'GOLD' ? 'Gold' : 'Silver' }}</div>
            <div class="metal-sub">{{ metalType === 'GOLD' ? 'XAU · 24K' : 'XAG · 999' }}</div>
          </div>
        </div>
        <div class="trend-badge" [class.up]="trend === 'up'" [class.down]="trend === 'down'" [class.neutral]="trend === 'neutral'">
          {{ trend === 'up' ? '▲' : trend === 'down' ? '▼' : '—' }}
          {{ rate?.changePercent != null ? (rate!.changePercent | number:'1.2-2') + '%' : '0.00%' }}
        </div>
      </div>

      <!-- Main price -->
      @if (loading) {
        <div class="skeleton-price"></div>
        <div class="skeleton-sub"></div>
      } @else if (rate) {
        <div class="main-price">
          <span class="currency">₹</span>
          <span class="price-value">{{ rate.pricePerGramInr | number:'1.2-2' }}</span>
          <span class="per-unit">/gram</span>
        </div>
        <div class="secondary-price">
          {{ svc.formatInr(rate.pricePer10GramInr) }} per 10g
        </div>
      } @else {
        <div class="no-data">Fetching live rates...</div>
      }

      <!-- Change row -->
      @if (rate && !loading) {
        <div class="change-row">
          <span class="change-amount" [class.positive]="(rate.changeInr ?? 0) > 0" [class.negative]="(rate.changeInr ?? 0) < 0">
            {{ (rate.changeInr ?? 0) > 0 ? '+' : '' }}{{ svc.formatInr(rate.changeInr) }} today
          </span>
        </div>

        <!-- Divider -->
        <div class="divider"></div>

        <!-- Day range -->
        <div class="day-range">
          <div class="range-item">
            <span class="range-label">Day Low</span>
            <span class="range-value">{{ svc.formatInr(rate.dayLowInr ?? rate.pricePerGramInr) }}</span>
          </div>
          <div class="range-bar-wrapper">
            <div class="range-bar">
              <div class="range-fill" [style.width]="rangePosition + '%'"></div>
            </div>
          </div>
          <div class="range-item right">
            <span class="range-label">Day High</span>
            <span class="range-value">{{ svc.formatInr(rate.dayHighInr ?? rate.pricePerGramInr) }}</span>
          </div>
        </div>

        <!-- Date -->
        <div class="rate-date">As of {{ svc.formatDate(rate.rateDate) }}</div>
      }
    </div>
  `,
  styles: [`
    .rate-card {
      border-radius: 20px; padding: 1.5rem;
      border: 1px solid transparent;
      box-shadow: 0 4px 20px rgba(0,0,0,.06);
      transition: transform .2s, box-shadow .2s;
      position: relative; overflow: hidden;
    }
    .rate-card:hover { transform: translateY(-2px); box-shadow: 0 8px 30px rgba(0,0,0,.1); }

    .gold-card {
      background: linear-gradient(135deg, #fffbf0 0%, #fff8e1 100%);
      border-color: #fde68a;
    }
    .gold-card::before {
      content: ''; position: absolute; top: -30px; right: -30px;
      width: 120px; height: 120px; border-radius: 50%;
      background: rgba(245,158,11,0.08);
    }
    .silver-card {
      background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
      border-color: #cbd5e1;
    }
    .silver-card::before {
      content: ''; position: absolute; top: -30px; right: -30px;
      width: 120px; height: 120px; border-radius: 50%;
      background: rgba(100,116,139,0.08);
    }

    .card-header {
      display: flex; justify-content: space-between; align-items: flex-start;
      margin-bottom: 1.25rem;
    }
    .metal-info { display: flex; align-items: center; gap: .75rem; }
    .metal-icon { font-size: 2rem; }
    .metal-name { font-size: 1rem; font-weight: 700; color: #111827; }
    .metal-sub  { font-size: .7rem; color: #9ca3af; margin-top: .1rem; }

    .trend-badge {
      padding: .3rem .75rem; border-radius: 20px;
      font-size: .8rem; font-weight: 600;
    }
    .trend-badge.up { background: #dcfce7; color: #16a34a; }
    .trend-badge.down { background: #fee2e2; color: #dc2626; }
    .trend-badge.neutral { background: #f3f4f6; color: #6b7280; }

    .main-price {
      display: flex; align-items: baseline; gap: .25rem;
      margin-bottom: .35rem;
    }
    .currency { font-size: 1.4rem; font-weight: 600; color: #374151; }
    .price-value { font-size: 2.6rem; font-weight: 800; color: #111827; line-height: 1; }
    .per-unit { font-size: .85rem; color: #9ca3af; margin-left: .2rem; }

    .secondary-price { font-size: .875rem; color: #6b7280; margin-bottom: .75rem; }

    .change-row { margin-bottom: .75rem; }
    .change-amount { font-size: .85rem; font-weight: 600; }
    .change-amount.positive { color: #16a34a; }
    .change-amount.negative { color: #dc2626; }

    .divider { height: 1px; background: rgba(0,0,0,.06); margin: .75rem 0; }

    .day-range {
      display: grid; grid-template-columns: 1fr auto 1fr;
      gap: .5rem; align-items: center; margin-bottom: .75rem;
    }
    .range-item { }
    .range-item.right { text-align: right; }
    .range-label { font-size: .7rem; color: #9ca3af; display: block; }
    .range-value { font-size: .8rem; font-weight: 600; color: #374151; }

    .range-bar-wrapper { padding: 0 .25rem; }
    .range-bar {
      height: 4px; background: #e5e7eb; border-radius: 2px; overflow: hidden;
    }
    .range-fill {
      height: 100%; border-radius: 2px; transition: width .5s ease;
    }
    .gold-card .range-fill { background: #f59e0b; }
    .silver-card .range-fill { background: #64748b; }

    .rate-date { font-size: .7rem; color: #9ca3af; text-align: right; }
    .no-data { font-size: .875rem; color: #9ca3af; padding: 1rem 0; }

    /* Skeletons */
    .skeleton-price {
      height: 3rem; background: linear-gradient(90deg, #f3f4f6 25%, #e5e7eb 50%, #f3f4f6 75%);
      background-size: 200% 100%; animation: shimmer 1.5s infinite;
      border-radius: 8px; margin-bottom: .5rem;
    }
    .skeleton-sub {
      height: 1rem; width: 60%; background: linear-gradient(90deg, #f3f4f6 25%, #e5e7eb 50%, #f3f4f6 75%);
      background-size: 200% 100%; animation: shimmer 1.5s infinite;
      border-radius: 4px;
    }
    @keyframes shimmer { to { background-position: -200% 0; } }
  `]
})
export class RateCardComponent {
  @Input() rate:      MetalRate | null = null;
  @Input() metalType: MetalType        = 'GOLD';
  @Input() trend:     'up' | 'down' | 'neutral' = 'neutral';
  @Input() loading:   boolean          = false;

  svc = inject(RateService);

  get rangePosition(): number {
    if (!this.rate?.dayLowInr || !this.rate?.dayHighInr) return 50;
    const range = this.rate.dayHighInr - this.rate.dayLowInr;
    if (range === 0) return 50;
    return Math.min(100, Math.max(0, ((this.rate.pricePerGramInr - this.rate.dayLowInr) / range) * 100));
  }
}
