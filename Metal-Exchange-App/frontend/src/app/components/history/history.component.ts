import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { RouterModule } from '@angular/router';
import { RateService } from '../../services/rate.service';
import { MetalRate, TimeRange } from '../../models/metal-rate.model';
import { HeaderComponent } from '../header/header.component';

@Component({
  selector: 'app-history',
  standalone: true,
  imports: [CommonModule, RouterModule, HeaderComponent, DecimalPipe],
  template: `
    <app-header />

    <main class="history-page">
      <div class="page-header">
        <div>
          <h1>Rate History</h1>
          <p>Complete daily gold and silver rates with change analysis</p>
        </div>
        <div class="filters">
          <div class="filter-group">
            <label>Time Range</label>
            <div class="range-btns">
              @for (r of ranges; track r) {
                <button [class.active]="selectedRange() === r" (click)="setRange(r)">{{ r }}</button>
              }
            </div>
          </div>
          <div class="filter-group">
            <label>Metal</label>
            <div class="range-btns">
              @for (m of metals; track m) {
                <button [class.active]="selectedMetal() === m" (click)="selectedMetal.set(m)">{{ m }}</button>
              }
            </div>
          </div>
        </div>
      </div>

      @if (loading()) {
        <div class="loading-state">
          <div class="spinner"></div><span>Loading history...</span>
        </div>
      } @else {
        <div class="table-card">
          <table class="history-table">
            <thead>
              <tr>
                <th (click)="sortBy('date')">Date {{ sortIcon('date') }}</th>
                <th (click)="sortBy('gram')">Per Gram ₹ {{ sortIcon('gram') }}</th>
                <th (click)="sortBy('10g')">Per 10g ₹ {{ sortIcon('10g') }}</th>
                <th (click)="sortBy('ounce')">Per Ounce ₹ {{ sortIcon('ounce') }}</th>
                <th (click)="sortBy('change')">Change ₹ {{ sortIcon('change') }}</th>
                <th (click)="sortBy('pct')">Change % {{ sortIcon('pct') }}</th>
                <th>Day High</th>
                <th>Day Low</th>
                <th>Source</th>
              </tr>
            </thead>
            <tbody>
              @for (rate of sortedRates(); track rate.id) {
                <tr [class.gold-row]="rate.metal === 'GOLD'" [class.silver-row]="rate.metal === 'SILVER'">
                  <td class="date-col">
                    <span class="metal-dot" [class.gold-dot]="rate.metal === 'GOLD'"></span>
                    {{ svc.formatDate(rate.rateDate) }}
                  </td>
                  <td class="price-col">{{ rate.pricePerGramInr | number:'1.2-2' }}</td>
                  <td class="price-col bold">{{ rate.pricePer10GramInr | number:'1.2-2' }}</td>
                  <td class="price-col">{{ rate.pricePerOunceInr | number:'1.0-0' }}</td>
                  <td class="change-col" [class.pos]="(rate.changeInr??0) > 0" [class.neg]="(rate.changeInr??0) < 0">
                    {{ (rate.changeInr??0) > 0 ? '+' : '' }}{{ (rate.changeInr??0) | number:'1.2-2' }}
                  </td>
                  <td class="change-col" [class.pos]="(rate.changePercent??0) > 0" [class.neg]="(rate.changePercent??0) < 0">
                    {{ (rate.changePercent??0) > 0 ? '+' : '' }}{{ (rate.changePercent??0) | number:'1.3-3' }}%
                  </td>
                  <td class="price-col">{{ (rate.dayHighInr ?? rate.pricePerGramInr) | number:'1.2-2' }}</td>
                  <td class="price-col">{{ (rate.dayLowInr ?? rate.pricePerGramInr) | number:'1.2-2' }}</td>
                  <td class="source-col">{{ rate.source }}</td>
                </tr>
              } @empty {
                <tr><td colspan="9" class="empty">No data for selected range and metal.</td></tr>
              }
            </tbody>
          </table>
        </div>

        <div class="summary-footer">
          Showing {{ sortedRates().length }} records ·
          Metal: {{ selectedMetal() }} · Range: {{ selectedRange() }}
        </div>
      }
    </main>
  `,
  styles: [`
    .history-page { max-width:1280px; margin:0 auto; padding:0 1.5rem 3rem; }
    .page-header {
      display:flex; justify-content:space-between; align-items:flex-start;
      margin-bottom:1.5rem; flex-wrap:wrap; gap:1rem;
    }
    h1 { font-size:1.5rem; font-weight:700; color:#111827; margin:0 0 .25rem; }
    p  { font-size:.875rem; color:#6b7280; margin:0; }
    .filters { display:flex; gap:1rem; flex-wrap:wrap; }
    .filter-group label { font-size:.7rem; font-weight:600; color:#9ca3af; text-transform:uppercase; display:block; margin-bottom:.4rem; }
    .range-btns { display:flex; gap:.35rem; }
    .range-btns button {
      padding:.3rem .75rem; border-radius:8px; font-size:.8rem;
      border:1px solid #e5e7eb; background:white; cursor:pointer; color:#6b7280;
    }
    .range-btns button.active { background:#1a1a2e; color:white; border-color:#1a1a2e; }
    .loading-state { display:flex; gap:1rem; align-items:center; padding:3rem; color:#9ca3af; }
    .spinner { width:24px;height:24px;border-radius:50%;border:3px solid #e5e7eb;border-top-color:#f59e0b;animation:spin .8s linear infinite; }
    @keyframes spin { to{transform:rotate(360deg)} }
    .table-card { background:white;border-radius:16px;border:1px solid #e5e7eb;overflow:hidden;box-shadow:0 1px 3px rgba(0,0,0,.06); }
    .history-table { width:100%;border-collapse:collapse;font-size:.8rem; }
    .history-table th {
      text-align:left;padding:.75rem 1rem;
      background:#f9fafb;color:#6b7280;font-weight:600;
      font-size:.7rem;text-transform:uppercase;letter-spacing:.04em;
      border-bottom:1px solid #e5e7eb;cursor:pointer;user-select:none;
    }
    .history-table th:hover { background:#f3f4f6; }
    .history-table td { padding:.65rem 1rem;border-bottom:1px solid #f3f4f6; }
    .history-table tr:hover td { background:#fafafa; }
    .gold-row td { background:rgba(245,158,11,.02); }
    .silver-row td { background:rgba(100,116,139,.02); }
    .date-col { display:flex;align-items:center;gap:.5rem;color:#6b7280;white-space:nowrap; }
    .metal-dot { width:8px;height:8px;border-radius:50%;background:#64748b;flex-shrink:0; }
    .gold-dot { background:#f59e0b; }
    .price-col { font-weight:500;color:#111827; }
    .price-col.bold { font-weight:700; }
    .change-col { font-weight:600; }
    .change-col.pos { color:#16a34a; }
    .change-col.neg { color:#dc2626; }
    .source-col { font-size:.7rem;color:#9ca3af; }
    .empty { text-align:center;color:#9ca3af;padding:2.5rem!important; }
    .summary-footer { text-align:right;font-size:.75rem;color:#9ca3af;margin-top:.75rem; }
  `]
})
export class HistoryComponent implements OnInit {

