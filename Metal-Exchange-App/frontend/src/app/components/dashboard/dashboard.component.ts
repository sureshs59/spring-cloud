import {
  Component, OnInit, OnDestroy, inject, signal,
  computed, effect, ElementRef, ViewChild
} from '@angular/core';
import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Chart, registerables } from 'chart.js';
import { RateService } from '../../services/rate.service';
import { MetalRate, TimeRange } from '../../models/metal-rate.model';
import { RateCardComponent } from '../rate-card/rate-card.component';
import { HeaderComponent } from '../header/header.component';
import { toSignal } from '@angular/core/rxjs-interop';
import { switchMap } from 'rxjs';

Chart.register(...registerables);

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, RateCardComponent, HeaderComponent, DecimalPipe, DatePipe],
  template: `
    <app-header />

    <main class="dashboard">

      <!-- Live status bar -->
      <div class="status-bar">
        <div class="status-left">
          <span class="live-dot" [class.pulse]="!rateService.isLoading()"></span>
          <span class="status-text">Live rates · Updated {{ lastUpdated() }}</span>
        </div>
        <div class="status-right">
          <button class="refresh-btn" (click)="refreshRates()" [disabled]="refreshing()">
            <span class="refresh-icon" [class.spin]="refreshing()">↻</span>
            {{ refreshing() ? 'Refreshing...' : 'Refresh' }}
          </button>
        </div>
      </div>

      @if (rateService.lastError()) {
        <div class="error-banner">
          ⚠ {{ rateService.lastError() }}
        </div>
      }

      <!-- Hero rate cards -->
      <section class="rate-cards">
        <app-rate-card
          [rate]="rateService.goldRate()"
          [trend]="rateService.goldTrend()"
          metalType="GOLD"
          [loading]="rateService.isLoading()"
        />
        <app-rate-card
          [rate]="rateService.silverRate()"
          [trend]="rateService.silverTrend()"
          metalType="SILVER"
          [loading]="rateService.isLoading()"
        />

        <!-- Today's summary card -->
        <div class="summary-card">
          <div class="summary-header">Market Summary</div>
          <div class="summary-row">
            <span class="summary-label">Gold/Silver Ratio</span>
            <span class="summary-value">{{ goldSilverRatio() | number:'1.1-1' }}x</span>
          </div>
          <div class="summary-row">
            <span class="summary-label">Gold 10g</span>
            <span class="summary-value">{{ rateService.formatInr(rateService.goldRate()?.pricePer10GramInr) }}</span>
          </div>
          <div class="summary-row">
            <span class="summary-label">Silver 10g</span>
            <span class="summary-value">{{ rateService.formatInr(rateService.silverRate()?.pricePer10GramInr) }}</span>
          </div>
          <div class="summary-row">
            <span class="summary-label">Data Source</span>
            <span class="summary-value source">{{ rateService.goldRate()?.source ?? 'metalpriceapi.com' }}</span>
          </div>
        </div>
      </section>

      <!-- Chart section -->
      <section class="chart-section">
        <div class="chart-header">
          <div class="chart-title">
            <h2>Price History</h2>
            <span class="chart-subtitle">INR per gram · {{ selectedRange() }}</span>
          </div>
          <div class="range-selector">
            @for (range of timeRanges; track range) {
              <button
                class="range-btn"
                [class.active]="selectedRange() === range"
                (click)="setRange(range)"
              >{{ range }}</button>
            }
          </div>
        </div>

        <div class="chart-wrapper">
          @if (chartLoading()) {
            <div class="chart-loading">
              <div class="loading-spinner"></div>
              <span>Loading chart data...</span>
            </div>
          }
          <canvas #chartCanvas id="rateChart"></canvas>
        </div>
      </section>

      <!-- Stats row -->
      <section class="stats-row">
        <div class="stat-card gold-stat">
          <div class="stat-label">Gold {{ selectedRange() }} High</div>
          <div class="stat-value">{{ rateService.formatInr(goldHigh()) }}</div>
        </div>
        <div class="stat-card gold-stat">
          <div class="stat-label">Gold {{ selectedRange() }} Low</div>
          <div class="stat-value">{{ rateService.formatInr(goldLow()) }}</div>
        </div>
        <div class="stat-card silver-stat">
          <div class="stat-label">Silver {{ selectedRange() }} High</div>
          <div class="stat-value">{{ rateService.formatInr(silverHigh()) }}</div>
        </div>
        <div class="stat-card silver-stat">
          <div class="stat-label">Silver {{ selectedRange() }} Low</div>
          <div class="stat-value">{{ rateService.formatInr(silverLow()) }}</div>
        </div>
      </section>

      <!-- Recent history table -->
      <section class="history-section">
        <h2>Recent Rates</h2>
        <div class="table-wrapper">
          <table class="rate-table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Gold /gram</th>
                <th>Gold 10g</th>
                <th>Gold Change</th>
                <th>Silver /gram</th>
                <th>Silver 10g</th>
                <th>Silver Change</th>
              </tr>
            </thead>
            <tbody>
              @for (row of tableRows(); track row.date) {
                <tr>
                  <td class="date-cell">{{ row.date }}</td>
                  <td class="price-cell">{{ rateService.formatInr(row.goldGram) }}</td>
                  <td class="price-cell">{{ rateService.formatInr(row.gold10g) }}</td>
                  <td class="change-cell" [class.positive]="row.goldChange > 0" [class.negative]="row.goldChange < 0">
                    {{ row.goldChange > 0 ? '+' : '' }}{{ row.goldChangePct | number:'1.2-2' }}%
                  </td>
                  <td class="price-cell">{{ rateService.formatInr(row.silverGram) }}</td>
                  <td class="price-cell">{{ rateService.formatInr(row.silver10g) }}</td>
                  <td class="change-cell" [class.positive]="row.silverChange > 0" [class.negative]="row.silverChange < 0">
                    {{ row.silverChange > 0 ? '+' : '' }}{{ row.silverChangePct | number:'1.2-2' }}%
                  </td>
                </tr>
              } @empty {
                <tr><td colspan="7" class="empty-row">No data available. Click Refresh to fetch rates.</td></tr>
              }
            </tbody>
          </table>
        </div>
      </section>

    </main>
  `,
  styles: [`
    .dashboard {
      max-width: 1280px;
      margin: 0 auto;
      padding: 0 1.5rem 3rem;
    }

    /* Status bar */
    .status-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: .6rem 0;
      border-bottom: 1px solid #e5e7eb;
      margin-bottom: 1.5rem;
    }
    .status-left { display: flex; align-items: center; gap: .5rem; }
    .live-dot {
      width: 8px; height: 8px; border-radius: 50%;
      background: #22c55e;
    }
    .live-dot.pulse { animation: pulse 2s infinite; }
    @keyframes pulse {
      0%, 100% { opacity: 1; }
      50% { opacity: 0.3; }
    }
    .status-text { font-size: .8rem; color: #6b7280; }
    .refresh-btn {
      display: flex; align-items: center; gap: .4rem;
      padding: .4rem .9rem; border-radius: 8px;
      border: 1px solid #d1d5db; background: white;
      font-size: .8rem; cursor: pointer; color: #374151;
      transition: all .15s;
    }
    .refresh-btn:hover:not(:disabled) { background: #f9fafb; border-color: #9ca3af; }
    .refresh-btn:disabled { opacity: .5; cursor: not-allowed; }
    .refresh-icon { font-size: 1rem; display: inline-block; }
    .refresh-icon.spin { animation: spin .8s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }

    /* Error banner */
    .error-banner {
      background: #fef3c7; border: 1px solid #f59e0b;
      border-radius: 8px; padding: .75rem 1rem;
      font-size: .875rem; color: #92400e; margin-bottom: 1rem;
    }

    /* Rate cards */
    .rate-cards {
      display: grid;
      grid-template-columns: 1fr 1fr 1fr;
      gap: 1.25rem;
      margin-bottom: 2rem;
    }
    @media (max-width: 900px) {
      .rate-cards { grid-template-columns: 1fr; }
    }

    /* Summary card */
    .summary-card {
      background: white;
      border-radius: 16px;
      padding: 1.5rem;
      border: 1px solid #e5e7eb;
      box-shadow: 0 1px 3px rgba(0,0,0,.06);
    }
    .summary-header {
      font-size: .75rem; font-weight: 600;
      text-transform: uppercase; letter-spacing: .05em;
      color: #9ca3af; margin-bottom: 1rem;
    }
    .summary-row {
      display: flex; justify-content: space-between; align-items: center;
      padding: .5rem 0; border-bottom: 1px solid #f3f4f6;
    }
    .summary-row:last-child { border-bottom: none; }
    .summary-label { font-size: .8rem; color: #6b7280; }
    .summary-value { font-size: .9rem; font-weight: 600; color: #111827; }
    .summary-value.source { font-size: .75rem; color: #9ca3af; font-weight: 400; }

    /* Chart */
    .chart-section {
      background: white; border-radius: 16px;
      padding: 1.5rem; border: 1px solid #e5e7eb;
      box-shadow: 0 1px 3px rgba(0,0,0,.06);
      margin-bottom: 1.5rem;
    }
    .chart-header {
      display: flex; justify-content: space-between; align-items: center;
      margin-bottom: 1.25rem;
    }
    .chart-title h2 { font-size: 1.1rem; font-weight: 700; color: #111827; margin: 0 0 .2rem; }
    .chart-subtitle { font-size: .75rem; color: #9ca3af; }
    .range-selector { display: flex; gap: .4rem; }
    .range-btn {
      padding: .35rem .8rem; border-radius: 8px; font-size: .8rem;
      border: 1px solid #e5e7eb; background: white; cursor: pointer;
      color: #6b7280; font-weight: 500; transition: all .15s;
    }
    .range-btn.active { background: #1a1a2e; color: white; border-color: #1a1a2e; }
    .chart-wrapper {
      position: relative; height: 320px;
    }
    .chart-loading {
      position: absolute; inset: 0; display: flex;
      flex-direction: column; align-items: center; justify-content: center;
      gap: 1rem; color: #9ca3af; font-size: .875rem; z-index: 1;
    }
    .loading-spinner {
      width: 28px; height: 28px; border-radius: 50%;
      border: 3px solid #e5e7eb; border-top-color: #f59e0b;
      animation: spin .8s linear infinite;
    }
    canvas { width: 100% !important; }

    /* Stats row */
    .stats-row {
      display: grid; grid-template-columns: repeat(4, 1fr);
      gap: 1rem; margin-bottom: 1.5rem;
    }
    @media (max-width: 768px) { .stats-row { grid-template-columns: 1fr 1fr; } }
    .stat-card {
      border-radius: 12px; padding: 1.25rem;
      border-left: 4px solid transparent;
    }
    .gold-stat { background: #fffbeb; border-color: #f59e0b; }
    .silver-stat { background: #f8fafc; border-color: #64748b; }
    .stat-label { font-size: .75rem; color: #9ca3af; margin-bottom: .4rem; }
    .stat-value { font-size: 1rem; font-weight: 700; color: #111827; }

    /* History table */
    .history-section {
      background: white; border-radius: 16px;
      padding: 1.5rem; border: 1px solid #e5e7eb;
      box-shadow: 0 1px 3px rgba(0,0,0,.06);
    }
    .history-section h2 {
      font-size: 1.1rem; font-weight: 700; color: #111827;
      margin: 0 0 1.25rem;
    }
    .table-wrapper { overflow-x: auto; }
    .rate-table {
      width: 100%; border-collapse: collapse; font-size: .875rem;
    }
    .rate-table th {
      text-align: left; padding: .75rem 1rem;
      background: #f9fafb; color: #6b7280; font-weight: 600;
      font-size: .75rem; text-transform: uppercase; letter-spacing: .04em;
      border-bottom: 1px solid #e5e7eb;
    }
    .rate-table td {
      padding: .75rem 1rem; border-bottom: 1px solid #f3f4f6;
      color: #374151;
    }
    .rate-table tr:hover td { background: #fafafa; }
    .date-cell { color: #6b7280; font-size: .8rem; }
    .price-cell { font-weight: 600; color: #111827; }
    .change-cell { font-weight: 600; }
    .change-cell.positive { color: #16a34a; }
    .change-cell.negative { color: #dc2626; }
    .empty-row { text-align: center; color: #9ca3af; padding: 2rem !important; }
  `]
})
export class DashboardComponent implements OnInit, OnDestroy {

