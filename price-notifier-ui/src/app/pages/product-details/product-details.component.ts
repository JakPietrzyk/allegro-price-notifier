import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartOptions } from 'chart.js';
import { ProductService } from '../../services/product/product.service';
import {PRODUCT_DETAILS_CONSTANTS} from '../../core/constants/product-details.constants';
import {ProductDetails} from '../../models/product-details.model';
import {PriceHistory} from '../../models/price-history.model';

@Component({
  selector: 'app-product-details',
  standalone: true,
  imports: [CommonModule, RouterLink, BaseChartDirective],
  providers: [DatePipe],
  templateUrl: './product-details.component.html',
  styleUrl: './product-details.component.scss'
})
export class ProductDetailsComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly productService = inject(ProductService);
  private readonly datePipe = inject(DatePipe);

  protected readonly texts = PRODUCT_DETAILS_CONSTANTS;

  product = signal<ProductDetails | null>(null);
  loading = signal<boolean>(true);

  lowestPrice = computed(() => {
    const p = this.product();
    if (!p || !p.priceHistory?.length) {
      return p?.currentPrice ?? 0;
    }
    return Math.min(...p.priceHistory.map(h => h.price));
  });

  public lineChartData: ChartConfiguration<'line'>['data'] = {
    labels: [],
    datasets: [{
      data: [],
      label: this.texts.UI.CHART_LABEL,
      fill: true,
      tension: 0.4,
      borderColor: '#3f51b5',
      backgroundColor: 'rgba(63, 81, 181, 0.2)'
    }]
  };

  public lineChartOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: true },
      tooltip: { mode: 'index', intersect: false }
    },
    scales: {
      y: { beginAtZero: false }
    }
  };

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadProduct(id);
    }
  }

  private loadProduct(id: string) {
    this.loading.set(true);

    this.productService.getById(id).subscribe({
      next: (data: ProductDetails) => {
        this.product.set(data);
        this.updateChart(data.priceHistory);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      }
    });
  }

  private updateChart(history: PriceHistory[]) {
    const sorted = [...history].sort((a, b) =>
      new Date(a.checkedAt).getTime() - new Date(b.checkedAt).getTime()
    );

    const labels = sorted.map(h =>
      this.datePipe.transform(h.checkedAt, 'dd.MM HH:mm') || ''
    );
    const prices = sorted.map(h => h.price);

    this.lineChartData = {
      ...this.lineChartData,
      labels,
      datasets: [{
        ...this.lineChartData.datasets[0],
        data: prices
      }]
    };
  }
}