  svc = inject(RateService);

  ranges: TimeRange[] = ['7D', '30D', '90D', '1Y'];
  metals = ['ALL', 'GOLD', 'SILVER'];

  selectedRange = signal<TimeRange>('30D');
  selectedMetal = signal<string>('ALL');
  loading       = signal(true);
  sortField     = signal<string>('date');
  sortAsc       = signal(false);

  private allData = signal<MetalRate[]>([]);

  filteredRates = computed(() =>
    this.selectedMetal() === 'ALL'
      ? this.allData()
      : this.allData().filter(r => r.metal === this.selectedMetal())
  );

  sortedRates = computed(() => {
    const data = [...this.filteredRates()];
    const field = this.sortField();
    const asc   = this.sortAsc();
    return data.sort((a, b) => {
      let va: any, vb: any;
      switch(field) {
        case 'gram':   va = a.pricePerGramInr;   vb = b.pricePerGramInr;   break;
        case '10g':    va = a.pricePer10GramInr;  vb = b.pricePer10GramInr; break;
        case 'ounce':  va = a.pricePerOunceInr;   vb = b.pricePerOunceInr;  break;
        case 'change': va = a.changeInr??0;        vb = b.changeInr??0;      break;
        case 'pct':    va = a.changePercent??0;    vb = b.changePercent??0;  break;
        default:       va = a.rateDate;            vb = b.rateDate;
      }
      const cmp = va < vb ? -1 : va > vb ? 1 : 0;
      return asc ? cmp : -cmp;
    });
  });

  ngOnInit(): void { this.loadData(); }

  setRange(r: TimeRange): void {
    this.selectedRange.set(r);
    this.loadData();
  }

  sortBy(field: string): void {
    if (this.sortField() === field) this.sortAsc.update(v => !v);
    else { this.sortField.set(field); this.sortAsc.set(false); }
  }

  sortIcon(field: string): string {
    if (this.sortField() !== field) return '↕';
    return this.sortAsc() ? '↑' : '↓';
  }

  private loadData(): void {
    this.loading.set(true);
    const days = this.svc.getDaysForRange(this.selectedRange());
    this.svc.getAllHistory(days).subscribe({
      next: d => { this.allData.set(d); this.loading.set(false); },
      error: ()=> this.loading.set(false)
    });
  }
}