  @ViewChild('chartCanvas') chartCanvas!: ElementRef<HTMLCanvasElement>;

  rateService = inject(RateService);

  timeRanges: TimeRange[] = ['7D', '30D', '90D', '1Y'];
  selectedRange = this.rateService.selectedTimeRange;
  chartLoading  = signal(true);
  refreshing    = signal(false);
  private isBrowser = typeof window !== 'undefined';

  private chart: Chart | null = null;
  private historyData: MetalRate[] = [];

  // ── Computed stats ──────────────────────────────────────────────
  lastUpdated = computed(() => {
    const ts = this.rateService.todayRates()?.timestamp;
    return ts ? new Date(ts).toLocaleTimeString('en-IN') : 'Never';
  });

  goldSilverRatio = computed(() => {
    const g = this.rateService.goldRate()?.pricePerGramInr;
    const s = this.rateService.silverRate()?.pricePerGramInr;
    return g && s && s > 0 ? g / s : 0;
  });

  goldHigh   = computed<number | null>(() => {
    const vals = this.historyData.filter(r => r.metal === 'GOLD').map(r => r.pricePerGramInr);
    return vals.length ? Math.max(...vals) : null;
  });
  goldLow    = computed<number | null>(() => {
    const vals = this.historyData.filter(r => r.metal === 'GOLD').map(r => r.pricePerGramInr);
    return vals.length ? Math.min(...vals) : null;
  });
  silverHigh = computed<number | null>(() => {
    const vals = this.historyData.filter(r => r.metal === 'SILVER').map(r => r.pricePerGramInr);
    return vals.length ? Math.max(...vals) : null;
  });
  silverLow  = computed<number | null>(() => {
    const vals = this.historyData.filter(r => r.metal === 'SILVER').map(r => r.pricePerGramInr);
    return vals.length ? Math.min(...vals) : null;
  });

  tableRows = computed(() => {
    const goldMap = new Map(
      this.historyData.filter(r => r.metal === 'GOLD').map(r => [r.rateDate, r])
    );
    const silverMap = new Map(
      this.historyData.filter(r => r.metal === 'SILVER').map(r => [r.rateDate, r])
    );
    const dates = [...new Set(this.historyData.map(r => r.rateDate))].sort().reverse().slice(0, 10);

    return dates.map(date => ({
      date: this.rateService.formatDate(date),
      goldGram: goldMap.get(date)?.pricePerGramInr,
      gold10g: goldMap.get(date)?.pricePer10GramInr,
      goldChange: goldMap.get(date)?.changeInr ?? 0,
      goldChangePct: goldMap.get(date)?.changePercent ?? 0,
      silverGram: silverMap.get(date)?.pricePerGramInr,
      silver10g: silverMap.get(date)?.pricePer10GramInr,
      silverChange: silverMap.get(date)?.changeInr ?? 0,
      silverChangePct: silverMap.get(date)?.changePercent ?? 0,
    }));
  });

  // ── Auto-reload chart when time range changes ───────────────────
  private rangeEffect = effect(
    () => {
      const range = this.selectedRange();
      this.loadChartData(range);
    },
    { allowSignalWrites: true }
  );

  ngOnInit(): void {
    this.loadChartData(this.selectedRange());
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
  }

  setRange(range: TimeRange): void {
    this.rateService.selectedTimeRange.set(range);
  }

  refreshRates(): void {
    this.refreshing.set(true);
    this.rateService.triggerFetch().subscribe({
      next: () => {
        this.loadChartData(this.selectedRange());
        this.refreshing.set(false);
      },
      error: () => this.refreshing.set(false)
    });
  }

  private loadChartData(range: TimeRange): void {
    const days = this.rateService.getDaysForRange(range);
    this.chartLoading.set(true);

    this.rateService.getAllHistory(days).subscribe({
      next: (data) => {
        this.historyData = data;
        this.renderChart(data);
        this.chartLoading.set(false);
      },
      error: () => this.chartLoading.set(false)
    });
  }

  private renderChart(data: MetalRate[]): void {
    if (!this.isBrowser) {
      return;
    }

    const goldData   = data.filter(r => r.metal === 'GOLD').sort((a,b) => a.rateDate.localeCompare(b.rateDate));
    const silverData = data.filter(r => r.metal === 'SILVER').sort((a,b) => a.rateDate.localeCompare(b.rateDate));
    const labels     = goldData.map(r => this.rateService.formatDate(r.rateDate));

    setTimeout(() => {
      if (!this.chartCanvas) return;
      const ctx = this.chartCanvas.nativeElement.getContext('2d');
      if (!ctx) return;

      this.chart?.destroy();
      this.chart = new Chart(ctx, {
        type: 'line',
        data: {
          labels,
          datasets: [
            {
              label: 'Gold (₹/gram)',
              data: goldData.map(r => r.pricePerGramInr),
              borderColor: '#f59e0b',
              backgroundColor: 'rgba(245,158,11,0.08)',
              borderWidth: 2.5,
              pointRadius: goldData.length > 30 ? 0 : 3,
              pointHoverRadius: 5,
              fill: true,
              tension: 0.4,
              yAxisID: 'yGold'
            },
            {
              label: 'Silver (₹/gram)',
              data: silverData.map(r => r.pricePerGramInr),
              borderColor: '#64748b',
              backgroundColor: 'rgba(100,116,139,0.08)',
              borderWidth: 2.5,
              pointRadius: silverData.length > 30 ? 0 : 3,
              pointHoverRadius: 5,
              fill: true,
              tension: 0.4,
              yAxisID: 'ySilver'
            }
          ]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          interaction: { mode: 'index', intersect: false },
          plugins: {
            legend: {
              position: 'top',
              labels: { usePointStyle: true, boxWidth: 8, padding: 20, font: { size: 12 } }
            },
            tooltip: {
              backgroundColor: '#1a1a2e',
              titleColor: '#9ca3af',
              bodyColor: '#f3f4f6',
              padding: 12,
              cornerRadius: 8,
              callbacks: {
                label: (ctx) => {
                  const raw = ctx.parsed.y;
                  const value = typeof raw === 'number' ? raw : Number(raw || 0);
                  return ` ${ctx.dataset.label}: ${new Intl.NumberFormat('en-IN',{style:'currency',currency:'INR',maximumFractionDigits:2}).format(value)}`;
                }
              }
            }
          },
          scales: {
            x: {
              grid: { display: false },
              ticks: { maxTicksLimit: 8, font: { size: 11 }, color: '#9ca3af' }
            },
            yGold: {
              type: 'linear', position: 'left',
              grid: { color: '#f3f4f6' },
              ticks: {
                font: { size: 11 }, color: '#f59e0b',
                callback: (val) => '₹' + Number(val).toLocaleString('en-IN')
              }
            },
            ySilver: {
              type: 'linear', position: 'right',
              grid: { drawOnChartArea: false },
              ticks: {
                font: { size: 11 }, color: '#64748b',
                callback: (val) => '₹' + Number(val).toLocaleString('en-IN')
              }
            }
          }
        }
      });
    }, 50);
  }
}
